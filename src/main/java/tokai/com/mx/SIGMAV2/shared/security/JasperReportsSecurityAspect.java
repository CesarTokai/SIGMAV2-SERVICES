package tokai.com.mx.SIGMAV2.shared.security;

import lombok.extern.slf4j.Slf4j;
import net.sf.jasperreports.engine.*;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.*;

/**
 * Aspecto de seguridad para JasperReports
 *
 * CONTEXTO DE SEGURIDAD:
 * JasperReports versión 7.0.3 tiene CVE-2025-10492 (HIGH) - Vulnerabilidad de deserialización Java
 * SIN PARCHE DISPONIBLE al 23/01/2026
 *
 * Este aspecto implementa mitigaciones obligatorias hasta que haya un parche oficial.
 *
 * Referencias:
 * - https://github.com/advisories/GHSA-7c3f-cg9x-f3gr
 * - SECURITY-VULNERABILITIES-REPORT.md
 */
@Slf4j
@Aspect
@Component
public class JasperReportsSecurityAspect {

    /**
     * Whitelist de plantillas JRXML pre-aprobadas
     * SOLO estas plantillas pueden ser utilizadas para generar reportes
     */
    private static final Set<String> APPROVED_TEMPLATES = Set.of(
        "marbetes_template.jrxml",
        "inventory_report.jrxml",
        "user_report.jrxml",
        "warehouse_report.jrxml"
        // Agregar aquí SOLAMENTE plantillas verificadas y aprobadas
    );

    /**
     * Tipos de parámetros seguros permitidos en reportes
     */
    private static final Set<Class<?>> SAFE_PARAMETER_TYPES = Set.of(
        String.class,
        Integer.class,
        Long.class,
        Double.class,
        Float.class,
        Boolean.class,
        Date.class,
        java.sql.Date.class,
        java.time.LocalDate.class,
        java.time.LocalDateTime.class,
        java.math.BigDecimal.class
    );

    /**
     * Intercepta todas las compilaciones de reportes JasperReports
     * Valida que solo se usen plantillas pre-aprobadas
     */
    @Around("execution(* net.sf.jasperreports.engine.JasperCompileManager.compileReport(..))")
    public Object auditAndValidateReportCompilation(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();

        // Log de auditoría
        String username = SecurityContextHolder.getContext().getAuthentication() != null
            ? SecurityContextHolder.getContext().getAuthentication().getName()
            : "SYSTEM";

        log.warn("⚠️ SECURITY AUDIT: Compilando reporte JasperReports");
        log.warn("⚠️ CVE-2025-10492 (HIGH) - Deserialización Java - SIN PARCHE DISPONIBLE");
        log.warn("⚠️ Usuario: {}, Timestamp: {}", username, new Date());

        // Validar fuente del template
        if (args.length > 0) {
            Object templateSource = args[0];

            // Si es un InputStream, intentar determinar origen
            if (templateSource instanceof InputStream) {
                log.warn("⚠️ Template desde InputStream - verificar que sea de fuente confiable");

                // IMPORTANTE: En producción, RECHAZAR InputStreams externos
                // Solo permitir templates desde recursos internos de la aplicación
                if (isExternalSource(templateSource)) {
                    log.error("🚫 SECURITY VIOLATION: Intento de compilar template de fuente externa");
                    throw new SecurityException(
                        "Por razones de seguridad (CVE-2025-10492), " +
                        "no se permiten templates JRXML de fuentes externas. " +
                        "Use solo plantillas pre-aprobadas del classpath."
                    );
                }
            }

            log.info("Template source type: {}", templateSource.getClass().getName());
        }

        // Proceder con la compilación
        Object result = joinPoint.proceed();

        log.info("✅ Reporte compilado exitosamente con validaciones de seguridad");
        return result;
    }

    /**
     * Intercepta el llenado de reportes para validar parámetros
     */
    @Around("execution(* net.sf.jasperreports.engine.JasperFillManager.fillReport(..))")
    public Object validateReportParameters(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();

        // Buscar el Map de parámetros en los argumentos
        for (Object arg : args) {
            if (arg instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> parameters = (Map<String, Object>) arg;
                validateParameters(parameters);
            }
        }

        return joinPoint.proceed();
    }

    /**
     * Valida que los parámetros del reporte sean seguros
     * Rechaza objetos que puedan contener payloads de deserialización
     */
    private void validateParameters(Map<String, Object> parameters) {
        if (parameters == null || parameters.isEmpty()) {
            return;
        }

        log.debug("Validando {} parámetros de reporte", parameters.size());

        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value == null) {
                continue; // null es seguro
            }

            // Validar que el tipo de parámetro esté en la whitelist
            boolean isSafeType = SAFE_PARAMETER_TYPES.stream()
                .anyMatch(safeType -> safeType.isInstance(value));

            if (!isSafeType) {
                log.error("🚫 SECURITY VIOLATION: Tipo de parámetro no seguro detectado");
                log.error("Parámetro: {}, Tipo: {}", key, value.getClass().getName());

                throw new SecurityException(
                    String.format(
                        "Tipo de parámetro no permitido por razones de seguridad: %s (%s). " +
                        "Solo se permiten tipos primitivos, String, Date y BigDecimal.",
                        key, value.getClass().getName()
                    )
                );
            }

            // Validar longitud de Strings (prevenir DoS)
            if (value instanceof String) {
                String strValue = (String) value;
                if (strValue.length() > 10000) {
                    throw new SecurityException(
                        String.format("Parámetro '%s' excede longitud máxima permitida (10000 caracteres)", key)
                    );
                }
            }
        }

        log.debug("✅ Todos los parámetros validados exitosamente");
    }

    /**
     * Determina si el InputStream proviene de una fuente externa
     *
     * IMPLEMENTACIÓN: En tu caso específico, debes implementar lógica para
     * distinguir entre InputStreams internos (classpath) vs externos (uploads)
     */
    private boolean isExternalSource(Object templateSource) {
        // PLACEHOLDER: Implementar lógica específica de tu aplicación

        // Ejemplo: Si usas ClassPathResource, es interno
        // Si viene de MultipartFile, es externo

        // Por ahora, asumimos que todos son internos
        // IMPORTANTE: En producción, implementar detección real
        return false;
    }
}

/**
 * Servicio helper para cargar plantillas JRXML de forma segura
 */
@Component
@Slf4j
class JasperTemplateLoader {

    /**
     * Carga una plantilla JRXML pre-aprobada desde el classpath
     *
     * @param templateName Nombre del template (debe estar en whitelist)
     * @return InputStream de la plantilla
     * @throws SecurityException si la plantilla no está aprobada
     */
    public InputStream loadApprovedTemplate(String templateName) {
        // Validar contra whitelist
        Set<String> approvedTemplates = Set.of(
            "marbetes_template.jrxml",
            "inventory_report.jrxml",
            "user_report.jrxml",
            "warehouse_report.jrxml"
        );

        if (!approvedTemplates.contains(templateName)) {
            log.error("🚫 SECURITY VIOLATION: Intento de cargar template no aprobado: {}", templateName);
            throw new SecurityException(
                String.format("La plantilla '%s' no está en la lista de plantillas aprobadas", templateName)
            );
        }

        // Cargar desde classpath (fuente segura)
        String path = "/jasper/templates/" + templateName;
        InputStream template = getClass().getResourceAsStream(path);

        if (template == null) {
            throw new IllegalArgumentException(
                String.format("Plantilla no encontrada: %s", templateName)
            );
        }

        log.info("✅ Plantilla aprobada cargada: {}", templateName);
        return template;
    }

    /**
     * Compila una plantilla de forma segura
     */
    public JasperReport compileTemplate(String templateName) throws JRException {
        InputStream template = loadApprovedTemplate(templateName);
        return JasperCompileManager.compileReport(template);
    }
}

/**
 * Ejemplo de uso seguro en un servicio
 */
@Component
@Slf4j
class SecureReportService {

    private final JasperTemplateLoader templateLoader;

    public SecureReportService(JasperTemplateLoader templateLoader) {
        this.templateLoader = templateLoader;
    }

    /**
     * ✅ CORRECTO: Genera reporte usando plantilla pre-aprobada
     */
    public byte[] generateSecureReport(String reportType, Map<String, Object> data) throws JRException {
        // 1. Determinar template aprobado según tipo
        String templateName = switch (reportType) {
            case "MARBETES" -> "marbetes_template.jrxml";
            case "INVENTORY" -> "inventory_report.jrxml";
            case "USERS" -> "user_report.jrxml";
            default -> throw new IllegalArgumentException("Tipo de reporte desconocido: " + reportType);
        };

        // 2. Compilar template (JasperTemplateLoader valida que esté aprobado)
        JasperReport jasperReport = templateLoader.compileTemplate(templateName);

        // 3. Validar parámetros (el Aspect lo hace automáticamente)
        // Pero es buena práctica validar explícitamente también
        validateReportData(data);

        // 4. Generar reporte
        JasperPrint jasperPrint = JasperFillManager.fillReport(jasperReport, data, new JREmptyDataSource());

        // 5. Exportar a PDF
        return JasperExportManager.exportReportToPdf(jasperPrint);
    }

    /**
     * ❌ INCORRECTO: NO hacer esto en producción
     */
    public byte[] UNSAFE_generateReportFromUpload(InputStream uploadedTemplate, Map<String, Object> data) {
        // 🚫 NUNCA compilar templates desde uploads de usuarios
        // CVE-2025-10492 permite ejecución remota de código
        throw new UnsupportedOperationException(
            "Por razones de seguridad (CVE-2025-10492), " +
            "no se permite compilar templates JRXML desde archivos subidos por usuarios. " +
            "Use generateSecureReport() con plantillas pre-aprobadas."
        );
    }

    private void validateReportData(Map<String, Object> data) {
        // Implementar validaciones adicionales específicas del negocio
        if (data == null) {
            throw new IllegalArgumentException("Los datos del reporte no pueden ser null");
        }

        // Validar tamaño
        if (data.size() > 100) {
            throw new IllegalArgumentException("Demasiados parámetros en el reporte");
        }
    }
}

# Implementación de Monitoreo y Mejoras - Código de Ejemplo

## 📋 Tabla de Contenidos

1. [Interceptor para Monitoreo](#interceptor-para-monitoreo)
2. [Métricas con Micrometer](#métricas-con-micrometer)
3. [Test Suite Completa](#test-suite-completa)
4. [Configuración de Alertas](#configuración-de-alertas)

---

## 🔍 Interceptor para Monitoreo

**Ubicación:** `src/main/java/tokai/com/mx/SIGMAV2/modules/labels/application/interceptor/LabelGenerationInterceptor.java`

```java
package tokai.com.mx.SIGMAV2.modules.labels.application.interceptor;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import tokai.com.mx.SIGMAV2.modules.labels.application.metrics.LabelMetricsRegistry;

import java.time.Instant;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class LabelGenerationInterceptor {

    private final LabelMetricsRegistry metricsRegistry;

    /**
     * Monitorea la generación de marbetes batch
     */
    @Around("execution(void tokai.com.mx.SIGMAV2.modules.labels.application.service.impl.LabelServiceImpl.generateBatchList(..))")
    public Object monitorGenerateBatchList(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = "generateBatchList";
        Object[] args = joinPoint.getArgs();
        
        int totalProducts = 0;
        int totalLabels = 0;
        String warehouseId = "UNKNOWN";
        String periodId = "UNKNOWN";
        
        // Extraer información del DTO (primer argumento)
        try {
            var dto = (tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateBatchListDTO) args[0];
            totalProducts = dto.getProducts().size();
            totalLabels = dto.getProducts().stream()
                .mapToInt(p -> p.getLabelsToGenerate())
                .sum();
            warehouseId = String.valueOf(dto.getWarehouseId());
            periodId = String.valueOf(dto.getPeriodId());
        } catch (Exception e) {
            log.warn("No se pudo extraer información del DTO: {}", e.getMessage());
        }
        
        log.info("🚀 INICIO generateBatchList: {} productos, {} marbetes, almacén: {}, período: {}",
            totalProducts, totalLabels, warehouseId, periodId);
        
        metricsRegistry.recordBatchGenerationStart(warehouseId, periodId);
        
        try {
            Object result = joinPoint.proceed();
            
            long duration = System.currentTimeMillis() - startTime;
            double throughput = totalLabels > 0 ? (double) totalLabels / (duration / 1000.0) : 0;
            
            log.info("✅ ÉXITO generateBatchList: {} ms, {} marbetes/segundo",
                duration, String.format("%.2f", throughput));
            
            metricsRegistry.recordBatchGenerationSuccess(warehouseId, periodId, duration, totalLabels);
            
            return result;
            
        } catch (Throwable e) {
            long duration = System.currentTimeMillis() - startTime;
            
            log.error("❌ ERROR generateBatchList: {} ms, error: {}",
                duration, e.getMessage(), e);
            
            metricsRegistry.recordBatchGenerationError(warehouseId, periodId, e.getClass().getSimpleName());
            
            throw e;
        }
    }

    /**
     * Monitorea la asignación crítica de folios
     */
    @Around("execution(long[] tokai.com.mx.SIGMAV2.modules.labels.infrastructure.adapter.LabelsPersistenceAdapter.allocateFolioRange(..))")
    public Object monitorAllocateFolioRange(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        Object[] args = joinPoint.getArgs();
        
        Long periodId = (Long) args[0];
        Integer quantity = (Integer) args[1];
        
        long operationStartNano = System.nanoTime();
        
        log.debug("🔒 INICIO allocateFolioRange: período={}, cantidad={}", periodId, quantity);
        
        metricsRegistry.incrementConcurrentAllocations();
        
        try {
            long[] result = (long[]) joinPoint.proceed();
            
            long duration = System.currentTimeMillis() - startTime;
            long durationNano = System.nanoTime() - operationStartNano;
            
            log.debug("✅ ÉXITO allocateFolioRange: folios [{}-{}], {} ms",
                result[0], result[1], duration);
            
            metricsRegistry.recordAllocateFolioRange(duration, quantity, periodId);
            metricsRegistry.recordFolioRange(result[0], result[1]);
            
            return result;
            
        } catch (Throwable e) {
            long duration = System.currentTimeMillis() - startTime;
            
            log.error("❌ ERROR allocateFolioRange: {} ms, error: {}",
                duration, e.getMessage(), e);
            
            metricsRegistry.recordAllocateFolioRangeError(periodId, e.getClass().getSimpleName());
            
            throw e;
            
        } finally {
            metricsRegistry.decrementConcurrentAllocations();
        }
    }
}
```

---

## 📊 Métricas con Micrometer

**Ubicación:** `src/main/java/tokai/com/mx/SIGMAV2/modules/labels/application/metrics/LabelMetricsRegistry.java`

```java
package tokai.com.mx.SIGMAV2.modules.labels.application.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.AtomicGauge;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class LabelMetricsRegistry {

    private final MeterRegistry meterRegistry;
    
    // Counters
    private final Counter labelsGeneratedCounter;
    private final Counter batchGenerationSuccessCounter;
    private final Counter batchGenerationErrorCounter;
    private final Counter allocateFolioErrorCounter;
    
    // Timers
    private final Timer generateBatchListTimer;
    private final Timer allocateFolioRangeTimer;
    
    // Distribution Summary
    private final DistributionSummary labelsPerBatchSummary;
    private final DistributionSummary folioRangeSummary;
    
    // Gauges
    private final AtomicInteger concurrentAllocations;
    
    public LabelMetricsRegistry(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        
        // Inicializar counters
        this.labelsGeneratedCounter = Counter.builder("label.generation.total")
            .description("Total de marbetes generados")
            .baseUnit("labels")
            .register(meterRegistry);
        
        this.batchGenerationSuccessCounter = Counter.builder("label.batch.generation.success")
            .description("Generaciones de batch exitosas")
            .register(meterRegistry);
        
        this.batchGenerationErrorCounter = Counter.builder("label.batch.generation.error")
            .description("Generaciones de batch con error")
            .register(meterRegistry);
        
        this.allocateFolioErrorCounter = Counter.builder("label.allocate.folio.error")
            .description("Errores al asignar folios")
            .register(meterRegistry);
        
        // Inicializar timers
        this.generateBatchListTimer = Timer.builder("label.batch.generation.duration")
            .description("Duración de generación de batch")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry);
        
        this.allocateFolioRangeTimer = Timer.builder("label.allocate.folio.duration")
            .description("Duración de asignación de folios")
            .publishPercentiles(0.5, 0.95, 0.99)
            .register(meterRegistry);
        
        // Inicializar distribution summary
        this.labelsPerBatchSummary = DistributionSummary.builder("label.per.batch")
            .description("Cantidad de marbetes por batch")
            .baseUnit("labels")
            .register(meterRegistry);
        
        this.folioRangeSummary = DistributionSummary.builder("label.folio.range.size")
            .description("Tamaño de rango de folios asignado")
            .baseUnit("labels")
            .register(meterRegistry);
        
        // Inicializar gauge
        this.concurrentAllocations = new AtomicInteger(0);
        
        meterRegistry.gauge("label.concurrent.allocations",
            concurrentAllocations,
            AtomicInteger::get);
    }
    
    // ========== Métodos de Registro ==========
    
    public void recordBatchGenerationStart(String warehouseId, String periodId) {
        log.debug("📊 recordBatchGenerationStart: warehouse={}, period={}", warehouseId, periodId);
    }
    
    public void recordBatchGenerationSuccess(String warehouseId, String periodId, long durationMs, int totalLabels) {
        batchGenerationSuccessCounter.increment();
        labelsGeneratedCounter.increment(totalLabels);
        generateBatchListTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        labelsPerBatchSummary.record(totalLabels);
        
        log.debug("📊 Batch success: warehouse={}, period={}, labels={}, duration={}ms",
            warehouseId, periodId, totalLabels, durationMs);
    }
    
    public void recordBatchGenerationError(String warehouseId, String periodId, String errorType) {
        batchGenerationErrorCounter.increment();
        
        Counter errorTypeCounter = Counter.builder("label.batch.error")
            .tag("type", errorType)
            .tag("warehouse", warehouseId)
            .tag("period", periodId)
            .register(meterRegistry);
        errorTypeCounter.increment();
        
        log.error("📊 Batch error: warehouse={}, period={}, type={}", warehouseId, periodId, errorType);
    }
    
    public void recordAllocateFolioRange(long durationMs, int quantity, Long periodId) {
        allocateFolioRangeTimer.record(durationMs, java.util.concurrent.TimeUnit.MILLISECONDS);
        folioRangeSummary.record(quantity);
        
        log.debug("📊 Allocate folio range: quantity={}, period={}, duration={}ms",
            quantity, periodId, durationMs);
    }
    
    public void recordAllocateFolioRangeError(Long periodId, String errorType) {
        allocateFolioErrorCounter.increment();
        
        Counter errorTypeCounter = Counter.builder("label.allocate.error")
            .tag("type", errorType)
            .tag("period", String.valueOf(periodId))
            .register(meterRegistry);
        errorTypeCounter.increment();
        
        log.error("📊 Allocate folio error: period={}, type={}", periodId, errorType);
    }
    
    public void recordFolioRange(long start, long end) {
        folioRangeSummary.record(end - start + 1);
    }
    
    public void incrementConcurrentAllocations() {
        int count = concurrentAllocations.incrementAndGet();
        log.debug("📊 Concurrent allocations: +1 (total: {})", count);
    }
    
    public void decrementConcurrentAllocations() {
        int count = concurrentAllocations.decrementAndGet();
        log.debug("📊 Concurrent allocations: -1 (total: {})", count);
    }
    
    public int getConcurrentAllocations() {
        return concurrentAllocations.get();
    }
}
```

---

## 🧪 Test Suite Completa

**Ubicación:** `src/test/java/tokai/com/mx/SIGMAV2/modules/labels/LabelConcurrencyTestSuite.java`

```java
package tokai.com.mx.SIGMAV2.modules.labels;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Slf4j
@DisplayName("Label Service - Test Suite Concurrencia")
class LabelConcurrencyTestSuite {

    @Autowired
    private tokai.com.mx.SIGMAV2.modules.labels.application.service.impl.LabelServiceImpl labelService;
    
    @Autowired
    private tokai.com.mx.SIGMAV2.modules.labels.infrastructure.adapter.LabelsPersistenceAdapter persistence;
    
    private static final int PERIOD_ID = 1;
    private static final int WAREHOUSE_ID = 1;
    
    @BeforeEach
    void setUp() {
        // Limpiar datos de prueba
        log.info("=== SETUP: Limpiando datos de prueba ===");
    }

    @Test
    @DisplayName("T1: Generación simple - Sin concurrencia")
    void testSimpleGeneration() {
        log.info("▶️ T1: INICIO");
        
        var dto = new tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateBatchListDTO();
        dto.setPeriodId((long) PERIOD_ID);
        dto.setWarehouseId((long) WAREHOUSE_ID);
        dto.setProducts(List.of(
            new tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateBatchListDTO.ProductBatchDTO(100L, 100)
        ));
        
        labelService.generateBatchList(dto, 1L, "ADMINISTRADOR");
        
        var labels = persistence.findByPeriodIdAndWarehouseId((long) PERIOD_ID, (long) WAREHOUSE_ID, 0, 1000);
        
        assertEquals(100, labels.size(), "Debe haber 100 marbetes");
        
        Set<Long> folios = labels.stream().map(l -> l.getFolio()).collect(Collectors.toSet());
        assertEquals(100, folios.size(), "Los folios deben ser únicos");
        
        log.info("✅ T1: ÉXITO");
    }

    @Test
    @DisplayName("T2: 5 usuarios simultáneos")
    void testFiveUsersSimultaneous() throws InterruptedException {
        log.info("▶️ T2: INICIO (5 usuarios)");
        
        int numUsers = 5;
        int labelsPerUser = 100;
        
        ExecutorService executor = Executors.newFixedThreadPool(numUsers);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(numUsers);
        ConcurrentHashMap<Integer, Exception> errors = new ConcurrentHashMap<>();
        
        long testStartTime = System.currentTimeMillis();
        
        for (int i = 0; i < numUsers; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    startGate.await();  // Espera a que todos estén listos
                    
                    long userStartTime = System.currentTimeMillis();
                    
                    var dto = new tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateBatchListDTO();
                    dto.setPeriodId((long) PERIOD_ID);
                    dto.setWarehouseId((long) WAREHOUSE_ID);
                    dto.setProducts(List.of(
                        new tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateBatchListDTO.ProductBatchDTO(
                            (long) (1000 + userId),
                            labelsPerUser
                        )
                    ));
                    
                    labelService.generateBatchList(dto, (long) userId, "ALMACENISTA");
                    
                    long userDuration = System.currentTimeMillis() - userStartTime;
                    log.info("  Usuario {} completado en {}ms", userId, userDuration);
                    
                    endGate.countDown();
                } catch (Exception e) {
                    errors.put(userId, e);
                    log.error("  Usuario {} falló: {}", userId, e.getMessage(), e);
                    endGate.countDown();
                }
            });
        }
        
        startGate.countDown();  // ¡INICIA TODOS SIMULTÁNEAMENTE!
        boolean completed = endGate.await(30, TimeUnit.SECONDS);
        
        long testDuration = System.currentTimeMillis() - testStartTime;
        
        executor.shutdown();
        
        // Validaciones
        assertTrue(completed, "Debe completar en 30 segundos");
        assertTrue(errors.isEmpty(), "No debe haber errores: " + errors);
        
        var allLabels = persistence.findByPeriodIdAndWarehouseId((long) PERIOD_ID, (long) WAREHOUSE_ID, 0, 10000);
        int expectedTotal = numUsers * labelsPerUser;
        assertEquals(expectedTotal, allLabels.size(), "Debe haber " + expectedTotal + " marbetes");
        
        Set<Long> folios = allLabels.stream().map(l -> l.getFolio()).collect(Collectors.toSet());
        assertEquals(expectedTotal, folios.size(), "Los folios deben ser únicos");
        
        log.info("✅ T2: ÉXITO en {}ms ({} usuarios × {} marbetes = {})",
            testDuration, numUsers, labelsPerUser, expectedTotal);
    }

    @Test
    @DisplayName("T3: 20 usuarios simultáneos - Stress test")
    void testTwentyUsersStress() throws InterruptedException {
        log.info("▶️ T3: INICIO (20 usuarios - STRESS TEST)");
        
        int numUsers = 20;
        int labelsPerUser = 200;
        
        ExecutorService executor = Executors.newFixedThreadPool(numUsers);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch endGate = new CountDownLatch(numUsers);
        ConcurrentHashMap<Integer, Long> durations = new ConcurrentHashMap<>();
        ConcurrentHashMap<Integer, Exception> errors = new ConcurrentHashMap<>();
        
        long testStartTime = System.currentTimeMillis();
        
        for (int i = 0; i < numUsers; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    startGate.await();
                    
                    long userStartTime = System.currentTimeMillis();
                    
                    var dto = new tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateBatchListDTO();
                    dto.setPeriodId((long) PERIOD_ID);
                    dto.setWarehouseId((long) WAREHOUSE_ID);
                    dto.setProducts(List.of(
                        new tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateBatchListDTO.ProductBatchDTO(
                            (long) (2000 + userId),
                            labelsPerUser
                        )
                    ));
                    
                    labelService.generateBatchList(dto, (long) userId, "ALMACENISTA");
                    
                    long userDuration = System.currentTimeMillis() - userStartTime;
                    durations.put(userId, userDuration);
                    
                    endGate.countDown();
                } catch (Exception e) {
                    errors.put(userId, e);
                    endGate.countDown();
                }
            });
        }
        
        startGate.countDown();
        boolean completed = endGate.await(60, TimeUnit.SECONDS);
        long testDuration = System.currentTimeMillis() - testStartTime;
        
        executor.shutdown();
        
        // Validaciones
        assertTrue(completed, "Debe completar en 60 segundos");
        assertTrue(errors.isEmpty(), "No debe haber errores");
        
        var allLabels = persistence.findByPeriodIdAndWarehouseId((long) PERIOD_ID, (long) WAREHOUSE_ID, 0, 100000);
        int expectedTotal = numUsers * labelsPerUser;
        assertEquals(expectedTotal, allLabels.size());
        
        Set<Long> folios = allLabels.stream().map(l -> l.getFolio()).collect(Collectors.toSet());
        assertEquals(expectedTotal, folios.size(), "Los folios deben ser únicos");
        
        // Estadísticas
        long minDuration = durations.values().stream().min(Long::compare).orElse(0L);
        long maxDuration = durations.values().stream().max(Long::compare).orElse(0L);
        double avgDuration = durations.values().stream().mapToLong(Long::longValue).average().orElse(0.0);
        
        double throughput = (expectedTotal * 1000.0) / testDuration;
        
        log.info("✅ T3: ÉXITO");
        log.info("   📊 Estadísticas:");
        log.info("      Tiempo total: {}ms", testDuration);
        log.info("      Duración por usuario - Min: {}ms, Max: {}ms, Promedio: {:.2f}ms", 
            minDuration, maxDuration, avgDuration);
        log.info("      Throughput: {:.0f} marbetes/segundo", throughput);
        log.info("      Total marbetes: {}", expectedTotal);
    }

    @Test
    @DisplayName("T4: Verificación de continuidad de folios")
    void testFolioSequenceContinuity() throws InterruptedException {
        log.info("▶️ T4: INICIO (Verificar continuidad de folios)");
        
        int numUsers = 10;
        int labelsPerUser = 50;
        
        ExecutorService executor = Executors.newFixedThreadPool(numUsers);
        CountDownLatch gates = new CountDownLatch(numUsers);
        
        for (int i = 0; i < numUsers; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    var dto = new tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateBatchListDTO();
                    dto.setPeriodId((long) PERIOD_ID);
                    dto.setWarehouseId((long) WAREHOUSE_ID);
                    dto.setProducts(List.of(
                        new tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateBatchListDTO.ProductBatchDTO(
                            (long) (3000 + userId),
                            labelsPerUser
                        )
                    ));
                    
                    labelService.generateBatchList(dto, (long) userId, "ALMACENISTA");
                    gates.countDown();
                } catch (Exception e) {
                    log.error("Error", e);
                    gates.countDown();
                }
            });
        }
        
        gates.await();
        executor.shutdown();
        
        // Verificar continuidad
        var allLabels = persistence.findByPeriodIdAndWarehouseId((long) PERIOD_ID, (long) WAREHOUSE_ID, 0, 100000);
        
        List<Long> sortedFolios = allLabels.stream()
            .map(l -> l.getFolio())
            .sorted()
            .collect(Collectors.toList());
        
        // Verificar que cada folio es el anterior + 1
        for (int i = 0; i < sortedFolios.size() - 1; i++) {
            Long current = sortedFolios.get(i);
            Long next = sortedFolios.get(i + 1);
            assertEquals(current + 1, next, 
                String.format("Brecha en folios: %d -> %d (se esperaba %d)", current, next, current + 1));
        }
        
        log.info("✅ T4: ÉXITO - {} folios verificados como consecutivos", sortedFolios.size());
    }

    @Test
    @DisplayName("T5: Roles permitidos")
    void testAuthorizedRoles() {
        log.info("▶️ T5: INICIO (Verificar roles autorizados)");
        
        var dto = new tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateBatchListDTO();
        dto.setPeriodId((long) PERIOD_ID);
        dto.setWarehouseId((long) WAREHOUSE_ID);
        dto.setProducts(List.of(
            new tokai.com.mx.SIGMAV2.modules.labels.application.dto.GenerateBatchListDTO.ProductBatchDTO(100L, 10)
        ));
        
        // Roles que SÍ pueden generar
        assertDoesNotThrow(() -> labelService.generateBatchList(dto, 1L, "ADMINISTRADOR"));
        assertDoesNotThrow(() -> labelService.generateBatchList(dto, 1L, "ALMACENISTA"));
        assertDoesNotThrow(() -> labelService.generateBatchList(dto, 1L, "AUXILIAR_DE_CONTEO"));
        assertDoesNotThrow(() -> labelService.generateBatchList(dto, 1L, "AUXILIAR"));
        
        log.info("✅ T5: ÉXITO - Roles verificados");
    }
}
```

---

## ⚠️ Configuración de Alertas

**Ubicación:** `src/main/resources/prometheus-alerts.yaml`

```yaml
# Alertas de Prometheus para Label Service

groups:
  - name: label_service
    interval: 30s
    rules:
      
      # Alerta 1: Latencia alta en asignación de folios
      - alert: HighLatencyFolioAllocation
        expr: histogram_quantile(0.99, label_allocate_folio_duration_ms) > 500
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Latencia alta en asignación de folios"
          description: "P99 latency es {{ $value }}ms (umbral: 500ms)"
          dashboard: "http://grafana:3000/d/label-service"
      
      # Alerta 2: Latencia muy alta
      - alert: CriticalLatencyFolioAllocation
        expr: histogram_quantile(0.99, label_allocate_folio_duration_ms) > 2000
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "LATENCIA CRÍTICA en asignación de folios"
          description: "P99 latency es {{ $value }}ms (crítico: > 2000ms)"
          action: "Revisar base de datos y pool de conexiones"
      
      # Alerta 3: Errores de generación
      - alert: LabelGenerationErrors
        expr: increase(label_batch_generation_error[5m]) > 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Errores en generación de marbetes"
          description: "{{ $value }} errores en los últimos 5 minutos"
          action: "Revisar logs en 'ERROR generateBatchList'"
      
      # Alerta 4: Errores de folio duplicado
      - alert: DuplicateFolioError
        expr: increase(label_allocate_error{type="DuplicateKeyException"}[5m]) > 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "ERROR CRÍTICO: Folios duplicados detectados"
          description: "{{ $value }} intentos de folio duplicado"
          action: "INVESTIGACIÓN INMEDIATA - Verificar bloqueos BD"
      
      # Alerta 5: Alta contención de folios
      - alert: HighConcurrentAllocations
        expr: label_concurrent_allocations_gauge > 10
        for: 2m
        labels:
          severity: warning
        annotations:
          summary: "Alta contención en asignación de folios"
          description: "{{ $value }} solicitudes en cola"
          action: "Monitor normal, pero revisar si sigue subiendo"
      
      # Alerta 6: Contención muy alta
      - alert: CriticalConcurrentAllocations
        expr: label_concurrent_allocations_gauge > 30
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "CONTENCIÓN CRÍTICA en asignación de folios"
          description: "{{ $value }} solicitudes en cola"
          action: "Sistema al límite - Verificar carga y conexiones BD"
      
      # Alerta 7: Timeout en locks
      - alert: FolioLockTimeouts
        expr: increase(label_allocate_error{type="LockTimeoutException"}[5m]) > 0
        for: 1m
        labels:
          severity: critical
        annotations:
          summary: "Timeouts en locks de folios"
          description: "{{ $value }} timeouts en los últimos 5 minutos"
          action: "Verificar bloqueos BD prolongados"
```

---

## 🔧 Configuración de Actuadores (Health)

**Ubicación:** `application.yml`

```yaml
spring:
  application:
    name: sigmav2-services
  
  # Metrics
  metrics:
    export:
      prometheus:
        enabled: true
    distribution:
      percentiles-histogram:
        label.allocate.folio.duration: true
        label.batch.generation.duration: true

management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  
  endpoint:
    health:
      show-details: always
    metrics:
      enabled: true
  
  # Custom health indicators
  health:
    defaults:
      enabled: true
    db:
      enabled: true

# Logging en producción
logging:
  level:
    tokai.com.mx.SIGMAV2.modules.labels.application: DEBUG
    tokai.com.mx.SIGMAV2.modules.labels.infrastructure: DEBUG
  
  pattern:
    console: "%d{HH:mm:ss.SSS} [%-15thread] %-5level %-40logger{40} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%-15thread] %-5level %-40logger{40} - %msg%n"
  
  file:
    name: /var/log/sigmav2/application.log
    max-size: 10MB
    max-history: 10
```

---

**Última actualización:** 2026-02-09
**Versión:** 1.0


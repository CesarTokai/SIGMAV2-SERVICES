package tokai.com.mx.SIGMAV2.modules.MultiWarehouse.application.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * Utilidad para parsear strings de periodo a {@link LocalDate}.
 *
 * <p>Formatos soportados:
 * <ul>
 *   <li>{@code MM-yyyy}  — ejemplo: "01-2026"</li>
 *   <li>{@code yyyy-MM}  — ejemplo: "2026-01"</li>
 *   <li>{@code yyyy-MM-dd} — ejemplo: "2026-01-01"</li>
 *   <li>{@code yyyy/MM}  — ejemplo: "2026/01"</li>
 *   <li>{@code MM/yyyy}  — ejemplo: "01/2026"</li>
 * </ul>
 * Siempre retorna el primer día del mes ({@code atDay(1)}).
 */
public final class PeriodParserUtil {

    private static final DateTimeFormatter FMT_MM_YYYY   = DateTimeFormatter.ofPattern("MM-yyyy");
    private static final DateTimeFormatter FMT_YYYY_MM   = DateTimeFormatter.ofPattern("yyyy-MM");
    private static final DateTimeFormatter FMT_YYYY_MM_S = DateTimeFormatter.ofPattern("yyyy/MM");
    private static final DateTimeFormatter FMT_MM_YYYY_S = DateTimeFormatter.ofPattern("MM/yyyy");

    private PeriodParserUtil() { /* utilidad estática */ }

    /**
     * Parsea un string de periodo a {@link LocalDate} (primer día del mes).
     *
     * @param period string en alguno de los formatos soportados
     * @return {@link LocalDate} con el primer día del mes, o {@code null} si no se pudo parsear
     */
    public static LocalDate parse(String period) {
        if (period == null || period.isBlank()) return null;
        String p = period.trim();

        try {
            if (p.matches("\\d{2}-\\d{4}"))   return YearMonth.parse(p, FMT_MM_YYYY).atDay(1);
            if (p.matches("\\d{4}-\\d{2}"))   return YearMonth.parse(p, FMT_YYYY_MM).atDay(1);
            if (p.matches("\\d{4}/\\d{2}"))   return YearMonth.parse(p, FMT_YYYY_MM_S).atDay(1);
            if (p.matches("\\d{2}/\\d{4}"))   return YearMonth.parse(p, FMT_MM_YYYY_S).atDay(1);
            if (p.matches("\\d{4}-\\d{2}-\\d{2}")) return LocalDate.parse(p);
        } catch (Exception ignored) { /* fallthrough → null */ }

        return null;
    }
}


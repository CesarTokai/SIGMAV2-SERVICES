# DTO Consolidation Analysis - Labels Module

**Date**: 2026-04-29  
**Status**: Analysis  
**Scope**: 30 DTOs across request/response/detail categories

---

## Executive Summary

Found **6 consolidation opportunities** affecting ~40% of DTOs. Estimated cleanup: reduce 30 → 20 DTOs without losing functionality.

**Quick wins:**
1. CountEventDTO ↔ UpdateCountDTO (merge)
2. PrintRequestDTO ↔ PrintSelectedLabelsRequestDTO (merge)
3. LabelDetailDTO ↔ LabelDetailForPrintDTO (consolidate with @JsonView)
4. LabelForCountDTO ↔ LabelForCountRequestDTO (clarify naming)
5. Remove legacy RequestDTO (deprecated)

---

## Complete DTO Matrix

### Category 1: REQUEST/INPUT DTOs (11 total)

| DTO | Purpose | Fields | Candidate Consolidation |
|-----|---------|--------|------------------------|
| **LabelRequestDTO** | Request labels for product (DEPRECATED) | periodId, warehouseId, productId, requestedLabels | REMOVE - use GenerateBatchListDTO |
| **GenerateBatchDTO** | Generate single batch | periodId, warehouseId, productIds, quantities | Keep - core generation |
| **GenerateBatchListDTO** | Generate multiple batches | batches list | Keep - core generation |
| **GenerateFileRequestDTO** | Generate TXT export | periodId, warehouseId, (filters) | Keep - reporting |
| **PrintRequestDTO** | Print labels by period/warehouse | periodId, warehouseId, **folios**, productId, forceReprint, withQR | **MERGE with PrintSelectedLabelsRequestDTO** |
| **PrintSelectedLabelsRequestDTO** | Print specific folios | **folios**, periodId, warehouseId, infoType | **MERGE with PrintRequestDTO** |
| **PrintSelectedLabelsAutoWarehouseDTO** | Print with auto-detect warehouse | folios, periodId | Keep - specialized use case |
| **AutoDetectWarehouseReprintDTO** | Auto-detect warehouse for reprint | folios, periodId | **CONSOLIDATE with PrintSelectedLabelsAutoWarehouseDTO** (rename) |
| **CancelLabelRequestDTO** | Cancel a label | folio, periodId, motivoCancelacion | Keep - specialized domain operation |
| **LabelForCountRequestDTO** | Lookup label for counting | folio, periodId, warehouseId (opt) | Keep - validation DTO |
| **LabelCountListRequestDTO** | Request count list (unclear purpose) | (?) | **INVESTIGATE - seems unused** |
| **LabelSummaryRequestDTO** | Request summary data | periodId, warehouseId, filters | Keep - reporting |
| **UpdateCancelledStockDTO** | Update cancelled stock quantities | folio, periodId, existenciasActuales | Keep - domain operation |

**Action**: Remove 1 (requestLabels), Merge 3 pairs = **8 consolidated → 5 DTOs** (-38%)

---

### Category 2: RESPONSE/OUTPUT DTOs (7 total)

| DTO | Purpose | Fields | Notes |
|-----|---------|--------|-------|
| **GenerateBatchResponseDTO** | Batch generation result | statusCode, message, batchId, startFolio, endFolio, totalGenerated | Keep - clear contract |
| **GenerateFileResponseDTO** | File export result | statusCode, fileName, bytes, message | Keep - file download response |
| **PendingPrintCountResponseDTO** | Pending print count | periodId, warehouseId, totalPending, byStatus | Keep - status report |
| **LabelStatusResponseDTO** | Single label status | folio, estado, conteo1, conteo2, cancelado | Keep - minimal status |
| **LabelSummaryResponseDTO** | Summary statistics | periodId, warehouseId, totalLabels, byStatus, byWarehouse | Keep - aggregation |
| **LabelCancelledDTO** | Cancelled label data | folio, motivo, canceladoAt, canceladoBy, reactivado | Keep - audit trail |
| **AutoDetectWarehouseResponseDTO** | Warehouse detection result | warehouseId, claveAlmacen, nombreAlmacen, confidence | Keep - unique response |

**Action**: No consolidation needed. All have distinct contracts.

---

### Category 3: DETAIL/REPORTING DTOs (8 total) ⚠️ HIGHEST DUPLICATION

| DTO | Purpose | Fields | Scope | Consolidation |
|-----|---------|--------|-------|----------------|
| **LabelDetailDTO** | Basic label details | folio, estado, product info, warehouse info, existencias, comment | Query response (list) | **CONSOLIDATE with LabelDetailForPrintDTO using @JsonView** |
| **LabelDetailForPrintDTO** | Pre-print label details | Same as LabelDetailDTO + conteo1/2, diferencia, createdBy, statusConteo | Pre-print query | **CONSOLIDATE with LabelDetailDTO** |
| **LabelForCountDTO** | Label for counting UI | folio, estado, product, warehouse, conteo1/2, diferencia, conteo*Comentario, conteo*Usuario | Counting operation | Keep - specialized view |
| **LabelForCountRequestDTO** | (Request for label lookup) | folio, periodId, warehouseId | Lookup request | Keep - validation |
| **LabelFullDetailDTO** | Complete label history | 140+ lines: includes counts, prints, cancellations, history, nested DTOs | Full audit/detail view | Keep - audit trail, too different |
| **LabelWithCommentsReportDTO** | (Report variant) | (?) | Report | **INVESTIGATE - unclear purpose** |
| **MarbeteReportDTO** | Marbete report | (?) | Report | **INVESTIGATE - unclear purpose** |
| **LabelListFilterDTO** | Filter criteria | (query filters) | Filtering | Keep - query filters |

**Action**: 
- Consolidate LabelDetailDTO + LabelDetailForPrintDTO (use @JsonView("details") / @JsonView("print"))
- Investigate LabelWithCommentsReportDTO, MarbeteReportDTO (may be report-only)
- **Estimated**: 8 → 5 DTOs (-38%)

---

### Category 4: COUNT/EVENT DTOs (3 total) ⚠️ HIGH DUPLICATION

| DTO | Purpose | Fields | Consolidation |
|-----|---------|--------|----------------|
| **CountEventDTO** | Register count (C1 or C2) | folio, periodId, warehouseId (opt), countedValue, comment | Register event | **MERGE with UpdateCountDTO** |
| **UpdateCountDTO** | Update existing count | folio, periodId, countedValue, observaciones, comment | Update event | **MERGE with CountEventDTO** |
| **LabelCountListRequestDTO** | Request count list | (?) | (Unknown) | **INVESTIGATE** |
| **CountEventHistoryDTO** | (nested in LabelFullDetailDTO) | countNumber, value, recordedAt, user info, action, comment | History item | Keep - nested DTO |

**Analysis**: 
- CountEventDTO vs UpdateCountDTO: **95% identical**. Difference is `observaciones` field in UpdateCountDTO.
- Solution: Merge into `CountEventDTO` with optional `operation` enum (REGISTER|UPDATE) or two separate endpoints.

**Action**: Merge 2 → 1 DTO (-33%)

---

## Consolidation Strategy

### Phase 1: Low-Risk Merges (Immediate)

1. **CountEventDTO + UpdateCountDTO → CountEventDTO**
   - Add enum: `operation: "REGISTER" | "UPDATE"` (default: REGISTER)
   - Move `observaciones` → generic `metadata` field
   - Impact: Services must route based on `operation` flag
   - Effort: 2 services affected

2. **PrintRequestDTO + PrintSelectedLabelsRequestDTO → PrintRequestDTO**
   - Add `selectionMode: "BY_PERIOD" | "BY_FOLIO"` (default: BY_PERIOD)
   - Keep all fields optional to support both modes
   - Remove `infoType` — handle via separate query param
   - Impact: Print service refactoring
   - Effort: 1 service affected

3. **Remove LabelRequestDTO**
   - Move all callers to `GenerateBatchListDTO`
   - Check frontend/clients first
   - Impact: Deprecation path needed
   - Effort: Coordination with frontend

### Phase 2: Medium-Risk Consolidations (After review)

4. **LabelDetailDTO + LabelDetailForPrintDTO → LabelDetailDTO (with @JsonView)**
   ```java
   @JsonView(Views.Summary.class)
   private Long folio;
   
   @JsonView(Views.Print.class)
   private BigDecimal conteo1Valor;
   ```
   - Effort: Medium (requires mapper review)
   - Benefit: Single detail DTO, conditional fields

### Phase 3: Investigation Required

5. **LabelWithCommentsReportDTO** — Is this used?
6. **MarbeteReportDTO** — Is this used?
7. **LabelCountListRequestDTO** — What does this do?
8. **LabelListFilterDTO** — Should be combined with `LabelSummaryRequestDTO`?

---

## Recommended Naming After Consolidation

| Current | Proposed | Reason |
|---------|----------|--------|
| CountEventDTO → **CountEventDTO** | Unified for create + update | Single DTO for count operations |
| PrintRequestDTO → **PrintRequestDTO** | Unified with mode selector | Single print entry point |
| LabelRequestDTO | **DEPRECATED** | Use GenerateBatchListDTO |
| LabelDetailDTO | **LabelDetailDTO** | Unified with @JsonView |
| LabelDetailForPrintDTO | **REMOVED** → LabelDetailDTO | Merged |
| LabelForCountDTO | **RENAME → CountingLabelDTO** (optional) | Clearer purpose: for counting UI |

---

## Impact Assessment

| Metric | Current | After Consolidation | Improvement |
|--------|---------|---------------------|-------------|
| Total DTOs | 30 | 21 | -30% |
| Request DTOs | 11 | 8 | -27% |
| Detail/Report DTOs | 8 | 5 | -38% |
| Code duplication | High (3 pairs) | Low | Significant |
| Service refactoring effort | N/A | ~3 services affected | Medium |
| Frontend coordination | N/A | Required (LabelRequestDTO removal) | Medium |

---

## Implementation Roadmap

### Step 1: Investigation (1 day)
- [ ] Find usage of `LabelWithCommentsReportDTO`, `MarbeteReportDTO`, `LabelCountListRequestDTO`
- [ ] Check if `LabelDetailDTO` and `LabelDetailForPrintDTO` fields really need separation
- [ ] Audit all frontend calls for deprecated `LabelRequestDTO`

### Step 2: Phase 1 Merges (2-3 days)
- [ ] Merge CountEventDTO + UpdateCountDTO
- [ ] Merge PrintRequestDTO + PrintSelectedLabelsRequestDTO  
- [ ] Merge PrintSelectedLabelsAutoWarehouseDTO + AutoDetectWarehouseReprintDTO
- [ ] Update services and tests
- [ ] Create mappers if needed

### Step 3: Phase 2 Consolidations (2-3 days)
- [ ] Consolidate LabelDetailDTO with @JsonView
- [ ] Update serialization logic
- [ ] Test report outputs

### Step 4: Deprecation + Cleanup (1-2 days)
- [ ] Remove LabelRequestDTO
- [ ] Coordinate with frontend
- [ ] Update docs

**Total effort**: ~6-8 days for full consolidation

---

## Files to Review

- `src/main/java/tokai/com/mx/SIGMAV2/modules/labels/application/dto/` (all files)
- `src/main/java/tokai/com/mx/SIGMAV2/modules/labels/application/service/impl/` (service usages)
- Frontend code (client calls to deprecated DTOs)

---

## Decision Points for Architecture Review

1. **Should CountEventDTO and UpdateCountDTO be merged?** (Recommended: YES, reduces duplication)
2. **Should PrintRequestDTO variants be unified?** (Recommended: YES, simplifies print logic)
3. **Should LabelDetailDTO consolidate with LabelDetailForPrintDTO?** (Recommended: YES, with @JsonView)
4. **Is LabelRequestDTO still used?** (Recommendation: REMOVE, migrate to GenerateBatchListDTO)
5. **What are `LabelWithCommentsReportDTO` and `MarbeteReportDTO` for?** (Investigation required)

---

**Next step**: Review findings with team, confirm decisions, then execute Phase 1 merges.

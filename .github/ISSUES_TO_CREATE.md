# Issues a Crear - Labels Module Review

## Issue #1: [ARCH] Audit and consolidate duplicate DTOs in labels module

**Type**: HITL (design review needed)
**Priority**: High
**Blocker de**: #2

```
## What to audit

~30+ DTOs in labels module. Identify duplication and consolidation opportunities.

## Acceptance criteria

- [ ] Create matrix mapping DTO name → purpose → similar DTOs
- [ ] Identify 3+ consolidation candidates (e.g., multiple 'Request' DTOs with same fields)
- [ ] Design doc with mapping strategy (before refactoring)
- [ ] Document in CONTEXT.md if creating glossary

## Blocked by

None - can start immediately

## Notes

Output: design-doc-dtos.md with consolidation roadmap. Scope: src/main/java/tokai/com/mx/SIGMAV2/modules/labels/application/dto/
```

---

## Issue #2: [ARCH] Refactor LabelsPersistenceAdapter into domain-specific adapters

**Type**: HITL
**Priority**: High
**Blocker de**: #4 (future: decoupling)
**Bloqueado por**: #1 (recommended to understand DTO structure first)

```
## What to build

Split LabelsPersistenceAdapter (551 lines) into 3 focused adapters:
- CountEventAdapter (count event persistence)
- CancelledAdapter (cancellation audit)
- SequenceAdapter (folio sequence management)

Reduces god object, improves testability, clarifies responsibilities.

## Acceptance criteria

- [ ] Extract CountEventAdapter with findCountsByFolio(), saveCountEvent() 
- [ ] Extract CancelledAdapter with cancel/reactivate logic
- [ ] Extract SequenceAdapter with getNextFolio(), reset() methods
- [ ] All tests pass (existing test coverage validates behavior)
- [ ] Update LabelServiceImpl to use new adapters
- [ ] Document adapter responsibilities in code comments

## Blocked by

None - can start immediately (but recommend doing #1 first for context)

## Notes

No new tests needed - adapter refactoring is internal. Existing test suite validates correctness.
```

---

## Issue #3: [TEST] Add unit tests for LabelGenerationService

**Type**: AFK
**Priority**: High
**Blocker de**: None
**Bloqueado por**: None

```
## What to build

Unit test coverage for LabelGenerationService batch generation logic.

## Acceptance criteria

- [ ] Test batch creation with valid request (happy path)
- [ ] Test folio sequence increment and reset
- [ ] Test warehouse access validation
- [ ] Test invalid period/warehouse error handling
- [ ] Achieve ≥80% code coverage for service
- [ ] Test handles LabelRequest state correctly
- [ ] Mocking strategy: mock LabelsPersistenceAdapter, WarehouseAccessService

## Test file location

src/test/java/tokai/com/mx/SIGMAV2/modules/labels/application/service/impl/LabelGenerationServiceTest.java

## Blocked by

None - can start immediately
```

---

## Issue #4: [TEST] Add unit tests for LabelQueryService

**Type**: AFK
**Priority**: High
**Blocker de**: None
**Bloqueado por**: None

```
## What to build

Unit test coverage for LabelQueryService query logic including N+1 detection scenarios.

## Acceptance criteria

- [ ] Test findLabelsByPeriodAndWarehouse with pagination
- [ ] Test filter scenarios (by status, product, etc.)
- [ ] Test access control (warehouse validation)
- [ ] Test N+1 scenario: verify no lazy-load surprises in result mapping
- [ ] Achieve ≥80% code coverage for service
- [ ] Test sorting and ordering
- [ ] Include edge case: empty results, large dataset pagination

## Test file location

src/test/java/tokai/com/mx/SIGMAV2/modules/labels/application/service/impl/LabelQueryServiceTest.java

## Blocked by

None - can start immediately

## Notes

Validate that queries use fetch=JOIN where needed to prevent N+1. See JpaLabelRepository for existing @Query examples.
```

---

## Publish command

Once `gh` is installed, run:

```bash
gh issue create --title "[ARCH] Audit and consolidate duplicate DTOs in labels module" --body "<BODY_1>" --label "needs-triage"
gh issue create --title "[ARCH] Refactor LabelsPersistenceAdapter into domain-specific adapters" --body "<BODY_2>" --label "needs-triage"
gh issue create --title "[TEST] Add unit tests for LabelGenerationService" --body "<BODY_3>" --label "needs-triage"
gh issue create --title "[TEST] Add unit tests for LabelQueryService" --body "<BODY_4>" --label "needs-triage"
```

Or use GitHub web UI to create manually.

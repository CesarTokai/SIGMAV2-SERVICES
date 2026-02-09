# Análisis Práctico de Concurrencia - Guía de Implementación y Testing

## 📋 Tabla de Contenidos

1. [Diagramas de Secuencia](#diagramas-de-secuencia)
2. [Casos de Prueba](#casos-de-prueba)
3. [Monitoreo en Producción](#monitoreo-en-producción)
4. [Troubleshooting](#troubleshooting)
5. [Mejoras Futuras](#mejoras-futuras)

---

## 🎯 Diagramas de Secuencia

### Diagrama 1: Caso Ideal (Una Solicitud Serena)

```
Usuario: Admin
┌─────────────────────────────────────────────────────────────┐
│                                                             │
│  1. POST /api/labels/generate-batch-list                  │
│     {periodId: 123, warehouseId: 456, products: [...]}   │
│                           │                               │
│                           ▼                               │
│                    LabelService                           │
│                    validateWarehouseAccess ✓              │
│                           │                               │
│                           ▼                               │
│                    for producto 1:                        │
│              allocateFolioRange(123, 100)                │
│                           │                               │
│                           ▼                               │
│              ┌─────────────────────────┐                 │
│              │   LabelFolioSequence    │                 │
│              │   (BD PESSIMISTIC_WRITE)│                 │
│              │   ultimoFolio = 5000    │                 │
│              └─────────────────────────┘                 │
│                           │                               │
│              Lee: 5000 → Calcula: 5001-5100             │
│              Actualiza: ultimoFolio = 5100               │
│                           │                               │
│                    Retorna [5001-5100] ✓                 │
│                           │                               │
│              Create 100 Label objects                     │
│              saveAll(labels) - BD INSERT                 │
│                           │                               │
│                    for producto 2:                        │
│              allocateFolioRange(123, 50)                 │
│                           │                               │
│              Lee: 5100 → Calcula: 5101-5150             │
│              Actualiza: ultimoFolio = 5150               │
│                           │                               │
│                    Retorna [5101-5150] ✓                 │
│                           │                               │
│              Create 50 Label objects                      │
│              saveAll(labels) - BD INSERT                 │
│                           │                               │
│                    Response 200 OK                        │
│              {totalGenerated: 150, status: 'success'}    │
│                                                             │
└─────────────────────────────────────────────────────────────┘

RESULTADO: ✅ 150 marbetes generados con folios 5001-5150
TIEMPO TOTAL: ~200-300ms
```

### Diagrama 2: Múltiples Usuarios Simultáneos (La Realidad)

```
TIEMPO→
│
│  10:15:20.000 ─ User A (Admin) solicita 100 marbetes
│  10:15:20.005 ─ User B (Almacenista) solicita 50 marbetes
│  10:15:20.010 ─ User C (Auxiliar) solicita 75 marbetes
│
├─ 10:15:20.100
│  ┌─────────────────────────────────────────────────────────────────┐
│  │ User A - allocateFolioRange(123, 100)                          │
│  │ ├─ synchronized ✓ (first to enter)                             │
│  │ ├─ BD findById PESSIMISTIC_WRITE ✓ (locks row)                │
│  │ ├─ Read: ultimoFolio = 5000                                   │
│  │ ├─ Calculate: [5001-5100]                                      │
│  │ ├─ Update: ultimoFolio = 5100                                 │
│  │ ├─ commit & release lock                                       │
│  │ └─ Return [5001-5100] ✓ 10:15:20.120                          │
│  └─────────────────────────────────────────────────────────────────┘
│         ⏳ User B WAITING (cant get lock)
│         ⏳ User C WAITING (cant get lock)
│
├─ 10:15:20.125
│  ┌─────────────────────────────────────────────────────────────────┐
│  │ User B - allocateFolioRange(123, 50)                           │
│  │ ├─ synchronized ✓ (now its turn)                               │
│  │ ├─ BD findById PESSIMISTIC_WRITE ✓                            │
│  │ ├─ Read: ultimoFolio = 5100 (updated by A)                    │
│  │ ├─ Calculate: [5101-5150]                                      │
│  │ ├─ Update: ultimoFolio = 5150                                 │
│  │ ├─ commit & release lock                                       │
│  │ └─ Return [5101-5150] ✓ 10:15:20.145                          │
│  └─────────────────────────────────────────────────────────────────┘
│         ⏳ User C STILL WAITING
│
├─ 10:15:20.150
│  ┌─────────────────────────────────────────────────────────────────┐
│  │ User C - allocateFolioRange(123, 75)                           │
│  │ ├─ synchronized ✓ (finally!)                                   │
│  │ ├─ BD findById PESSIMISTIC_WRITE ✓                            │
│  │ ├─ Read: ultimoFolio = 5150 (updated by B)                    │
│  │ ├─ Calculate: [5151-5225]                                      │
│  │ ├─ Update: ultimoFolio = 5225                                 │
│  │ ├─ commit & release lock                                       │
│  │ └─ Return [5151-5225] ✓ 10:15:20.170                          │
│  └─────────────────────────────────────────────────────────────────┘
│
├─ 10:15:20.300 - User A responds with 100 marbetes ✓
├─ 10:15:20.320 - User B responds with 50 marbetes ✓
├─ 10:15:20.340 - User C responds with 75 marbetes ✓
│
└─ RESULTADO: ✅ SIN DUPLICADOS, CONTINUIDAD PERFECTA

FOLIOS DISTRIBUIDOS:
├─ User A: 5001-5100 (100 marbetes)
├─ User B: 5101-5150 (50 marbetes)
└─ User C: 5151-5225 (75 marbetes)

TIEMPO TOTAL: ~340ms
SERIALIZACIÓN: ~220ms (esperas)
```

### Diagrama 3: Cluster - Múltiples Servidores

```
SERVIDOR 1              SERVIDOR 2              BASE DE DATOS
(JVM 1)                 (JVM 2)
│                       │                       │
│                       │                       │
User A                  User B                  │
(Admin)                (Almacenista)            │
│                       │                       │
├─allocateFolioRange──┐ │                       │
│  (100)              │ ├─allocateFolioRange───>│
│                     │ │  (50)                 │
│                     │ │                       │
│ synchronized ✓      │ │ synchronized ✓        │
│ (only in Srv1)      │ │ (only in Srv2)        │
│                     │ │                       │
├─findById────────────┼─┼──────────────────────>│
│ PESSIMISTIC_WRITE ✓ │ │ LOCK ACQUIRED (Srv1)  │
│                     │ │                       │
│                     │ ├─findById────────────>│
│                     │ │ PESSIMISTIC_WRITE     │
│                     │ │ WAITING... ⏳          │
│                     │ │                       │
├─Read: 5000 ────────>│ │                       │
│ Calculate [5001-   │ │ (User B blocked)      │
│  5100]             │ │                       │
├─Update: 5100 ────>│ │                       │
│ COMMIT & RELEASE ─>│ │ Lock Released ✓       │
│ (returns [5001-   │ │                       │
│  5100]) ✓          │ │ LOCK ACQUIRED NOW     │
│                     │ ├─Read: 5100 ────────>│
│                     │ │ Calculate [5101-    │
│                     │ │  5150]              │
│                     │ ├─Update: 5150 ─────>│
│                     │ │ COMMIT & RELEASE ──>│
│                     │ │ (returns [5101-    │
│                     │ │  5150]) ✓          │
│                     │ │                     │
└─────────────────────┴─┴─────────────────────

✅ RESULTADO FINAL:
   - User A: 5001-5100 (sin duplicados)
   - User B: 5101-5150 (sin duplicados)
   - CONTINUIDAD GARANTIZADA (PESSIMISTIC_WRITE)
   - SIN RACE CONDITIONS (BD es el árbitro)
```

---

## 🧪 Casos de Prueba

### Test 1: Generación Simple (Caso Base)

**Archivo:** `LabelServiceImplTest.java`

```java
@Test
void testGenerateSingleBatch() {
    // Arrange
    GenerateBatchListDTO dto = new GenerateBatchListDTO();
    dto.setPeriodId(1L);
    dto.setWarehouseId(1L);
    dto.setProducts(List.of(
        new ProductBatchDTO(100L, 50)  // 50 marbetes
    ));
    
    // Act
    labelService.generateBatchList(dto, 1L, "ADMINISTRADOR");
    
    // Assert
    List<Label> labels = persistence.findByPeriodIdAndWarehouseId(1L, 1L, 0, 50);
    assertEquals(50, labels.size());
    
    // Verificar continuidad
    for (int i = 0; i < labels.size() - 1; i++) {
        assertEquals(
            labels.get(i).getFolio() + 1,
            labels.get(i + 1).getFolio(),
            "Los folios deben ser consecutivos"
        );
    }
}
```

### Test 2: Concurrencia en Una Instancia

**Archivo:** `LabelServiceConcurrencyTest.java`

```java
@Test
void testConcurrentGenerationSingleInstance() throws InterruptedException {
    // Arrange
    int numThreads = 10;
    int labelsPerThread = 50;
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch endLatch = new CountDownLatch(numThreads);
    
    List<long[]> results = Collections.synchronizedList(new ArrayList<>());
    List<Exception> errors = Collections.synchronizedList(new ArrayList<>());
    
    // Act
    for (int i = 0; i < numThreads; i++) {
        final int threadNum = i;
        executor.submit(() -> {
            try {
                startLatch.await();  // Espera a que todos estén listos
                
                GenerateBatchListDTO dto = new GenerateBatchListDTO();
                dto.setPeriodId(1L);
                dto.setWarehouseId(1L);
                dto.setProducts(List.of(
                    new ProductBatchDTO(100L + threadNum, labelsPerThread)
                ));
                
                long startFolio = -1;
                // Interceptar y capturar los folios asignados
                labelService.generateBatchList(dto, 1L, "ADMINISTRADOR");
                
                endLatch.countDown();
            } catch (Exception e) {
                errors.add(e);
            }
        });
    }
    
    startLatch.countDown();  // Inicia todos simultáneamente
    endLatch.await();        // Espera a que terminen
    
    // Assert
    assertTrue(errors.isEmpty(), "No debe haber excepciones: " + errors);
    
    List<Label> allLabels = persistence.findByPeriodIdAndWarehouseId(1L, 1L, 0, 10000);
    int totalExpected = numThreads * labelsPerThread;
    assertEquals(totalExpected, allLabels.size(), "Debe haber 500 marbetes en total");
    
    // Verificar que NO HAY DUPLICADOS
    Set<Long> folios = allLabels.stream()
        .map(Label::getFolio)
        .collect(Collectors.toSet());
    assertEquals(allLabels.size(), folios.size(), 
        "Debe haber " + allLabels.size() + " folios únicos, pero hay " + folios.size());
    
    // Verificar continuidad
    List<Long> sortedFolios = allLabels.stream()
        .map(Label::getFolio)
        .sorted()
        .collect(Collectors.toList());
    
    for (int i = 0; i < sortedFolios.size() - 1; i++) {
        assertEquals(sortedFolios.get(i) + 1, sortedFolios.get(i + 1),
            "Los folios deben ser consecutivos");
    }
    
    executor.shutdown();
}

// RESULTADO ESPERADO:
// ✅ 500 marbetes generados
// ✅ Sin duplicados de folios
// ✅ Folios consecutivos (ej: 1000-1499)
// ✅ Sin excepciones
```

### Test 3: Stress Test

**Archivo:** `LabelServiceStressTest.java`

```java
@Test
@DisplayName("Generar 10,000 marbetes desde múltiples threads")
void testHighVolumeGeneration() throws InterruptedException {
    // Arrange
    int numThreads = 20;
    int labelsPerThread = 500;
    ExecutorService executor = Executors.newFixedThreadPool(numThreads);
    
    List<Exception> errors = Collections.synchronizedList(new ArrayList<>());
    long startTime = System.currentTimeMillis();
    
    // Act
    for (int i = 0; i < numThreads; i++) {
        final int threadNum = i;
        executor.submit(() -> {
            try {
                GenerateBatchListDTO dto = new GenerateBatchListDTO();
                dto.setPeriodId(1L);
                dto.setWarehouseId(1L);
                dto.setProducts(List.of(
                    new ProductBatchDTO(1000L + threadNum, labelsPerThread)
                ));
                labelService.generateBatchList(dto, 1L, "ADMINISTRADOR");
            } catch (Exception e) {
                errors.add(e);
            }
        });
    }
    
    executor.shutdown();
    executor.awaitTermination(5, TimeUnit.MINUTES);
    long totalTime = System.currentTimeMillis() - startTime;
    
    // Assert
    assertTrue(errors.isEmpty(), "No debe haber errores: " + errors);
    
    List<Label> allLabels = persistence.findByPeriodIdAndWarehouseId(1L, 1L, 0, 50000);
    int totalExpected = numThreads * labelsPerThread;
    assertEquals(totalExpected, allLabels.size());
    
    System.out.println("✅ Generados " + totalExpected + " marbetes en " + totalTime + "ms");
    System.out.println("📊 Throughput: " + (totalExpected * 1000 / totalTime) + " marbetes/segundo");
    
    // Verificar folios únicos
    Set<Long> folios = allLabels.stream()
        .map(Label::getFolio)
        .collect(Collectors.toSet());
    assertEquals(totalExpected, folios.size());
}

// RESULTADO ESPERADO:
// ✅ Generados 10000 marbetes en 2000-3000ms
// 📊 Throughput: 3300-5000 marbetes/segundo
// ✅ Sin duplicados
```

### Test 4: Validación de Acceso por Roles

**Archivo:** `LabelServiceRoleTest.java`

```java
@Test
void testOnlyAuthorizedRolesCanGenerate() {
    // Arrange
    GenerateBatchListDTO dto = new GenerateBatchListDTO();
    dto.setPeriodId(1L);
    dto.setWarehouseId(1L);
    dto.setProducts(List.of(new ProductBatchDTO(100L, 50)));
    
    // Act & Assert - ADMINISTRADOR ✓
    assertDoesNotThrow(() -> 
        labelService.generateBatchList(dto, 1L, "ADMINISTRADOR"));
    
    // ALMACENISTA ✓ (con acceso al almacén)
    assertDoesNotThrow(() -> 
        labelService.generateBatchList(dto, 1L, "ALMACENISTA"));
    
    // AUXILIAR_DE_CONTEO ✓
    assertDoesNotThrow(() -> 
        labelService.generateBatchList(dto, 1L, "AUXILIAR_DE_CONTEO"));
    
    // USUARIO_NORMAL ✗
    assertThrows(UnauthorizedAccessException.class, () ->
        labelService.generateBatchList(dto, 1L, "USUARIO_NORMAL"));
    
    // INVITADO ✗
    assertThrows(UnauthorizedAccessException.class, () ->
        labelService.generateBatchList(dto, 1L, "INVITADO"));
}
```

---

## 📊 Monitoreo en Producción

### Métricas Clave a Monitorear

#### 1. Tiempo de Asignación de Folios

```yaml
Métrica: label_service_allocate_folio_duration_ms
Valores Esperados:
  - P50: 10-20ms
  - P95: 50-100ms
  - P99: 100-200ms
  
Alerta Si:
  - P99 > 500ms (posible contención)
  - P99 > 2000ms (problema serio)
```

#### 2. Tasa de Generación

```yaml
Métrica: label_service_labels_generated_total
Tipo: Counter (incremental)
Etiquetas:
  - period_id
  - warehouse_id
  - user_role

Valores Esperados: 100-1000 marbetes/segundo en operación normal
```

#### 3. Concurrencia Actual

```yaml
Métrica: label_service_concurrent_allocations_gauge
Tipo: Gauge
Valores:
  - 0 = Nadie generando
  - 1-5 = Normal
  - 5-10 = Ocupado
  - 10+ = Muy ocupado
```

#### 4. Errores y Excepciones

```yaml
Métrica: label_service_errors_total
Etiquetas:
  - error_type: (DUPLICATE, LOCK_TIMEOUT, DB_ERROR, etc.)
  - severity: (CRITICAL, HIGH, MEDIUM, LOW)

Alerta Inmediata Si:
  - Cualquier error de DUPLICATE
  - Cualquier error de LOCK_TIMEOUT
```

### Configuración de Logs

**Archivo:** `application-prod.yml`

```yaml
logging:
  level:
    tokai.com.mx.SIGMAV2.modules.labels:
      - DEBUG    # Muestra entrada/salida de métodos
      - service.impl.LabelServiceImpl: DEBUG
      - adapter.LabelsPersistenceAdapter: DEBUG
    
  pattern:
    # Agregar thread name para ver concurrencia
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%-15thread] %-5level %logger{36} - %msg%n"
    file: "%d{yyyy-MM-dd HH:mm:ss.SSS} [%-15thread] %-5level %logger{36} - %msg%n"
```

### Dashboard Grafana

```json
{
  "dashboard": {
    "title": "Label Generation Monitoring",
    "panels": [
      {
        "title": "Allocation Time (ms)",
        "targets": [
          {
            "expr": "histogram_quantile(0.99, label_service_allocate_folio_duration_ms)"
          }
        ]
      },
      {
        "title": "Labels Generated per Minute",
        "targets": [
          {
            "expr": "rate(label_service_labels_generated_total[1m])"
          }
        ]
      },
      {
        "title": "Concurrent Allocations",
        "targets": [
          {
            "expr": "label_service_concurrent_allocations_gauge"
          }
        ]
      }
    ]
  }
}
```

---

## 🔧 Troubleshooting

### Problema 1: Folios Duplicados

**Síntomas:**
- Error al guardar en BD: "Duplicate entry '5050' for key 'PRIMARY'"
- Logs muestran dos solicitudes con el mismo rango

**Causas Posibles:**
1. Bloqueo PESSIMISTIC_WRITE no funciona
2. Múltiples JVMs sin BD centralizada
3. Fallo en transacción

**Solución:**
```java
// Verificar que el repositorio tiene @Lock
@Lock(LockModeType.PESSIMISTIC_WRITE)
@NonNull
Optional<LabelFolioSequence> findById(@NonNull Long id);

// Verificar base de datos
SELECT * FROM label_folio_sequence WHERE period_id = 123;
SELECT COUNT(*) FROM labels WHERE folio IN (5001-5100);

// Ejecutar test de concurrencia
mvn test -Dtest=LabelServiceConcurrencyTest
```

### Problema 2: Solicitudes Llentas o Timeout

**Síntomas:**
- P99 latency > 2000ms
- Algunos usuarios ven timeout
- Logs muestran "WAITING FOR BD LOCK ⏳"

**Causas Posibles:**
1. Contención alta de concurrencia
2. Operación anterior en BD tarda mucho
3. Falta de índices

**Solución:**
```sql
-- Verificar índices
SHOW INDEXES FROM label_folio_sequence;
-- Debe haber índice en period_id

CREATE INDEX idx_period_id ON label_folio_sequence(period_id);

-- Verificar tabla locks
SHOW PROCESSLIST;
-- Buscar transacciones bloqueadas

-- Verificar performance
EXPLAIN SELECT * FROM label_folio_sequence WHERE period_id = 123;
```

### Problema 3: Deadlock Entre Servicios

**Síntomas:**
- Error: "Deadlock found when trying to get lock"
- Usuarios reciben 500 error intermitentemente
- Logs: "Waiting for table locks..."

**Causa:**
```
Thread A: Labels → Print Service
Thread B: Print Service → Labels
= CIRCULAR DEPENDENCY = DEADLOCK
```

**Solución:**
```java
// 1. Revisar orden de acceso (debe ser consistente)
// 2. No mezclar transacciones muy largas
@Transactional
public void generateBatchList(GenerateBatchListDTO dto, ...) {
    // 1. CORTO: allocateFolioRange()
    long[] range = persistence.allocateFolioRange(periodId, cantidad);
    
    // 2. CORTO: saveAll()
    persistence.saveAll(labels);
    
    // 3. NO: llamar a otro servicio que hace más transacciones
    // printService.print(range);  // ❌ MALO
}
```

### Problema 4: Base de Datos No Responde

**Síntomas:**
- Timeout en conexión BD
- "Connection refused" o "Connection timeout"
- Todos los usuarios afectados

**Checklist:**
```bash
# 1. Verificar conectividad
ping [host_bd]

# 2. Verificar puerto
telnet [host_bd] 3306

# 3. Verificar si la BD está corriendo
mysql -h [host_bd] -u [user] -p -e "SELECT 1"

# 4. Revisar logs de BD
tail -100 /var/log/mysql/error.log

# 5. Verificar pool de conexiones en application.yml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 20000  # 20 segundos
```

---

## 🚀 Mejoras Futuras

### 1. Agregación de Métricas (Corto Plazo)

```java
@Component
public class LabelGenerationMetrics {
    private final Timer allocateFolioTimer;
    private final Counter labelsGeneratedCounter;
    private final AtomicInteger concurrentAllocations;
    
    public void recordAllocationTime(long durationMs) {
        allocateFolioTimer.record(durationMs, TimeUnit.MILLISECONDS);
    }
    
    public void recordLabelsGenerated(int count) {
        labelsGeneratedCounter.increment(count);
    }
    
    public int getConcurrentAllocations() {
        return concurrentAllocations.get();
    }
}
```

### 2. Caché de Secuencia (Medio Plazo)

```java
// Reducir consultas a BD
public class CachedFolioSequence {
    private long nextFolio;
    private final int cacheSize = 1000;
    
    public synchronized long[] allocate(int quantity) {
        if (nextFolio + quantity > cachedUpto) {
            // Necesita refrescar
            refreshCacheFromDb();
        }
        long first = nextFolio;
        nextFolio += quantity;
        return new long[]{first, nextFolio - 1};
    }
}
```

### 3. Distributed Lock (Redis)

```java
// Para ambientes de cluster muy grandes
@Component
public class RedisDistributedLock {
    private final RedisTemplate<String, String> redis;
    
    public void executeWithLock(String key, Runnable task) throws Exception {
        String lockValue = UUID.randomUUID().toString();
        Boolean locked = redis.opsForValue().setIfAbsent(key, lockValue, 5, TimeUnit.SECONDS);
        
        if (locked) {
            try {
                task.run();
            } finally {
                String value = redis.opsForValue().get(key);
                if (lockValue.equals(value)) {
                    redis.delete(key);
                }
            }
        }
    }
}
```

### 4. Generación Pre-Asignada (Largo Plazo)

```java
// Anticipar y pre-generar folios
@Scheduled(fixedRate = 300000)  // Cada 5 minutos
public void preAllocateFolios() {
    // Si quedan menos de 1000 folios disponibles
    if (availableFolios() < 1000) {
        allocateFolios(10000);
        log.info("Pre-asignados 10000 folios");
    }
}
```

---

## 📋 Checklist de Producción

- [ ] ¿Está habilitado el log DEBUG para LabelService?
- [ ] ¿Existe índice en `label_folio_sequence.period_id`?
- [ ] ¿El pool de conexiones tiene mínimo 5 y máximo 20?
- [ ] ¿Hay alertas configuradas para duplicate keys?
- [ ] ¿Hay alertas configuradas para lock timeouts?
- [ ] ¿Se ha ejecutado test de concurrencia?
- [ ] ¿Se ha testeado con 20+ usuarios simultáneos?
- [ ] ¿Monitores de Grafana están activos?
- [ ] ¿Documentación de roles actualizada?
- [ ] ¿Plan de rollback en caso de error?

---

**Última actualización:** 2026-02-09
**Versión:** 1.0


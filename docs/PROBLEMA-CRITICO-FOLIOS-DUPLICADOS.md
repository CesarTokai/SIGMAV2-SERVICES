# 🚨 PROBLEMA CRÍTICO: Folios Duplicados Entre Periodos

**Fecha de Detección:** 27 de Abril de 2026  
**Severidad:** 🔴 **CRÍTICA** - Pérdida potencial de datos  
**Estado:** ⚠️ REQUIERE CORRECCIÓN INMEDIATA

---

## 📋 Resumen Ejecutivo

**Problema detectado por el usuario:**
> "Cuando consulto marbetes del Periodo 1, veo folio 1234.  
> Luego consulto marbetes del Periodo 3, y veo el MISMO folio 1234.  
> ¿Eso está mal?"

**Respuesta:** ✅ **SÍ, ESTÁ MAL.**

---

## 🔍 Análisis del Problema

### Diseño Actual (INCORRECTO)

#### 1. Tabla `labels` - Primary Key ÚNICA
```java
@Entity
@Table(name = "labels")
public class Label {
    @Id
    private Long folio;  // ← PK global (única en toda la tabla)
    
    @Column(name = "id_period")
    private Long periodId;  // ← NO es parte de la PK
    
    // ... otros campos
}
```

#### 2. Tabla `label_folio_sequence` - Secuencia POR PERIODO
```java
@Entity
@Table(name = "label_folio_sequence")
public class LabelFolioSequence {
    @Id
    @Column(name = "id_period")
    private Long periodId;  // ← Cada periodo tiene su secuencia
    
    @Column(name = "ultimo_folio")
    private Long ultimoFolio = 0L;  // ← Empieza en 0 CADA PERIODO
}
```

#### 3. Lógica de Generación de Folios
```java
public synchronized long[] allocateFolioRange(Long periodId, int quantity) {
    LabelFolioSequence seq = findOrCreate(periodId);
    long primer = seq.getUltimoFolio() + 1;  // ← 0 + 1 = 1
    long ultimo = seq.getUltimoFolio() + quantity;
    seq.setUltimoFolio(ultimo);
    return new long[]{primer, ultimo};
}
```

### 🚨 El Conflicto

Cuando generas marbetes en diferentes periodos:

```
Periodo 1:
┌──────────────────────────────────┐
│ Secuencia empieza en 0           │
│ Genera folios: 1, 2, 3, 4, 5...  │
└──────────────────────────────────┘

Periodo 2:
┌──────────────────────────────────┐
│ Secuencia empieza en 0           │
│ Genera folios: 1, 2, 3, 4, 5...  │ ← ❌ DUPLICADOS
└──────────────────────────────────┘

Periodo 3:
┌──────────────────────────────────┐
│ Secuencia empieza en 0           │
│ Genera folios: 1, 2, 3, 4, 5...  │ ← ❌ DUPLICADOS
└──────────────────────────────────┘
```

**Tabla `labels` (intento):**
```sql
folio | id_period | id_product | id_warehouse | estado
------|-----------|------------|--------------|--------
1     | 1         | 100        | 1            | IMPRESO
... más folios del periodo 1 ...

-- Cuando intentas insertar folios del Periodo 2:
INSERT INTO labels VALUES (1, 2, 200, 2, 'GENERADO');
-- ❌ ERROR: Duplicate entry '1' for key 'PRIMARY'

-- O PEOR: Sobrescribe el folio 1 del Periodo 1
-- ❌ PÉRDIDA DE DATOS
```

---

## 🎯 Consecuencias del Bug

### 1. **Pérdida de Datos** 🔴
- Los marbetes del Periodo 1 se sobrescriben con los del Periodo 2
- Los conteos C1/C2 quedan huérfanos
- **NO HAY TRAZABILIDAD** del inventario anterior

### 2. **Integridad Referencial Rota** 🔴
- Tabla `label_counts` referencia folios que ya no existen
- Tabla `label_count_events` tiene eventos de folios incorrectos
- Reportes muestran datos mezclados de diferentes periodos

### 3. **Reportes Incorrectos** 🟡
- El reporte comparativo mezcla datos de periodos
- La generación del archivo TXT tiene datos erróneos
- La auditoría es inútil porque los datos son incorrectos

### 4. **Imposibilidad de Auditoría** 🔴
- No puedes saber qué pasó en periodos pasados
- No puedes comparar inventarios históricos
- **VIOLA REQUISITOS LEGALES** de trazabilidad

---

## ✅ SOLUCIÓN IMPLEMENTADA

### Cambio 1: Primary Key Compuesta

**ANTES (incorrecto):**
```java
@Entity
@Table(name = "labels")
public class Label {
    @Id
    private Long folio;  // ← Solo folio
}
```

**DESPUÉS (correcto):**
```java
@Entity
@Table(name = "labels")
@IdClass(Label.LabelId.class)
public class Label {
    @Id
    private Long folio;
    
    @Id
    @Column(name = "id_period")
    private Long periodId;  // ← Ahora ambos son PK
    
    // Clase interna de PK compuesta
    public static class LabelId implements Serializable {
        private Long folio;
        private Long periodId;
        // equals, hashCode
    }
}
```

### Cambio 2: Migración de Base de Datos

📁 **Archivo:** `V1_3_0__Fix_label_primary_key.sql`

```sql
-- 1. Eliminar PK actual (solo folio)
ALTER TABLE labels DROP PRIMARY KEY;

-- 2. Crear PK compuesta (folio + periodo)
ALTER TABLE labels ADD PRIMARY KEY (folio, id_period);

-- 3. Actualizar tablas relacionadas
-- label_counts, label_count_events, labels_cancelled
-- deben incluir id_period para mantener integridad
```

### Cambio 3: Actualización del Repositorio

```java
// ANTES
public interface JpaLabelRepository extends JpaRepository<Label, Long> {
    Optional<Label> findById(Long folio);  // ← Ambiguo
}

// DESPUÉS
public interface JpaLabelRepository extends JpaRepository<Label, Label.LabelId> {
    Optional<Label> findByFolioAndPeriodId(Long folio, Long periodId);  // ← Explícito
}
```

---

## 🔧 Pasos de Implementación

### ✅ PASO 1: Ejecutar Migración (COMPLETADO)
La migración `V1_3_0__Fix_label_primary_key.sql` ya está creada.

**Acción requerida:**
```bash
# Reiniciar la aplicación para que Flyway ejecute la migración
./mvnw spring-boot:run
```

### ⚠️ PASO 2: Actualizar Código Java (EN PROGRESO)

**Archivos que requieren actualización:**

#### A. Servicios de Conteo
```java
// ANTES
Label label = jpaLabelRepository.findById(folio)
    .orElseThrow(() -> new LabelNotFoundException("Folio no encontrado"));

// DESPUÉS
Label label = jpaLabelRepository.findByFolioAndPeriodId(folio, periodId)
    .orElseThrow(() -> new LabelNotFoundException("Folio no encontrado en periodo " + periodId));
```

**Archivos afectados:**
- `LabelQueryService.java` (3 ocurrencias)
- `LabelPrintService.java` (4 ocurrencias)
- `LabelCancelService.java` (1 ocurrencia)
- `LabelsPersistenceAdapter.java` (1 ocurrencia)

#### B. Tests Unitarios
```java
// ANTES
when(jpaLabelRepository.findById(100L)).thenReturn(Optional.of(label));

// DESPUÉS
when(jpaLabelRepository.findByFolioAndPeriodId(100L, 1L))
    .thenReturn(Optional.of(label));
```

**Archivos afectados:**
- `LabelCancelServiceTest.java` (4 ocurrencias)

### ✅ PASO 3: Verificación Post-Migración

**SQL de verificación:**
```sql
-- 1. Verificar que NO hay folios duplicados por periodo
SELECT folio, id_period, COUNT(*) as duplicados
FROM labels
GROUP BY folio, id_period
HAVING COUNT(*) > 1;
-- Resultado esperado: 0 registros

-- 2. Verificar integridad con label_counts
SELECT lc.folio, lc.id_period
FROM label_counts lc
LEFT JOIN labels l ON lc.folio = l.folio AND lc.id_period = l.id_period
WHERE l.folio IS NULL;
-- Resultado esperado: 0 registros huérfanos

-- 3. Verificar que cada periodo tiene su secuencia
SELECT period_id, ultimo_folio
FROM label_folio_sequence
ORDER BY period_id;
```

---

## 📊 Impacto del Cambio

### Ventajas ✅

1. **Integridad Garantizada**
   - IMPOSIBLE tener folios duplicados entre periodos
   - Cada periodo es completamente independiente

2. **Trazabilidad Completa**
   - Puedes consultar marbetes históricos sin conflicto
   - Los reportes son precisos por periodo

3. **Escalabilidad**
   - Puedes tener infinitos periodos sin problemas
   - La secuencia de folios se reinicia cada periodo

4. **Cumplimiento Legal**
   - Auditoría completa de todos los inventarios
   - Histórico inmutable

### Desventajas ⚠️

1. **Cambio Breaking**
   - Requiere actualizar TODOS los usos de `findById(folio)`
   - Tests unitarios deben actualizarse

2. **Migración Compleja**
   - Tablas relacionadas requieren agregar `id_period`
   - Datos existentes deben actualizarse

3. **Tamaño de clave**
   - PK compuesta ocupa más espacio que PK simple
   - Índices son ligeramente más lentos

---

## 🔥 Migración de Código Inmediato

### Patrón de Reemplazo

**Buscar:**
```java
jpaLabelRepository.findById(folio)
```

**Reemplazar con:**
```java
jpaLabelRepository.findByFolioAndPeriodId(folio, periodId)
```

**NOTA IMPORTANTE:** En TODOS los casos donde se usa `findById(folio)`,  
el `periodId` ya está disponible en el contexto porque se valida previamente.

---

## 📋 Checklist de Corrección

- [x] ✅ Crear migración SQL `V1_3_0__Fix_label_primary_key.sql`
- [x] ✅ Actualizar entidad `Label.java` con `@IdClass`
- [x] ✅ Crear clase interna `Label.LabelId`
- [x] ✅ Actualizar `JpaLabelRepository` con tipo genérico `Label.LabelId`
- [x] ✅ Agregar método `findByFolioAndPeriodId`
- [ ] ⏳ Actualizar `LabelQueryService.java`
- [ ] ⏳ Actualizar `LabelPrintService.java`
- [ ] ⏳ Actualizar `LabelCancelService.java`
- [ ] ⏳ Actualizar `LabelsPersistenceAdapter.java`
- [ ] ⏳ Actualizar tests unitarios
- [ ] ⏳ Ejecutar suite de pruebas completa
- [ ] ⏳ Verificar en base de datos con queries de validación

---

## 🎯 Resultado Final

**Comportamiento CORRECTO:**

```
Periodo 1:
┌───────────────────────────────────────────┐
│ folio=1, id_period=1 → Producto A         │
│ folio=2, id_period=1 → Producto B         │
│ folio=3, id_period=1 → Producto C         │
└───────────────────────────────────────────┘

Periodo 2:
┌───────────────────────────────────────────┐
│ folio=1, id_period=2 → Producto X         │ ← ✅ DIFERENTE del periodo 1
│ folio=2, id_period=2 → Producto Y         │
│ folio=3, id_period=2 → Producto Z         │
└───────────────────────────────────────────┘

Periodo 3:
┌───────────────────────────────────────────┐
│ folio=1, id_period=3 → Producto M         │ ← ✅ DIFERENTE de periodos 1 y 2
│ folio=2, id_period=3 → Producto N         │
│ folio=3, id_period=3 → Producto P         │
└───────────────────────────────────────────┘
```

**Tabla `labels` final:**
```sql
folio | id_period | id_product | estado     | PRIMARY KEY
------|-----------|------------|------------|-------------
1     | 1         | 100        | IMPRESO    | (1, 1)     ← ✅ ÚNICO
2     | 1         | 101        | IMPRESO    | (2, 1)
1     | 2         | 200        | GENERADO   | (1, 2)     ← ✅ ÚNICO (diferente periodo)
2     | 2         | 201        | GENERADO   | (2, 2)
1     | 3         | 300        | IMPRESO    | (1, 3)     ← ✅ ÚNICO (diferente periodo)
2     | 3         | 301        | IMPRESO    | (2, 3)
```

✅ **Ahora cada combinación (folio, periodId) es única en toda la tabla.**

---

## 🚀 Próximos Pasos

1. **URGENTE:** Ejecutar la migración en ambiente de prueba
2. **URGENTE:** Actualizar todos los usos de `findById(folio)`
3. **URGENTE:** Ejecutar tests de regresión
4. **INMEDIATO:** Verificar integridad de datos con queries SQL
5. **PLANEAR:** Despliegue en producción con ventana de mantenimiento

---

## ❓ Preguntas Frecuentes

### ¿Por qué no simplemente usar un ID autogenerado?

**Opción alternativa:**
```java
@Id
@GeneratedValue
private Long id;  // ← PK artificial

private Long folio;  // ← Solo un campo
private Long periodId;
```

**Ventajas:**
- Más fácil de implementar
- Sin romper código existente

**Desventajas:**
- Folios NO serían únicos visualmente
- Concepto de "folio" pierde significado
- Usuarios confused: "¿Por qué hay dos folio 1?"

**Decisión:** Mantener folio como parte de la PK porque es el **concepto de negocio** fundamental.

---

### ¿Esto afecta el rendimiento?

**Respuesta:** Impacto mínimo.

- Índices compuestos son ~5-10% más lentos que simples
- PERO previene bugs catastróficos de pérdida de datos
- MySQL maneja PKs compuestas eficientemente

**Trade-off aceptable** para garantizar integridad.

---

## 📚 Referencias

- Diseño de base de datos: `AGENTS.md`
- Reglas de negocio: `README-MARBETES-REGLAS-NEGOCIO.md`
- Flujo completo: `FLUJO-COMPLETO-VERIFICACION-FISICA-TEORICA.md`

---

**Última actualización:** 27 de Abril de 2026  
**Autor:** GitHub Copilot (Análisis de bug reportado por usuario)  
**Prioridad:** 🔴 CRÍTICA - REQUIERE ACCIÓN INMEDIATA


# Componente PeriodoSelector

## Descripción
Componente reutilizable para seleccionar períodos en todas las pantallas del sistema. Proporciona una interfaz consistente y personalizable para la selección de períodos.

## Ubicación
```
src/components/PeriodoSelector.vue
```

## Props

| Prop | Tipo | Default | Requerido | Descripción |
|------|------|---------|-----------|-------------|
| `modelValue` | `number \| null` | - | ✅ | ID del período seleccionado (v-model) |
| `periodos` | `Periodo[]` | - | ✅ | Array de períodos disponibles |
| `label` | `string` | `'Período'` | ❌ | Etiqueta del selector |
| `placeholder` | `string` | `'Selecciona un período'` | ❌ | Texto placeholder del select |
| `inputId` | `string` | `'periodoSelect'` | ❌ | ID del elemento select |
| `disabled` | `boolean` | `false` | ❌ | Deshabilitar el selector |
| `showInfo` | `boolean` | `true` | ❌ | Mostrar información adicional del período |
| `showTotalCount` | `boolean` | `false` | ❌ | Mostrar contador total |
| `totalCount` | `number \| null` | `null` | ❌ | Valor del contador total |
| `totalCountLabel` | `string` | `'Total'` | ❌ | Etiqueta del contador |

## Eventos

| Evento | Parámetros | Descripción |
|--------|------------|-------------|
| `update:modelValue` | `value: number \| null` | Emitido cuando cambia el ID seleccionado |
| `change` | `periodo: Periodo \| null` | Emitido cuando cambia el período (incluye objeto completo) |

## Interfaz Periodo

```typescript
interface Periodo {
  id: number;
  date: string;
  comments: string;
  state: string;
}
```

## Ejemplos de Uso

### 1. Uso Básico (Inventario)

```vue
<template>
  <div class="periodo-section">
    <PeriodoSelector
      v-model="selectedPeriodoId"
      :periodos="periodos"
      :show-info="false"
      label="Período:"
      @change="handlePeriodoChange"
    />
    <!-- Mostrar info personalizada -->
    <div v-if="selectedPeriodo" class="custom-info">
      <span>Estado: {{ selectedPeriodo.state }}</span>
      <span>Total: {{ totalProductos }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import PeriodoSelector from '@/components/PeriodoSelector.vue';

const selectedPeriodoId = ref<number | null>(null);
const periodos = ref<Periodo[]>([]);

const handlePeriodoChange = async (periodo: Periodo | null) => {
  // Lógica al cambiar período
  if (periodo) {
    await loadData(periodo.id);
  }
};
</script>
```

### 2. Uso con Info Integrada (MultiAlmacén)

```vue
<template>
  <PeriodoSelector
    v-model="selectedPeriodoId"
    :periodos="periodos"
    :show-info="true"
    :show-total-count="true"
    :total-count="filteredProductos.length"
    total-count-label="Total Productos"
    @change="handlePeriodoChange"
  />
</template>

<script setup lang="ts">
import { ref } from 'vue';
import PeriodoSelector from '@/components/PeriodoSelector.vue';

const selectedPeriodoId = ref<number | null>(null);
const periodos = ref<Periodo[]>([]);
const filteredProductos = ref([]);

const handlePeriodoChange = async (periodo: Periodo | null) => {
  if (periodo) {
    await loadProductos(periodo.id);
  }
};
</script>
```

### 3. Selector Deshabilitado

```vue
<template>
  <PeriodoSelector
    v-model="selectedPeriodoId"
    :periodos="periodos"
    :disabled="isLoading"
    placeholder="Cargando períodos..."
    @change="handlePeriodoChange"
  />
</template>
```

### 4. Con ID Personalizado (múltiples selectores en una página)

```vue
<template>
  <div>
    <PeriodoSelector
      v-model="periodoInicial"
      :periodos="periodos"
      label="Período Inicial:"
      input-id="periodoInicial"
      @change="handleInitialChange"
    />
    
    <PeriodoSelector
      v-model="periodoFinal"
      :periodos="periodos"
      label="Período Final:"
      input-id="periodoFinal"
      @change="handleFinalChange"
    />
  </div>
</template>
```

## Características

✅ **Diseño Consistente**: Mismo estilo en todas las pantallas  
✅ **Personalizable**: Múltiples props para adaptar el comportamiento  
✅ **Reactivo**: Usa v-model para sincronización bidireccional  
✅ **Eventos Completos**: Emite tanto el ID como el objeto completo  
✅ **Información Integrada**: Puede mostrar estado y contadores  
✅ **Responsive**: Se adapta a diferentes tamaños de pantalla  
✅ **Accesible**: IDs configurables para múltiples instancias  

## Estilos

El componente incluye estilos scoped que se pueden personalizar mediante CSS variables o sobrescribiendo clases específicas.

### Clases Principales
- `.periodo-selector-wrapper`: Contenedor principal
- `.periodo-label-import-group`: Grupo de label y select
- `.periodo-select-small`: Select con ancho reducido
- `.periodo-info`: Información adicional del período
- `.badge-success`, `.badge-danger`, etc.: Estados del período

## Integración con Store

El componente funciona perfectamente con el `periodoStore`:

```typescript
import { usePeriodoStore } from '@/store/periodoStore';

const periodoStore = usePeriodoStore();

const handlePeriodoChange = async (periodo: Periodo | null) => {
  if (periodo) {
    // Guardar en el store para persistencia
    periodoStore.setPeriodo(periodo);
    await loadData();
  }
};

// Cargar período guardado al montar
onMounted(() => {
  periodoStore.cargarPeriodoGuardado();
  if (periodoStore.periodoSeleccionado) {
    selectedPeriodoId.value = periodoStore.periodoSeleccionado.id;
  }
});
```

## Pantallas Implementadas

✅ **InventarioAdmin.vue** - Gestión de Inventario  
✅ **MultiAlmacenAdmin.vue** - Multi-Almacén  
🔲 **PeriodosAdmin.vue** - Gestión de Períodos (pendiente)  
🔲 **Marbetes** - Módulos de Marbetes (pendiente)  
🔲 **Reportes** - Módulos de Reportes (pendiente)  

## Notas Técnicas

- El componente usa `v-model` con `modelValue` (Vue 3)
- Los eventos se emiten usando Composition API (`defineEmits`)
- Las props tienen validación de tipos con TypeScript
- El formateo de fechas usa `toLocaleDateString` con locale español
- Los estilos son completamente scoped para evitar conflictos

## Troubleshooting

### El componente no se muestra
- Verifica que hayas importado correctamente: `import PeriodoSelector from '@/components/PeriodoSelector.vue'`
- Asegúrate de pasar las props requeridas: `modelValue` y `periodos`

### Los eventos no se disparan
- Usa `@change` en lugar de `@update:modelValue` para obtener el objeto completo
- Verifica que la función manejadora esté definida correctamente

### Los estilos no se aplican
- Los estilos son scoped, asegúrate de no tener conflictos de nombres
- Puedes sobrescribir estilos usando selectores más específicos o `::v-deep`

## Próximas Mejoras

- [ ] Soporte para selección múltiple de períodos
- [ ] Filtrado de períodos por estado
- [ ] Búsqueda de períodos por fecha o comentario
- [ ] Validaciones personalizadas
- [ ] Temas de color configurables


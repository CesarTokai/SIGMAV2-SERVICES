# Implementación de Periodo Global

## Objetivo
Permitir que al seleccionar un periodo en **PeriodosAdmin**, este quede guardado globalmente y se use automáticamente en las pantallas de:
- ConsultaCaptura
- ImpresionMarbetes
- ConteoMarbetes

## Componentes Modificados

### 1. **periodoStore.ts** (Ya existente)
El store ya tenía la funcionalidad de:
- Guardar periodo seleccionado en estado global
- Persistir en localStorage
- Cargar periodo guardado desde localStorage

```typescript
export const usePeriodoStore = defineStore('periodo', {
  state: () => ({
    periodoSeleccionado: null as null | {
      id: number;
      date: string;
      comments: string;
      state: string;
    }
  }),
  actions: {
    setPeriodo(periodo: any) {
      this.periodoSeleccionado = periodo;
      localStorage.setItem('periodoSeleccionado', JSON.stringify(periodo));
    },
    cargarPeriodoGuardado() {
      const guardado = localStorage.getItem('periodoSeleccionado');
      if (guardado) {
        this.periodoSeleccionado = JSON.parse(guardado);
      }
    }
  }
});
```

### 2. **PeriodosAdmin.vue** (Ya implementado)
Ya tenía la funcionalidad de:
- Llamar `handlePeriodoChange()` cuando cambia el selector
- Guardar el periodo en el store mediante `periodoStore.setPeriodo(periodo)`

### 3. **ConteoMarbetes.vue** ✅ ACTUALIZADO
**Cambios realizados:**
- ✅ Importado `usePeriodoStore`
- ✅ Inicializado el store
- ✅ Modificado `loadPeriodos()` para cargar el periodo del store al montar
- ✅ Agregado watcher para guardar en el store cuando el usuario cambie el periodo manualmente

**Código agregado:**
```typescript
import { usePeriodoStore } from '@/store/periodoStore';

const periodoStore = usePeriodoStore();

// En loadPeriodos
periodoStore.cargarPeriodoGuardado();
if (periodoStore.periodoSeleccionado) {
  const periodoGuardado = periodos.value.find(p => p.id === periodoStore.periodoSeleccionado?.id);
  if (periodoGuardado) {
    selectedPeriodo.value = periodoGuardado;
    selectedPeriodoId.value = periodoGuardado.id;
    console.log('✅ Periodo cargado desde store:', periodoGuardado);
  }
}

// Watcher actualizado
watch(selectedPeriodoId, (newId) => {
  if (newId !== null) {
    selectedPeriodo.value = periodos.value.find(p => p.id === newId) || null;
    if (selectedPeriodo.value) {
      periodoStore.setPeriodo(selectedPeriodo.value);
      console.log('✅ Periodo guardado en store:', selectedPeriodo.value);
    }
  }
});
```

### 4. **ConsultaCaptura.vue** ✅ ACTUALIZADO
**Cambios realizados:**
- ✅ Importado `usePeriodoStore`
- ✅ Inicializado el store
- ✅ Modificado `loadPeriodos()` para cargar el periodo del store al montar
- ✅ Actualizado watcher existente para guardar en el store cuando cambie el periodo

**Código agregado:**
```typescript
import { usePeriodoStore } from '@/store/periodoStore';

const periodoStore = usePeriodoStore();

// En loadPeriodos
periodoStore.cargarPeriodoGuardado();
if (periodoStore.periodoSeleccionado) {
  const periodoGuardado = periodos.value.find(p => p.id === periodoStore.periodoSeleccionado?.id);
  if (periodoGuardado) {
    selectedPeriodo.value = periodoGuardado;
    selectedPeriodoId.value = periodoGuardado.id;
    console.log('✅ Periodo cargado desde store:', periodoGuardado);
  }
}

// Watcher actualizado
watch(selectedPeriodoId, async (newId) => {
  // ...código existente...
  const found = periodos.value.find(p => p.id === Number(newId)) || null;
  selectedPeriodo.value = found;

  if (found) {
    periodoStore.setPeriodo(found);
    console.log('✅ Periodo guardado en store:', found);
  }
  // ...resto del código...
});
```

### 5. **ImpresionMarbetes.vue** ✅ ACTUALIZADO
**Cambios realizados:**
- ✅ Importado `usePeriodoStore`
- ✅ Inicializado el store
- ✅ Modificado `loadPeriodos()` para cargar el periodo del store al montar
- ✅ Actualizado watcher existente para guardar en el store cuando cambie el periodo

**Código agregado:**
```typescript
import { usePeriodoStore } from '@/store/periodoStore';

const periodoStore = usePeriodoStore();

// En loadPeriodos
periodoStore.cargarPeriodoGuardado();
if (periodoStore.periodoSeleccionado) {
  const periodoGuardado = periodos.value.find(p => p.id === periodoStore.periodoSeleccionado?.id);
  if (periodoGuardado) {
    selectedPeriodo.value = periodoGuardado;
    selectedPeriodoId.value = periodoGuardado.id;
    console.log('✅ Periodo cargado desde store:', periodoGuardado);
  }
}

// Watcher actualizado
watch(selectedPeriodoId, async (newId) => {
  // ...código existente...
  selectedPeriodo.value = periodos.value.find(p => p.id === Number(newId)) || null;
  
  if (selectedPeriodo.value) {
    periodoStore.setPeriodo(selectedPeriodo.value);
    console.log('✅ Periodo guardado en store:', selectedPeriodo.value);
  }
  // ...resto del código...
});
```

## Flujo de Funcionamiento

### Escenario 1: Usuario selecciona periodo en PeriodosAdmin
1. Usuario selecciona un periodo en el selector de PeriodosAdmin
2. Se dispara `@change="handlePeriodoChange"`
3. `handlePeriodoChange()` llama a `periodoStore.setPeriodo(periodo)`
4. El periodo se guarda en el estado global y en localStorage
5. Usuario navega a ConsultaCaptura/ImpresionMarbetes/ConteoMarbetes
6. Al montar, estas pantallas llaman a `periodoStore.cargarPeriodoGuardado()`
7. El periodo guardado se carga automáticamente en el selector

### Escenario 2: Usuario cambia periodo manualmente en otra pantalla
1. Usuario está en ConteoMarbetes (por ejemplo)
2. Cambia el periodo en el selector
3. El watcher detecta el cambio
4. Se actualiza el periodo local Y se guarda en el store
5. El cambio queda persistido para otras pantallas

### Escenario 3: Usuario recarga la página
1. La página se recarga
2. Al montar, `periodoStore.cargarPeriodoGuardado()` lee de localStorage
3. El ultimo periodo seleccionado se restaura automáticamente

## Beneficios

✅ **Consistencia**: El mismo periodo se mantiene en todas las pantallas
✅ **Persistencia**: El periodo se guarda incluso al recargar la página
✅ **Usabilidad**: El usuario no tiene que seleccionar el periodo en cada pantalla
✅ **Centralizado**: Un único punto de verdad para el periodo seleccionado
✅ **Bidireccional**: Funciona tanto desde PeriodosAdmin como desde las otras pantallas

## Logs de Debug

En la consola del navegador verás mensajes como:
- `✅ Periodo cargado desde store: {id: 1, date: "2024-01-15", ...}`
- `✅ Periodo guardado en store: {id: 2, date: "2024-02-20", ...}`

Estos logs ayudan a verificar que el sistema funciona correctamente.

## Pruebas Sugeridas

1. **Prueba básica:**
   - Seleccionar periodo en PeriodosAdmin
   - Navegar a ConteoMarbetes
   - Verificar que el periodo esté seleccionado

2. **Prueba de persistencia:**
   - Seleccionar periodo en cualquier pantalla
   - Recargar la página
   - Verificar que el periodo sigue seleccionado

3. **Prueba de sincronización:**
   - Cambiar periodo en ConsultaCaptura
   - Navegar a ImpresionMarbetes
   - Verificar que el nuevo periodo esté seleccionado

4. **Prueba de localStorage:**
   - Abrir DevTools > Application > Local Storage
   - Verificar que existe la clave `periodoSeleccionado`
   - Ver que se actualiza al cambiar el periodo


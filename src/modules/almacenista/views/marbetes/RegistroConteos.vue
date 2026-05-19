<script setup lang="ts">
import { ref, onMounted } from 'vue';
import axiosConfiguration from '@/config/axiosConfig';
import { ToastError } from '@/utils/SweetAlert';
import RegistroConteos from '@/components/RegistroConteos.vue';
import { usePeriodoStore } from '@/store/periodoStore';

interface Periodo {
  id: number;
  date: string;
  comments: string;
  state: string;
}

interface Almacen {
  id: number;
  clave: string;
  almacenname: string;
  activo: boolean;
}

const periodoStore = usePeriodoStore();

const periodos = ref<Periodo[]>([]);
const almacenes = ref<Almacen[]>([]);
const selectedPeriodoId = ref<number | null>(null);
const selectedAlmacenId = ref<number | null>(null);

// Cargar períodos
const loadPeriodos = async () => {
  try {
    const response = await axiosConfiguration.doGet('/periods?page=0&size=100');
    periodos.value = response.data.content || [];

    periodoStore.cargarPeriodoGuardado();

    if (periodoStore.periodoSeleccionado) {
      selectedPeriodoId.value = periodoStore.periodoSeleccionado.id;
    } else if (periodos.value.length > 0 && periodos.value[0]) {
      selectedPeriodoId.value = periodos.value[0]!.id;
    }
  } catch (error) {
    console.error('Error al cargar períodos:', error);
    ToastError('Error', 'No se pudieron cargar los períodos');
  }
};

// Cargar almacenes
const loadAlmacenes = async () => {
  try {
    // Cargar solo almacenes asignados al usuario actual
    const response = await axiosConfiguration.doGet('/warehouses/my-warehouses');
    const data = Array.isArray(response.data) ? response.data : (response.data?.data || []);
    almacenes.value = data.map((item: any) => ({
      id: item.id,
      clave: String(item.warehouseKey || ''),
      almacenname: String(item.nameWarehouse || ''),
      activo: !item.deleted
    })) as Almacen[];
    if (almacenes.value.length > 0 && almacenes.value[0]) {
      selectedAlmacenId.value = almacenes.value[0]!.id;
    }
  } catch (error) {
    console.error('Error al cargar almacenes:', error);
    ToastError('Error', 'No se pudieron cargar los almacenes');
  }
};

onMounted(async () => {
  await loadPeriodos();
  await loadAlmacenes();
});
</script>

<template>
  <div class="registro-conteos-page">
    <RegistroConteos
      :periodos="periodos"
      :almacenes="almacenes"
      :selectedPeriodoId="selectedPeriodoId"
      :selectedAlmacenId="selectedAlmacenId"
    />
  </div>
</template>

<style scoped>
.registro-conteos-page {
  padding: 20px;
}
</style>

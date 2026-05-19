import { defineStore } from 'pinia';

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


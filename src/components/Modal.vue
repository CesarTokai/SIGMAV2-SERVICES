<template>
  <b-modal :visible="isVisible" @hide="closeModal">
    <!-- Header Slot -->
    <template #modal-title>
      {{ title }}
    </template>

    <!-- Default Slot (Content) -->
    <div>
      <slot>
        {{ message }}
      </slot>
    </div>

    <!-- Footer Slot -->
    <template #modal-footer>
      <b-button variant="primary" @click="aceptModal">
        {{ textButton }}
      </b-button>
      <b-button variant="secondary" @click="closeModal">Cerrar</b-button>
    </template>
  </b-modal>
</template>

<script lang="ts">
import { defineComponent } from 'vue';

export default defineComponent({
  name: 'Modal',
  props: {
    title: { type: String, required: true },
    message: { type: String, required: true },
    isVisible: { type: Boolean, default: false },
    textButton: { type: String, default: 'Aceptar' }
  },
  emits: ['close', 'accept'],
  methods: {
    closeModal() {
      this.$emit('close');
    },
    aceptModal() {
      this.$emit('accept');
    }
  }
});
</script>

<style scoped>
</style>

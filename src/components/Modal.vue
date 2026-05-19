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
export default Vue.extend({
  props: {
    title: {
      type: String,
      required: true
    },
    message: {
      type: String,
      required: true
    },
    isVisible: {
      type: Boolean,
      default: false
    },
    textButton: {
      type: String,
      default: 'Aceptar'
    }
  },
});
</script>

<style scoped>
</style>

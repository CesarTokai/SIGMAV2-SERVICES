<template>
  <div class="searchbar-group">
    <input
      type="text"
      class="searchbar-input"
      :placeholder="placeholder"
      :value="modelValue"
      @input="$emit('update:modelValue', ($event.target as HTMLInputElement)?.value)"
    >
    <span class="searchbar-icon bi bi-search"></span>
  </div>
</template>

<script lang="ts">
export default {
  name: 'SearchBar',
  props: {
    placeholder: {
      type: String,
      default: 'Buscar...'
    },
    modelValue: {
      type: String,
      default: ''
    }
  }
}
</script>

<style scoped>
.searchbar-group {
  position: relative;
  width: 100%;
  display: flex;
  align-items: center;
  max-width: 220px; /* Más pequeño */
}

.searchbar-input {
  width: 100%;
  padding: 6px 32px 6px 10px; /* Menor padding */
  font-size: 13px; /* Más pequeño */
  border: 1px solid #d1d5db;
  border-radius: 6px;
  background: #fafbfc;
  color: #222;
  outline: none;
  box-shadow: none;
  transition: border-color 0.2s;
}

.searchbar-input:focus {
  border-color: #667eea;
  background: #fff;
}

.searchbar-icon {
  position: absolute;
  right: 10px;
  top: 50%;
  transform: translateY(-50%);
  color: #667eea;
  font-size: 16px; /* Más pequeño */
  pointer-events: none;
  opacity: 0.7;
}
</style>
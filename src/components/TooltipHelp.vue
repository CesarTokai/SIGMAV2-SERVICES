<template>
  <span class="tooltip-help">
    <span class="tooltip-icon" :title="text" :class="{ 'warning': type === 'warning', 'info': type === 'info' }">
      {{ icon }}
    </span>
  </span>
</template>

<script setup lang="ts">
import { computed } from 'vue';

interface Props {
  text: string;
  type?: 'info' | 'warning' | 'error';
}

const props = withDefaults(defineProps<Props>(), {
  type: 'info'
});

const icon = computed(() => {
  switch (props.type) {
    case 'warning':
      return '⚠️';
    case 'error':
      return '❌';
    default:
      return '❓';
  }
});
</script>

<style scoped>
.tooltip-help {
  display: inline-flex;
  align-items: center;
  margin-left: 6px;
}

.tooltip-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  font-size: 12px;
  cursor: help;
  transition: transform 0.2s ease;
  user-select: none;
}

.tooltip-icon:hover {
  transform: scale(1.2);
}

.tooltip-icon.info {
  opacity: 0.7;
}

.tooltip-icon.warning {
  opacity: 0.9;
}

/* Tooltip personalizado */
.tooltip-icon[title] {
  position: relative;
}

.tooltip-icon[title]:hover::after {
  content: attr(title);
  position: absolute;
  bottom: 100%;
  left: 50%;
  transform: translateX(-50%);
  padding: 8px 12px;
  background-color: rgba(0, 0, 0, 0.9);
  color: white;
  border-radius: 6px;
  font-size: 12px;
  white-space: nowrap;
  max-width: 300px;
  z-index: 1000;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.2);
  margin-bottom: 8px;
  line-height: 1.4;
}

.tooltip-icon[title]:hover::before {
  content: '';
  position: absolute;
  bottom: 100%;
  left: 50%;
  transform: translateX(-50%);
  border: 6px solid transparent;
  border-top-color: rgba(0, 0, 0, 0.9);
  margin-bottom: 2px;
  z-index: 1000;
}

@media (max-width: 768px) {
  .tooltip-icon[title]:hover::after {
    max-width: 200px;
    white-space: normal;
  }
}
</style>

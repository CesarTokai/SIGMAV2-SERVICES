<template>
  <b-button :variant="variant" @click="navigate">{{ displayText }}</b-button>
</template>

<!-- Usage:
This button will navigate to the route specified in the to prop when clicked and display the text specified in the displayText prop.
props: to, displayText

<NavigationButton to="/about" displayText="About"/>
-->

<script lang="ts">
import { defineComponent } from "vue";

export default defineComponent({
  name: "NavigationButton",
  props: {
    to: {
      type: String,
      required: true,
      default: "/",
    },
    displayText: {
      type: String,
      required: true,
      default: "Placeholder",
    },
    variant: {
      type: String,
      default: "primary",
    },
  },
  methods: {
    navigate(): void {
      // @ts-ignore
      (this as any).$router.push((this as any).to);
    },
  },
});
</script>

<style scoped>
.btn-primary {
  background-color: var(--primary-color);
  color: var(--primary-color-text);
}
</style>
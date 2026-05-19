<template>
  <div class="admin-layout">
    <AdminSidebar />
    <div class="content">
      <router-view />
    </div>
  </div>
</template>

<script lang="ts">
import AdminSidebar from '@/components/AdminSidebar.vue';

export default {
  components: {
    AdminSidebar,
  },
};
</script>

<style scoped>
.admin-layout {
  display: flex;
  min-height: 100vh;
}

.admin-layout > .content {
  flex: 1;
  padding: 1rem;
}
</style>
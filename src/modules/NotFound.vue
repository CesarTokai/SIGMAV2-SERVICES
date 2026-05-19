<template>
  <div class="not-found-container">
    <h1>404 - Page Not Found</h1>
    <p>We're sorry, but the page you are looking for does not exist.</p>
  </div>
</template>

<script>
export default {
  name: "NotFoundView",
};
</script>

<style scoped>
.not-found-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100vh;
  text-align: center;
}

.not-found-container h1 {
  font-size: 3rem;
  margin-bottom: 1rem;
}

.not-found-container p {
  font-size: 1.5rem;
  margin-bottom: 2rem;
}

.not-found-container a {
  font-size: 1.2rem;
  text-decoration: none;
  color: #3498db;
}
</style>
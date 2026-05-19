import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// https://vite.dev/config/
export default defineConfig({
  base: '/sigmav2/',
  plugins: [
    vue(),
    vueDevTools(),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  build: {
    chunkSizeWarningLimit: 600,
    rollupOptions: {
      output: {
        manualChunks: {
          // Librerías de UI y utilidades
          'vendor-vue': ['vue', 'vue-router', 'pinia'],
          'vendor-ui': ['sweetalert2', 'bootstrap-vue-next', 'bootstrap'],
          'vendor-http': ['axios'],
        }
      }
    }
  }
})

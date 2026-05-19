<template>
  <div class="recovery-container">
    <!-- Background decoration -->
    <div class="background-shapes">
      <div class="shape shape-1"></div>
      <div class="shape shape-2"></div>
      <div class="shape shape-3"></div>
    </div>

    <div class="recovery-card">
      <!-- Header section -->
      <div class="card-header">
        <div class="logo-container">
          <img src="/Logo.png" alt="SIGMA V2" class="logo" />
        </div>
        <h1 class="title">Recuperar Contraseña</h1>
        <p class="subtitle">
          Ingresa tu correo electrónico y te ayudaremos a recuperar el acceso a tu cuenta
        </p>
      </div>

      <!-- Form section -->
      <div class="card-body">
        <form @submit.prevent="handleSubmit" class="recovery-form">
          <div class="input-group">
            <label for="email" class="input-label">
              <i class="fas fa-envelope"></i>
              Correo Electrónico
            </label>
            <div class="input-wrapper">
              <input
                id="email"
                v-model="form.email"
                type="email"
                placeholder="ejemplo@dominio.com"
                class="form-input"
                :class="{ 'error': errors.email, 'success': isemailValid && form.email }"
                @input="validateemail"
                @blur="validateemail"
                :disabled="isLoading"
              />
              <div class="input-icons">
                <i v-if="isemailValid && form.email" class="fas fa-check success-icon"></i>
                <i v-if="errors.email" class="fas fa-times error-icon"></i>
              </div>
            </div>
            <div v-if="errors.email" class="error-message">
              <i class="fas fa-exclamation-circle"></i>
              {{ errors.email }}
            </div>
          </div>

          <button
            type="submit"
            class="submit-btn"
            :disabled="!isemailValid || isLoading"
            :class="{ 'loading': isLoading }"
          >
            <span v-if="!isLoading" class="btn-content">
              <i class="fas fa-paper-plane"></i>
              Solicitar recuperacion de contraseña
            </span>
            <span v-else class="btn-content">
              <i class="fas fa-spinner spinning"></i>
              Verificando...
            </span>
          </button>
        </form>

        <!-- Back button -->
        <div class="form-footer">
          <button @click="goToLogin" class="back-btn" :disabled="isLoading">
            <i class="fas fa-arrow-left"></i>
            Volver al Inicio de Sesión
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import axios from '../../../config/axiosConfig'
import Swal from 'sweetalert2'

const router = useRouter()

// Reactive data
const form = ref({
  email: ''
})

const errors = ref({
  email: ''
})

const isLoading = ref(false)

// Computed properties
const isemailValid = computed(() => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  return emailRegex.test(form.value.email) && !errors.value.email
})

// Methods
const validateemail = () => {
  if (!form.value.email.trim()) {
    errors.value.email = 'El email es obligatorio'
    return false
  }

  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/
  if (!emailRegex.test(form.value.email)) {
    errors.value.email = 'El formato del email no es válido'
    return false
  }

  errors.value.email = ''
  return true
}

const handleSubmit = async () => {
  if (!validateemail()) {
    return
  }

  isLoading.value = true

  try {
    // Ajusta la ruta si tu endpoint es diferente (por ejemplo: /request-recovery-password/verifyUser)
    const response = await axios.doPost('/auth/createRequest', {
      email: form.value.email
    })

    console.log(response)

    // Éxito: el backend devuelve un JSON { success: true, message: "..." }
    if (response?.status === 200 || response?.status === 201) {
      // Mostrar el mensaje de éxito
      await Swal.fire({
        icon: 'success',
        title: '¡Solicitud Enviada!',
        html: `
          <p style="margin-bottom: 12px;">${response.data?.message || 'Se ha enviado la solicitud de recuperación exitosamente.'}</p>
          <p style="color: #6b7280; font-size: 0.9rem;">Revisa tu correo electrónico para continuar con el proceso.</p>
        `,
        confirmButtonText: 'Ir al Login',
        confirmButtonColor: '#7fb79b',
        allowOutsideClick: false,
        allowEscapeKey: false
      })

      // Después de que el usuario cierre el alert, redirigir al login
      router.push({ name: 'login' })
    }
  } catch (error: any) {
    console.error('Error al verificar usuario:', error)

    // Extraer mensaje enviado por el backend: error.response.data.message
    const apiBody = error?.response?.data ?? null
    const apiMessage = apiBody?.message ?? apiBody?.error ?? null

    if (apiMessage) {
      // Mostrar mensaje específico del servidor
      await Swal.fire({
        icon: 'error',
        title: apiBody?.success === false ? 'Error' : 'Atención',
        text: String(apiMessage),
        confirmButtonText: 'Entendido',
        confirmButtonColor: '#ef4444'
      })
    } else if (error?.response?.status === 404) {
      await Swal.fire({
        icon: 'error',
        title: 'Usuario no encontrado',
        text: 'No se encontró ningún usuario con ese correo electrónico.',
        confirmButtonText: 'Intentar de nuevo',
        confirmButtonColor: '#ef4444'
      })
    } else {
      await Swal.fire({
        icon: 'error',
        title: 'Error',
        text: 'Ocurrió un error al procesar la solicitud. Por favor, inténtalo de nuevo.',
        confirmButtonText: 'Entendido',
        confirmButtonColor: '#ef4444'
      })
    }
  } finally {
    isLoading.value = false
  }
}

const goToLogin = () => {
  router.push({ name: 'login' })
}

// Lifecycle
onMounted(() => {
  const emailInput = document.getElementById('email')
  if (emailInput) {
    emailInput.focus()
  }
})
</script>



<style scoped>
* {
  box-sizing: border-box;
}

.recovery-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #7fb79b 0%, #2a6a55 100%);
  padding: 20px;
  position: relative;
  overflow: hidden;
}

/* Background shapes for visual appeal */
.background-shapes {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 1;
}

.shape {
  position: absolute;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
}

.shape-1 {
  width: 200px;
  height: 200px;
  top: 10%;
  left: 10%;
  animation: float 6s ease-in-out infinite;
}

.shape-2 {
  width: 150px;
  height: 150px;
  top: 60%;
  right: 15%;
  animation: float 4s ease-in-out infinite reverse;
}

.shape-3 {
  width: 100px;
  height: 100px;
  bottom: 20%;
  left: 20%;
  animation: float 5s ease-in-out infinite;
}

@keyframes float {
  0%, 100% { transform: translateY(0px); }
  50% { transform: translateY(-20px); }
}

/* Main card */
.recovery-card {
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 40px;
  width: 100%;
  max-width: 480px;
  box-shadow: 0 25px 50px rgba(0, 0, 0, 0.25);
  border: 1px solid rgba(255, 255, 255, 0.2);
  position: relative;
  z-index: 2;
  transition: transform 0.3s ease;
}

.recovery-card:hover {
  transform: translateY(-5px);
}

/* Header section */
.card-header {
  text-align: center;
  margin-bottom: 32px;
}

.logo-container {
  margin-bottom: 24px;
}

.logo {
  width: 80px;
  height: auto;
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, 0.1));
}

.title {
  font-size: 2rem;
  font-weight: 700;
  color: #1f2937;
  margin: 0 0 12px 0;
  background: linear-gradient(135deg, #2f6f5b, #7fb79b);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.subtitle {
  color: #6b7280;
  font-size: 0.95rem;
  line-height: 1.5;
  max-width: 300px;
  margin: 0 auto;
}

/* Form styles */
.recovery-form {
  margin-bottom: 24px;
}

.input-group {
  margin-bottom: 24px;
}

.input-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 8px;
  font-size: 0.9rem;
}

.input-label i {
  color: #667eea;
  font-size: 0.9rem;
}

.input-wrapper {
  position: relative;
  width: 100%;
}

.form-input {
  width: 100%;
  padding: 16px 50px 16px 16px;
  border: 2px solid #e5e7eb;
  border-radius: 12px;
  font-size: 1rem;
  background: #ffffff;
  transition: all 0.3s ease;
  outline: none;
}

.form-input:focus {
  border-color: #7fb79b;
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.1);
  transform: translateY(-1px);
}

.form-input.success {
  border-color: #10b981;
  background: #f0fdf4;
}

.form-input.error {
  border-color: #ef4444;
  background: #fef2f2;
}

.form-input:disabled {
  background: #f9fafb;
  cursor: not-allowed;
  opacity: 0.6;
}

.input-icons {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  display: flex;
  align-items: center;
}

.success-icon {
  color: #10b981;
  font-size: 1.1rem;
}

.error-icon {
  color: #ef4444;
  font-size: 1.1rem;
}

.error-message {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #ef4444;
  font-size: 0.85rem;
  margin-top: 6px;
  padding: 8px 12px;
  background: #fef2f2;
  border-radius: 8px;
  border: 1px solid #fecaca;
}

/* Buttons */
.submit-btn {
  width: 100%;
  padding: 16px;
  background: linear-gradient(135deg, #7fb79b, #2f6f5b);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 10px 25px rgba(102, 126, 234, 0.4);
}

.submit-btn:active:not(:disabled) {
  transform: translateY(0);
}

.submit-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}

.submit-btn.loading {
  pointer-events: none;
}

.btn-content {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.spinning {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

/* Footer buttons */
.form-footer {
  text-align: center;
  margin-top: 20px;
}

.back-btn {
  background: transparent;
  color: #6b7280;
  border: none;
  padding: 12px 16px;
  border-radius: 8px;
  font-size: 0.9rem;
  cursor: pointer;
  transition: all 0.3s ease;
  display: inline-flex;
  align-items: center;
  gap: 6px;
}

.back-btn:hover:not(:disabled) {
  background: #f3f4f6;
  color: #374151;
}

.back-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}


/* Responsive design */
@media (max-width: 640px) {
  .recovery-container {
    padding: 16px;
  }

  .recovery-card {
    padding: 24px;
  }

  .title {
    font-size: 1.75rem;
  }

  .subtitle {
    font-size: 0.9rem;
  }

  .form-input {
    padding: 14px 45px 14px 14px;
  }

  .submit-btn {
    padding: 14px;
  }
}

/* Dark mode support (if needed) */
@media (prefers-color-scheme: dark) {
  .recovery-card {
    background: rgba(31, 41, 55, 0.95);
    border: 1px solid rgba(255, 255, 255, 0.1);
  }

  .title {
    color: #f9fafb;
  }

  .subtitle {
    color: #d1d5db;
  }

  .input-label {
    color: #f3f4f6;
  }

  .form-input {
    background: #374151;
    border-color: #4b5563;
    color: #f9fafb;
  }

  .form-input:focus {
    border-color: #667eea;
    background: #4b5563;
  }

  .success-state h3 {
    color: #f9fafb;
  }

  .success-state p {
    color: #d1d5db;
  }
}
</style>

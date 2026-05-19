<template>
  <div class="background-container">
    <!-- Background shapes -->
    <div class="background-shapes">
      <div class="shape shape-1"></div>
      <div class="shape shape-2"></div>
      <div class="shape shape-3"></div>
    </div>

    <div class="login-card">
      <header class="card__header">
        <img class="brand__logo" :src="logo" alt="logo" v-if="logo" />
        <h1 class="title">Iniciar sesión</h1>
        <p class="subtitle">Ingresa tus credenciales para continuar</p>
      </header>

      <form class="card__body" @submit.prevent="onSubmit" novalidate>
        <div class="field">
          <label for="login-email" class="field__label">Correo electrónico</label>
          <input
            id="login-email"
            class="field__input"
            type="email"
            v-model="form.email"
            autocomplete="username"
            placeholder="tu@ejemplo.com"
            required
          />
        </div>

        <div class="field">
          <label for="login-password" class="field__label">Contraseña</label>
          <div class="password-wrap">
            <input
              id="login-password"
              class="field__input"
              :type="show ? 'password' : 'text'"
              v-model="form.password"
              autocomplete="current-password"
              placeholder="••••••••"
              required
            />
            <button
              type="button"
              class="toggle-btn"
              @click="show = !show"
              :title="show ? 'Mostrar contraseña' : 'Ocultar contraseña'"
            >
              <svg v-if="show" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"></path>
                <circle cx="12" cy="12" r="3"></circle>
              </svg>
              <svg v-else width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"></path>
                <line x1="1" y1="1" x2="23" y2="23"></line>
              </svg>
            </button>
          </div>
        </div>

        <p class="error" v-if="errorMessage">{{ errorMessage }}</p>

        <div class="actions">
          <button class="btn primary" type="submit" :disabled="isLoading">
            <span v-if="isLoading" class="spinner" aria-hidden="true"></span>
            <span v-if="!isLoading">Ingresar</span>
            <span v-else>Ingresando...</span>
          </button>

          <button class="btn link" type="button" @click="goToRecovery">¿Olvidaste tu contraseña?</button>
        </div>
      </form>

      <footer class="card__footer">
        <small>¿No tienes cuenta? Contacta al administrador.</small>
      </footer>
    </div>
  </div>
</template>

<script lang="ts">
import { defineComponent } from "vue";
import Swal from "sweetalert2";
import axios from "../../../config/axiosConfig";

export default defineComponent({
  mounted() {
    // limpiar sesión previa
    localStorage.removeItem("token");
    localStorage.removeItem("role");
    localStorage.removeItem("username");
    localStorage.removeItem("color");
    localStorage.removeItem("logo");
    localStorage.removeItem("email"); // Eliminar también el email al cerrar sesión

    console.log("Sesión previa eliminada al entrar a /login");
  },

  name: "Login",
  data() {
    return {
      form: {
        email: "",
        password: "",
      },
      show: true,
      errorMessage: "",
      isLoading: false,
    };
  },
  computed: {
    // leer logo desde localStorage fuera del template para evitar errores de tipado
    logo(): string {
      if (typeof window === "undefined") return "";
      try {
        return window.localStorage.getItem("logo") || "";
      } catch {
        return "";
      }
    },
  },
  methods: {
    async onSubmit() {
      if (!this.form.email.trim() || !this.form.password.trim()) {
        this.errorMessage = "El usuario y la contraseña son obligatorios.";
        return;
      }

      this.isLoading = true;
      this.errorMessage = "";

      try {
        const response = await axios.doPost("auth/login", {
          email: this.form.email.trim(),
          password: this.form.password.trim(),
        });

        // la API devuelve { data: { token, role, ... } }
        const payload = response.data?.data || response.data;
        const token = payload?.token;
        const role = payload?.role;
        const status = payload?.status;

        console.log("=== DEBUG LOGIN ===");
        console.log("Respuesta completa del servidor:", response.data);
        console.log("Token recibido:", token);
        console.log("Status del usuario:", status);
        console.log("Tipo de status:", typeof status);
        console.log("Status === false:", status === false);
        console.log("Status === true:", status === true);
        console.log("==================");

        if (!token) {
          this.errorMessage = "Respuesta inválida del servidor: token ausente.";
          this.isLoading = false;
          return;
        }

        // Validar si el usuario está inactivo (verificar tanto false como "false" string)
        if (status === false || status === "false" || status === 0) {
          console.error("⛔ USUARIO INACTIVO - Bloqueando acceso");

          await Swal.fire({
            icon: "warning",
            title: "Cuenta Inactiva",
            html: `
              <p style="margin-bottom: 12px;">Tu cuenta ha sido desactivada.</p>
              <p style="color: #6b7280; font-size: 0.9rem;">Por favor, contacta al administrador para reactivar tu cuenta.</p>
            `,
            confirmButtonText: "Entendido",
            confirmButtonColor: "#ef4444",
            allowOutsideClick: false
          });

          // Limpiar el formulario y no guardar nada en localStorage
          this.form.email = "";
          this.form.password = "";
          this.isLoading = false;
          return;
        }

        console.log("✅ Usuario activo - Permitiendo acceso");

        localStorage.setItem("token", token);
        localStorage.setItem("role", role || "");
        localStorage.setItem("email", payload?.email || "");
        localStorage.setItem("username", payload?.email || "");
        localStorage.setItem("color", payload?.color || "");
        localStorage.setItem("logo", payload?.logo || "");

        // Redirigir según el rol del usuario
        switch (role) {
          case "ADMINISTRADOR":
            await this.$router.push({ name: "adminDashboard" });
            break;
          case "ALMACENISTA":
            await this.$router.push({ name: "Almacenista" });
            break;
          case "AUXILIAR":
            await this.$router.push({ name: "Auxiliar" });
            break;
          case "AUXILIAR_DE_CONTEO":
            await this.$router.push({ name: "AuxiliarDeConteo" });
            break;
          default:
            this.errorMessage = "Rol no reconocido. Contacta al administrador.";
            localStorage.clear();
            break;
        }
      } catch (error: any) {
        if (error.response?.data === "Account not verified") {
          await Swal.fire({
            icon: "info",
            title: "Cuenta no verificada",
            text: "Por favor verifica tu cuenta antes de iniciar sesión.",
            confirmButtonText: "Aceptar",
            confirmButtonColor: "#32705d",
          });
          this.$router.push({ name: "VerifyManagerEmail" });
          return;
        }


        this.errorMessage = error.response?.data?.message || "Error al iniciar sesión. Intenta nuevamente.";
      } finally {
        this.isLoading = false;
      }
    },
    goToRecovery() {
      this.$router.push({ name: "passwordRecovery" });
    },
  },


});
</script>


<style scoped>
@import '@/assets/main.css';

.background-container {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #0f2b24 0%, #1a4a3f 25%, #1f5a4d 50%, #0f2b24 75%, #0a1f1a 100%);
  background-size: 400% 400%;
  animation: gradientShift 15s ease infinite;
}

@keyframes gradientShift {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}

.background-shapes {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 1;
}

.shape {
  position: absolute;
  opacity: 0;
  animation: float 20s ease-in-out infinite;
}

@keyframes float {
  0%, 100% {
    transform: translateY(0px) rotate(0deg);
    opacity: 0.15;
  }
  50% {
    transform: translateY(-30px) rotate(180deg);
    opacity: 0.25;
  }
}

.shape-1 {
  width: 400px;
  height: 400px;
  top: -100px;
  left: -150px;
  background: radial-gradient(circle at 30% 30%, rgba(56, 122, 99, 0.4) 0%, rgba(50, 112, 93, 0.1) 70%, transparent 100%);
  border-radius: 45% 55% 50% 50% / 50% 50% 50% 50%;
  animation: float 20s ease-in-out infinite;
  animation-delay: 0s;
}

.shape-2 {
  width: 350px;
  height: 350px;
  bottom: -80px;
  right: -100px;
  background: radial-gradient(circle at 70% 70%, rgba(76, 175, 140, 0.3) 0%, rgba(56, 122, 99, 0.05) 70%, transparent 100%);
  border-radius: 50% 50% 45% 55% / 50% 50% 50% 50%;
  animation: float 24s ease-in-out infinite;
  animation-delay: 2s;
}

.shape-3 {
  width: 300px;
  height: 300px;
  top: 30%;
  right: 5%;
  background: radial-gradient(circle at 50% 50%, rgba(100, 200, 170, 0.2) 0%, rgba(50, 112, 93, 0.04) 70%, transparent 100%);
  border-radius: 50% 50% 50% 50% / 45% 55% 50% 50%;
  animation: float 26s ease-in-out infinite;
  animation-delay: 4s;
}

.login-card {
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

.login-card:hover {
  transform: translateY(-5px);
}

.title {
  margin: 0;
  font-size: 20px;
  color: #123026;
}

.subtitle {
  margin: 6px 0 0;
  font-size: 13px;
  color: #546d64;
}

.card__body {
  margin-top: 14px;
  display: grid;
  gap: 12px;
}

.field {
  display: block;
}

.field__label {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  color: #425c56;
}

.field__input {
  width: 100%;
  padding: 10px 12px;
  border-radius: 8px;
  border: 1px solid #e3e9e6;
  background: #fbfffd;
  font-size: 14px;
  color: #0f2b24;
  box-sizing: border-box;
  outline: none;
  transition: border-color .15s, box-shadow .15s;
}

.field__input:focus {
  border-color: #7fb79b;
  box-shadow: 0 0 0 4px rgba(50,112,93,0.06);
}

.password-wrap {
  display: flex;
  align-items: center;
  gap: 8px;
}

.toggle-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  padding: 8px;
  border-radius: 6px;
  cursor: pointer;
  color: #387a63;
  transition: background-color 0.2s, color 0.2s;
  flex-shrink: 0;
}

.toggle-btn:hover {
  background-color: rgba(56, 122, 99, 0.1);
}

.toggle-btn:active {
  transform: scale(0.95);
}

.toggle-btn svg {
  stroke-linecap: round;
  stroke-linejoin: round;
}

.error {
  color: #b92525;
  background: #fff5f5;
  padding: 8px 10px;
  border-radius: 8px;
  font-size: 13px;
  margin: 0;
}

.actions {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-top: 4px;
}

.btn {
  padding: 10px 14px;
  border-radius: 10px;
  font-size: 14px;
  cursor: pointer;
  border: none;
}

.btn.primary {
  background: linear-gradient(90deg,#32705d,#2a6a55);
  color: #fff;
  flex: 1 1 auto;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
}

.btn.primary[disabled] {
  opacity: 0.6;
  cursor: not-allowed;
}

.btn.link {
  background: transparent;
  color: #2f6f5b;
  padding: 10px;
  font-size: 13px;
}

.spinner {
  width: 16px;
  height: 16px;
  border: 2px solid rgba(255,255,255,0.38);
  border-top-color: #fff;
  border-radius: 50%;
  display: inline-block;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.card__footer {
  margin-top: 12px;
  text-align: center;
  color: #647a73;
  font-size: 12px;
}
</style>

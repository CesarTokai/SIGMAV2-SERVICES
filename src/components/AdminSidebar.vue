<template>
  <div class="admin-sidebar">
    <!-- SIDEBAR DESKTOP -->
    <aside class="side" v-if="!isMobile" aria-label="Barra lateral de navegación">

      <!-- LOGO -->
      <div class="logo-wrap" @click="goHome" role="button" tabindex="0" aria-label="Ir al inicio">
        <div class="logo-container">
          <img v-if="logo" :src="logo" alt="./public/Logo.png" class="logo"/>
          <div v-else class="logo-fallback">
            <span class="logo-text">SIGMA</span>
          </div>
        </div>
        <div class="logo-subtitle">Panel de Administración</div>
      </div>

      <div class="sidebar-divider"></div>

      <!-- NAV LINKS -->
      <nav class="nav-list" aria-label="Menú administrativo">
        <ul>
          <li
            v-for="item in sidebarElements"
            :key="item.to"
            :class="{ active: isActive(item), 'has-children': item.children && item.children.length, 'open': openMenus[item.to] }"
          >
            <!-- Item con hijos: toggle acordeón -->
            <div
              v-if="item.children && item.children.length"
              class="nav-link accordion-trigger"
              :class="{ 'accordion-active': isChildActive(item) }"
              @click="toggleMenu(item.to)"
              role="button"
              tabindex="0"
              @keydown.enter="toggleMenu(item.to)"
            >
              <span class="label">{{ item.title }}</span>
              <span class="chevron" :class="{ 'chevron-open': openMenus[item.to] }">›</span>
            </div>

            <!-- Item normal sin hijos -->
            <RouterLink
              v-else
              :to="`/Admin${item.to}`"
              class="nav-link"
              @click="onNavigate"
            >
              <span class="label">{{ item.title }}</span>
            </RouterLink>

            <!-- Sub-menú acordeón -->
            <transition name="accordion">
              <ul v-if="item.children && item.children.length && openMenus[item.to]" class="sub-nav-list">
                <li v-for="subItem in item.children" :key="subItem.to" :class="{ active: isActive(subItem) }">
                  <RouterLink :to="`/Admin${subItem.to}`" class="sub-nav-link" @click="onNavigate">
                    <span class="label">{{ subItem.title }}</span>
                  </RouterLink>
                </li>
              </ul>
            </transition>
          </li>
        </ul>
      </nav>

      <!-- FOOTER -->
      <div class="sidebar-footer">
        <div class="user-info-mini">
          <div class="user-avatar">{{ userInitial }}</div>
          <div class="user-details">
            <span class="user-name">{{ username }}</span>
            <span class="user-role">Administrador</span>
          </div>
        </div>
        <button class="btn-logout" @click="closeSession" :disabled="isLoggingOut" :aria-busy="isLoggingOut" aria-label="Cerrar sesión">
          <span v-if="isLoggingOut" class="spinner" aria-hidden="true"></span>
          <template v-else>
            <span class="logout-icon">⏻</span>
            <span>Cerrar sesión</span>
          </template>
        </button>
      </div>
    </aside>

    <!-- MOBILE NAV -->
    <div class="mobile-nav" v-else>
      <div class="mobile-header">
        <button class="hamburger" @click="showMobile = true" aria-label="Abrir menú">
          <span></span><span></span><span></span>
        </button>
        <div class="mobile-brand" @click="goHome">
          <img v-if="logo" :src="logo" alt="logo" class="mobile-logo"/>
          <span v-else class="mobile-logo-text">⚡ SIGMA</span>
        </div>
        <div class="mobile-user-avatar">{{ userInitial }}</div>
      </div>

      <transition name="fade-overlay">
        <div class="mobile-overlay" v-if="showMobile" @click="showMobile = false"></div>
      </transition>

      <transition name="slide">
        <div class="mobile-drawer" v-if="showMobile" role="dialog" aria-label="Menú">
          <div class="mobile-drawer-header">
            <div class="mobile-drawer-logo">
              <img v-if="logo" :src="logo" alt="logo" class="logo"/>
              <span v-else class="logo-fallback-mobile">⚡ SIGMA</span>
            </div>
            <button class="close-btn" @click="showMobile = false" aria-label="Cerrar">✕</button>
          </div>

          <div class="mobile-user-card">
            <div class="user-avatar large">{{ userInitial }}</div>
            <div class="user-details">
              <span class="user-name">{{ username }}</span>
              <span class="user-role">Administrador</span>
            </div>
          </div>

          <div class="sidebar-divider"></div>

          <nav class="nav-list-mobile">
            <ul>
              <li
                v-for="item in sidebarElements"
                :key="item.to"
                :class="{ active: isActive(item), 'has-children': item.children && item.children.length, 'open': openMenus[item.to] }"
              >
                <div
                  v-if="item.children && item.children.length"
                  class="nav-link accordion-trigger"
                  :class="{ 'accordion-active': isChildActive(item) }"
                  @click="toggleMenu(item.to)"
                >
                  <span class="label">{{ item.title }}</span>
                  <span class="chevron" :class="{ 'chevron-open': openMenus[item.to] }">›</span>
                </div>

                <RouterLink
                  v-else
                  :to="`/Admin${item.to}`"
                  class="nav-link"
                  @click="closeMobileAndNavigate"
                >
                  <span class="label">{{ item.title }}</span>
                </RouterLink>

                <transition name="accordion">
                  <ul v-if="item.children && item.children.length && openMenus[item.to]" class="sub-nav-list">
                    <li v-for="subItem in item.children" :key="subItem.to" :class="{ active: isActive(subItem) }">
                      <RouterLink :to="`/Admin${subItem.to}`" class="sub-nav-link" @click="closeMobileAndNavigate">
                        <span class="label">{{ subItem.title }}</span>
                      </RouterLink>
                    </li>
                  </ul>
                </transition>
              </li>
            </ul>
          </nav>

          <div class="mobile-footer">
            <button class="btn-logout" @click="closeSession" :disabled="isLoggingOut" :aria-busy="isLoggingOut">
              <span v-if="isLoggingOut" class="spinner" aria-hidden="true"></span>
              <template v-else>
                <span class="logout-icon">⏻</span>
                <span>Cerrar sesión</span>
              </template>
            </button>
          </div>
        </div>
      </transition>
    </div>
  </div>
</template>

<script>
import { defineComponent } from "vue";
import { RouterLink } from "vue-router";
import axios from "../config/axiosConfig";
import { LoadAlert, ToastSuccess, ToastError } from "../utils/SweetAlert";

export default defineComponent({
  name: "AdminSidebar",
  components: { RouterLink },
  props: {
    sidebarElements: {
      type: Array,
      default: () => [
        { title: "Gestion de Periodos", to: "/PeriodosAdmin" },
        { title: "Gestion de Inventarios", to: "/InventarioAdmin" },
        { title: "Gestion de MultiAlmacen", to: "/MultiAlmacen" },
        { title: "Gestion de Almacenes", to: "/Almacen" },
        {
          title: "Gestion de Marbetes",
          to: "/MarbetesAdmin",
        },
        {
           title: "Reportes",
           to: "/DistribucionMarbetes",
           children: [
             { title: "Distribución de marbetes", to: "/DistribucionMarbetes" },
             { title: "Listados de marbetes", to: "/ListadoMarbetes" },
             { title: "Marbetes Pendientes", to: "/MarbetesPedientes" },
             { title: "Marbetes con diferencia", to: "/MarbetesConDiferencia" },
             { title: "Marbetes con comentarios", to: "/ReportesConComentarios" },
             { title: "Marbetes cancelados", to: "/MarbetesCancelados" },
             { title: "Comparativos", to: "/ComparativosMarbetes" },
             { title: "Almacén con detalle", to: "/AlmacenDetalle" },
             { title: "Producto con detalle", to: "/ProductoDetalle" },
             {title:  "Marbetes con Comentarios", to: "/ReporteComentarios"},
             { title: "Historial de Conteos", to: "/HistorialConteos" }
           ]
        },
        { title: "Gestion de Usuarios", to: "/user-management" },
        { title: "Generar Archivo", to: "/GenerarArchivo" },
      ],
    },
  },
  data() {
    return {
      isMobile: false,
      showMobile: false,
      isLoggingOut: false,
      openMenus: {},
    };
  },
  computed: {
    logo() {
      if (typeof window === "undefined") return "";
      try {
        return window.localStorage.getItem("logo") || "";
      } catch {
        return "";
      }
    },
    username() {
      try {
        return localStorage.getItem("username") || "Usuario";
      } catch {
        return "Usuario";
      }
    },
    userInitial() {
      return this.username ? this.username.charAt(0).toUpperCase() : "U";
    },
  },
  mounted() {
    this.checkScreenSize();
    window.addEventListener("resize", this.checkScreenSize);
    // Auto-abrir menú si una ruta hija está activa
    this.sidebarElements.forEach(item => {
      if (item.children && this.isChildActive(item)) {
        this.openMenus[item.to] = true;
      }
    });
  },
  beforeUnmount() {
    window.removeEventListener("resize", this.checkScreenSize);
  },
  methods: {
    checkScreenSize() {
      this.isMobile = window.innerWidth <= 768;
    },
    isActive(item) {
      if (!this.$route || !this.$route.path) return false;
      const current = String(this.$route.path).toLowerCase();
      return current === (`/admin${item.to}`).toLowerCase() || current === (`/Admin${item.to}`).toLowerCase();
    },
    isChildActive(item) {
      if (!item.children) return false;
      return item.children.some(child => this.isActive(child));
    },
    toggleMenu(key) {
      this.openMenus = { ...this.openMenus, [key]: !this.openMenus[key] };
    },
    onNavigate() {},
    closeMobileAndNavigate() {
      this.showMobile = false;
    },
    goHome() {
      this.$router.push({ name: "adminDashboard" }).catch(() => {});
    },
    async closeSession() {
      this.isLoggingOut = true;
      LoadAlert(true);
      try {
        await axios.doPost('auth/logout', {});
        ToastSuccess('Sesión cerrada', 'Has cerrado sesión correctamente.');
      } catch (e) {
        ToastError('Error', 'No se pudo cerrar la sesión de forma remota. Se procederá a limpiar la sesión local.');
      } finally {
        LoadAlert(false);
        this.isLoggingOut = false;
        try {
          localStorage.removeItem("username");
          localStorage.removeItem("token");
          localStorage.removeItem("role");
          localStorage.removeItem("editStoreId");
        } catch (err) {
          console.error('Error clearing localStorage (sidebar):', err);
        }
        this.$router.push({ name: "login" });
      }
    },
  },
});
</script>

<style scoped>
/* ================================================
   VARIABLES
   ================================================ */
:root {
  --sidebar-width: 270px;
  --accent: #ffffff;
  --text-primary: #ffffff;
  --text-secondary: rgba(255,255,255,0.75);
  --divider: rgba(255,255,255,0.2);
}

/* ================================================
   SIDEBAR CONTAINER
   ================================================ */
.admin-sidebar {
  display: flex;
  min-height: 100vh;
}

.side {
  width: 270px;
  min-height: 100vh;
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
  padding: 0;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  border-right: none;
  box-shadow: 4px 0 24px rgba(40, 167, 69, 0.35);
  position: relative;
  overflow: hidden;
}

/* Borde decorativo derecho */
.side::after {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 1px;
  height: 100%;
  background: linear-gradient(to bottom, transparent, rgba(255,255,255,0.3), transparent);
  pointer-events: none;
}

/* ================================================
   LOGO / HEADER
   ================================================ */
.logo-wrap {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  padding: 1.75rem 1rem 1.25rem;
  transition: opacity 0.2s;
  background: rgba(0,0,0,0.08);
}

.logo-wrap:hover {
  opacity: 0.85;
}

.logo-container {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 0.5rem;
}

.logo {
  max-width: 130px;
  width: 100%;
  height: auto;
  object-fit: contain;
  filter: brightness(0) invert(1) drop-shadow(0 2px 8px rgba(0,0,0,0.2));
}

.logo-fallback {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.logo-icon {
  font-size: 1.75rem;
  filter: drop-shadow(0 0 8px rgba(255,255,255,0.6));
}

.logo-text {
  font-size: 1.5rem;
  font-weight: 800;
  color: #ffffff;
  letter-spacing: 0.1em;
  text-shadow: 0 2px 8px rgba(0,0,0,0.2);
}

.logo-subtitle {
  font-size: 0.6875rem;
  font-weight: 600;
  color: rgba(255,255,255,0.75);
  text-transform: uppercase;
  letter-spacing: 0.12em;
}

/* ================================================
   DIVISOR
   ================================================ */
.sidebar-divider {
  height: 1px;
  background: linear-gradient(to right, transparent, rgba(255,255,255,0.35), transparent);
  margin: 0 1rem;
}

/* ================================================
   NAVEGACIÓN
   ================================================ */
.nav-list {
  width: 100%;
  flex: 1;
  overflow-y: auto;
  padding: 0.75rem 0.75rem;
  scrollbar-width: thin;
  scrollbar-color: rgba(255,255,255,0.2) transparent;
}

.nav-list::-webkit-scrollbar {
  width: 3px;
}

.nav-list::-webkit-scrollbar-track {
  background: transparent;
}

.nav-list::-webkit-scrollbar-thumb {
  background: rgba(255,255,255,0.25);
  border-radius: 4px;
}

.nav-list::-webkit-scrollbar-thumb:hover {
  background: rgba(255,255,255,0.5);
}

.nav-list ul,
.nav-list-mobile ul {
  list-style: none;
  padding: 0;
  margin: 0;
  width: 100%;
}

.nav-list li,
.nav-list-mobile li {
  width: 100%;
  margin-bottom: 0.125rem;
}

/* ================================================
   NAV LINKS
   ================================================ */
.nav-link {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  width: 100%;
  padding: 0.7rem 0.875rem;
  color: rgba(255,255,255,0.88);
  text-decoration: none;
  border-radius: 10px;
  font-size: 0.8375rem;
  font-weight: 500;
  background: transparent;
  transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1);
  position: relative;
  cursor: pointer;
  border: 1px solid transparent;
  box-sizing: border-box;
}

.nav-link:hover {
  background: rgba(255,255,255,0.18);
  color: #ffffff;
  border-color: rgba(255,255,255,0.25);
  transform: translateX(3px);
}

/* Item activo */
li.active > .nav-link,
li.active > a.nav-link {
  background: rgba(255,255,255,0.25);
  color: #ffffff;
  border-color: rgba(255,255,255,0.4);
  font-weight: 700;
  box-shadow: inset 3px 0 0 #ffffff, 0 2px 12px rgba(0,0,0,0.15);
}

/* Acordeón activo (padre con hijo activo) */
.accordion-active {
  color: #ffffff !important;
  font-weight: 600;
}


/* ================================================
   CHEVRON (acordeón)
   ================================================ */
.chevron {
  font-size: 1.1rem;
  color: rgba(255,255,255,0.65);
  transition: transform 0.25s cubic-bezier(0.4, 0, 0.2, 1), color 0.2s;
  flex-shrink: 0;
  line-height: 1;
}

.chevron-open {
  transform: rotate(90deg);
  color: #ffffff;
}

/* ================================================
   SUBMENÚ
   ================================================ */
.sub-nav-list {
  margin: 0.25rem 0 0.25rem 1rem;
  padding: 0.25rem 0 0.25rem 0.75rem;
  border-left: 1px solid rgba(255,255,255,0.3);
  overflow: hidden;
}

.sub-nav-list li {
  margin-bottom: 0.125rem;
}

.sub-nav-link {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.5rem 0.75rem;
  color: rgba(255,255,255,0.75);
  text-decoration: none;
  border-radius: 8px;
  font-size: 0.8rem;
  font-weight: 400;
  background: transparent;
  transition: all 0.18s ease;
  border: 1px solid transparent;
}

.sub-nav-link:hover {
  background: rgba(255,255,255,0.18);
  color: #ffffff;
  transform: translateX(3px);
}


li.active > .sub-nav-link {
  background: rgba(255,255,255,0.25);
  color: #ffffff;
  font-weight: 700;
  border-color: rgba(255,255,255,0.35);
}

/* ================================================
   ACORDEÓN ANIMACIÓN
   ================================================ */
.accordion-enter-active,
.accordion-leave-active {
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  max-height: 600px;
  opacity: 1;
}

.accordion-enter-from,
.accordion-leave-to {
  max-height: 0;
  opacity: 0;
}

/* ================================================
   USUARIO + FOOTER
   ================================================ */
.sidebar-footer {
  padding: 0.75rem;
  border-top: 1px solid rgba(255,255,255,0.2);
  background: rgba(0,0,0,0.12);
  display: flex;
  flex-direction: column;
  gap: 0.625rem;
}

.user-info-mini {
  display: flex;
  align-items: center;
  gap: 0.625rem;
  padding: 0.5rem 0.625rem;
  border-radius: 10px;
  background: rgba(255,255,255,0.15);
  border: 1px solid rgba(255,255,255,0.25);
}

.user-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: rgba(255,255,255,0.25);
  border: 2px solid rgba(255,255,255,0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 0.875rem;
  color: #fff;
  flex-shrink: 0;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
}

.user-avatar.large {
  width: 44px;
  height: 44px;
  font-size: 1.1rem;
}

.user-details {
  display: flex;
  flex-direction: column;
  gap: 0.125rem;
  overflow: hidden;
}

.user-name {
  font-size: 0.8125rem;
  font-weight: 600;
  color: #ffffff;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.user-role {
  font-size: 0.6875rem;
  color: rgba(255,255,255,0.75);
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 0.05em;
}

.btn-logout {
  width: 100%;
  background: rgba(255,255,255,0.12);
  border: 1px solid rgba(255,255,255,0.3);
  padding: 0.625rem 1rem;
  border-radius: 10px;
  cursor: pointer;
  color: #ffffff;
  font-weight: 600;
  font-size: 0.8125rem;
  transition: all 0.2s ease;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 0.5rem;
}

.btn-logout:hover:not(:disabled) {
  background: rgba(239, 68, 68, 0.55);
  border-color: rgba(239, 68, 68, 0.7);
  color: #ffffff;
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(239, 68, 68, 0.3);
}

.btn-logout:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.logout-icon {
  font-size: 0.9rem;
}

/* ================================================
   MOBILE NAVIGATION
   ================================================ */
.mobile-nav {
  width: 100%;
  background: linear-gradient(135deg, #28a745 0%, #20c997 100%);
}

.mobile-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0.875rem 1.25rem;
  background: rgba(0,0,0,0.1);
  border-bottom: 1px solid rgba(255,255,255,0.15);
  box-shadow: 0 2px 12px rgba(40,167,69,0.3);
}

.hamburger {
  background: transparent;
  border: none;
  width: 36px;
  height: 36px;
  cursor: pointer;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 5px;
  padding: 0;
  border-radius: 8px;
  transition: background 0.2s;
}

.hamburger:hover {
  background: rgba(255,255,255,0.15);
}

.hamburger span {
  display: block;
  width: 20px;
  height: 2px;
  background: #ffffff;
  border-radius: 2px;
  transition: all 0.2s;
}

.mobile-brand {
  display: flex;
  align-items: center;
  cursor: pointer;
}

.mobile-logo {
  height: 32px;
  filter: brightness(0) invert(1);
}

.mobile-logo-text {
  font-size: 1.125rem;
  font-weight: 800;
  color: #fff;
  letter-spacing: 0.05em;
}

.mobile-user-avatar {
  width: 34px;
  height: 34px;
  border-radius: 50%;
  background: rgba(255,255,255,0.25);
  border: 2px solid rgba(255,255,255,0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 0.875rem;
  color: #fff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.15);
}

/* Overlay oscuro detrás del drawer */
.mobile-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0,0,0,0.55);
  z-index: 999;
  backdrop-filter: blur(2px);
}

.mobile-drawer {
  position: fixed;
  top: 0;
  left: 0;
  bottom: 0;
  width: 290px;
  background: linear-gradient(160deg, #28a745 0%, #20c997 100%);
  box-shadow: 4px 0 32px rgba(40,167,69,0.4);
  z-index: 1000;
  padding: 0;
  display: flex;
  flex-direction: column;
  overflow-y: auto;
  scrollbar-width: thin;
  scrollbar-color: rgba(255,255,255,0.2) transparent;
}

.mobile-drawer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 1.25rem 1rem;
  background: rgba(0,0,0,0.1);
  border-bottom: 1px solid rgba(255,255,255,0.15);
}

.mobile-drawer-logo .logo {
  max-width: 100px;
  filter: brightness(0) invert(1);
}

.logo-fallback-mobile {
  font-size: 1.25rem;
  font-weight: 800;
  color: #fff;
}

.close-btn {
  background: rgba(255,255,255,0.15);
  border: 1px solid rgba(255,255,255,0.3);
  font-size: 1rem;
  width: 32px;
  height: 32px;
  border-radius: 8px;
  cursor: pointer;
  color: #ffffff;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
}

.close-btn:hover {
  color: #ffffff;
  background: rgba(239,68,68,0.5);
  border-color: rgba(239,68,68,0.7);
  transform: rotate(90deg);
}

.mobile-user-card {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 1rem 1.25rem;
}

.nav-list-mobile {
  flex: 1;
  overflow-y: auto;
  padding: 0.5rem 0.75rem;
}

.nav-list-mobile ul {
  list-style: none;
  padding: 0;
  margin: 0;
}

.mobile-footer {
  padding: 0.75rem;
  border-top: 1px solid rgba(255,255,255,0.2);
  background: rgba(0,0,0,0.12);
}

/* ================================================
   ANIMACIONES
   ================================================ */
.slide-enter-active,
.slide-leave-active {
  transition: transform 0.28s cubic-bezier(0.4, 0, 0.2, 1);
}

.slide-enter-from { transform: translateX(-100%); }
.slide-enter-to   { transform: translateX(0); }
.slide-leave-from { transform: translateX(0); }
.slide-leave-to   { transform: translateX(-100%); }

.fade-overlay-enter-active,
.fade-overlay-leave-active {
  transition: opacity 0.25s ease;
}

.fade-overlay-enter-from,
.fade-overlay-leave-to {
  opacity: 0;
}

.spinner {
  border: 2px solid rgba(255,255,255,0.3);
  border-top: 2px solid #ffffff;
  border-radius: 50%;
  width: 16px;
  height: 16px;
  animation: spin 0.6s linear infinite;
  flex-shrink: 0;
}

@keyframes spin {
  0%   { transform: rotate(0deg); }
  100% { transform: rotate(360deg); }
}

/* ================================================
   RESPONSIVE
   ================================================ */
@media (max-width: 768px) {
  .side { display: none; }
}

@media (max-width: 480px) {
  .mobile-drawer {
    width: 85vw;
    max-width: 290px;
  }
}

/* ================================================
   ACCESIBILIDAD
   ================================================ */
.nav-link:focus-visible,
.sub-nav-link:focus-visible,
.btn-logout:focus-visible {
  outline: 2px solid rgba(255,255,255,0.8);
  outline-offset: 2px;
}
</style>

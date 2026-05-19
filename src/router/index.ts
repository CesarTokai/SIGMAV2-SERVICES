import {
    createRouter,
    createWebHistory,
    type NavigationGuardNext,
    type RouteLocationNormalized,
    type RouteRecordRaw
} from 'vue-router';

// RUTAS PÚBLICAS (carga inmediata — pequeñas, siempre necesarias)
import Login from '@/modules/auth/views/Login.vue';
import PasswordRecovery from '@/modules/auth/views/PasswordRecovery.vue';

// RUTAS LAZY — se cargan solo cuando el usuario accede a ellas
const NotFound                    = () => import('../modules/NotFound.vue');

// Admin
const AdminLayout                 = () => import(/* webpackChunkName: "admin" */ '@/modules/admin/AdminLayout.vue');
const AdminUserManagement         = () => import(/* webpackChunkName: "admin" */ '@/components/Admin/AdminUserManagement.vue');
const RequestRecoveryPasswordList = () => import(/* webpackChunkName: "admin" */ '@/modules/request-recovery-password/RequestRecoveryPasswordList.vue');
const AlmacenAdmin                = () => import(/* webpackChunkName: "admin" */ '@/modules/admin/views/almacenAdmin/AlamacenAdmin.vue');
const AsignacionIndividual        = () => import(/* webpackChunkName: "admin" */ '@/modules/admin/views/almacenAdmin/AsignacionIndividual.vue');
const MultiAlmacenAdmin           = () => import(/* webpackChunkName: "admin" */ '@/modules/admin/views/multiAlmacenAdmin/MultiAlmacenAdmin.vue');
const PeriodosAdmin               = () => import(/* webpackChunkName: "admin" */ '@/modules/admin/views/periodosAdmin/PeriodosAdmin.vue');
const InventarioAdmin             = () => import(/* webpackChunkName: "admin" */ '@/modules/admin/views/inventarioAdmin/InventarioAdmin.vue');
const MarbetesAdmin               = () => import(/* webpackChunkName: "admin" */ '@/modules/admin/views/marbetesAdmin/MarbetesAdmin.vue');
const GenerarArchivoAdmin         = () => import(/* webpackChunkName: "admin" */ '@/modules/admin/views/generarArchivoAdmin/GenerarArchivoAdmin.vue');
const HistorialConteos            = () => import(/* webpackChunkName: "admin" */ '@/modules/admin/views/marbetesAdmin/HistorialConteos.vue');

// Admin — Reportes
const DistribucionMarbetes        = () => import(/* webpackChunkName: "reportes" */ '@/modules/admin/views/reportesAdmin/DistribucionMarbetes.vue');
const ListadoMarbetes             = () => import(/* webpackChunkName: "reportes" */ '@/modules/admin/views/reportesAdmin/ListadoMarbetes.vue');
const MarbetesPedientes           = () => import(/* webpackChunkName: "reportes" */ '@/modules/admin/views/reportesAdmin/MarbetesPedientes.vue');
const MarbetesConDiferencia       = () => import(/* webpackChunkName: "reportes" */ '@/modules/admin/views/reportesAdmin/MarbetesConDiferencia.vue');
const MarbetesCancelados          = () => import(/* webpackChunkName: "reportes" */ '@/modules/admin/views/reportesAdmin/MarbetesCancelados.vue');
const AlmacenDetalle              = () => import(/* webpackChunkName: "reportes" */ '@/modules/admin/views/reportesAdmin/AlmacenDetalle.vue');
const ProductoDetalle             = () => import(/* webpackChunkName: "reportes" */ '@/modules/admin/views/reportesAdmin/ProductoDetalle.vue');
const comparativosMarbetes        = () => import(/* webpackChunkName: "reportes" */ '@/modules/admin/views/reportesAdmin/ComparativosMarbetes.vue');
const ReportesConComentarios      = () => import(/* webpackChunkName: "reportes" */ '@/modules/admin/views/reportesAdmin/ReportesConComentarios.vue');

// Almacenista
const AlmacenistaDashboard        = () => import(/* webpackChunkName: "almacenista" */ '../modules/almacenista/Dashboard.vue');
const AlmacenistaMarbetesLayout   = () => import(/* webpackChunkName: "almacenista" */ '../modules/almacenista/views/marbetes/MarbetesLayout.vue');

// Auxiliar
const AuxiliarDashboard           = () => import(/* webpackChunkName: "auxiliar" */ '../modules/auxiliar/Dashboard.vue');
const AuxiliarMarbetesLayout      = () => import(/* webpackChunkName: "auxiliar" */ '../modules/auxiliar/views/marbetes/MarbetesLayout.vue');

// Auxiliar de Conteo
const AuxiliarConteoDashboard     = () => import(/* webpackChunkName: "auxiliar-conteo" */ '../modules/auxiliar_de_conteo/Dashboard.vue');
const AuxiliarConteoMarbetesLayout = () => import(/* webpackChunkName: "auxiliar-conteo" */ '../modules/auxiliar_de_conteo/views/marbetes/MarbetesLayout.vue');


// ==============================================
// DEFINICIÓN DE RUTAS
// ==============================================
const routes: Array<RouteRecordRaw> = [
    // ==============================================
    // RUTAS PÚBLICAS (No requieren autenticación)
    // ==============================================
    {
        path: "/",
        name: "home",
        component: Login,
        meta: {
            title: "SigmaV2 | Login",
        },
    },
    {
        path: "/login",
        name: "login",
        component: Login,
        meta: {
            title: "SigmaV2 | Login",
        },
    },
    {
        path: "/password-recovery",
        name: "passwordRecovery",
        component: PasswordRecovery,
        meta: {
            title: "SigmaV2 | Password Recovery",
        },
    },

    // ==============================================
    // RUTAS PROTEGIDAS - ROL: ADMINISTRADOR
    // ==============================================
    {
        path: "/Admin",
        component: AdminLayout,
        meta: { role: "ADMINISTRADOR" },
        children: [
            {
                path: "",
                name: "adminDashboard",
                component: PeriodosAdmin,
                meta: {
                    role: "ADMINISTRADOR",
                    title: "SigmaV2 | Dashboard Administrador"
                },
            },
            {
                path: "user-management",
                name: "userManagement",
                component: AdminUserManagement,
                meta: {
                    role: "ADMINISTRADOR",
                    title: "SigmaV2 | Gestión de Usuarios"
                },
            },
            {
                path: "recovery-requests",
                name: "recoveryRequests",
                component: RequestRecoveryPasswordList,
                meta: {
                    role: "ADMINISTRADOR",
                    title: "SigmaV2 | Solicitudes de Recuperación"
                },
            },
            {
                path: "Almacen",
                name: "Almacen",
                component: AlmacenAdmin,
                meta: {
                    role:"ADMINISTRADOR",
                    title: "SigmaV2 | Administración de Almacén"
                },
            },
            {
                path: "AsignacionIndividual",
                name: "AsignacionIndividual",
                component: AsignacionIndividual,
                meta: {
                    role:"ADMINISTRADOR",
                    title: "SigmaV2 | Asignación Individual de Almacenes"
                },
            },
            {
                path: "MultiAlmacen",
                name:"MultiAlmacen",
                component: MultiAlmacenAdmin,
                meta:{
                    role:"ADMINISTRADOR",
                    title: "SigmaV2 | Administración de Multi Almacén"
                },
            },
            {
                path:"PeriodosAdmin",
                name:"PeriodosAdmin",
                component: PeriodosAdmin,
                meta:{
                    role:"ADMINISTRADOR",
                    title: "SigmaV2 | Administración de Períodos"
                }

            },
            {
                path:"InventarioAdmin",
                name:"InventarioAdmin",
                component: InventarioAdmin,
                meta:{
                    role:"ADMINISTRADOR",
                    title: "SigmaV2 | Administración de Inventario"
                }
            },
            {
                path:"MarbetesAdmin",
                name:"MarbetesAdmin",
                component: MarbetesAdmin,
                meta:{
                    role:"ADMINISTRADOR",
                    title: "SigmaV2 | Administración de Marbetes"
                }
            },

            {
                path:"MarbetesConDiferencia",
                name:"MarbetesConDiferencia",
                component:MarbetesConDiferencia,
                meta:{
                role:"ADMINISTRADOR",
                    title: "SigmaV2 | Reporte de Marbetes con Diferencia"
                }
            },
            {
                path:"MarbetesPedientes",
                name:"MarbetesPedientes",
                component:MarbetesPedientes,
                meta:{
                role:"ADMINISTRADOR",
                    title: "SigmaV2 | Reporte de Marbetes Pendientes"
                }
            },
            {
                path:"GenerarArchivo",
                name:"GenerarAdmin",
                component: GenerarArchivoAdmin,
                meta:{
                    role:"ADMINISTRADOR",
                    title: "SigmaV2 | Generar Marbetes"
                }
            },
            {
                path:"DistribucionMarbetes",
                name:"DistribucionMarbetes",
                component: DistribucionMarbetes,
                meta:{
                    role:"ADMINISTRADOR",
                    title:"SIGMAV2 | Reporte de Distribución de Marbetes"
                }
            },
            {
                path:"ListadoMarbetes",
                name:"ListadoMarbetes",
                component: ListadoMarbetes,
                meta:{
                    role:"ADMINISTRADOR",
                    title:"SIGMAV2 | Reporte de Listado de Marbetes"
                }
            },
            {
                path:"MarbetesPendientes",
                name:"MarbetesPendientes",
                component:MarbetesPedientes,
                meta:{
                role:"ADMINISTRADOR",
                    title:"SIGMAV2 | Reporte de Marbetes Pendientes"
                }
            },
            {
                path:"MarbetesCancelados",
                name:"MarbetesCancelados",
                component:MarbetesCancelados,
                meta:{
                    role:"ADMINISTRADOR",
                    title:"SIGMAV2 | Reporte de Marbetes Cancelados"
                }

            },
            {
                path:"ComparativosMarbetes",
                name:"ComparativosMarbetes",
                component: comparativosMarbetes,
                meta:{
                    role:"ADMINISTRADOR",
                    title:"SIGMAV2 | Reporte de Comparativos de Marbetes"
                }
            },
            {
                path:"AlmacenDetalle",
                name:"AlmacenDetalle",
                component:AlmacenDetalle,
                meta:{
                    role:"ADMINISTRADOR",
                    title:"SIGMAV2 | Reporte de Almacén Detalle"
                }
            },
             {
              path:"ProductoDetalle",
              name:"ProductoDetalle",
              component:ProductoDetalle,
              meta:{
                  role:"ADMINISTRADOR",
                  title:"SIGMAV2 | Reporte de Producto Detalle"
              }
             },
              {
                  path:"HistorialConteos",
                  name:"HistorialConteos",
                  component: HistorialConteos,
                  meta:{
                      role:"ADMINISTRADOR",
                      title:"SIGMAV2 | Historial de Conteos"
                  }

              },
              {
                  path:"ReportesConComentarios",
                  name:"ReportesConComentarios",
                  component: ReportesConComentarios,
                  meta:{
                      role:"ADMINISTRADOR",
                      title:"SIGMAV2 | Reportes de Marbetes con Comentarios"
                  }
              },

        ],
    },

    // ==============================================
    // RUTAS PROTEGIDAS - ROL: ALMACENISTA
    // ==============================================
    {
        path: "/almacenista",
        name: "Almacenista",
        component: AlmacenistaDashboard,
        meta: {
            role: "ALMACENISTA",
            title: "SigmaV2 | Gestión de Marbetes",
        },
        children: [
            {
                path: "marbetes",
                name: "AlmacenistaMarbetes",
                component: AlmacenistaMarbetesLayout,
                meta: {
                    role: "ALMACENISTA",
                    title: "SigmaV2 | Gestión de Marbetes - Almacenista"
                }
            }
        ]
    },

    // ==============================================
    // RUTAS PROTEGIDAS - ROL: AUXILIAR
    // ==============================================
    {
        path: "/auxiliar",
        name: "Auxiliar",
        component: AuxiliarDashboard,
        meta: {
            role: "AUXILIAR",
            title: "SigmaV2 | Dashboard Auxiliar",
        },
        children: [
            {
                path: "marbetes",
                name: "AuxiliarMarbetes",
                component: AuxiliarMarbetesLayout,
                meta: {
                    role: "AUXILIAR",
                    title: "SigmaV2 | Gestión de Marbetes - Auxiliar"
                }
            }
        ]
    },

    // ==============================================
    // RUTAS PROTEGIDAS - ROL: AUXILIAR_DE_CONTEO
    // ==============================================
    {
        path: "/auxiliar-de-conteo",
        name: "AuxiliarDeConteo",
        component: AuxiliarConteoDashboard,
        meta: {
            role: "AUXILIAR_DE_CONTEO",
            title: "SigmaV2 | Dashboard Auxiliar de Conteo",
        },
        children: [
            {
                path: "marbetes",
                name: "AuxiliarConteoMarbetes",
                component: AuxiliarConteoMarbetesLayout,
                meta: {
                    role: "AUXILIAR_DE_CONTEO",
                    title: "SigmaV2 | Gestión de Marbetes - Auxiliar de Conteo"
                }
            }
        ]
    },

    // ==============================================
    // RUTA 404 - Not Found
    // ==============================================
    {
        path: "/:pathMatch(.*)*",
        name: "NotFound",
        component: NotFound,
        meta: {
            title: "SigmaV2 | Página no encontrada",
        },
    },
];

// ==============================================
// INICIALIZACIÓN DEL ROUTER
// ==============================================
const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes,
});

// ==============================================
// HELPER FUNCTIONS - JWT Validation
// ==============================================

/**
 * Valida el formato básico de un token JWT
 * @param token - Token JWT a validar
 * @returns true si el token tiene el formato correcto (3 partes separadas por punto)
 */
function isValidJWT(token: string | null): boolean {
    if (!token) return false;
    return token.split('.').length === 3;
}

/**
 * Decodifica un token JWT sin dependencias externas
 * @param token - Token JWT a decodificar
 * @returns Objeto con el payload decodificado o null si falla
 */
function decodeJWT(token: string | null): any | null {
    if (!token) return null;

    try {
        const parts = token.split('.');
        if (parts.length !== 3) return null;

        const payload = parts[1];
        if (!payload) return null;

        // Convertir base64url a base64
        let base64 = payload.replace(/-/g, '+').replace(/_/g, '/');

        // Agregar padding si es necesario
        while (base64.length % 4 !== 0) {
            base64 += '=';
        }

        // Decodificar base64
        const decodedStr = atob(base64);

        // Convertir a UTF-8 y parsear JSON
        const json = decodeURIComponent(
            Array.prototype.map.call(decodedStr, (c: string) => {
                return '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2);
            }).join('')
        );

        return JSON.parse(json);
    } catch (e) {
        return null;

    }
}

// ==============================================
// NAVIGATION GUARD - Autenticación y Autorización
// ==============================================
router.beforeEach((to: RouteLocationNormalized, from: RouteLocationNormalized, next: NavigationGuardNext) => {
    // Configuración
    const PUBLIC_PAGES = ["/", "/login", "/password-recovery", "/verifyEmail"];
    const isPublicPage = PUBLIC_PAGES.includes(to.path);

    const token = localStorage.getItem("token");
    const role = localStorage.getItem("role");



    // Helper: Limpiar autenticación
    const clearAuth = (): void => {
        localStorage.removeItem("token");
        localStorage.removeItem("role");
        localStorage.removeItem("username");
        localStorage.removeItem("editStoreId");
    };

    // Helper: Obtener ruta según rol
    const getRoleRoute = (userRole: string | null): string => {
        switch (userRole) {
            case "ADMINISTRADOR":
                return "/Admin";
            case "ALMACENISTA":
                return "/almacenista";
            case "AUXILIAR":
                return "/auxiliar";
            case "AUXILIAR_DE_CONTEO":
                return "/auxiliar-de-conteo";
            default:
                return "/login";
        }
    };

    // ==============================================
    // CASO 1: Usuario NO autenticado (sin token)
    // ==============================================
    if (!token) {
        if (isPublicPage) {
            return next(); // Permitir acceso a páginas públicas
        }
        return next("/login");
    }


    if (!isValidJWT(token)) {
        clearAuth();
        return isPublicPage ? next() : next("/login");
    }

    const decoded = decodeJWT(token);
    if (!decoded || typeof decoded !== 'object') {
        clearAuth();
        return isPublicPage ? next() : next("/login");
    }

    const currentTime = Math.floor(Date.now() / 1000);
    if (decoded.exp && decoded.exp < currentTime) {
        clearAuth();
        return isPublicPage ? next() : next("/login");
    }

    if (isPublicPage) {

        if (to.path === "/login") {
            return next();
        }

        const roleRoute = getRoleRoute(role);
        if (roleRoute !== "/login") {
            return next(roleRoute);
        }

        return next();
    }
    const requiredRole = to.meta?.role as string | undefined;
    if (requiredRole && requiredRole !== role) {
        clearAuth();
        return next("/login");
    }
    next();
});

export default router;

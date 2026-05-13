package tokai.com.mx.SIGMAV2.modules.labels.application.service;
/**
 * Puerto de entrada para obtener informacion del usuario autenticado.
 * Desacopla el controlador de la infraestructura del modulo de usuarios.
 */
public interface AuthenticatedUserService {
    Long getUserIdByEmail(String email);
}
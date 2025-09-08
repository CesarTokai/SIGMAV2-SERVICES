package tokai.com.mx.SIGMAV2.modules.warehouse.domain.port.input;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tokai.com.mx.SIGMAV2.modules.warehouse.adapter.web.dto.*;
import tokai.com.mx.SIGMAV2.modules.warehouse.domain.model.Warehouse;

import java.util.List;
import java.util.Optional;

public interface WarehouseService {

    // CRUD básico
    Warehouse create(WarehouseCreateDTO dto, Long createdBy);
    
    Warehouse update(Long id, WarehouseUpdateDTO dto, Long updatedBy);
    
    void delete(Long id, Long deletedBy);
    
    Optional<Warehouse> findById(Long id);
    
    Optional<Warehouse> findByWarehouseKey(String warehouseKey);
    
    // Listados y búsquedas
    List<Warehouse> findAll();
    
    Page<Warehouse> findAll(Pageable pageable);
    
    Page<Warehouse> findAllWithSearch(String search, Pageable pageable);
    
    // Validaciones
    boolean existsByWarehouseKey(String warehouseKey);
    
    boolean existsByNameWarehouse(String nameWarehouse);
    
    boolean existsByWarehouseKeyAndIdNot(String warehouseKey, Long id);
    
    boolean existsByNameWarehouseAndIdNot(String nameWarehouse, Long id);
    
    // Verificar dependencias antes de eliminar
    boolean hasDependencies(Long warehouseId);
    
    // Almacenes por usuario
    List<Warehouse> findWarehousesByUserId(Long userId);
    
    Page<Warehouse> findWarehousesByUserId(Long userId, Pageable pageable);
    
    // Verificar acceso
    boolean hasUserAccessToWarehouse(Long userId, Long warehouseId);
}
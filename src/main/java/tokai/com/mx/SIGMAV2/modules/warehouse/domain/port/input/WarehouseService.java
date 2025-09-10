package tokai.com.mx.SIGMAV2.modules.warehouse.domain.port.input;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import tokai.com.mx.SIGMAV2.modules.warehouse.adapter.web.dto.WarehouseCreateDTO;
import tokai.com.mx.SIGMAV2.modules.warehouse.adapter.web.dto.WarehouseUpdateDTO;
import tokai.com.mx.SIGMAV2.modules.warehouse.domain.model.Warehouse;

import java.util.List;
import java.util.Optional;

public interface WarehouseService {

    /// ///////////CRUD WAREHOUSE ////////////////////////////////////////////////////////////
    Warehouse createWarehouse(WarehouseCreateDTO dto, Long createdBy);
    Warehouse updateWarehouse(Long id, WarehouseUpdateDTO dto, Long updatedBy);
    void deleteWarehouse(Long id, Long deletedBy);
    List<Warehouse> findAllWarehouses();


    /// //////////////////////////////////////////////////////////////////////////////////////

    Optional<Warehouse> findByIdWarehouse(Long id);
    Optional<Warehouse> findByWarehouseKey(String warehouseKey);
    Page<Warehouse> findAllWarehouses(Pageable pageable);
    Page<Warehouse> findAllWithSearch(String search, Pageable pageable);
    boolean existsByWarehouseKey(String warehouseKey);
    boolean existsByNameWarehouse(String nameWarehouse);
    boolean existsByWarehouseKeyAndIdNot(String warehouseKey, Long id);
    boolean existsByNameWarehouseAndIdNot(String nameWarehouse, Long id);
    boolean hasDependencies(Long warehouseId);
    List<Warehouse> findWarehousesByUserId(Long userId);
    Page<Warehouse> findWarehousesByUserId(Long userId, Pageable pageable);
    List<Warehouse> findActiveWarehousesByUserId(Long userId);
    Page<Warehouse> findActiveWarehousesByUserId(Long userId, Pageable pageable);
}

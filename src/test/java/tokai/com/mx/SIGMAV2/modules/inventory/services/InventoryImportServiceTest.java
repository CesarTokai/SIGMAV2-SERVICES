package tokai.com.mx.SIGMAV2.modules.inventory.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import tokai.com.mx.SIGMAV2.modules.inventory.dto.ImportResultDto;
import tokai.com.mx.SIGMAV2.modules.inventory.entities.Period;
import tokai.com.mx.SIGMAV2.modules.inventory.entities.Product;
import tokai.com.mx.SIGMAV2.modules.inventory.entities.Warehouse;
import tokai.com.mx.SIGMAV2.modules.inventory.repositories.InventoryStockRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.repositories.InventoryPeriodRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.repositories.InventoryProductRepository;
import tokai.com.mx.SIGMAV2.modules.inventory.repositories.InventoryWarehouseRepository;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryImportServiceTest {
    
    @Mock
    private FileParserService fileParserService;
    
    @Mock
    private InventoryProductRepository productRepository;
    
    @Mock
    private InventoryWarehouseRepository warehouseRepository;
    
    @Mock
    private InventoryPeriodRepository periodRepository;
    
    @Mock
    private InventoryStockRepository inventoryStockRepository;
    
    @InjectMocks
    private InventoryImportService inventoryImportService;
    
    private Period testPeriod;
    private Warehouse testWarehouse;
    private Product testProduct;
    
    @BeforeEach
    void setUp() {
        testPeriod = new Period();
        testPeriod.setIdPeriod(1L);
        testPeriod.setPeriod(LocalDate.of(2025, 1, 1));
        testPeriod.setComments("Test period");
        
        testWarehouse = new Warehouse();
        testWarehouse.setIdWarehouse(1L);
        testWarehouse.setWarehouseKey("WH001");
        testWarehouse.setNameWarehouse("Test Warehouse");
        
        testProduct = new Product();
        testProduct.setIdProduct(1L);
        testProduct.setCveArt("PROD001");
        testProduct.setDescription("Test Product");
        testProduct.setUnitOfMeasure("PCS");
        testProduct.setStatus(Product.ProductStatus.A);
    }
    
    @Test
    void testImportInventory_PeriodNotFound() {
        // Arrange
        Long invalidPeriodId = 999L;
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "CVE_ART,DESCR,UNI_MED,EXIST,STATUS\nPROD001,Test,PCS,10,A".getBytes());
        
        when(periodRepository.findById(invalidPeriodId)).thenReturn(Optional.empty());
        
        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            inventoryImportService.importInventory(file, invalidPeriodId, 1L, InventoryImportService.ImportMode.MERGE, null);
        });
        
        assertEquals("Periodo no encontrado: 999", exception.getMessage());
    }
    
    @Test
    void testImportInventory_WarehouseNotFound() {
        // Arrange
        Long invalidWarehouseId = 999L;
        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", "CVE_ART,DESCR,UNI_MED,EXIST,STATUS\nPROD001,Test,PCS,10,A".getBytes());
        
        when(periodRepository.findById(1L)).thenReturn(Optional.of(testPeriod));
        when(warehouseRepository.findById(invalidWarehouseId)).thenReturn(Optional.empty());
        
        // Act & Assert
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            inventoryImportService.importInventory(file, 1L, invalidWarehouseId, InventoryImportService.ImportMode.MERGE, null);
        });
        
        assertEquals("Almacén no encontrado: 999", exception.getMessage());
    }
    
    @Test
    void testImportInventory_EmptyFile() throws Exception {
        // Arrange
        MockMultipartFile emptyFile = new MockMultipartFile("file", "test.csv", "text/csv", "".getBytes());
        
        when(periodRepository.findById(1L)).thenReturn(Optional.of(testPeriod));
        when(warehouseRepository.findById(1L)).thenReturn(Optional.of(testWarehouse));
        when(fileParserService.parseFile(any())).thenThrow(new IllegalArgumentException("El archivo CSV está vacío"));
        
        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            inventoryImportService.importInventory(emptyFile, 1L, 1L, InventoryImportService.ImportMode.MERGE, null);
        });
        
        assertTrue(exception.getMessage().contains("vacío"));
    }
}
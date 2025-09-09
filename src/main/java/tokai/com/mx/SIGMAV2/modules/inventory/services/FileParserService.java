package tokai.com.mx.SIGMAV2.modules.inventory.services;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import tokai.com.mx.SIGMAV2.modules.inventory.dto.InventoryImportRowDto;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class FileParserService {
    
    private static final String[] REQUIRED_HEADERS = {"CVE_ART", "DESCR", "UNI_MED", "EXIST", "STATUS"};
    
    public List<InventoryImportRowDto> parseFile(MultipartFile file) throws Exception {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("Archivo sin nombre");
        }
        
        if (filename.toLowerCase().endsWith(".xlsx") || filename.toLowerCase().endsWith(".xls")) {
            return parseExcelFile(file);
        } else if (filename.toLowerCase().endsWith(".csv")) {
            return parseCsvFile(file);
        } else {
            throw new IllegalArgumentException("Formato de archivo no soportado. Use XLSX, XLS o CSV.");
        }
    }
    
    private List<InventoryImportRowDto> parseExcelFile(MultipartFile file) throws Exception {
        List<InventoryImportRowDto> rows = new ArrayList<>();
        
        Workbook workbook = null;
        try {
            String filename = file.getOriginalFilename();
            if (filename != null && filename.toLowerCase().endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(file.getInputStream());
            } else {
                workbook = new HSSFWorkbook(file.getInputStream());
            }
            
            Sheet sheet = workbook.getSheetAt(0);
            
            // Validar encabezados
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new IllegalArgumentException("El archivo no contiene encabezados");
            }
            
            Map<String, Integer> headerMap = buildHeaderMap(headerRow);
            validateRequiredHeaders(headerMap);
            
            // Procesar filas de datos
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }
                
                InventoryImportRowDto dto = parseRowFromExcel(row, headerMap);
                dto.setRowNumber(i + 1);
                rows.add(dto);
            }
        } finally {
            if (workbook != null) {
                workbook.close();
            }
        }
        
        return rows;
    }
    
    private List<InventoryImportRowDto> parseCsvFile(MultipartFile file) throws Exception {
        List<InventoryImportRowDto> rows = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                throw new IllegalArgumentException("El archivo CSV está vacío");
            }
            
            String[] headers = headerLine.split(",");
            Map<String, Integer> headerMap = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                headerMap.put(headers[i].trim().toUpperCase(), i);
            }
            validateRequiredHeaders(headerMap);
            
            String line;
            int rowNumber = 2; // Empezar desde fila 2 (después de headers)
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }
                
                String[] values = line.split(",");
                InventoryImportRowDto dto = parseRowFromCsv(values, headerMap);
                dto.setRowNumber(rowNumber);
                rows.add(dto);
                rowNumber++;
            }
        }
        
        return rows;
    }
    
    private void validateRequiredHeaders(Map<String, Integer> headerMap) {
        for (String required : REQUIRED_HEADERS) {
            if (!headerMap.containsKey(required)) {
                throw new IllegalArgumentException("Falta el encabezado requerido: " + required);
            }
        }
    }
    
    private InventoryImportRowDto parseRowFromCsv(String[] values, Map<String, Integer> headerMap) {
        InventoryImportRowDto dto = new InventoryImportRowDto();
        
        dto.setCveArt(getValue(values, headerMap.get("CVE_ART")).trim());
        dto.setDescr(getValue(values, headerMap.get("DESCR")).trim());
        dto.setUniMed(getValue(values, headerMap.get("UNI_MED")).trim());
        
        String existStr = getValue(values, headerMap.get("EXIST")).trim();
        dto.setExist(existStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(existStr));
        
        String status = getValue(values, headerMap.get("STATUS")).trim().toUpperCase();
        dto.setStatus(status.isEmpty() ? "A" : status);
        
        // Warehouse key opcional
        if (headerMap.containsKey("WAREHOUSE_KEY")) {
            dto.setWarehouseKey(getValue(values, headerMap.get("WAREHOUSE_KEY")).trim());
        }
        
        return dto;
    }
    
    private String getValue(String[] values, Integer index) {
        if (index == null || index >= values.length) {
            return "";
        }
        return values[index];
    }
    
    private Map<String, Integer> buildHeaderMap(Row headerRow) {
        Map<String, Integer> headerMap = new HashMap<>();
        for (Cell cell : headerRow) {
            String header = getCellStringValue(cell).trim().toUpperCase();
            headerMap.put(header, cell.getColumnIndex());
        }
        return headerMap;
    }
    
    private InventoryImportRowDto parseRowFromExcel(Row row, Map<String, Integer> headerMap) {
        InventoryImportRowDto dto = new InventoryImportRowDto();
        
        dto.setCveArt(getCellStringValue(row.getCell(headerMap.get("CVE_ART"))).trim());
        dto.setDescr(getCellStringValue(row.getCell(headerMap.get("DESCR"))).trim());
        dto.setUniMed(getCellStringValue(row.getCell(headerMap.get("UNI_MED"))).trim());
        
        String existStr = getCellStringValue(row.getCell(headerMap.get("EXIST"))).trim();
        dto.setExist(existStr.isEmpty() ? BigDecimal.ZERO : new BigDecimal(existStr));
        
        String status = getCellStringValue(row.getCell(headerMap.get("STATUS"))).trim().toUpperCase();
        dto.setStatus(status.isEmpty() ? "A" : status);
        
        // Warehouse key opcional
        if (headerMap.containsKey("WAREHOUSE_KEY")) {
            dto.setWarehouseKey(getCellStringValue(row.getCell(headerMap.get("WAREHOUSE_KEY"))).trim());
        }
        
        return dto;
    }
    
    private String getCellStringValue(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf(cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
    
    private boolean isRowEmpty(Row row) {
        for (Cell cell : row) {
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                return false;
            }
        }
        return true;
    }
}
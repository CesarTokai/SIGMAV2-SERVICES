package tokai.com.mx.SIGMAV2.modules.inventory.dto;

public class ImportLogEntryDto {
    
    private int rowNumber;
    private String cveArt;
    private String operation; // INSERT, UPDATE, SKIP, ERROR
    private String description;
    private String previousValue;
    private String newValue;
    private String errorMessage;
    
    // Constructors
    public ImportLogEntryDto() {}
    
    public ImportLogEntryDto(int rowNumber, String cveArt, String operation, String description) {
        this.rowNumber = rowNumber;
        this.cveArt = cveArt;
        this.operation = operation;
        this.description = description;
    }
    
    // Static factory methods
    public static ImportLogEntryDto insert(int rowNumber, String cveArt, String description) {
        return new ImportLogEntryDto(rowNumber, cveArt, "INSERT", description);
    }
    
    public static ImportLogEntryDto update(int rowNumber, String cveArt, String description, String previousValue, String newValue) {
        ImportLogEntryDto entry = new ImportLogEntryDto(rowNumber, cveArt, "UPDATE", description);
        entry.setPreviousValue(previousValue);
        entry.setNewValue(newValue);
        return entry;
    }
    
    public static ImportLogEntryDto skip(int rowNumber, String cveArt, String reason) {
        return new ImportLogEntryDto(rowNumber, cveArt, "SKIP", reason);
    }
    
    public static ImportLogEntryDto error(int rowNumber, String cveArt, String errorMessage) {
        ImportLogEntryDto entry = new ImportLogEntryDto(rowNumber, cveArt, "ERROR", "Error de procesamiento");
        entry.setErrorMessage(errorMessage);
        return entry;
    }
    
    // Getters and Setters
    public int getRowNumber() {
        return rowNumber;
    }
    
    public void setRowNumber(int rowNumber) {
        this.rowNumber = rowNumber;
    }
    
    public String getCveArt() {
        return cveArt;
    }
    
    public void setCveArt(String cveArt) {
        this.cveArt = cveArt;
    }
    
    public String getOperation() {
        return operation;
    }
    
    public void setOperation(String operation) {
        this.operation = operation;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getPreviousValue() {
        return previousValue;
    }
    
    public void setPreviousValue(String previousValue) {
        this.previousValue = previousValue;
    }
    
    public String getNewValue() {
        return newValue;
    }
    
    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }
    
    public String getErrorMessage() {
        return errorMessage;
    }
    
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
    
    // Convertir a línea CSV
    public String toCsvLine() {
        return String.format("%d,%s,%s,\"%s\",\"%s\",\"%s\",\"%s\"",
            rowNumber,
            cveArt != null ? cveArt : "",
            operation,
            description != null ? description.replace("\"", "\"\"") : "",
            previousValue != null ? previousValue.replace("\"", "\"\"") : "",
            newValue != null ? newValue.replace("\"", "\"\"") : "",
            errorMessage != null ? errorMessage.replace("\"", "\"\"") : ""
        );
    }
    
    public static String getCsvHeader() {
        return "ROW_NUMBER,CVE_ART,OPERATION,DESCRIPTION,PREVIOUS_VALUE,NEW_VALUE,ERROR_MESSAGE";
    }
}
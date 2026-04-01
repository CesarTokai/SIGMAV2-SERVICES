# 🛠️ Compilar Plantilla JRXML a JASPER

**Problema:** El código espera `marbete_qr.jasper` (compilado) pero solo existe `marbete_qr.jrxml` (XML).

**Solución:** Compilar JRXML → JASPER

---

## 📋 Método 1: Usar JasperReports CLI (Recomendado)

### Paso 1: Descargar herramienta
```bash
# Ya está en tu proyecto Maven
# No necesitas descargar nada
```

### Paso 2: Compilar el JRXML

**Windows (PowerShell):**
```bash
cd "C:\Users\cesarg\Documents\DESARROLLO DE SOFTWARE\SIGMAV2-SERVICES"

# Navegar a carpeta de reportes
cd src\main\resources\reports

# Compilar con Maven
mvn net.sf.jasperreports:jasperreports-maven-plugin:compile
```

**O manualmente con Java:**
```bash
java -cp "path/to/jasperreports-6.21.5.jar:..." \
  net.sf.jasperreports.engine.JasperCompileManager \
  marbete_qr.jrxml
```

### Paso 3: Verificar
```bash
# Debe generar: marbete_qr.jasper
ls *.jasper
```

---

## 📋 Método 2: Plugin Maven (Lo correcto)

Agregar al `pom.xml` en `<build><plugins>`:

```xml
<plugin>
    <groupId>net.sf.jasperreports</groupId>
    <artifactId>jasperreports-maven-plugin</artifactId>
    <version>6.21.5</version>
    <configuration>
        <sourceDirectory>src/main/resources/reports</sourceDirectory>
        <outputDirectory>src/main/resources/reports</outputDirectory>
    </configuration>
    <executions>
        <execution>
            <phase>compile</phase>
            <goals>
                <goal>compile</goal>
            </goals>
        </execution>
    </executions>
</plugin>
```

Luego compilar:
```bash
cd "C:\Users\cesarg\Documents\DESARROLLO DE SOFTWARE\SIGMAV2-SERVICES"
.\mvnw.cmd clean compile
```

Esto compila automáticamente todos los `.jrxml` en `.jasper`.

---

## 📋 Método 3: Script Rápido (Windows PowerShell)

Crea un archivo `compile-reports.ps1`:

```powershell
# Compilar todos los JRXMLs a JASPER
$REPORTS_DIR = "src\main\resources\reports"
$JAVA_HOME = "C:\Program Files\Java\jdk-21"  # Ajusta según tu instalación

Get-ChildItem -Path $REPORTS_DIR -Filter "*.jrxml" | ForEach-Object {
    $filename = $_.BaseName
    Write-Host "Compilando: $filename" -ForegroundColor Cyan
    
    & "$JAVA_HOME\bin\java.exe" `
        -cp "." `
        net.sf.jasperreports.engine.JasperCompileManager `
        "$REPORTS_DIR\$($_.Name)"
    
    if (Test-Path "$REPORTS_DIR\$filename.jasper") {
        Write-Host "✅ $filename.jasper creado" -ForegroundColor Green
    } else {
        Write-Host "❌ Error compilando $filename" -ForegroundColor Red
    }
}
```

Ejecutar:
```bash
.\compile-reports.ps1
```

---

## ✅ Verificar

Después de compilar, debes tener:

```
src/main/resources/reports/
├── marbete_qr.jrxml       ← Original (XML)
├── marbete_qr.jasper      ← Compilado (BINARIO) ✅
├── distribucion_marbetes.jrxml
├── distribucion_marbetes.jasper
└── ...otros reportes
```

---

## 🚀 Una vez compilado

### Recompilación automática en Maven

Agregar en `pom.xml`:

```xml
<plugins>
    <!-- ...otros plugins... -->
    
    <plugin>
        <groupId>net.sf.jasperreports</groupId>
        <artifactId>jasperreports-maven-plugin</artifactId>
        <version>6.21.5</version>
        <configuration>
            <sourceDirectory>${project.basedir}/src/main/resources/reports</sourceDirectory>
            <outputDirectory>${project.basedir}/src/main/resources/reports</outputDirectory>
            <compiler>net.sf.jasperreports.engine.design.JRJdtCompiler</compiler>
        </configuration>
        <executions>
            <execution>
                <phase>process-resources</phase>
                <goals>
                    <goal>compile</goal>
                </goals>
            </execution>
        </executions>
        <dependencies>
            <dependency>
                <groupId>net.sf.jasperreports</groupId>
                <artifactId>jasperreports</artifactId>
                <version>6.21.5</version>
            </dependency>
        </dependencies>
    </plugin>
</plugins>
```

Ahora cada vez que corras `mvn clean install`, compilará automáticamente los JRXML.

---

## 🔍 Troubleshooting

### Error: "cannot find symbol: class JasperCompileManager"
**Causa:** Faltan las dependencias de JasperReports  
**Solución:** Verificar que `jasperreports` esté en `pom.xml` (ya está)

### Error: "marbete_qr.jrxml not found"
**Causa:** El archivo JRXML no está en `src/main/resources/reports/`  
**Solución:** Verificar ruta exacta:
```bash
ls -la src/main/resources/reports/marbete_qr.jrxml
```

### Error: "Compilation error" en JasperReports
**Causa:** Sintaxis JRXML inválida o campo no existe  
**Solución:** Revisar `marbete_qr.jrxml` - campos deben coincidir con `MarbeteReportDTO`:
- `nomMarbete`
- `clave`
- `descr`
- `codigo`
- `descripcion`
- `almacen`
- `fecha`
- `qrImage` (BufferedImage)

---

## 📝 Resumido

**Opción rápida (Comando único):**

```bash
cd "C:\Users\cesarg\Documents\DESARROLLO DE SOFTWARE\SIGMAV2-SERVICES"
.\mvnw.cmd clean compile
```

Esto compila el proyecto Y los reportes JRXML automáticamente.

---

**¡Listo! Después de compilar, ya puedes usar las APIs de QR.** 🚀


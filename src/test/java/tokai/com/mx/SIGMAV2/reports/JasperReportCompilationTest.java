package tokai.com.mx.SIGMAV2.reports;

import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.JRException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

class JasperReportCompilationTest {

    @Test
    void compileCartaTresCuadros() throws Exception {
        try (InputStream jrxmlStream = new ClassPathResource("reports/Carta_Tres_Cuadros.jrxml").getInputStream()) {
            try {
                JasperReport report = JasperCompileManager.compileReport(jrxmlStream);
                assertNotNull(report);
            } catch (JRException e) {
                Throwable root = e.getCause() != null ? e.getCause() : e;
                fail("JRXML no compiló: " + e.getMessage() + " | causa: " + root.getMessage());
            }
        }
    }
}

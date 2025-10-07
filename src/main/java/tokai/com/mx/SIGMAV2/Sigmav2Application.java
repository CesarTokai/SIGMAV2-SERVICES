package tokai.com.mx.SIGMAV2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@SpringBootApplication
@EntityScan(basePackages = {"tokai.com.mx.SIGMAV2.modules", "tokai.com.mx.SIGMAV2.shared"})
@EnableJpaRepositories(basePackages = {"tokai.com.mx.SIGMAV2.modules", "tokai.com.mx.SIGMAV2.shared"})
@EnableAsync
public class Sigmav2Application {

    public static void main(String[] args) {
        SpringApplication.run(Sigmav2Application.class, args);
    }

    // Task executor para operaciones de auditoría asíncronas
    @Bean(name = "auditThreadPool")
    public Executor auditThreadPool() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("audit-async-");
        executor.initialize();
        return executor;
    }

}

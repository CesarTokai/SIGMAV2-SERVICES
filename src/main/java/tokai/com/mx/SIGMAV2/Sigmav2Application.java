package tokai.com.mx.SIGMAV2;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EntityScan(basePackages = "tokai.com.mx.SIGMAV2.modules")
@EnableJpaRepositories(basePackages = "tokai.com.mx.SIGMAV2.modules")
public class Sigmav2Application {

	public static void main(String[] args) {
		SpringApplication.run(Sigmav2Application.class, args);
	}

}

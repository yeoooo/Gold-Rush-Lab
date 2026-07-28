package io.devyeoooo.Gold_Rush_Lab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.resilience.annotation.EnableResilientMethods;

@SpringBootApplication
@EnableResilientMethods
@EnableJpaAuditing
public class GoldRushLabApplication {

	public static void main(String[] args) {
		SpringApplication.run(GoldRushLabApplication.class, args);
	}

}

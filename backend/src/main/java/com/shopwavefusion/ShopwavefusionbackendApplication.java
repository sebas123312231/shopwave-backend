package com.shopwavefusion;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import io.swagger.v3.oas.annotations.servers.Server;
@OpenAPIDefinition(info = @Info(title = "ShopWave API", version = "1.0"),
        security = @SecurityRequirement(name = "bearerToken"),
        servers = @Server(url = "/", description = "Current server"))
@SecuritySchemes(@SecurityScheme(name = "bearerToken", type = SecuritySchemeType.HTTP,
        scheme = "bearer", bearerFormat = "JWT"))
@SpringBootApplication(scanBasePackages = "com.shopwavefusion.rework")
@EntityScan("com.shopwavefusion.rework.domain")
@EnableJpaRepositories("com.shopwavefusion.rework.repository")
@EnableMethodSecurity
public class ShopwavefusionbackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(ShopwavefusionbackendApplication.class, args);
	}

}

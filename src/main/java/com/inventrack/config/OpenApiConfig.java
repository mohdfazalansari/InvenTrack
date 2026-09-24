package com.inventrack.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "InvenTrack API",
                version = "1.0.0",
                description = "Role-based Inventory and Order Management REST API with Spring Boot 3, Spring Security, JWT, and PostgreSQL.",
                contact = @Contact(name = "InvenTrack Engineering Team")
        )
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        scheme = "bearer",
        description = "Enter JWT Bearer token obtained from /api/auth/login"
)
public class OpenApiConfig {
}

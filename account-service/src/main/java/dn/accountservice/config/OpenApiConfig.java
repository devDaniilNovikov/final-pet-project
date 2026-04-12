package dn.accountservice.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Account Service API",
                version = "1.0",
                description = "REST API for managing marketplace accounts, addresses and ban operations",
                contact = @Contact(name = "Danila Novikov")
        ),
        servers = @Server(url = "http://localhost:3000", description = "Local development server")
)
public class OpenApiConfig {
}


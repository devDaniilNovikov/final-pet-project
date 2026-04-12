package dn.orderservice.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Order Service API",
                version = "1.0",
                description = "REST API for managing marketplace orders with SAGA pattern support",
                contact = @Contact(name = "Danila Novikov")
        ),
        servers = @Server(url = "http://localhost:3001", description = "Local development server")
)
public class OpenApiConfig {
}


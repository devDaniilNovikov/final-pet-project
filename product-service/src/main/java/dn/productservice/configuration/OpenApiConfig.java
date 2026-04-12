package dn.productservice.configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Product Service API",
                version = "1.0",
                description = "REST API for managing marketplace products and categories",
                contact = @Contact(name = "Danila Novikov")
        ),
        servers = @Server(url = "http://localhost:3002", description = "Local development server")
)
public class OpenApiConfig {
}


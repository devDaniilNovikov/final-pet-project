package dn.productservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class ProductServiceApplication {

     static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }

}

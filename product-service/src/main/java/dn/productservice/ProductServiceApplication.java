package dn.productservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "dn.shared",
        "dn.productservice"
})
@EnableCaching
@EnableScheduling
public class ProductServiceApplication {

     static void main(String[] args) {
        SpringApplication.run(ProductServiceApplication.class, args);
    }

}

package dn.accountservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@SpringBootApplication(scanBasePackages = {"dn.shared",
        "dn.accountservice"
})
@EnableSpringDataWebSupport
@EnableCaching
public class AccountServiceApplication {

    static void main(String[] args) {
        SpringApplication.run(AccountServiceApplication.class, args);
    }

}

package dn.notificationservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;import org.springframework.cache.annotation.EnableCaching;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = {
        "dn.shared",
        "dn.notificationservice"
})
@EnableCaching
@EnableScheduling
@EnableKafka
public class NotificationServiceApplication {

     static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }

}

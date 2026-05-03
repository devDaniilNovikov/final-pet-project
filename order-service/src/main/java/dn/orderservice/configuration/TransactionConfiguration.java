package dn.orderservice.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@RequiredArgsConstructor
public class TransactionConfiguration {


    private final PlatformTransactionManager platformTransactionManager;
    private final TransactionDefinition transactionDefinition;
    private final TransactionManager transactionManager;



    @Bean
    public TransactionTemplate transactionTemplate(){
        TransactionTemplate transactionTemplate = new TransactionTemplate(
                platformTransactionManager,
                transactionDefinition
        );
        return new TransactionTemplate(platformTransactionManager,transactionDefinition);
    }



}

package dn.accountservice.config.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

@Configuration
public class KafkaConfig {



    @Value("${custom.kafka.topics.account-created}")
    private String accountCreatedTopic;

    @Value("${custom.kafka.topics.account-banned}")
    private String accountBannedTopic;

    @Value("${custom.kafka.topics.account-deleted}")
    private String accountDeletedTopic;


    @Bean
    public DefaultKafkaProducerFactory<String, Object> defaultKafkaProducerFactory(KafkaProperties properties) {
        Map<String, Object> props = properties.buildProducerProperties(null);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        return new DefaultKafkaProducerFactory<>(props);
    }


    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(KafkaProperties kafkaProperties) {
        return new KafkaTemplate<>(defaultKafkaProducerFactory(kafkaProperties));
    }

    @Bean
    public NewTopic accountCreateTopic() {
        return TopicBuilder.name(accountCreatedTopic)
                .replicas(3)
                .partitions(5)
                .build();
    }

    @Bean
    public NewTopic accountBannedTopic() {
        return TopicBuilder.name(accountBannedTopic)
                .replicas(3)
                .partitions(5)
                .build();
    }

    @Bean
    public NewTopic accountDeletedTopic() {
        return TopicBuilder.name(accountDeletedTopic)
                .replicas(3)
                .partitions(5)
                .build();
    }



}

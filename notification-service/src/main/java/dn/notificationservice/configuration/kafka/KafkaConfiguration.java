package dn.notificationservice.configuration.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;
@Configuration
@Slf4j
public class KafkaConfiguration {


    @Value("${custom.kafka.topics.account-created}")
    private String accountCreatedTopic;

    @Value("${custom.kafka.topics.account-banned}")
    private String accountBannedTopic;

    @Value("${custom.kafka.topics.account-deleted}")
    private String accountDeletedTopic;

    @Value("${app.kafka.custom-topics.order-service-dlt}")
    private String deadLetterTopic;

    @Value("${app.kafka.replicas.count.value}")
    private int replicasCount;

    @Value("${app.kafka.partitions.count.value}")
    private int partitionsCount;


    @Bean
    public DefaultKafkaProducerFactory<String, Object> defaultKafkaProducerFactory(KafkaProperties properties) {
        Map<String, Object> props = properties.buildProducerProperties(null);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        props.forEach(log::info);
        return new DefaultKafkaProducerFactory<>(props);
    }


    private NewTopic createTopic(String topicName) {
        return TopicBuilder.name(topicName)
                .replicas(replicasCount)
                .partitions(partitionsCount)
                .configs(Map.of(ProducerConfig.ACKS_CONFIG, "all"))
                .build();
    }

    @Bean
    public NewTopic createDeadLetterTopic() {
        return createTopic(deadLetterTopic);
    }


    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(KafkaProperties kafkaProperties) {
        return new KafkaTemplate<>(defaultKafkaProducerFactory(kafkaProperties));
    }

    @Bean
    public NewTopic accountCreateTopic() {
        return createTopic(accountCreatedTopic);
    }

    @Bean
    public NewTopic accountBannedTopic() {
        return createTopic(accountBannedTopic);
    }

    @Bean
    public NewTopic accountDeletedTopic() {
        return createTopic(accountDeletedTopic);
    }



}

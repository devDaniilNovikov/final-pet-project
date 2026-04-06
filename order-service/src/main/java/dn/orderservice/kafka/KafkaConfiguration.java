package dn.orderservice.kafka;

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
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.retrytopic.DeadLetterPublishingRecovererFactory;
import org.springframework.kafka.retrytopic.DestinationTopic;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;

@Configuration
public class KafkaConfiguration {

    @Value("${app.kafka.events.product-created}")
    private String productCreatedEvent;

    @Value("${app.kafka.events.product-updated}")
    private String productUpdatedEvent;

    @Value("${app.kafka.events.product-deleted}")
    private String productDeletedEvent;

    @Value("${app.kafka.custom-topics.order-service-dlt}")
    private String orderDltTopic;




    @Bean
    public ProducerFactory<String, Object> defaultKafkaProducerFactory(KafkaProperties properties) {
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
    public DefaultErrorHandler errorOrderHandler(KafkaTemplate<String,Object> kafkaTemplate){
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        FixedBackOff fixedBackOff = new FixedBackOff(1000L,3);
        return new DefaultErrorHandler(recoverer,fixedBackOff);
    }



    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(KafkaProperties kafkaProperties) {
        return new KafkaTemplate<>(defaultKafkaProducerFactory(kafkaProperties));
    }

    @Bean
    public NewTopic productCreatedTopic() {
        return TopicBuilder.name(productCreatedEvent)
                .replicas(3)
                .partitions(5)
                .build();
    }

    @Bean
    public NewTopic dltTopic() {
        return TopicBuilder.name(orderDltTopic)
                .replicas(3)
                .partitions(5)
                .build();
    }

    @Bean
    public NewTopic productUpdatedTopic() {
        return TopicBuilder.name(productUpdatedEvent)
                .replicas(3)
                .partitions(5)
                .build();
    }

    @Bean
    public NewTopic productDeletedTopic() {
        return TopicBuilder.name(productDeletedEvent)
                .replicas(3)
                .partitions(5)
                .build();
    }
}

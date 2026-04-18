package dn.productservice.configuration;

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
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.util.backoff.FixedBackOff;

import java.util.Map;

@Configuration
public class KafkaConfiguration {

    @Value("${app.kafka.events.item-reserved}")
    private String itemReservedEvent;

    @Value("${app.kafka.events.item-reserved-failed}")
    private String itemReservedFailedEvent;

    @Value("${app.kafka.events.payment-success}")
    private String paymentSuccessEvent;

    @Value("${app.kafka.events.payment-failed}")
    private String paymentFailedEvent;

    @Value("${app.kafka.custom-topic.product-servive-dlt}")
    private String productDltTopic;


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
    public ProducerFactory<String, String> stringKafkaProducerFactory(KafkaProperties properties) {
        Map<String, Object> props = properties.buildProducerProperties(null);
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(ProducerConfig.ACKS_CONFIG, "all");
        props.put(ProducerConfig.RETRIES_CONFIG, 3);
        props.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        props.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        return new DefaultKafkaProducerFactory<>(props);
    }

    @Bean
    public DefaultErrorHandler productDeadLetterTopic(KafkaTemplate<String,Object> kafkaTemplate){
        DeadLetterPublishingRecoverer deadLetterPublishingRecoverer = new DeadLetterPublishingRecoverer(kafkaTemplate);
        FixedBackOff fixedBackOff = new FixedBackOff(1000,3);
        return new DefaultErrorHandler(deadLetterPublishingRecoverer,fixedBackOff);
    }

    @Bean
    public DefaultErrorHandler productStringDeadLetterTopic(KafkaTemplate<String,String> stringKafkaTemplate){
        DeadLetterPublishingRecoverer deadLetterPublishingRecoverer = new DeadLetterPublishingRecoverer(stringKafkaTemplate);
        FixedBackOff fixedBackOff = new FixedBackOff(1000,3);
        return new DefaultErrorHandler(deadLetterPublishingRecoverer,fixedBackOff);

    }


    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(KafkaProperties kafkaProperties) {
        return new KafkaTemplate<>(defaultKafkaProducerFactory(kafkaProperties));
    }

    @Bean
    public KafkaTemplate<String, String> stringKafkaTemplate(KafkaProperties kafkaProperties) {
        return new KafkaTemplate<>(stringKafkaProducerFactory(kafkaProperties));
    }

    @Bean
    public NewTopic itemReservedTopic() {
        return TopicBuilder.name(itemReservedEvent)
                .replicas(3)
                .partitions(5)
                .build();
    }

    @Bean
    public NewTopic itemFailedReservedTopic() {
        return TopicBuilder.name(itemReservedFailedEvent)
                .replicas(3)
                .partitions(5)
                .build();
    }

    @Bean
    public NewTopic productDltTopic(){
        return TopicBuilder.name(productDltTopic)
                .replicas(3)
                .partitions(6)
                .build();
    }

    @Bean
    public NewTopic paymentSuccessTopic() {
        return TopicBuilder.name(paymentSuccessEvent)
                .replicas(3)
                .partitions(5)
                .build();
    }

    @Bean
    public NewTopic paymentFailedTopic() {
        return TopicBuilder.name(paymentFailedEvent)
                .replicas(3)
                .partitions(5)
                .build();
    }
}

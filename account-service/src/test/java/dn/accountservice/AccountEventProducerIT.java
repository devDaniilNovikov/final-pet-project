package dn.accountservice;

import dn.accountservice.config.kafka.AccountEventProducer;
import dn.accountservice.event.AccountBannedEvent;
import dn.accountservice.event.AccountCreatedEvent;
import dn.accountservice.event.AccountDeletedEvent;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.MessageListener;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.ContainerTestUtils;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EmbeddedKafka(partitions = 1, brokerProperties = { "listeners=PLAINTEXT://localhost:9092", "port=9092" })
@ActiveProfiles("test")
public class AccountEventProducerIT {

    @MockBean
    private DataSource dataSource;

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.autoconfigure.exclude", () ->
            "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
            "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
            "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration");
        registry.add("spring.liquibase.enabled", () -> "false");
        registry.add("spring.cache.type", () -> "none");
    }

    @Autowired
    private AccountEventProducer accountEventProducer;

    @Autowired
    private EmbeddedKafkaBroker embeddedKafkaBroker;

    @Value("${custom.kafka.topics.account-created}")
    private String accountCreatedTopic;

    @Value("${custom.kafka.topics.account-banned}")
    private String accountBannedTopic;

    @Value("${custom.kafka.topics.account-deleted}")
    private String accountDeletedTopic;

    private KafkaMessageListenerContainer<String, Object> container;
    private BlockingQueue<ConsumerRecord<String, Object>> records;

    @BeforeEach
    void setUp() {
        records = new LinkedBlockingQueue<>();
        Map<String, Object> consumerProps = KafkaTestUtils.consumerProps("test-group", "true", embeddedKafkaBroker);
        consumerProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");

        DefaultKafkaConsumerFactory<String, Object> consumerFactory = new DefaultKafkaConsumerFactory<>(
                consumerProps,
                new StringDeserializer(),
                new JsonDeserializer<>(Object.class, false)
        );

        ContainerProperties containerProperties = new ContainerProperties(
                accountCreatedTopic, accountBannedTopic, accountDeletedTopic
        );

        container = new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
        container.setupMessageListener((MessageListener<String, Object>) records::add);
        container.start();
        ContainerTestUtils.waitForAssignment(container, embeddedKafkaBroker.getPartitionsPerTopic() * 3);
    }

    @AfterEach
    void tearDown() {
        if (container != null) {
            container.stop();
        }
    }

    @Test
    void sendAccountCreatedEvent_sendsToKafka() throws InterruptedException {
        // Arrange
        records.clear();
        String accountId = UUID.randomUUID().toString();
        AccountCreatedEvent event = new AccountCreatedEvent("testuser", accountId);

        // Act
        accountEventProducer.sendAccountCreatedEvent(event);

        // Assert
        ConsumerRecord<String, Object> singleRecord = records.poll(5, TimeUnit.SECONDS);
        assertThat(singleRecord).isNotNull();
        assertThat(singleRecord.topic()).isEqualTo(accountCreatedTopic);
        
        // Deserialize and check payload (Since it's Object.class deserialized by Jackson, it's a Map)
        Map<String, Object> payload = (Map<String, Object>) singleRecord.value();
        assertThat(payload.get("id")).isEqualTo(accountId);
        assertThat(payload.get("username")).isEqualTo("testuser");
    }

    @Test
    void sendAccountBannedEvent_sendsToKafka() throws InterruptedException {
        // Arrange
        records.clear();
        String accountId = UUID.randomUUID().toString();
        AccountBannedEvent event = new AccountBannedEvent(accountId, Instant.now().plusSeconds(3600), "Spam");

        // Act
        accountEventProducer.sendAccountBannedEvent(event);

        // Assert
        ConsumerRecord<String, Object> singleRecord = records.poll(5, TimeUnit.SECONDS);
        assertThat(singleRecord).isNotNull();
        assertThat(singleRecord.topic()).isEqualTo(accountBannedTopic);

        Map<String, Object> payload = (Map<String, Object>) singleRecord.value();
        assertThat(payload.get("id")).isEqualTo(accountId);
        assertThat(payload.get("reason")).isEqualTo("Spam");
    }

    @Test
    void sendAccountDeletedEvent_sendsToKafka() throws InterruptedException {
        // Arrange
        records.clear();
        String accountId = UUID.randomUUID().toString();
        AccountDeletedEvent event = new AccountDeletedEvent(accountId, Instant.now());

        // Act
        accountEventProducer.sendAccountDeletedEvent(event);

        // Assert
        ConsumerRecord<String, Object> singleRecord = records.poll(5, TimeUnit.SECONDS);
        assertThat(singleRecord).isNotNull();
        assertThat(singleRecord.topic()).isEqualTo(accountDeletedTopic);

        Map<String, Object> payload = (Map<String, Object>) singleRecord.value();
        assertThat(payload.get("id")).isEqualTo(accountId);
    }
}

package dn.notificationservice.kafka;
import dn.notificationservice.service.NotificationService;
import dn.shared.event.account.AccountCreatedEvent;
import dn.shared.event.account.AccountDeletedEvent;import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;import java.util.UUID;


@RequiredArgsConstructor
@Component
@Slf4j
public class AccountEventListener {

    private final NotificationService notificationService;


    @KafkaListener(
            topics = "${app.kafka.events.topics.account-created}",
            groupId = "notification-service-group",
            concurrency = "true",
            splitIterables = true
    )
    public void listenAccountCreatedEvent(AccountCreatedEvent accountCreatedEvent){
        UUID eventId = accountCreatedEvent.eventId();
        UUID accountId = accountCreatedEvent.accountId();
        notificationService.createAccountCreatedNotification(eventId,accountId, accountCreatedEvent.username());
        log.info("Received AccountCreatedEvent for accountId={}, eventId={}",accountCreatedEvent,eventId);
    }

    @KafkaListener(
            topics = "${app.kafka.events.topics.account-deleted}",
            groupId = "notification-service-group",
            concurrency = "true",
            splitIterables = true
    )
    public void listenAccountDeletedEvent(AccountDeletedEvent accountDeletedEvent){
        UUID eventId = accountDeletedEvent.eventId();
        UUID accountId = accountDeletedEvent.id();
        notificationService.createAccountDeletedNotification(eventId,accountId, accountDeletedEvent.username());
        log.info("Received AccountDeletedEvent for accountId={}, eventId={}",accountDeletedEvent,eventId);
    }

}

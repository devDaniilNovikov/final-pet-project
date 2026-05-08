package dn.notificationservice.service;

import dn.notificationservice.entity.NotificationEntity;
import dn.notificationservice.enums.NotificationChannel;
import dn.notificationservice.enums.NotificationStatus;
import dn.notificationservice.enums.NotificationType;
import dn.notificationservice.exception.IdempotencyException;
import dn.notificationservice.exception.NotificationNotFoundException;
import dn.notificationservice.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.MessageFormat;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {



    private final NotificationRepository notificationRepository;

    @Transactional
    public void createAccountCreatedNotification(UUID sourceEventId,
                                                 UUID recipientAccountId,
                                                 String recipientContact) {
        validIdempotency(sourceEventId,recipientAccountId);
        NotificationEntity notification = buildNotification(
                sourceEventId,
                recipientAccountId,
                recipientContact,
                NotificationType.ACCOUNT_CREATED,
                NotificationChannel.PUSH
        );
        log.info("Built notification is: {}",notification);
        try {
            notificationRepository.save(notification);
        } catch (DataIntegrityViolationException exception) {
            validIdempotency(sourceEventId,recipientAccountId);
            throw exception;
        }
    }

    @Transactional
    public void createAccountDeletedNotification(UUID sourceEventId,
                                                 UUID recipientAccountId,
                                                 String recipientContact) {
        validIdempotency(sourceEventId,recipientAccountId);
        NotificationEntity notification = buildNotification(
                sourceEventId,
                recipientAccountId,
                recipientContact,
                NotificationType.ACCOUNT_DELETED,
                NotificationChannel.PUSH
        );
        log.info("Built notification is: {}",notification);
        try {
            notificationRepository.save(notification);
        } catch (DataIntegrityViolationException exception) {
            throw new RuntimeException(exception.getMessage());
        }
    }

    public List<NotificationEntity> findAllByStatus(int pageNumber, int pageSize) {
        var pageRequest = PageRequest.of(pageNumber, pageSize);
        return notificationRepository.findAllByNotificationStatus(
                pageRequest,
                NotificationStatus.FAILED
        );
    }





    @Transactional
    public void createOrderCreatedNotification(UUID sourceEventId,
                                               UUID buyerAccountId,
                                               String buyerContact,
                                               UUID sellerId,
                                               String sellerContact) {
        validIdempotency(sourceEventId,buyerAccountId);
        validIdempotency(sourceEventId,sellerId);
        NotificationEntity buyerNotification = buildNotification(
                sourceEventId,
                buyerAccountId,
                buyerContact,
                NotificationType.ORDER_CREATED,
                NotificationChannel.EMAIL);
        NotificationEntity sellerNotification = buildNotification(
                sourceEventId,
                sellerId,
                sellerContact,
                NotificationType.ORDER_CREATED,
                NotificationChannel.EMAIL);
        log.info("Built notification for buyer: {}, seller: {}",buyerNotification,sellerNotification);
        try {
            notificationRepository.save(buyerNotification);
            notificationRepository.save(sellerNotification);
        } catch (DataIntegrityViolationException exception) {
            validIdempotency(sourceEventId,buyerAccountId);
            throw exception;
        }
    }

    private NotificationEntity buildNotification(UUID sourceEventId,
                                                 UUID recipientAccountId,
                                                 String recipientContact,
                                                 NotificationType notificationType,
                                                 NotificationChannel notificationChannel){
        String title = switch (notificationType) {
            case ACCOUNT_CREATED -> "Добро пожаловать";
            case ORDER_CREATED -> "Ваш заказ оформлен";
            case ACCOUNT_BANNED -> "Аккаунт заблокирован";
            case ACCOUNT_UNBANNED -> "Аккаунт разблокирован";
            default -> throw new IllegalArgumentException("Unknown notification type");
        };
        String message = switch (notificationType) {
            case ACCOUNT_CREATED -> "Аккаунт успешно создан";
            case ORDER_CREATED -> "Ваш заказ принят в обработку";
            case ACCOUNT_BANNED -> "Ваш аккаунт был заблокирован";
            case ACCOUNT_UNBANNED -> "Ваш аккаунт был разблокирован";
            default ->  throw new IllegalArgumentException("Unknown notification type");
        };
        return NotificationEntity.builder()
                .sourceEventId(sourceEventId)
                .recipientContact(recipientContact)
                .recipientAccountId(recipientAccountId)
                .notificationType(notificationType)
                .notificationChannel(notificationChannel)
                .notificationStatus(NotificationStatus.PENDING)
                .title(title)
                .message(message)
                .build();
    }

    private void validIdempotency(UUID sourceEventId,
                                  UUID recipientAccountId){
        if (notificationRepository.existsBySourceEventIdAndRecipientAccountId(sourceEventId,recipientAccountId)) {
            throw new IdempotencyException(
                    MessageFormat.format("Notification with sourceEventId: {0} already exists", sourceEventId),
                    HttpStatus.BAD_REQUEST
            );

        }
    }

    public NotificationEntity findById(UUID id){
        return notificationRepository.findById(id)
                .orElseThrow(()->new NotificationNotFoundException(
                        MessageFormat.format("Notification with id: {0} not found",id),
                        HttpStatus.NOT_FOUND
                ));

    }

    public List<UUID> getFailedNotification (List<NotificationEntity> notifications){
        return notifications.stream()
                .filter(notification -> notification!=null && notification.getRecipientAccountId()!=null)
                .map(NotificationEntity::getId)
                .toList();

    }



}

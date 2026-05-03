package dn.notificationservice.entity;

import dn.notificationservice.enums.NotificationChannel;
import dn.notificationservice.enums.NotificationStatus;
import dn.notificationservice.enums.NotificationType;
import jakarta.persistence.*;
import lombok.*;
import org.apache.commons.lang3.builder.EqualsBuilder;import org.apache.commons.lang3.builder.HashCodeBuilder;import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Table(
        schema = "marketplace",
        name = "notifications",
        indexes = {
                @Index(name = "idx_notifications_recipient_account_id",  columnList = "recipient_account_id"),
                @Index(name = "idx_notifications_status", columnList = "status")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_notifications_source_event_id",
                                  columnNames = {"source_event_id", "recipient_account_id"})
        }
)
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "recipient_account_id", nullable = false)
    private UUID recipientAccountId;

    @Column(name = "source_event_id", nullable = false)
    private UUID sourceEventId;

    @Column(name = "notification_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationType notificationType;

    @Column(name = "channel", nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationChannel notificationChannel;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private NotificationStatus notificationStatus;

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "recipient_contact", nullable = false)
    private String recipientContact;

    @Column(name = "message", nullable = false)
    private String message;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "sent_at")
    private Instant sentAt;

    @Column(name = "retry_count",nullable = false)
    private Integer retryCount;

    @Column(name = "next_retry_time")
    private Instant nextRetryTime;

    @Column(name = "failure_reason")
    private String failureReason;

    @PrePersist
    public void prePersist() {
        this.notificationStatus = NotificationStatus.PENDING;
        this.retryCount = 0;
        this.nextRetryTime = Instant.now();
    }





    @Override public boolean equals(Object o) {
    if (this == o) return true;

    if (o == null || getClass() != o.getClass()) return false;

    NotificationEntity that = (NotificationEntity) o;

    return new EqualsBuilder()
            .append(id, that.id)
            .append(recipientAccountId, that.recipientAccountId)
            .append(sourceEventId, that.sourceEventId)
            .isEquals();
}



   @Override
    public int hashCode() {
    return new HashCodeBuilder(17, 37)
            .append(id).append(recipientAccountId)
            .append(sourceEventId).toHashCode();
}}

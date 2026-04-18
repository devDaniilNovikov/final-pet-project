package dn.shared.outbox;

import dn.shared.outbox.OutboxStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@Table(schema = "marketplace",name = "outbox")
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OutboxEntity {


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id",nullable = false)
    private UUID id;

    @Column(name = "aggregate_id",nullable = false)
    private UUID aggregateId;

    @Column(name = "topic",nullable = false)
    private String topic;

    @Column(name = "payload",nullable = false)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "outbox_status",nullable = false)
    private OutboxStatus outboxStatus;

    @Column(name = "created_at",nullable = false)
    @CreationTimestamp
    @DateTimeFormat(pattern = "yyyy-Mm-dd")
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @Override
    public String toString() {
        return "OutboxEntity{" +
                "id=" + id +
                ", aggregateId=" + aggregateId +
                ", topic='" + topic + '\'' +
                ", payload='" + payload + '\'' +
                ", outboxStatus=" + outboxStatus +
                ", createdAt=" + createdAt +
                ", processedAt=" + processedAt +
                '}';
    }
}

package dn.orderservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@Table(schema = "marketplace",name = "processed_events")
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessedEventEntity {

    @Id
    @Column(name = "event_id",nullable = false)
    private UUID id;
}

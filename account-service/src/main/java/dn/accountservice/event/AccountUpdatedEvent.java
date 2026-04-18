package dn.accountservice.event;

import lombok.Builder;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Builder
public record AccountUpdatedEvent(String id,
                                  Instant updatedTime){}

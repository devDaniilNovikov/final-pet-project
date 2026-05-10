package dn.accountservice.service;

import dn.accountservice.entity.AccountEntity;
import dn.shared.event.account.*;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class AccountEventFactory {

    public AccountCreatedEvent createAccountCreatedEvent(AccountEntity accountEntity){
        return AccountCreatedEvent.builder()
                .id(accountEntity.getId())
                .username(accountEntity.getUsername())
                .eventId(UUID.randomUUID())
                .build();

    }

    public AccountUpdatedEvent createAccountUpdatedEvent(AccountEntity accountEntity){
        return AccountUpdatedEvent.builder()
                .id(accountEntity.getId())
                .eventId(UUID.randomUUID())
                .updatedTime(Instant.now())
                .build();
    }



    public AccountBannedEvent createAccountBannedEvent(AccountEntity accountEntity,
                                                      String reason,
                                                      Instant unbanDate){
        return AccountBannedEvent.builder()
                .id(accountEntity.getId())
                .eventId(UUID.randomUUID())
                .reason(reason)
                .unbanDate(unbanDate)
                .build();

    }

    public AccountUnbannedEvent createAccountUnbannedEvent(AccountEntity accountEntity){
        return AccountUnbannedEvent.builder()
                .id(accountEntity.getId())
                .eventId(UUID.randomUUID())
                .unbannedDate(Instant.now())
                .build();

    }

    public AccountDeletedEvent createAccountDeletedEvent(AccountEntity accountEntity){
        return AccountDeletedEvent.builder()
                .id(accountEntity.getId())
                .eventId(UUID.randomUUID())
                .deletedDate(Instant.now())
                .build();

    }
}

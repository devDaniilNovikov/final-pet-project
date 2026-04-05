package dn.accountservice.service;

import dn.accountservice.config.kafka.AccountEventProducer;
import dn.accountservice.dto.BanRequest;
import dn.accountservice.dto.account.AccountRequest;
import dn.accountservice.dto.account.AccountResponse;
import dn.accountservice.dto.account.ListAccountResponse;
import dn.accountservice.entity.AccountEntity;
import dn.accountservice.event.AccountBannedEvent;
import dn.accountservice.event.AccountCreatedEvent;
import dn.accountservice.event.AccountDeletedEvent;
import dn.accountservice.exception.AccountNotFoundException;
import dn.accountservice.mapper.AccountMapper;
import dn.accountservice.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private AccountMapper accountMapper;

    @Mock
    private AccountEventProducer accountEventProducer;

    @InjectMocks
    private AccountService accountService;

    @Captor
    private ArgumentCaptor<AccountCreatedEvent> createdEventCaptor;

    @Captor
    private ArgumentCaptor<AccountDeletedEvent> deletedEventCaptor;

    @Captor
    private ArgumentCaptor<AccountBannedEvent> bannedEventCaptor;

    private UUID accountId;
    private String accountIdStr;
    private AccountEntity accountEntity;
    private AccountResponse accountResponse;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        accountIdStr = accountId.toString();

        accountEntity = new AccountEntity();
        accountEntity.setId(accountId);
        accountEntity.setUsername("testuser");
        accountEntity.setEmail("test@example.com");

        accountResponse = AccountResponse.builder()
                .id(accountIdStr)
                .username("testuser")
                .email("test@example.com")
                .build();
    }

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("должен вернуть аккаунт когда он существует")
        void shouldReturnAccountWhenExists() {
            when(accountRepository.findById(accountId)).thenReturn(Optional.of(accountEntity));
            when(accountMapper.toResponse(accountEntity)).thenReturn(accountResponse);

            AccountResponse result = accountService.findById(accountIdStr);

            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(accountIdStr);
            assertThat(result.getUsername()).isEqualTo("testuser");
            verify(accountRepository).findById(accountId);
        }

        @Test
        @DisplayName("должен выбросить AccountNotFoundException когда аккаунт не найден")
        void shouldThrowWhenNotFound() {
            when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.findById(accountIdStr))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessageContaining(accountIdStr);
        }

        @Test
        @DisplayName("должен выбросить IllegalArgumentException при невалидном UUID")
        void shouldThrowOnInvalidUuid() {
            assertThatThrownBy(() -> accountService.findById("not-a-uuid"))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }

    @Nested
    @DisplayName("findAccountByName")
    class FindAccountByName {

        @Test
        @DisplayName("должен вернуть аккаунт по имени")
        void shouldReturnAccountByName() {
            when(accountRepository.findByUsername("testuser")).thenReturn(Optional.of(accountEntity));
            when(accountMapper.toResponse(accountEntity)).thenReturn(accountResponse);

            AccountResponse result = accountService.findAccountByName("testuser");

            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("должен выбросить AccountNotFoundException когда имя не найдено")
        void shouldThrowWhenNameNotFound() {
            when(accountRepository.findByUsername("unknown")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.findAccountByName("unknown"))
                    .isInstanceOf(AccountNotFoundException.class)
                    .hasMessageContaining("unknown");
        }
    }

    @Nested
    @DisplayName("createAccount")
    class CreateAccount {

        @Test
        @DisplayName("должен создать аккаунт и отправить событие в Kafka")
        void shouldCreateAndSendEvent() {
            AccountRequest request = AccountRequest.builder()
                    .username("newuser")
                    .email("new@example.com")
                    .build();

            when(accountMapper.toEntity(request)).thenReturn(accountEntity);
            when(accountRepository.save(accountEntity)).thenReturn(accountEntity);
            when(accountMapper.toResponse(accountEntity)).thenReturn(accountResponse);

            AccountResponse result = accountService.createAccount(request);

            assertThat(result).isNotNull();
            verify(accountRepository).save(accountEntity);
            verify(accountEventProducer).sendAccountCreatedEvent(createdEventCaptor.capture());

            AccountCreatedEvent event = createdEventCaptor.getValue();
            assertThat(event.username()).isEqualTo("testuser");
            assertThat(event.id()).isEqualTo(accountIdStr);
        }
    }

    @Nested
    @DisplayName("updateAccount")
    class UpdateAccount {

        @Test
        @DisplayName("должен обновить аккаунт когда он существует")
        void shouldUpdateWhenExists() {
            AccountRequest request = AccountRequest.builder()
                    .username("updated")
                    .email("updated@example.com")
                    .build();

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(accountEntity));
            when(accountRepository.save(accountEntity)).thenReturn(accountEntity);

            accountService.updateAccount(accountIdStr, request);

            verify(accountMapper).updateEntity(request, accountEntity);
            verify(accountRepository).save(accountEntity);
        }

        @Test
        @DisplayName("должен выбросить AccountNotFoundException при обновлении несуществующего аккаунта")
        void shouldThrowWhenUpdateNotFound() {
            AccountRequest request = AccountRequest.builder()
                    .username("updated")
                    .build();

            when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.updateAccount(accountIdStr, request))
                    .isInstanceOf(AccountNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("findAll")
    class FindAll {

        @Test
        @DisplayName("должен вернуть список аккаунтов с пагинацией")
        void shouldReturnPaginatedAccounts() {
            PageRequest pageRequest = PageRequest.of(0, 10);
            PageImpl<AccountEntity> page = new PageImpl<>(List.of(accountEntity));

            when(accountRepository.findAll(pageRequest)).thenReturn(page);
            when(accountMapper.toResponse(accountEntity)).thenReturn(accountResponse);

            ListAccountResponse result = accountService.findAll(10, 0);

            assertThat(result.getAccounts()).hasSize(1);
            assertThat(result.getAccounts().get(0).getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("должен вернуть пустой список когда аккаунтов нет")
        void shouldReturnEmptyList() {
            PageRequest pageRequest = PageRequest.of(0, 10);
            PageImpl<AccountEntity> emptyPage = new PageImpl<>(Collections.emptyList());

            when(accountRepository.findAll(pageRequest)).thenReturn(emptyPage);

            ListAccountResponse result = accountService.findAll(10, 0);

            assertThat(result.getAccounts()).isEmpty();
        }
    }

    @Nested
    @DisplayName("getAccountsByIds")
    class GetAccountsByIds {

        @Test
        @DisplayName("должен вернуть аккаунты по списку id")
        void shouldReturnAccountsByIds() {
            List<String> ids = List.of(accountIdStr);
            List<AccountEntity> entities = List.of(accountEntity);
            List<AccountResponse> responses = List.of(accountResponse);

            when(accountRepository.findAllById(List.of(accountId))).thenReturn(entities);
            when(accountMapper.toResponseList(entities)).thenReturn(responses);

            ListAccountResponse result = accountService.getAccountsByIds(ids);

            assertThat(result.getAccounts()).hasSize(1);
        }

        @Test
        @DisplayName("должен выбросить IllegalArgumentException при пустом списке id")
        void shouldThrowOnEmptyIds() {
            assertThatThrownBy(() -> accountService.getAccountsByIds(Collections.emptyList()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("empty");
        }
    }

    @Nested
    @DisplayName("deleteAccountById")
    class DeleteAccountById {

        @Test
        @DisplayName("должен удалить аккаунт и отправить событие")
        void shouldDeleteAndSendEvent() {
            accountService.deleteAccountById(accountIdStr);

            verify(accountRepository).deleteById(accountId);
            verify(accountEventProducer).sendAccountDeletedEvent(deletedEventCaptor.capture());

            AccountDeletedEvent event = deletedEventCaptor.getValue();
            assertThat(event.id()).isEqualTo(accountIdStr);
            assertThat(event.deletedDate()).isNotNull();
        }
    }

    @Nested
    @DisplayName("deleteAccountsByIds")
    class DeleteAccountsByIds {

        @Test
        @DisplayName("должен удалить аккаунты и отправить события для каждого")
        void shouldDeleteAllAndSendEvents() {
            UUID id2 = UUID.randomUUID();
            List<String> ids = List.of(accountIdStr, id2.toString());

            accountService.deleteAccountsByIds(ids);

            verify(accountRepository).deleteAllByIdInBatch(List.of(accountId, id2));
            verify(accountEventProducer, times(2)).sendAccountDeletedEvent(any(AccountDeletedEvent.class));
        }

        @Test
        @DisplayName("должен выбросить IllegalArgumentException при пустом списке")
        void shouldThrowOnEmptyIds() {
            assertThatThrownBy(() -> accountService.deleteAccountsByIds(Collections.emptyList()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("empty");

            verifyNoInteractions(accountRepository);
        }
    }

    @Nested
    @DisplayName("banAccount")
    class BanAccount {

        @Test
        @DisplayName("должен забанить аккаунт и отправить событие")
        void shouldBanAndSendEvent() {
            Instant unbanDate = Instant.now().plusSeconds(86400);
            BanRequest banRequest = BanRequest.builder()
                    .reason("spam")
                    .unbanDate(unbanDate)
                    .build();

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(accountEntity));
            when(accountRepository.save(accountEntity)).thenReturn(accountEntity);

            accountService.banAccount(accountIdStr, banRequest);

            assertThat(accountEntity.getBanInfo()).isNotNull();
            assertThat(accountEntity.getBanInfo().getIsBanned()).isTrue();
            assertThat(accountEntity.getBanInfo().getReason()).isEqualTo("spam");
            assertThat(accountEntity.getBanInfo().getUnbanDate()).isEqualTo(unbanDate);

            verify(accountEventProducer).sendAccountBannedEvent(bannedEventCaptor.capture());
            AccountBannedEvent event = bannedEventCaptor.getValue();
            assertThat(event.id()).isEqualTo(accountIdStr);
            assertThat(event.reason()).isEqualTo("spam");
        }

        @Test
        @DisplayName("должен выбросить AccountNotFoundException при бане несуществующего аккаунта")
        void shouldThrowWhenBanNotFound() {
            BanRequest banRequest = BanRequest.builder()
                    .reason("spam")
                    .unbanDate(Instant.now())
                    .build();

            when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.banAccount(accountIdStr, banRequest))
                    .isInstanceOf(AccountNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("banAccountsByIds")
    class BanAccountsByIds {

        @Test
        @DisplayName("должен забанить несколько аккаунтов и отправить события")
        void shouldBanMultipleAndSendEvents() {
            UUID id2 = UUID.randomUUID();
            AccountEntity entity2 = new AccountEntity();
            entity2.setId(id2);
            entity2.setUsername("user2");

            Instant unbanDate = Instant.now().plusSeconds(86400);
            BanRequest banRequest = BanRequest.builder()
                    .reason("violation")
                    .unbanDate(unbanDate)
                    .build();

            List<String> ids = List.of(accountIdStr, id2.toString());

            when(accountRepository.findAllById(List.of(accountId, id2)))
                    .thenReturn(List.of(accountEntity, entity2));
            when(accountRepository.save(any(AccountEntity.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0));

            accountService.banAccountsByIds(ids, banRequest);

            verify(accountRepository, times(2)).save(any(AccountEntity.class));
            verify(accountEventProducer, times(2)).sendAccountBannedEvent(any(AccountBannedEvent.class));
        }

        @Test
        @DisplayName("должен выбросить IllegalArgumentException при пустом списке")
        void shouldThrowOnEmptyIds() {
            BanRequest banRequest = BanRequest.builder().build();

            assertThatThrownBy(() -> accountService.banAccountsByIds(Collections.emptyList(), banRequest))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("empty");
        }
    }

    @Nested
    @DisplayName("unbanAccount")
    class UnbanAccount {

        @Test
        @DisplayName("должен разбанить аккаунт")
        void shouldUnban() {
            AccountEntity.BanInfo existingBan = new AccountEntity.BanInfo();
            existingBan.setIsBanned(true);
            existingBan.setReason("spam");
            accountEntity.setBanInfo(existingBan);

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(accountEntity));
            when(accountRepository.save(accountEntity)).thenReturn(accountEntity);

            accountService.unbanAccount(accountIdStr);

            assertThat(accountEntity.getBanInfo().getIsBanned()).isFalse();
            verify(accountRepository).save(accountEntity);
        }

        @Test
        @DisplayName("должен выбросить AccountNotFoundException при разбане несуществующего аккаунта")
        void shouldThrowWhenUnbanNotFound() {
            when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> accountService.unbanAccount(accountIdStr))
                    .isInstanceOf(AccountNotFoundException.class);
        }
    }
}

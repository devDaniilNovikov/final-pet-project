package dn.accountservice.controller;

import dn.accountservice.AbstractAccountIT;
import dn.accountservice.dto.BanRequest;
import dn.accountservice.dto.account.AccountRequest;
import dn.accountservice.entity.AccountEntity;
import dn.accountservice.repository.AccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.test.context.EmbeddedKafka;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@EmbeddedKafka(partitions = 1, topics = {"account.created", "account.banned", "account.deleted", "account.unbanned"})
class AccountControllerIT extends AbstractAccountIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private AccountRepository accountRepository;

    private AccountEntity savedAccount;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
        savedAccount = accountRepository.save(buildAccount("alice", "alice@test.com"));
    }

    // --- GET /api/v1/accounts/{eventId} ---

    @Test
    void getAccountById_existingAccount_returns200() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/v1/accounts/{eventId}", Map.class, savedAccount.getId().toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("username")).isEqualTo("alice");
        assertThat(response.getBody().get("email")).isEqualTo("alice@test.com");
    }

    @Test
    void getAccountById_unknownId_returns404() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/v1/accounts/{eventId}", Map.class, UUID.randomUUID().toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void getAccountById_invalidUuid_returns400() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/v1/accounts/{eventId}", Map.class, "not-a-uuid");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // --- GET /api/v1/accounts?pageSize=&pageNumber= ---

    @Test
    void getListAccount_returns200WithAccounts() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/v1/accounts?pageSize=10&pageNumber=0", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("accounts");
    }

    // --- GET /api/v1/accounts/search?name= ---

    @Test
    void getAccountByName_existingName_returns200() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/v1/accounts/search?name=alice", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().get("username")).isEqualTo("alice");
    }

    @Test
    void getAccountByName_unknownName_returns404() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/v1/accounts/search?name=nobody", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // --- GET /api/v1/accounts/by-ids ---

    @Test
    void getAccountsByIds_returnsMatchingAccounts() {
        ResponseEntity<Map> response = restTemplate.getForEntity(
                "/api/v1/accounts/by-ids?accountIds={eventId}", Map.class,
                savedAccount.getId().toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsKey("accounts");
    }

    // --- PATCH /api/v1/accounts/{eventId} ---

    @Test
    void updateAccount_validRequest_returns200() {
        AccountRequest update = AccountRequest.builder()
                .username("alice-updated")
                .email("alice-updated@test.com")
                .build();

        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/accounts/{eventId}", HttpMethod.PATCH,
                new HttpEntity<>(update), Void.class,
                savedAccount.getId().toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        AccountEntity updated = accountRepository.findById(savedAccount.getId()).orElseThrow();
        assertThat(updated.getUsername()).isEqualTo("alice-updated");
        assertThat(updated.getEmail()).isEqualTo("alice-updated@test.com");
    }

    @Test
    void updateAccount_blankUsername_returns400() {
        AccountRequest update = AccountRequest.builder()
                .username("")
                .email("ok@test.com")
                .build();

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/accounts/{eventId}", HttpMethod.PATCH,
                new HttpEntity<>(update), Map.class,
                savedAccount.getId().toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void updateAccount_invalidEmail_returns400() {
        AccountRequest update = AccountRequest.builder()
                .username("alice")
                .email("not-an-email")
                .build();

        ResponseEntity<Map> response = restTemplate.exchange(
                "/api/v1/accounts/{eventId}", HttpMethod.PATCH,
                new HttpEntity<>(update), Map.class,
                savedAccount.getId().toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // --- POST /api/v1/accounts/{eventId}/ban ---

    @Test
    void banAccount_validRequest_setsIsBannedTrue() {
        BanRequest banRequest = BanRequest.builder()
                .accountId(savedAccount.getId().toString())
                .reason("Spam activity")
                .unbanDate(Instant.now().plusSeconds(86400))
                .build();

        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/api/v1/accounts/{eventId}/ban",
                banRequest, Void.class,
                savedAccount.getId().toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        AccountEntity banned = accountRepository.findById(savedAccount.getId()).orElseThrow();
        assertThat(banned.getBanInfo()).isNotNull();
        assertThat(banned.getBanInfo().getIsBanned()).isTrue();
        assertThat(banned.getBanInfo().getReason()).isEqualTo("Spam activity");
    }

    @Test
    void banAccount_unknownId_returns404() {
        BanRequest banRequest = BanRequest.builder()
                .accountId(UUID.randomUUID().toString())
                .reason("reason")
                .unbanDate(Instant.now().plusSeconds(3600))
                .build();

        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/accounts/{eventId}/ban",
                banRequest, Map.class,
                UUID.randomUUID().toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // --- POST /api/v1/accounts/{eventId}/unban ---

    @Test
    void unbanAccount_bannedAccount_setsIsBannedFalse() {
        AccountEntity.BanInfo ban = new AccountEntity.BanInfo("spam", Instant.now().plusSeconds(3600), true);
        savedAccount.setBanInfo(ban);
        accountRepository.save(savedAccount);

        ResponseEntity<Void> response = restTemplate.postForEntity(
                "/api/v1/accounts/{eventId}/unban", null, Void.class,
                savedAccount.getId().toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        AccountEntity unbanned = accountRepository.findById(savedAccount.getId()).orElseThrow();
        assertThat(unbanned.getBanInfo().getIsBanned()).isFalse();
    }

    @Test
    void unbanAccount_unknownId_returns404() {
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "/api/v1/accounts/{eventId}/unban", null, Map.class,
                UUID.randomUUID().toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // --- DELETE /api/v1/accounts/{eventId} ---

    @Test
    void deleteAccountById_existingAccount_returns204AndRemovesFromDb() {
        ResponseEntity<Void> response = restTemplate.exchange(
                "/api/v1/accounts/{eventId}", HttpMethod.DELETE,
                null, Void.class,
                savedAccount.getId().toString());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(accountRepository.findById(savedAccount.getId())).isEmpty();
    }

    // --- helpers ---

    private AccountEntity buildAccount(String username, String email) {
        AccountEntity e = new AccountEntity();
        e.setUsername(username);
        e.setEmail(email);
        e.setKeycloakId(UUID.randomUUID());  // required NOT NULL field
        return e;
    }
}

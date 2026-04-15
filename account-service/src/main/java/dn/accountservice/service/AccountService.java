package dn.accountservice.service;
import dn.accountservice.config.kafka.AccountEventProducer;
import dn.accountservice.dto.BanRequest;
import dn.accountservice.dto.account.ListAccountResponse;
import dn.accountservice.dto.account.AccountRequest;
import dn.accountservice.dto.account.AccountResponse;
import dn.accountservice.entity.AccountEntity;
import dn.accountservice.event.*;
import dn.accountservice.exception.AccountNotFoundException;
import dn.accountservice.mapper.AccountMapper;
import dn.accountservice.repository.AccountRepository;
import dn.accountservice.utils.IdMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.text.MessageFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@CacheConfig(cacheManager = "redisCacheManager")
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final AccountEventProducer accountEventProducer;
    private final CacheManager cacheManager;

    @Cacheable(value = "accounts",key = "'id:' + #accountId")
    public AccountResponse findById(String accountId){
        UUID id = IdMapper.mapToUUIDFromString(accountId);
        return accountRepository.findById(id)
                .map(accountMapper::toResponse)
                .orElseThrow(()->new AccountNotFoundException(
                        MessageFormat.format("Account with id={0} not found",accountId)));
    }


    @Transactional
    public void banAccountsByIds(List<String> accountIds,
                                 BanRequest request) {
        if (accountIds.isEmpty()) {
            throw new IllegalArgumentException("Ids can't be empty");
        }
        List<UUID> ids = IdMapper.mapToListUUIDFromString(accountIds);
        List<AccountEntity> accounts = accountRepository.findAllById(ids)
                .stream()
                .map(accountEntity -> {
                    AccountEntity.BanInfo banInfo = new AccountEntity.BanInfo();
                    banInfo.setIsBanned(true);
                    banInfo.setUnbanDate(request.getUnbanDate());
                    banInfo.setReason(request.getReason());
                    accountEntity.setBanInfo(banInfo);
                    return accountEntity;
                })
                .toList();
        accountRepository.saveAll(accounts);
        log.info("Banned accounts with ids={}", ids);
        accounts.forEach(account -> accountEventProducer.sendAccountBannedEvent(
                AccountBannedEvent.builder()
                            .id(account.getId().toString())
                            .reason(request.getReason())
                            .unbanDate(request.getUnbanDate())
                            .build()
        ));
    }



    public ListAccountResponse findAll(int pageNumber, int pageSize) {
        ListAccountResponse listAccountResponse = new ListAccountResponse();
        PageRequest  pageRequest = PageRequest.of(pageNumber, pageSize);
        List<AccountResponse> accountEntities =  accountRepository.findAll(pageRequest)
                .stream()
                .map(accountMapper::toResponse)
                .toList();
        listAccountResponse.setAccounts(accountEntities);
        listAccountResponse.setTotalPages(pageNumber);
        listAccountResponse.setTotalElements(pageSize);
        return listAccountResponse;
    }

    @Cacheable(value = "accounts",key = "'username:' + #username")
    public AccountResponse findAccountByName(String username) {
        return accountRepository.findByUsername(username)
                .map(accountMapper::toResponse)
                .orElseThrow(()->new AccountNotFoundException(
                        MessageFormat.format("Account with name={0} not found",username
                )));
    }

    @Transactional
    public AccountResponse createAccount(AccountRequest accountRequest) {
        AccountEntity accountEntity = accountMapper.toEntity(accountRequest);
        var accountForSave = accountRepository.save(accountEntity);
        accountEventProducer.sendAccountCreatedEvent(
                new AccountCreatedEvent(accountForSave.getUsername(),
                accountForSave.getId().toString())
        );
        return accountMapper.toResponse(accountForSave);
    }

    @Transactional
    @CacheEvict(value = "accounts", key = "'id:' + #accountId")
    public void updateAccount(String accountId,
                              AccountRequest accountRequest) {
        UUID id = IdMapper.mapToUUIDFromString(accountId);
        accountRepository.findById(id)
                .ifPresentOrElse(account -> {
                    Objects.requireNonNull(cacheManager.getCache("accounts")).evict("username:" + account.getUsername());
                    accountMapper.updateEntity(accountRequest,account);
                    accountRepository.save(account);
                    accountEventProducer.sendAccountUpdatedEvent(
                            AccountUpdatedEvent.builder()
                                    .id(id.toString())
                                    .updatedTime(account.getUpdatedAt())
                                    .build());
                    log.info("Updated account with id: {}", accountId);
                }, () -> {
                    throw new AccountNotFoundException(
                            MessageFormat.format("Account with id={0} not found",accountId));
                });
    }

    public ListAccountResponse getAccountsByIds(List<String> accountIds) {
        ListAccountResponse listAccountResponse = new ListAccountResponse();
        if (accountIds.isEmpty()) {
            throw new IllegalArgumentException("Ids can't be empty");
        }
        List<UUID> ids = IdMapper.mapToListUUIDFromString(accountIds);
        listAccountResponse.setAccounts(accountMapper.toResponseList(accountRepository.findAllById(ids)));
        return listAccountResponse;
    }





    @Transactional
    @CacheEvict(value = "accounts", key = "'id:' + #accountId")
    public void deleteAccountById(String accountId) {
        var account = accountRepository.findById(IdMapper.mapToUUIDFromString(accountId))
                .orElseThrow(()->new AccountNotFoundException(
                        MessageFormat.format("Account with id={0} not found",accountId)));
        Objects.requireNonNull(cacheManager.getCache("accounts"))
                .evict("username:" + account.getUsername());
        accountRepository.deleteById(account.getId());
        accountEventProducer.sendAccountDeletedEvent(AccountDeletedEvent.builder()
                .id(account.getId().toString())
                .deletedDate(Instant.now())
                .build());
        log.info("Deleted account with id={}", accountId);
    }

    @Transactional
    @CacheEvict(value = "accounts",allEntries = true)
    public void deleteAccountsByIds(List<String> accountIds) {
        if (accountIds.isEmpty()) {
            throw new IllegalArgumentException("Ids can't be empty");
        }
        List<UUID> ids = IdMapper.mapToListUUIDFromString(accountIds);
        var accountsForDelete = accountRepository.findAllById(ids);
        var essentialsIds = accountsForDelete.stream()
                        .map(AccountEntity::getId)
                        .toList();
        accountRepository.deleteAllByIdInBatch(essentialsIds);
        essentialsIds.forEach(id -> {
                 accountEventProducer.sendAccountDeletedEvent(AccountDeletedEvent.builder()
                            .id(id.toString())
                            .deletedDate(Instant.now())
                            .build());
        });
        log.info("Deleted accounts with ids={}", accountIds);
    }

    @Transactional
    @CacheEvict(value = "accounts", key = "'id:' + #accountId")
    public void unbanAccount(String accountId) {
        UUID id = IdMapper.mapToUUIDFromString(accountId);
        accountRepository.findById(id)
                .ifPresentOrElse(account->{
                    Objects.requireNonNull(cacheManager.getCache("accounts"))
                            .evict("username:" + account.getUsername());
                    AccountEntity.BanInfo banInfo = new AccountEntity.BanInfo();
                    banInfo.setIsBanned(false);
                    banInfo.setReason(null);
                    banInfo.setUnbanDate(null);
                    account.setBanInfo(banInfo);
                    accountRepository.save(account);
                    accountEventProducer.sendAccountUnbannedEvent(
                            AccountUnbannedEvent.builder()
                                    .unbannedDate(Instant.now())
                                    .id(id.toString())
                                    .build()
                    );
                    log.info("Unbanned account with id={}", accountId);
                },()->{
                    throw new AccountNotFoundException(
                            MessageFormat.format("Account with id={0} not found", accountId));
                });
    }

    @Transactional
    @CacheEvict(value = "accounts", key = "'id:' + #id")
    public void banAccount(String id, BanRequest banRequest) {
        UUID accountId = IdMapper.mapToUUIDFromString(id);
        accountRepository.findById(accountId)
                .ifPresentOrElse(account->{
                    Objects.requireNonNull(cacheManager.getCache("accounts"))
                            .evict("username:" + account.getUsername());
                    AccountEntity.BanInfo banInfo = new AccountEntity.BanInfo();
                    banInfo.setReason(banRequest.getReason());
                    banInfo.setUnbanDate(banRequest.getUnbanDate());
                    banInfo.setIsBanned(true);
                    account.setBanInfo(banInfo);
                    accountRepository.save(account);
                    accountEventProducer.sendAccountBannedEvent(
                            new AccountBannedEvent(account.getId().toString(),
                                    banInfo.getUnbanDate(),
                                    banInfo.getReason())
                    );
                    var unbanDate = banInfo.getUnbanDate();
                    log.info("Banned account with id={}, unban date={}",accountId, unbanDate);
                },()->{
                    throw new AccountNotFoundException(
                            MessageFormat.format("Account with id={0} not found",accountId));
                });
    }
}

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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheConfig;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.text.MessageFormat;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@CacheConfig(cacheManager = "redisCacheManager")
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final AccountEventProducer accountEventProducer;

    @Cacheable(value = "accounts",key = "#accountId")
    public AccountResponse findById(String accountId){
        UUID id = mapFromString(accountId);
        return accountRepository.findById(id)
                .map(accountMapper::toResponse)
                .orElseThrow(()->new AccountNotFoundException(
                        MessageFormat.format("Account with id: {0} not found",accountId)));
    }


    @Transactional
    public void banAccountsByIds(List<String> accountIds,
                                 BanRequest request) {
        if (accountIds.isEmpty()) {
            throw new IllegalArgumentException("Ids can't be empty");
        }
        List<UUID> ids = mapFromStringListToUUIDList(accountIds);
        List<AccountEntity> accounts = accountRepository.findAllById(ids)
                .stream()
                .map(accountEntity -> {
                    AccountEntity.BanInfo banInfo = new AccountEntity.BanInfo();
                    banInfo.setIsBanned(true);
                    banInfo.setUnbanDate(request.getUnbanDate());
                    banInfo.setReason(request.getReason());
                    accountEntity.setBanInfo(banInfo);
                    return accountRepository.save(accountEntity);
                })
                .toList();
        log.info("Banned accounts with ids: {}", ids);
        accounts.forEach(account -> {
            accountEventProducer.sendAccountBannedEvent(AccountBannedEvent.builder()
                            .id(account.getId().toString())
                            .reason(request.getReason())
                            .unbanDate(request.getUnbanDate())
                            .build());
        });
    }



    public ListAccountResponse findAll(int pageSize, int pageNumber) {
        ListAccountResponse listAccountResponse = new ListAccountResponse();
        PageRequest  pageRequest = PageRequest.of(pageNumber, pageSize);
        List<AccountResponse> accountEntities =  accountRepository.findAll(pageRequest)
                .stream()
                .map(accountMapper::toResponse)
                .toList();
        listAccountResponse.setAccounts(accountEntities);
        return listAccountResponse;
    }

    @Cacheable(value = "accounts",key = "#accountName")
    public AccountResponse findAccountByName(String accountName) {
        return accountRepository.findByUsername(accountName)
                .map(accountMapper::toResponse)
                .orElseThrow(()->new AccountNotFoundException(
                        MessageFormat.format("Account with name: {0} not found",accountName
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
    @Caching(evict = {
            @CacheEvict(value = "accounts", key = "#accountId"),
            @CacheEvict(value = "accounts", key = "#accountRequest.username")
    })
    public void updateAccount(String accountId,
                              AccountRequest accountRequest) {
        UUID id = mapFromString(accountId);
        accountRepository.findById(id)
                .ifPresentOrElse(account -> {
                    accountMapper.updateEntity(accountRequest,account);
                    accountRepository.save(account);
                    log.info("Updated account with id: {}", accountId);
                }, () -> {
                    throw new AccountNotFoundException(
                            MessageFormat.format("Account with id: {0} not found",accountId));
                });
    }

    public ListAccountResponse getAccountsByIds(List<String> accountIds) {
        ListAccountResponse listAccountResponse = new ListAccountResponse();
        if (accountIds.isEmpty()) {
            throw new IllegalArgumentException("Ids can't be empty");
        }
        List<UUID> ids = mapFromStringListToUUIDList(accountIds);
        listAccountResponse.setAccounts(accountMapper.toResponseList(accountRepository.findAllById(ids)));
        return listAccountResponse;
    }

    private List<UUID> mapFromStringListToUUIDList(List<String> accountIds) {
        return accountIds.stream()
                .map(UUID::fromString)
                .toList();
    }

    private  UUID mapFromString(String accountId){
        return UUID.fromString(accountId);
    }



    @Transactional
    @CacheEvict(value = "accounts", key = "#accountId")
    public void deleteAccountById(String accountId) {
        accountRepository.deleteById(mapFromString(accountId));
        accountEventProducer.sendAccountDeletedEvent(AccountDeletedEvent.builder()
                .id(accountId)
                .deletedDate(Instant.now())
                .build());
        log.info("Deleted account with id: {}", accountId);
    }

    @Transactional
    @CacheEvict(value = "accounts", key = "#accountIds",allEntries = true)
    public void deleteAccountsByIds(List<String> accountIds) {
        if (accountIds.isEmpty()) {
            throw new IllegalArgumentException("Ids can't be empty");
        }
        List<UUID> ids = mapFromStringListToUUIDList(accountIds);
        accountRepository.deleteAllByIdInBatch(ids);
        accountIds.forEach(id -> {
            accountEventProducer.sendAccountDeletedEvent(AccountDeletedEvent.builder()
                            .id(id)
                            .deletedDate(Instant.now())
                            .build());
        });
        log.info("Deleted accounts with ids: {}", accountIds);
    }

    @Transactional
    @CacheEvict(value = "accounts", key = "#accountId")
    public void unbanAccount(String accountId) {
        UUID id = mapFromString(accountId);
        accountRepository.findById(id)
                .ifPresentOrElse(a->{
                    AccountEntity.BanInfo banInfo = new AccountEntity.BanInfo();
                    banInfo.setIsBanned(false);
                    a.setBanInfo(banInfo);
                    accountRepository.save(a);
                    log.info("Unbanned account with id: {}", accountId);
                },()->{
                    throw new AccountNotFoundException(
                            MessageFormat.format("Account with id: {0} not found", accountId));
                });
    }

    @Transactional
    @CacheEvict(value = "accounts", key = "#id")
    public void banAccount(String id, BanRequest banRequest) {
        UUID accountId = mapFromString(id);
        accountRepository.findById(accountId)
                .ifPresentOrElse(a->{
                    AccountEntity.BanInfo banInfo = new AccountEntity.BanInfo();
                    banInfo.setReason(banRequest.getReason());
                    banInfo.setUnbanDate(banRequest.getUnbanDate());
                    banInfo.setIsBanned(true);
                    a.setBanInfo(banInfo);
                    accountRepository.save(a);
                    accountEventProducer.sendAccountBannedEvent(
                            new AccountBannedEvent(a.getId().toString(),
                                    banInfo.getUnbanDate(),
                                    banInfo.getReason())
                    );
                    var unbanDate = banInfo.getUnbanDate();
                    log.info("Banned account with id: {}, unban date: {}",accountId, unbanDate);
                },()->{
                    throw new AccountNotFoundException(
                            MessageFormat.format("Account with id: {0} not found",accountId));
                });
    }
}

package dn.accountservice.service;
import dn.accountservice.dto.BanRequest;
import dn.shared.event.account.*;
import dn.accountservice.dto.account.ListAccountResponse;
import dn.accountservice.dto.account.AccountRequest;
import dn.accountservice.dto.account.AccountResponse;
import dn.accountservice.entity.AccountEntity;
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
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@CacheConfig(cacheManager = "redisCacheManager")
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final AccountOutboxService outboxService;
    private final CacheManager cacheManager;
    private final AccountEventFactory accountEventFactory;

    @Cacheable(value = "accounts",key = "'eventId:' + #accountId")
    public AccountResponse findById(String accountId){
        UUID id = IdMapper.mapToUUIDFromString(accountId);
        return accountRepository.findById(id)
                .map(accountMapper::toResponse)
                .orElseThrow(() -> new AccountNotFoundException(
                        MessageFormat.format("Account with eventId={0} not found",accountId)));
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
        accounts.forEach(account -> {
            var accountBannedEvent = accountEventFactory.createAccountBannedEvent(
                    account,
                    request.getReason(),
                    request.getUnbanDate()
            );
            outboxService.createOutbox(accountBannedEvent);
        });
        log.info("Banned accounts with ids={}", ids);

    }




    public ListAccountResponse findAll(int pageNumber, int pageSize) {
        ListAccountResponse listAccountResponse = new ListAccountResponse();
        PageRequest pageRequest = PageRequest.of(pageNumber, pageSize);
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
        attachAddresses(accountEntity);
        var accountForSave = accountRepository.save(accountEntity);
        var accountCreateEvent = accountEventFactory.createAccountCreatedEvent(accountEntity);
        outboxService.createOutbox(accountCreateEvent);
        return accountMapper.toResponse(accountForSave);
    }

    @Transactional
    @CacheEvict(value = "accounts", key = "'eventId:' + #accountId")
    public void updateAccount(String accountId,
                              AccountRequest accountRequest) {
        UUID id = IdMapper.mapToUUIDFromString(accountId);
        accountRepository.findById(id)
                .ifPresentOrElse(account -> {
                    Objects.requireNonNull(cacheManager.getCache("accounts")).evict("username:" + account.getUsername());
                    accountMapper.updateEntity(accountRequest,account);
                    accountRepository.save(account);
                    var accountUpdatedEvent = accountEventFactory.createAccountUpdatedEvent(account);
                    outboxService.createOutbox(accountUpdatedEvent);
                    log.info("Updated account with eventId: {}", accountId);
                }, () -> {
                    throw new AccountNotFoundException(
                            MessageFormat.format("Account with eventId={0} not found",accountId));
                });
    }

    public ListAccountResponse getAccountsByIds(List<String> accountIds) {
        ListAccountResponse listAccountResponse = new ListAccountResponse();
        if (accountIds.isEmpty()) {
            throw new IllegalArgumentException("Ids can't be empty");
        }
        List<UUID> ids = IdMapper.mapToListUUIDFromString(accountIds);
        List<AccountResponse> accounts = accountRepository.findAllById(ids)
                .stream()
                .map(accountMapper::toResponse)
                .toList();
        listAccountResponse.setAccounts(accounts);
        return listAccountResponse;
    }





    @Transactional
    @CacheEvict(value = "accounts", key = "'eventId:' + #accountId")
    public void deleteAccountById(String accountId) {
        var account = accountRepository.findById(IdMapper.mapToUUIDFromString(accountId))
                .orElseThrow(()->new AccountNotFoundException(
                        MessageFormat.format("Account with eventId={0} not found",accountId)));
        Objects.requireNonNull(cacheManager.getCache("accounts"))
                .evict("username:" + account.getUsername());
        account.setUpdatedAt(Instant.now());
        accountRepository.deleteById(account.getId());
        var accountDeletedEvent = accountEventFactory.createAccountDeletedEvent(account);
        outboxService.createOutbox(accountDeletedEvent);
        log.info("Deleted account with eventId={}", accountId);
    }

    @Transactional
    @CacheEvict(value = "accounts",allEntries = true)
    public void deleteAccountsByIds(List<UUID> accountIds) {
        if (accountIds.isEmpty()) {
            throw new IllegalArgumentException("Ids can't be empty");
        }
        var accountsForDelete = accountRepository.findAllById(accountIds);
        List<UUID> essentialsIds = accountsForDelete.stream()
                        .map(AccountEntity::getId)
                        .toList();
        accountRepository.deleteAllByIdInBatch(essentialsIds);
        accountsForDelete.forEach(account -> {
            var accountDeletedEvent = accountEventFactory.createAccountDeletedEvent(account);
            outboxService.createOutbox(accountDeletedEvent);
        });
        log.info("Deleted accounts with ids={}", accountIds);
    }

    @Transactional
    @CacheEvict(value = "accounts", key = "'eventId:' + #accountId")
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
                    var accountBannedEvent = accountEventFactory.createAccountUnbannedEvent(account);
                    outboxService.createOutbox(accountBannedEvent);
                    log.info("Unbanned account with eventId={}", accountId);
                },()->{
                    throw new AccountNotFoundException(
                            MessageFormat.format("Account with eventId={0} not found", accountId));
                });
    }



    @Transactional
    @CacheEvict(value = "accounts", key = "'eventId:' + #eventId")
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
                    var accountBannedEvent = accountEventFactory.createAccountBannedEvent(
                            account,banRequest.getReason(),banRequest.getUnbanDate()
                    );
                    outboxService.createOutbox(accountBannedEvent);
                    var unbanDate = banInfo.getUnbanDate();
                    log.info("Banned account with eventId={}, unban date={}",accountId, unbanDate);
                },()->{
                    throw new AccountNotFoundException(
                            MessageFormat.format("Account with eventId={0} not found",accountId));
                });
    }

    private void attachAddresses(AccountEntity accountEntity) {
        if (accountEntity.getAddresses() == null) {
            return;
        }
        accountEntity.getAddresses().forEach(address -> address.setAccount(accountEntity));
    }
}

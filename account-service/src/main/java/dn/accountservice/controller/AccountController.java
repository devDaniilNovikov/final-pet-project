package dn.accountservice.controller;


import dn.accountservice.dto.BanRequest;
import dn.accountservice.dto.account.AccountRequest;
import dn.accountservice.dto.account.AccountResponse;
import dn.accountservice.dto.account.ListAccountResponse;
import dn.accountservice.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AccountController {

    private static final String BASE = "/api/v1/accounts";
    private static final String CREATE_ACCOUNT = BASE;
    private static final String GET_LIST_ACCOUNT = BASE;
    private static final String GET_ACCOUNT_BY_NAME = BASE + "/search";
    private static final String GET_ACCOUNTS_BY_IDS = BASE + "/by-ids";
    private static final String UPDATE_ACCOUNT = BASE + "/{accountId}";
    private static final String DELETE_ACCOUNT = BASE + "/{accountId}";
    private static final String DELETE_ACCOUNTS_BY_IDS = BASE;
    private static final String GET_ACCOUNT_BY_ID = BASE + "/{accountId}";
    private static final String BAN_ACCOUNT_BY_ID = BASE + "/{accountId}/ban";
    private static final String UNBAN_ACCOUNT_BY_ID = BASE + "/{accountId}/unban";
    private static final String BAN_ACCOUNTS_BY_IDS = BASE + "/ban";

    private final AccountService accountService;

    @GetMapping(GET_ACCOUNT_BY_NAME)
    @ResponseStatus(HttpStatus.OK)
    public AccountResponse getAccountByName(@RequestParam String name) {
        return accountService.findAccountByName(name);
    }

    @GetMapping(GET_ACCOUNTS_BY_IDS)
    @ResponseStatus(HttpStatus.OK)
    public ListAccountResponse getAccountListByIds(@RequestParam List<String> accountIds) {
        return accountService.getAccountsByIds(accountIds);
    }

    @PatchMapping(UPDATE_ACCOUNT)
    @ResponseStatus(HttpStatus.OK)
    public void updateAccount(@PathVariable String accountId,
                              @Valid @RequestBody AccountRequest accountRequest) {
        accountService.updateAccount(accountId,accountRequest);
    }

    @PostMapping(CREATE_ACCOUNT)
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createAccount(@Valid @RequestBody AccountRequest accountRequest) {
        return accountService.createAccount(accountRequest);
    }


    @GetMapping(GET_LIST_ACCOUNT)
    @ResponseStatus(HttpStatus.OK)
    public ListAccountResponse getListAccount(@RequestParam int pageNumber,
                                              @RequestParam int pageSize) {
        return accountService.findAll(pageNumber, pageSize);
    }

    @GetMapping(GET_ACCOUNT_BY_ID)
    @ResponseStatus(HttpStatus.OK)
    public AccountResponse getAccountById(@PathVariable String accountId) {
        return accountService.findById(accountId);
    }

    @DeleteMapping(DELETE_ACCOUNT)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccountById(@PathVariable String accountId) {
        accountService.deleteAccountById(accountId);
    }

    @DeleteMapping(DELETE_ACCOUNTS_BY_IDS)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccountsByIds(@RequestParam List<String> accountIds) {
        accountService.deleteAccountsByIds(accountIds);
    }

    @PostMapping(BAN_ACCOUNT_BY_ID)
    @ResponseStatus(HttpStatus.OK)
    public void banAccountById(@PathVariable String accountId,
                               @Valid @RequestBody BanRequest banRequest) {
        accountService.banAccount(accountId,banRequest);
    }

    @PostMapping(UNBAN_ACCOUNT_BY_ID)
    @ResponseStatus(HttpStatus.OK)
    public void unbanAccount(@PathVariable String accountId){
        accountService.unbanAccount(accountId);
    }

    @PostMapping(BAN_ACCOUNTS_BY_IDS)
    @ResponseStatus(HttpStatus.OK)
    public void banAccountsById(@RequestParam List<String> accountIds,
                                @RequestBody BanRequest banRequest){
        accountService.banAccountsByIds(accountIds,banRequest);
    }
}

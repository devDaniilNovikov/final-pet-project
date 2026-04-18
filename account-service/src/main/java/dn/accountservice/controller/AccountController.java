package dn.accountservice.controller;


import dn.accountservice.dto.BanRequest;
import dn.accountservice.dto.account.AccountRequest;
import dn.accountservice.dto.account.AccountResponse;
import dn.accountservice.dto.account.ListAccountResponse;
import dn.accountservice.exception.ErrorBody;
import dn.accountservice.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Accounts", description = "API для управления аккаунтами, адресами и блокировками")
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

    @Operation(summary = "Найти аккаунт по имени", description = "Возвращает аккаунт по точному совпадению имени пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Аккаунт найден",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse(responseCode = "404", description = "Аккаунт не найден",
                    content = @Content(schema = @Schema(implementation = ErrorBody.class)))
    })
    @GetMapping(GET_ACCOUNT_BY_NAME)
    @ResponseStatus(HttpStatus.OK)
    public AccountResponse getAccountByName(
            @Parameter(description = "Имя пользователя", required = true, example = "john_doe")
            @RequestParam String name) {
        return accountService.findAccountByName(name);
    }

    @Operation(summary = "Получить аккаунты по списку ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список аккаунтов",
                    content = @Content(schema = @Schema(implementation = ListAccountResponse.class))),
            @ApiResponse(responseCode = "400", description = "Неверный запрос",
                    content = @Content(schema = @Schema(implementation = ErrorBody.class)))
    })
    @GetMapping(GET_ACCOUNTS_BY_IDS)
    @ResponseStatus(HttpStatus.OK)
    public ListAccountResponse getAccountListByIds(
            @Parameter(description = "Список UUID аккаунтов", required = true)
            @RequestParam List<String> accountIds) {
        return accountService.getAccountsByIds(accountIds);
    }

    @Operation(summary = "Обновить аккаунт", description = "Обновляет данные существующего аккаунта")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Аккаунт обновлён"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации",
                    content = @Content(schema = @Schema(implementation = ErrorBody.class))),
            @ApiResponse(responseCode = "404", description = "Аккаунт не найден",
                    content = @Content(schema = @Schema(implementation = ErrorBody.class)))
    })
    @PatchMapping(UPDATE_ACCOUNT)
    @ResponseStatus(HttpStatus.OK)
    public void updateAccount(
            @Parameter(description = "UUID аккаунта", required = true) @PathVariable String accountId,
            @Valid @RequestBody AccountRequest accountRequest) {
        accountService.updateAccount(accountId, accountRequest);
    }

    @Operation(summary = "Создать аккаунт", description = "Регистрирует новый аккаунт пользователя")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Аккаунт создан",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации",
                    content = @Content(schema = @Schema(implementation = ErrorBody.class)))
    })
    @PostMapping(CREATE_ACCOUNT)
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createAccount(@Valid @RequestBody AccountRequest accountRequest) {
        return accountService.createAccount(accountRequest);
    }

    @Operation(summary = "Получить список всех аккаунтов", description = "Возвращает постраничный список аккаунтов")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Список аккаунтов",
                    content = @Content(schema = @Schema(implementation = ListAccountResponse.class)))
    })
    @GetMapping(GET_LIST_ACCOUNT)
    @ResponseStatus(HttpStatus.OK)
    public ListAccountResponse getListAccount(
            @Parameter(description = "Размер страницы", example = "10") @RequestParam int pageSize,
            @Parameter(description = "Номер страницы (с 0)", example = "0") @RequestParam int pageNumber) {
        return accountService.findAll(pageNumber, pageSize);
    }

    @Operation(summary = "Получить аккаунт по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Аккаунт найден",
                    content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse(responseCode = "404", description = "Аккаунт не найден",
                    content = @Content(schema = @Schema(implementation = ErrorBody.class)))
    })
    @GetMapping(GET_ACCOUNT_BY_ID)
    @ResponseStatus(HttpStatus.OK)
    public AccountResponse getAccountById(
            @Parameter(description = "UUID аккаунта", required = true) @PathVariable String accountId) {
        return accountService.findById(accountId);
    }

    @Operation(summary = "Удалить аккаунт по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Аккаунт удалён"),
            @ApiResponse(responseCode = "404", description = "Аккаунт не найден",
                    content = @Content(schema = @Schema(implementation = ErrorBody.class)))
    })
    @DeleteMapping(DELETE_ACCOUNT)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccountById(
            @Parameter(description = "UUID аккаунта", required = true) @PathVariable String accountId) {
        accountService.deleteAccountById(accountId);
    }

    @Operation(summary = "Удалить несколько аккаунтов по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Аккаунты удалены"),
            @ApiResponse(responseCode = "400", description = "Неверный запрос",
                    content = @Content(schema = @Schema(implementation = ErrorBody.class)))
    })
    @DeleteMapping(DELETE_ACCOUNTS_BY_IDS)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteAccountsByIds(
            @Parameter(description = "Список UUID аккаунтов", required = true) @RequestParam List<String> accountIds) {
        accountService.deleteAccountsByIds(accountIds);
    }

    @Operation(summary = "Заблокировать аккаунт по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Аккаунт заблокирован"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации",
                    content = @Content(schema = @Schema(implementation = ErrorBody.class))),
            @ApiResponse(responseCode = "404", description = "Аккаунт не найден",
                    content = @Content(schema = @Schema(implementation = ErrorBody.class)))
    })
    @PostMapping(BAN_ACCOUNT_BY_ID)
    @ResponseStatus(HttpStatus.OK)
    public void banAccountById(
            @Parameter(description = "UUID аккаунта", required = true) @PathVariable String accountId,
            @Valid @RequestBody BanRequest banRequest) {
        accountService.banAccount(accountId, banRequest);
    }

    @Operation(summary = "Разблокировать аккаунт по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Аккаунт разблокирован"),
            @ApiResponse(responseCode = "404", description = "Аккаунт не найден",
                    content = @Content(schema = @Schema(implementation = ErrorBody.class)))
    })
    @PostMapping(UNBAN_ACCOUNT_BY_ID)
    @ResponseStatus(HttpStatus.OK)
    public void unbanAccount(
            @Parameter(description = "UUID аккаунта", required = true) @PathVariable String accountId) {
        accountService.unbanAccount(accountId);
    }

    @Operation(summary = "Заблокировать несколько аккаунтов по ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Аккаунты заблокированы"),
            @ApiResponse(responseCode = "400", description = "Ошибка валидации",
                    content = @Content(schema = @Schema(implementation = ErrorBody.class)))
    })
    @PostMapping(BAN_ACCOUNTS_BY_IDS)
    @ResponseStatus(HttpStatus.OK)
    public void banAccountsById(
            @Parameter(description = "Список UUID аккаунтов", required = true) @RequestParam List<String> accountIds,
            @Valid @RequestBody BanRequest banRequest) {
        accountService.banAccountsByIds(accountIds, banRequest);
    }
}

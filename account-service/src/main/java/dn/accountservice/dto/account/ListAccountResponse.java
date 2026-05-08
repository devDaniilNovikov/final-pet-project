package dn.accountservice.dto.account;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Schema(description = "Постраничный список аккаунтов")
@Getter
@Setter
public class ListAccountResponse {

    @Schema(description = "Список аккаунтов")
    private List<AccountResponse> accounts;

    @Schema(description = "Количество страниц с аккаунтами")
    private int totalPages;

    @Schema(description = "Количество элементов на одной странице")
    private long totalElements;
}

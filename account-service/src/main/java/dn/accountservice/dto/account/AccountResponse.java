package dn.accountservice.dto.account;

import dn.accountservice.dto.BanResponse;
import dn.accountservice.dto.address.AddressResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Schema(description = "Данные аккаунта пользователя")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AccountResponse {

    @Schema(description = "Уникальный идентификатор аккаунта", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID id;

    @Schema(description = "Имя пользователя", example = "john_doe")
    private String username;

    @Schema(description = "URL аватара", example = "https://cdn.example.com/avatar.png")
    private String avatarUrl;

    @Schema(description = "Email пользователя", example = "john@example.com")
    private String email;

    @Schema(description = "ID пользователя в Keycloak", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private UUID keycloakId;

    @Schema(description = "Дата создания аккаунта")
    private Instant createdAt;

    @Schema(description = "Дата последнего обновления аккаунта")
    private Instant updatedAt;

    @Schema(description = "Номер телефона", example = "+79001234567")
    private String phoneNumber;

    @Schema(description = "Список адресов пользователя")
    private List<AddressResponse> addresses;

    @Schema(description = "Информация о блокировке аккаунта")
    private BanResponse banResponse;
}

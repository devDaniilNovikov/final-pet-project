package dn.accountservice.dto.account;

import dn.accountservice.dto.address.AddressRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.List;

@Schema(description = "Запрос на создание/обновление аккаунта")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AccountRequest {

    @Schema(description = "Имя пользователя", example = "john_doe", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    private String username;

    @Schema(description = "URL аватара пользователя", example = "https://cdn.example.com/avatar.png")
    private String avatarUrl;

    @Schema(description = "Email пользователя", example = "john@example.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank
    @Email
    private String email;

    @Schema(description = "Номер телефона", example = "+79001234567")
    private String phoneNumber;

    @Schema(description = "Список адресов пользователя")
    private List<AddressRequest> addresses;
}

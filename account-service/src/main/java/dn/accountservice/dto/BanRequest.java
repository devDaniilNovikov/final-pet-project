package dn.accountservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Schema(description = "Запрос на блокировку аккаунта")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class BanRequest {

    @Schema(description = "UUID аккаунта для блокировки", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty
    private String accountId;

    @Schema(description = "Причина блокировки", example = "Нарушение правил сервиса", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "Reason can't be empty!")
    private String reason;

    @Schema(description = "Дата снятия блокировки (ISO-8601)", example = "2026-12-31T23:59:59Z", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull
    private Instant unbanDate;
}

package dn.accountservice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.Instant;

@Schema(description = "Информация о блокировке аккаунта")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class BanResponse {

    @Schema(description = "Причина блокировки", example = "Нарушение правил сервиса")
    private String reason;

    @Schema(description = "Дата снятия блокировки")
    private Instant unbanDate;

    @Schema(description = "Флаг: аккаунт заблокирован", example = "true")
    private Boolean isBanned;
}

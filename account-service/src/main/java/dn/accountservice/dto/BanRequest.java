package dn.accountservice.dto;


import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class BanRequest {

    @NotEmpty
    private String accountId;

    @NotEmpty(message = "Reason can't be empty!")
    private String reason;

    @NotNull
    private Instant unbanDate;
}

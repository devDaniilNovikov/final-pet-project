package dn.accountservice.dto;


import lombok.*;

import java.time.Instant;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class BanResponse {

    private String reason;
    private Instant unbanDate;
    private Boolean isBanned;
}

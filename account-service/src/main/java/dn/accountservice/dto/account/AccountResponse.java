package dn.accountservice.dto.account;
import dn.accountservice.dto.BanResponse;
import dn.accountservice.dto.address.AddressResponse;
import lombok.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AccountResponse {

    private String id;
    private String username;
    private String avatarUrl;
    private String email;
    private UUID keycloakId;
    private Instant createdAt;
    private Instant updatedAt;
    private String phoneNumber;
    private List<AddressResponse> addresses;
    private BanResponse banResponse;



}

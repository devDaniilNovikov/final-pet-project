package dn.accountservice.dto.account;
import dn.accountservice.dto.address.AddressRequest;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.util.List;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AccountRequest {

    @NotBlank
    private String username;

    private String avatarUrl;
    @NotBlank
    @Email
    private String email;
    private String phoneNumber;
    private List<AddressRequest> addresses;

}

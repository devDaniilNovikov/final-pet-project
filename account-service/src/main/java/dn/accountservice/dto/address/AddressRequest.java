package dn.accountservice.dto.address;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AddressRequest {


    @NotBlank(message = "street can't be blank")
    private String street;

    @NotBlank(message = "building can't be blank")
    private String building;

    @NotBlank(message = "city can't be blank")
    private String city;

    @NotBlank(message = "zipCode can't be blank")
    private String zipCode;

    private String apartment;

    private String commentForCourier;
}

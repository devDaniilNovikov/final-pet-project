package dn.accountservice.dto.address;


import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AddressResponse {

    private String id;
    private String street;
    private String building;
    private String city;
    private String zipCode;
    private String apartment;
    private String commentForCourier;
    private String accountId;
}

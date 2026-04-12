package dn.accountservice.dto.address;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "Данные адреса пользователя")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AddressResponse {

    @Schema(description = "UUID адреса", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private String id;

    @Schema(description = "Улица", example = "ул. Ленина")
    private String street;

    @Schema(description = "Номер дома", example = "42")
    private String building;

    @Schema(description = "Город", example = "Москва")
    private String city;

    @Schema(description = "Почтовый индекс", example = "125009")
    private String zipCode;

    @Schema(description = "Номер квартиры/офиса", example = "101")
    private String apartment;

    @Schema(description = "Комментарий для курьера")
    private String commentForCourier;

    @Schema(description = "UUID аккаунта-владельца адреса", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
    private String accountId;
}

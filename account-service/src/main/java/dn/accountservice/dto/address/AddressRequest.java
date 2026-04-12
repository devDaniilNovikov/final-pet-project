package dn.accountservice.dto.address;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Schema(description = "Запрос на создание/обновление адреса")
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class AddressRequest {

    @Schema(description = "Улица", example = "ул. Ленина", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "street can't be blank")
    private String street;

    @Schema(description = "Номер дома", example = "42", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "building can't be blank")
    private String building;

    @Schema(description = "Город", example = "Москва", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "city can't be blank")
    private String city;

    @Schema(description = "Почтовый индекс", example = "125009", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "zipCode can't be blank")
    private String zipCode;

    @Schema(description = "Номер квартиры/офиса", example = "101")
    private String apartment;

    @Schema(description = "Комментарий для курьера", example = "Домофон не работает, позвонить по телефону")
    private String commentForCourier;
}

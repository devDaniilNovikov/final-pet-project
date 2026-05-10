package dn.accountservice.mapper;

import dn.accountservice.dto.address.AddressRequest;
import dn.accountservice.dto.address.AddressResponse;
import dn.accountservice.entity.AddressEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AddressMapper {

    @Mapping(source = "account.id", target = "id")
    AddressResponse toResponse(AddressEntity entity);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "account", ignore = true)
    AddressEntity toEntity(AddressRequest request);
}


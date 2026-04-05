package dn.accountservice.mapper;

import dn.accountservice.dto.BanRequest;
import dn.accountservice.dto.BanResponse;
import dn.accountservice.entity.AccountEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BanMapper {

    BanResponse toResponse(AccountEntity.BanInfo banInfo);

    @Mapping(target = "isBanned", ignore = true)
    AccountEntity.BanInfo toEntity(BanRequest request);

    @Mapping(target = "isBanned", ignore = true)
    BanResponse toResponse(BanRequest request);
}


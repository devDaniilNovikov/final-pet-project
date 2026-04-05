package dn.accountservice.mapper;

import dn.accountservice.dto.account.AccountRequest;
import dn.accountservice.dto.account.AccountResponse;
import dn.accountservice.dto.account.ListAccountResponse;
import dn.accountservice.entity.AccountEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(componentModel = "spring", uses = {AddressMapper.class, BanMapper.class})
public interface AccountMapper {

    @Mapping(source = "banInfo", target = "banResponse")
    AccountResponse toResponse(AccountEntity entity);

    List<AccountResponse> toResponseList(List<AccountEntity> entities);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "banInfo", ignore = true)
    @Mapping(target = "keycloakId", ignore = true)
    AccountEntity toEntity(AccountRequest request);



    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "banInfo", ignore = true)
    @Mapping(target = "keycloakId", ignore = true)
    @Mapping(target = "addresses", ignore = true)
    void updateEntity(AccountRequest request, @MappingTarget AccountEntity entity);

}


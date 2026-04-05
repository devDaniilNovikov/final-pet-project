package dn.accountservice.dto.account;


import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class ListAccountResponse {

    private List<AccountResponse> accounts;
}


package io.paradoxical.cassieq.auth;

import io.paradoxical.cassieq.model.accounts.AccountName;
import io.paradoxical.cassieq.model.auth.AuthorizationLevel;
import lombok.Value;

import java.security.Principal;
import java.util.EnumSet;

@Value
public class AccountPrincipal implements Principal {

    private final AccountName accountName;
    private final EnumSet<AuthorizationLevel> authorizationLevels;

    @Override
    public String getName() {
        return "account-principal";
    }
}

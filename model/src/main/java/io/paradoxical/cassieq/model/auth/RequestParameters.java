package io.paradoxical.cassieq.model.auth;

import io.paradoxical.cassieq.model.accounts.AccountKey;
import io.paradoxical.cassieq.model.accounts.AccountName;

import java.util.EnumSet;

public interface RequestParameters {
    AccountName getAccountName();

    EnumSet<AuthorizationLevel> getAuthorizationLevels();

    boolean verify(AccountKey key) throws Exception;
}

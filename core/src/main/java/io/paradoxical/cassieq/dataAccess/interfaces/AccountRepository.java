package io.paradoxical.cassieq.dataAccess.interfaces;

import io.paradoxical.cassieq.model.accounts.AccountDefinition;
import io.paradoxical.cassieq.model.accounts.AccountName;

import java.util.List;
import java.util.Optional;

public interface AccountRepository {

    Optional<AccountDefinition> createAccount(AccountName accountName);

    Optional<AccountDefinition> getAccount(AccountName accountName);

    List<AccountDefinition> getAllAccounts();

    void deleteAccount(AccountName accountName);

    void updateAccount(AccountDefinition accountDefinition);
}

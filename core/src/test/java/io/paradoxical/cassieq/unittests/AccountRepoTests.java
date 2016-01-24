package io.paradoxical.cassieq.unittests;

import com.google.common.collect.ImmutableMap;
import io.paradoxical.cassieq.dataAccess.interfaces.AccountRepository;
import io.paradoxical.cassieq.model.accounts.AccountDefinition;
import io.paradoxical.cassieq.model.accounts.AccountKey;
import io.paradoxical.cassieq.model.accounts.AccountName;
import io.paradoxical.cassieq.model.accounts.KeyName;
import io.paradoxical.cassieq.model.accounts.WellKnownKeyNames;
import org.junit.Test;

import java.util.HashMap;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class AccountRepoTests extends DbTestBase {
    @Test
    public void test_create_account() {
        final AccountRepository repo = getDefaultInjector().getInstance(AccountRepository.class);

        final Optional<AccountDefinition> test_create_account = repo.createAccount(AccountName.valueOf("test_create_account"));

        assertThat(test_create_account).isPresent();

        assertThat(test_create_account.get().getKeys()).isNotEmpty();

        assertThat(test_create_account.get().getKeys()).containsKeys(WellKnownKeyNames.Primary.getKeyName(), WellKnownKeyNames.Secondary.getKeyName());
    }

    @Test
    public void test_delete_account() {
        final AccountRepository repo = getDefaultInjector().getInstance(AccountRepository.class);

        final AccountName account = AccountName.valueOf("test_delete_account");

        final Optional<AccountDefinition> test_create_account = repo.createAccount(account);

        assertThat(test_create_account).isPresent();

        repo.deleteAccount(account);

        assertThat(repo.getAccount(account)).isEmpty();
    }

    @Test
    public void test_update_account() {
        final AccountRepository repo = getDefaultInjector().getInstance(AccountRepository.class);

        final AccountName account = AccountName.valueOf("test_update_account");

        final Optional<AccountDefinition> accountDefinition = repo.createAccount(account);

        assertThat(accountDefinition).isPresent();

        assertThat(accountDefinition.get().getKeys()).isNotEmpty();

        assertThat(accountDefinition.get().getKeys()).containsKeys(WellKnownKeyNames.Primary.getKeyName(), WellKnownKeyNames.Secondary.getKeyName());

        final HashMap<KeyName, AccountKey> stringAccountKeyHashMap = new HashMap<>(accountDefinition.get().getKeys());

        stringAccountKeyHashMap.remove(WellKnownKeyNames.Primary.getKeyName());

        accountDefinition.get().setKeys(ImmutableMap.copyOf(stringAccountKeyHashMap));

        repo.updateAccount(accountDefinition.get());

        assertThat(repo.getAccount(account).get().getKeys()).doesNotContainKeys(WellKnownKeyNames.Primary.getKeyName());
    }
}

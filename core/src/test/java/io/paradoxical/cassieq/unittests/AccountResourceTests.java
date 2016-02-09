package io.paradoxical.cassieq.unittests;

import categories.BuildVerification;
import io.paradoxical.cassieq.admin.resources.api.v1.AccountResource;
import io.paradoxical.cassieq.model.accounts.AccountDefinition;
import io.paradoxical.cassieq.model.accounts.AccountName;
import io.paradoxical.cassieq.model.accounts.KeyCreateRequest;
import io.paradoxical.cassieq.model.accounts.KeyName;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

@Category(BuildVerification.class)
public class AccountResourceTests extends DbTestBase {
    private AccountResource resource;

    @Before
    public void setup() {
        resource = getDefaultInjector().getInstance(AccountResource.class);
    }

    @Test
    public void can_add_account() {
        final AccountName accountName = AccountName.valueOf("can_add_account");

        final Response response = resource.createAccount(accountName);

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.CREATED);

        final AccountDefinition entity = (AccountDefinition) resource.getAccount(accountName).getEntity();

        assertThat(entity).isNotNull();
    }

    @Test
    public void can_delete_account() {
        final AccountName accountName = AccountName.valueOf("can_delete_account");

        final Response response = resource.createAccount(accountName);

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.CREATED);

        final AccountDefinition entity = (AccountDefinition) resource.getAccount(accountName).getEntity();

        assertThat(entity).isNotNull();

        assertThat(resource.deleteAccount(accountName).getStatusInfo()).isEqualTo(Response.Status.NO_CONTENT);

        assertThat(resource.getAccount(accountName).getStatusInfo()).isEqualTo(Response.Status.NOT_FOUND);
    }

    @Test
    public void can_add_key_to_account() {
        final AccountName accountName = AccountName.valueOf("can_add_key_to_account");

        final Response response = resource.createAccount(accountName);

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.CREATED);

        final AccountDefinition entity = (AccountDefinition) resource.getAccount(accountName).getEntity();

        assertThat(entity).isNotNull();

        final KeyName keyName = KeyName.valueOf("new-key");

        resource.addNewKey(accountName, new KeyCreateRequest(keyName));

        final AccountDefinition updatedAccount = (AccountDefinition) resource.getAccount(accountName).getEntity();

        assertThat(updatedAccount.getKeys()).containsKey(keyName);
    }

    @Test
    public void can_delete_key_from_account() {
        final AccountName accountName = AccountName.valueOf("can_delete_key_from_account");

        final Response response = resource.createAccount(accountName);

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.CREATED);

        final AccountDefinition entity = (AccountDefinition) resource.getAccount(accountName).getEntity();

        assertThat(entity).isNotNull();

        final KeyName keyName = KeyName.valueOf("new-key");

        resource.addNewKey(accountName, new KeyCreateRequest(keyName));

        final AccountDefinition updatedAccount = (AccountDefinition) resource.getAccount(accountName).getEntity();

        assertThat(updatedAccount.getKeys()).containsKey(keyName);

        resource.deleteAccountKey(accountName, keyName);

        final AccountDefinition deletedKeyAccountDef = (AccountDefinition) resource.getAccount(accountName).getEntity();

        assertThat(deletedKeyAccountDef.getKeys()).doesNotContainKey(keyName);
    }
}

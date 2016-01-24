package io.paradoxical.cassieq.unittests;

import io.paradoxical.cassieq.admin.resources.api.v1.AccountResource;
import io.paradoxical.cassieq.model.accounts.AccountDefinition;
import io.paradoxical.cassieq.model.accounts.AccountName;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.Response;

import static org.assertj.core.api.Assertions.assertThat;

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

        assertThat(resource.deleteAccount(accountName).getStatusInfo()).isEqualTo(Response.Status.OK);

        assertThat(resource.getAccount(accountName).getStatusInfo()).isEqualTo(Response.Status.NOT_FOUND);
    }

    @Test
    public void can_add_key_to_account() {
        final AccountName accountName = AccountName.valueOf("can_add_key_to_account");

        final Response response = resource.createAccount(accountName);

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.CREATED);

        final AccountDefinition entity = (AccountDefinition) resource.getAccount(accountName).getEntity();

        assertThat(entity).isNotNull();

        resource.addNewKey(accountName, "new-key");

        final AccountDefinition updatedAccount = (AccountDefinition) resource.getAccount(accountName).getEntity();

        assertThat(updatedAccount.getKeys()).containsKey("new-key");
    }

    @Test
    public void can_delete_key_from_account() {
        final AccountName accountName = AccountName.valueOf("can_delete_key_from_account");

        final Response response = resource.createAccount(accountName);

        assertThat(response.getStatusInfo()).isEqualTo(Response.Status.CREATED);

        final AccountDefinition entity = (AccountDefinition) resource.getAccount(accountName).getEntity();

        assertThat(entity).isNotNull();

        final String keyName = "new-key";

        resource.addNewKey(accountName, keyName);

        final AccountDefinition updatedAccount = (AccountDefinition) resource.getAccount(accountName).getEntity();

        assertThat(updatedAccount.getKeys()).containsKey(keyName);

        resource.deleteAccountKey(accountName, keyName);

        final AccountDefinition deletedKeyAccountDef = (AccountDefinition) resource.getAccount(accountName).getEntity();

        assertThat(deletedKeyAccountDef.getKeys()).doesNotContainKey(keyName);
    }
}

package io.paradoxical.cassieq.model.accounts;

import com.datastax.driver.core.Row;
import com.google.common.collect.ImmutableSet;
import io.paradoxical.cassieq.dataAccess.Tables;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDefinition {

    private AccountName accountName;

    private ImmutableSet<AccountKey> keys = ImmutableSet.of();

    public static AccountDefinition fromRow(final Row row) {
        return AccountDefinition.builder()
                .accountName(AccountName.valueOf(row.getString(Tables.Account.ACCOUNT_NAME)))
                .keys(ImmutableSet.copyOf(row.getSet(Tables.Account.KEYS, AccountKey.class)))
                .build();
    }
}

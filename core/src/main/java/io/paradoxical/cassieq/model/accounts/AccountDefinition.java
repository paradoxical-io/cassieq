package io.paradoxical.cassieq.model.accounts;

import com.datastax.driver.core.Row;
import com.godaddy.logging.LoggingScope;
import com.godaddy.logging.Scope;
import com.google.common.collect.ImmutableMap;
import io.paradoxical.cassieq.dataAccess.Tables;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDefinition {

    private AccountName accountName;

    @LoggingScope(scope = Scope.SKIP)
    private ImmutableMap<KeyName, AccountKey> keys = ImmutableMap.of();

    public static AccountDefinition fromRow(final Row row) {
        final Map<String, String> dbKeyMap = row.getMap(Tables.Account.KEYS, String.class, String.class);

        final ImmutableMap<KeyName, AccountKey> accountKeys =
                dbKeyMap.entrySet()
                        .stream()
                        .reduce(ImmutableMap.<KeyName, AccountKey>builder(),
                                (builder, keyEntry) -> builder.put(KeyName.valueOf(keyEntry.getKey()),
                                                                   AccountKey.valueOf(keyEntry.getValue())),
                                (one, same) -> one).build();

        return AccountDefinition.builder()
                                .accountName(AccountName.valueOf(row.getString(Tables.Account.ACCOUNT_NAME)))
                                .keys(accountKeys)
                                .build();
    }
}

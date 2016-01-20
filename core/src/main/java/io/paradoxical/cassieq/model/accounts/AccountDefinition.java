package io.paradoxical.cassieq.model.accounts;

import com.datastax.driver.core.Row;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import io.paradoxical.cassieq.dataAccess.Tables;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDefinition {

    private AccountName accountName;

    private ImmutableMap<String, AccountKey> keys = ImmutableMap.of();

    public static AccountDefinition fromRow(final Row row) {
        final Map<String, String> dbKeyMap = row.getMap(Tables.Account.KEYS, String.class, String.class);

        final ImmutableMap<String, AccountKey> accountKeys =
                dbKeyMap.entrySet()
                      .stream()
                      .reduce(ImmutableMap.<String, AccountKey>builder(),
                              (builder, keyEntry) -> builder.put(keyEntry.getKey(),
                                                                 AccountKey.valueOf(keyEntry.getValue())),
                              (one, same) -> one).build();

        return AccountDefinition.builder()
                                .accountName(AccountName.valueOf(row.getString(Tables.Account.ACCOUNT_NAME)))
                                .keys(accountKeys)
                                .build();
    }
}

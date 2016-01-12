package io.paradoxical.cassieq.model.accounts;

import com.datastax.driver.core.Row;
import com.google.common.collect.ImmutableSet;
import io.paradoxical.cassieq.dataAccess.Tables;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountDefinition {

    private AccountName accountName;

    private ImmutableSet<AccountKey> keys = ImmutableSet.of();

    public static AccountDefinition fromRow(final Row row) {
        final Set<String> keySet = row.getSet(Tables.Account.KEYS, String.class);

        return AccountDefinition.builder()
                                .accountName(AccountName.valueOf(row.getString(Tables.Account.ACCOUNT_NAME)))
                                .keys(ImmutableSet.copyOf(keySet.stream().map(AccountKey::valueOf).iterator()))
                                .build();
    }
}

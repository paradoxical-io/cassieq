package io.paradoxical.cassieq.dataAccess;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import io.paradoxical.cassieq.dataAccess.interfaces.AccountRepository;
import io.paradoxical.cassieq.model.accounts.AccountDefinition;
import io.paradoxical.cassieq.model.accounts.AccountKey;
import io.paradoxical.cassieq.model.accounts.AccountName;
import static com.datastax.driver.core.querybuilder.QueryBuilder.*;
import static java.util.stream.Collectors.toList;

import java.security.SecureRandom;
import java.util.List;
import java.util.Optional;

public class AccountRepositoryImpl extends RepositoryBase implements AccountRepository {

    private final Session session;
    private static final SecureRandom secureRandom = new SecureRandom();

    @Inject
    public AccountRepositoryImpl(final Session session) {
        this.session = session;
    }

    @Override
    public Optional<AccountDefinition> createAccount(final AccountName accountName) {
        AccountKey primaryKey = AccountKey.random(secureRandom);
        AccountKey secondaryKey = AccountKey.random(secureRandom);

        final Insert createNewAccount = insertInto(Tables.Account.TABLE_NAME)
                .ifNotExists()
                .value(Tables.Account.ACCOUNT_NAME, accountName.get())
                .value(Tables.Account.KEYS, ImmutableSet.of(primaryKey.get(), secondaryKey.get()));

        final ResultSet createAccountResult = session.execute(createNewAccount);

        if(createAccountResult.wasApplied()){
            return Optional.of(AccountDefinition.builder()
                              .accountName(accountName)
                              .keys(ImmutableSet.of(primaryKey, secondaryKey))
                                                .build());
        }

        return Optional.empty();
    }

    @Override
    public List<AccountDefinition> getAllAccounts() {
        final Select selectAll = QueryBuilder.select().all().from(Tables.Account.TABLE_NAME);

        final List<Row> allAccountRows = session.execute(selectAll).all();

        final List<AccountDefinition> accounts = allAccountRows.stream().map(AccountDefinition::fromRow).collect(toList());

        return accounts;
    }

    @Override
    public void deleteAccount(final AccountName accountName) {

    }
}

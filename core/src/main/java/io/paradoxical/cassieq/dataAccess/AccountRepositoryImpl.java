package io.paradoxical.cassieq.dataAccess;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.Statement;
import com.datastax.driver.core.querybuilder.Insert;
import com.datastax.driver.core.querybuilder.QueryBuilder;
import com.datastax.driver.core.querybuilder.Select;
import com.godaddy.logging.Logger;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import io.paradoxical.cassieq.dataAccess.interfaces.AccountRepository;
import io.paradoxical.cassieq.model.accounts.AccountDefinition;
import io.paradoxical.cassieq.model.accounts.AccountKey;
import io.paradoxical.cassieq.model.accounts.AccountName;
import io.paradoxical.cassieq.model.accounts.KeyName;
import io.paradoxical.cassieq.model.accounts.WellKnownKeyNames;

import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import static com.datastax.driver.core.querybuilder.QueryBuilder.delete;
import static com.datastax.driver.core.querybuilder.QueryBuilder.eq;
import static com.datastax.driver.core.querybuilder.QueryBuilder.insertInto;
import static com.datastax.driver.core.querybuilder.QueryBuilder.select;
import static com.datastax.driver.core.querybuilder.QueryBuilder.set;
import static com.datastax.driver.core.querybuilder.QueryBuilder.update;
import static com.godaddy.logging.LoggerFactory.getLogger;
import static java.util.stream.Collectors.toList;

public class AccountRepositoryImpl extends RepositoryBase implements AccountRepository {
    private static final Logger logger = getLogger(AccountRepositoryImpl.class);

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

        final ImmutableMap<KeyName, AccountKey> keys = ImmutableMap.of(
                WellKnownKeyNames.Primary.getKeyName(), primaryKey,
                WellKnownKeyNames.Secondary.getKeyName(), secondaryKey);

        final Insert createNewAccount = insertInto(Tables.Account.TABLE_NAME)
                .ifNotExists()
                .value(Tables.Account.ACCOUNT_NAME, accountName.get())
                .value(Tables.Account.KEYS, saveableMap(keys));

        final ResultSet createAccountResult = session.execute(createNewAccount);

        if (createAccountResult.wasApplied()) {
            return Optional.of(AccountDefinition.builder()
                                                .accountName(accountName)
                                                .keys(ImmutableMap.of(
                                                        WellKnownKeyNames.Primary.getKeyName(), primaryKey,
                                                        WellKnownKeyNames.Secondary.getKeyName(), secondaryKey))
                                                .build());
        }

        return Optional.empty();
    }

    @Override
    public Optional<AccountDefinition> getAccount(final AccountName accountName) {

        final Select.Where selectAccount = select().all().from(Tables.Account.TABLE_NAME)
                                                   .where(eq(Tables.Account.ACCOUNT_NAME, accountName.get()));

        return Optional.ofNullable(getOne(session.execute(selectAccount), AccountDefinition::fromRow));
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
        final Statement update =
                delete().all().from(Tables.Account.TABLE_NAME)
                        .where(eq(Tables.Account.ACCOUNT_NAME, accountName.get()));

        if (session.execute(update).wasApplied()) {
            logger.with("account-name", accountName).success("Deleted account");
        }
    }

    @Override
    public void updateAccount(final AccountDefinition accountDefinition) {
        final Statement update =
                update(Tables.Account.TABLE_NAME).where(eq(Tables.Account.ACCOUNT_NAME, accountDefinition.getAccountName().get()))
                                                 .with(set(Tables.Account.KEYS, saveableMap(accountDefinition.getKeys())));

        if (session.execute(update).wasApplied()) {
            logger.with(accountDefinition).success("Updated account");
        }
    }

    private ImmutableMap<String, String> saveableMap(final ImmutableMap<KeyName, AccountKey> keys) {
        final HashMap<String, String> target = new HashMap<>();

        keys.keySet().forEach(entry -> target.put(entry.get(), keys.get(entry).get()));

        return ImmutableMap.copyOf(target);
    }
}

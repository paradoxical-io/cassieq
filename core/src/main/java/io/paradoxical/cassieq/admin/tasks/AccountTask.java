package io.paradoxical.cassieq.admin.tasks;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import io.dropwizard.servlets.tasks.Task;
import io.paradoxical.cassieq.dataAccess.interfaces.AccountRepository;
import io.paradoxical.cassieq.factories.DataContextFactory;
import io.paradoxical.cassieq.model.accounts.AccountDefinition;
import io.paradoxical.cassieq.model.accounts.AccountKey;

import java.io.PrintWriter;
import java.util.List;

import static java.util.stream.Collectors.toList;

@SuppressWarnings("unused")
public class AccountTask extends Task {

    private final DataContextFactory dataContextFactory;

    @Inject
    protected AccountTask(final DataContextFactory dataContextFactory) {
        super("list-accounts");
        this.dataContextFactory = dataContextFactory;
    }

    @Override
    public void execute(final ImmutableMultimap<String, String> parameters, final PrintWriter output) throws Exception {
        final AccountRepository accountRepository = dataContextFactory.getAccountRepository();

        final List<AccountDefinition> allAccounts = accountRepository.getAllAccounts();

        allAccounts.forEach(account -> {

            final List<AccountKey> keys = account.getKeys().stream().collect(toList());

            output.format("account: %s\n    primary-key: %s\n    secondary-key: %s\n\n", account.getAccountName().get(), keys.get(0), keys.get(1));
        });
    }
}

package io.paradoxical.cassieq.discoverable.auth;

import com.godaddy.logging.Logger;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.paradoxical.cassieq.configurations.AuthConfig;
import io.paradoxical.cassieq.dataAccess.interfaces.AccountRepository;
import io.paradoxical.cassieq.factories.DataContextFactory;
import io.paradoxical.cassieq.model.accounts.AccountDefinition;
import io.paradoxical.cassieq.model.time.Clock;
import org.joda.time.Duration;

import static com.godaddy.logging.LoggerFactory.getLogger;

public class SignedRequestAuthenticator implements Authenticator<AuthorizedRequestCredentials, AccountPrincipal> {

    private static final Logger logger = getLogger(SignedRequestAuthenticator.class);

    private final AccountRepository accountRepository;
    private final Clock clock;
    private final AuthConfig authConfig;

    @Inject
    public SignedRequestAuthenticator(
            final DataContextFactory dataContextFactory,
            final Clock clock,
            final AuthConfig authConfig) {
        this.clock = clock;
        this.authConfig = authConfig;
        accountRepository = dataContextFactory.getAccountRepository();
    }

    @Override
    public Optional<AccountPrincipal> authenticate(final AuthorizedRequestCredentials credentials) throws AuthenticationException {
        final RequestParameters requestParameters = credentials.getRequestParameters();

        final java.util.Optional<AccountDefinition> account = accountRepository.getAccount(requestParameters.getAccountName());

        if (account.isPresent()) {

            final CredentialsVerificationContext credentialsVerificationContext =
                    new CredentialsVerificationContext(
                            account.get().getKeys().values(),
                            clock,
                            Duration.millis(authConfig.getAllowedClockSkew().toMilliseconds()));

            try {
                if (credentials.verify(credentialsVerificationContext)) {
                    final AccountPrincipal accountPrincipal =
                            new AccountPrincipal(
                                    requestParameters.getAccountName(),
                                    requestParameters.getAuthorizationLevels());

                    return Optional.of(accountPrincipal);
                }
            }
            catch (Exception e) {
                logger.error(e, "Credential Verification Failed");

                throw new AuthenticationException("Credential Verification Failed", e);
            }
        }

        return Optional.absent();
    }
}

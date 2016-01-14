package io.paradoxical.cassieq.auth;

import com.godaddy.logging.Logger;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.paradoxical.cassieq.dataAccess.interfaces.AccountRepository;
import io.paradoxical.cassieq.factories.DataContextFactory;
import io.paradoxical.cassieq.model.accounts.AccountDefinition;
import io.paradoxical.cassieq.model.auth.AuthorizedRequestCredentials;
import io.paradoxical.cassieq.model.auth.RequestParameters;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import static com.godaddy.logging.LoggerFactory.getLogger;

public class SignedRequestAuthenticator implements Authenticator<AuthorizedRequestCredentials, AccountPrincipal> {

    private static final Logger logger = getLogger(SignedRequestAuthenticator.class);


    private final AccountRepository accountRepository;

    @Inject
    public SignedRequestAuthenticator(DataContextFactory dataContextFactory) {
        accountRepository = dataContextFactory.getAccountRepository();
    }

    @Override
    public Optional<AccountPrincipal> authenticate(final AuthorizedRequestCredentials credentials) throws AuthenticationException {
        final RequestParameters requestParameters = credentials.getRequestParameters();

        final java.util.Optional<AccountDefinition> account = accountRepository.getAccount(requestParameters.getAccountName());

        if(account.isPresent()){
            try {
                if(credentials.verify(account.get().getKeys())){

                    return Optional.of(new AccountPrincipal(requestParameters.getAccountName(), requestParameters.getAuthorizationLevels()));
                }
            }
            catch (Exception e) {
                logger.error(e, "Error");
                throw new AuthenticationException("Credential Verification Failed", e);
            }
        }

        return Optional.absent();
    }
}

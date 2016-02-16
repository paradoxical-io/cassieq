package io.paradoxical.cassieq.discoverable.auth.parsers;

import io.paradoxical.cassieq.discoverable.auth.RequestAuthParameters;
import io.paradoxical.cassieq.model.accounts.AccountName;

import javax.ws.rs.container.ContainerRequestContext;

public interface AuthParametersParser<T extends RequestAuthParameters> {
    java.util.Optional<T> tryParse(ContainerRequestContext requestContext, final AccountName accountName);
}

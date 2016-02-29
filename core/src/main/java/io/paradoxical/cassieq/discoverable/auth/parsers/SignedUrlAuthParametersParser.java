package io.paradoxical.cassieq.discoverable.auth.parsers;

import com.google.common.base.Strings;
import io.paradoxical.cassieq.discoverable.auth.SignedUrlAuthParameters;
import io.paradoxical.cassieq.discoverable.auth.SignedUrlParameterNames;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.accounts.AccountName;
import io.paradoxical.cassieq.model.auth.AuthorizationLevel;
import io.paradoxical.cassieq.model.auth.SignatureGenerator;
import org.joda.time.DateTime;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.MultivaluedMap;
import java.util.EnumSet;

public class SignedUrlAuthParametersParser implements AuthParametersParser<SignedUrlAuthParameters> {

    @Override
    public java.util.Optional<SignedUrlAuthParameters> tryParse(final ContainerRequestContext requestContext, final AccountName accountName) {
        return java.util.Optional.ofNullable(parseSignedUrlParameters(requestContext, accountName));
    }

    private SignedUrlAuthParameters parseSignedUrlParameters(
            final ContainerRequestContext requestContext,
            final AccountName accountName) {

        final MultivaluedMap<String, String> queryParameters = requestContext.getUriInfo().getQueryParameters();

        final String sig = queryParameters.getFirst(SignedUrlParameterNames.Signature.getParameterName());

        if (Strings.isNullOrEmpty(sig)) {
            return null;
        }

        final EnumSet<AuthorizationLevel> authorizationLevels = parseAuthorizationLevel(queryParameters);

        final java.util.Optional<DateTime> startTime = parseTimeParam(queryParameters, SignedUrlParameterNames.StartTime);
        final java.util.Optional<DateTime> endTime = parseTimeParam(queryParameters, SignedUrlParameterNames.EndTime);
        final java.util.Optional<QueueName> authQueueName = parseQueueName(queryParameters);

        return SignedUrlAuthParameters.builder()
                                      .accountName(accountName)
                                      .authorizationLevels(authorizationLevels)
                                      .querySignature(sig)
                                      .endDateTime(endTime)
                                      .startDateTime(startTime)
                                      .queueName(authQueueName)
                                      .build();

    }

    private java.util.Optional<QueueName> parseQueueName(
            final MultivaluedMap<String, String> queryParameters) {

        final String queueName = queryParameters.getFirst(SignedUrlParameterNames.Queue.getParameterName());

        if (Strings.isNullOrEmpty(queueName)) {
            return java.util.Optional.empty();
        }

        return java.util.Optional.of(QueueName.valueOf(queueName));
    }

    private java.util.Optional<DateTime> parseTimeParam(
            final MultivaluedMap<String, String> queryParameters,
            final SignedUrlParameterNames parameter) {
        final String timeParam = queryParameters.getFirst(parameter.getParameterName());

        if (Strings.isNullOrEmpty(timeParam)) {
            return java.util.Optional.empty();
        }

        return java.util.Optional.of(SignatureGenerator.parseDateTime(timeParam));
    }

    private EnumSet<AuthorizationLevel> parseAuthorizationLevel(final MultivaluedMap<String, String> queryParameters) {
        String authParam = queryParameters.getFirst(SignedUrlParameterNames.AuthorizationLevels.getParameterName());

        return AuthorizationLevel.parse(authParam);
    }

}

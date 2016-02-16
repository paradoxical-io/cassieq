package io.paradoxical.cassieq.discoverable.auth;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import io.paradoxical.cassieq.model.QueueName;
import io.paradoxical.cassieq.model.auth.AuthorizationLevel;
import io.paradoxical.cassieq.model.auth.SignatureGenerator;
import io.paradoxical.cassieq.model.auth.SignedUrlSignatureGenerator;
import lombok.Getter;
import lombok.NonNull;
import org.joda.time.DateTime;

import javax.annotation.Nonnull;
import javax.crypto.Mac;
import javax.validation.constraints.NotNull;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Optional;

public enum SignedUrlParameterNames {
    AuthorizationLevels("auth"),
    Signature("sig"),
    StartTime("st"),
    EndTime("et"),
    Queue("q");

    @Getter
    private final String parameterName;

    SignedUrlParameterNames(@NonNull final String parameterName) {
        this.parameterName = parameterName;
    }

    public static SignedUrlParameterBuilder queryBuilder() {
        return new SignedUrlParameterBuilder();
    }

    @FunctionalInterface
    public interface SignedUrlGeneratorBuilder {
        String build(Mac hmac);
    }

    public static class SignedUrlParameterBuilder {
        private final Joiner.MapJoiner mapJoiner = Joiner.on('&')
                                                         .withKeyValueSeparator("=");

        private final LinkedHashMap<String, String> queryParamBuilder = Maps.newLinkedHashMap();

        private void addParam(SignedUrlParameterNames parameter, String value) {
            queryParamBuilder.put(parameter.getParameterName(), value);
        }

        public SignedUrlGeneratorBuilder fromSignatureGenerator(SignedUrlSignatureGenerator signedUrlSignatureGenerator) {
            return hmac -> auth(signedUrlSignatureGenerator.getAuthorizationLevels())
                    .queueName(signedUrlSignatureGenerator.getQueueName())
                    .startTime(signedUrlSignatureGenerator.getStartDateTime())
                    .endTime(signedUrlSignatureGenerator.getEndDateTime())
                    .sig(signedUrlSignatureGenerator.computeSignature(hmac)) // order matters, put this last to keep it pretty
                    .build();
        }


        public SignedUrlParameterBuilder sig(@NotNull @NonNull @Nonnull String signature) {
            addParam(Signature, signature);
            return this;
        }

        public SignedUrlParameterBuilder auth(@NotNull @NonNull @Nonnull EnumSet<AuthorizationLevel> levels) {
            addParam(AuthorizationLevels, AuthorizationLevel.stringify(levels));
            return this;
        }

        public SignedUrlParameterBuilder startTime(@NotNull @NonNull @Nonnull DateTime startTime) {
            final String startTimeParam = SignatureGenerator.formatDateTime(startTime);

            addParam(StartTime, startTimeParam);
            return this;
        }

        public SignedUrlParameterBuilder endTime(@NotNull @NonNull @Nonnull DateTime endTime) {
            final String endTimeParam = SignatureGenerator.formatDateTime(endTime);

            addParam(EndTime, endTimeParam);
            return this;
        }

        public SignedUrlParameterBuilder startTime(@NotNull @NonNull @Nonnull Optional<DateTime> startTimeOption) {
            startTimeOption.ifPresent(this::startTime);
            return this;
        }

        public SignedUrlParameterBuilder endTime(@NotNull @NonNull @Nonnull Optional<DateTime> endTimeOption) {
            endTimeOption.ifPresent(this::endTime);
            return this;
        }

        public SignedUrlParameterBuilder queueName(@NotNull @NonNull @Nonnull QueueName queueName) {
            addParam(Queue, queueName.get());
            return this;
        }

        public SignedUrlParameterBuilder queueName(@NotNull @NonNull @Nonnull Optional<QueueName> startTimeOption) {
            startTimeOption.ifPresent(this::queueName);
            return this;
        }

        public String build() {
            return mapJoiner.join(queryParamBuilder);
        }
    }
}

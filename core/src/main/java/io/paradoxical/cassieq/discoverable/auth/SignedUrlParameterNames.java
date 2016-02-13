package io.paradoxical.cassieq.discoverable.auth;

import com.google.common.base.Joiner;
import com.google.common.collect.Maps;
import io.paradoxical.cassieq.model.auth.AuthorizationLevel;
import io.paradoxical.cassieq.model.auth.SignedUrlSignatureGenerator;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.DateTime;

import javax.annotation.Nonnull;
import javax.validation.constraints.NotNull;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Optional;

public enum SignedUrlParameterNames {
    AuthorizationLevels("auth"),
    Signature("sig"),
    StartTime("st"),
    EndTime("et");

    @Getter
    private final String parameterName;

    SignedUrlParameterNames(@NonNull final String parameterName) {
        this.parameterName = parameterName;
    }

    public static SignedUrlParameterBuilder builder() {
        return new SignedUrlParameterBuilder();
    }

    public static class SignedUrlParameterBuilder {
        private final Joiner.MapJoiner mapJoiner = Joiner.on('&')
                                                         .withKeyValueSeparator("=");
        private final LinkedHashMap<String, String> queryParamBuilder = Maps.newLinkedHashMap();

        public SignedUrlParameterBuilder sig(@NotNull @NonNull @Nonnull String signature) {
            queryParamBuilder.put(Signature.getParameterName(), signature);

            return this;
        }

        public SignedUrlParameterBuilder auth(@NotNull @NonNull @Nonnull EnumSet<AuthorizationLevel> levels) {
            final String shortForm = levels.stream()
                                           .reduce(StringUtils.EMPTY,
                                                   (acc, level) -> acc + level.getShortForm(),
                                                   (acc1, acc2) -> acc1 + acc2);

            queryParamBuilder.put(AuthorizationLevels.getParameterName(), shortForm);

            return this;
        }

        public SignedUrlParameterBuilder startTime(@NotNull @NonNull @Nonnull DateTime startTime) {
            queryParamBuilder.put(StartTime.getParameterName(), SignedUrlSignatureGenerator.formatDateTime(startTime));
            return this;
        }

        public SignedUrlParameterBuilder endTime(@NotNull @NonNull @Nonnull DateTime endTime) {
            queryParamBuilder.put(EndTime.getParameterName(), SignedUrlSignatureGenerator.formatDateTime(endTime));
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

        public String build() {
            return mapJoiner.join(queryParamBuilder);
        }
    }
}

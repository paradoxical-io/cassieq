package io.paradoxical.cassieq.auth;

import io.paradoxical.cassieq.model.auth.AuthorizationLevel;
import lombok.Getter;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

public enum SignedUrlParameterNames {
    AuthorizationLevels("auth"),
    Signature("sig");

    @Getter
    private final String parameterName;

    SignedUrlParameterNames(@NonNull final String parameterName) {
        this.parameterName = parameterName;
    }

    public static SignedUrlParameterBuilder builder(){
        return new SignedUrlParameterBuilder();
    }

    public static class SignedUrlParameterBuilder{
        private List<String> queryParamBuilder = new ArrayList<>();

        public SignedUrlParameterBuilder sig(String signature){
            queryParamBuilder.add(Signature.getParameterName() + "=" + signature);

            return this;
        }

        public SignedUrlParameterBuilder auth(EnumSet<AuthorizationLevel> levels){
            final String shortForm = levels.stream()
                                           .reduce(StringUtils.EMPTY,
                                                   (acc, level) -> acc + level.getShortForm(),
                                                   (acc1, acc2) -> acc1 + acc2);

            queryParamBuilder.add(AuthorizationLevels.getParameterName() + "=" + shortForm);

            return this;
        }

        public String build(){
            return String.join("&", queryParamBuilder);
        }
    }
}

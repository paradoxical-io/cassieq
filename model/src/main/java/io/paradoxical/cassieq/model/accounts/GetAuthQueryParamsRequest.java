package io.paradoxical.cassieq.model.accounts;

import io.paradoxical.cassieq.model.auth.AuthorizationLevel;
import lombok.Value;

import javax.validation.constraints.NotNull;
import java.util.List;

@Value
public class GetAuthQueryParamsRequest {
    @NotNull
    private AccountName accountName;

    @NotNull
    private KeyName keyName;

    @NotNull
    private List<AuthorizationLevel> levels;
}

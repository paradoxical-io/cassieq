package io.paradoxical.cassieq.model.accounts;

import io.paradoxical.cassieq.model.auth.AuthorizationLevel;
import lombok.Value;
import org.joda.time.DateTime;

import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Optional;

@Value
public class GetAuthQueryParamsRequest {
    @NotNull
    private AccountName accountName;

    @NotNull
    private KeyName keyName;

    @NotNull
    private List<AuthorizationLevel> levels;

    private Optional<DateTime> startTime;

    private Optional<DateTime> endTime;
}

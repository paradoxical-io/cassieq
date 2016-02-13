package io.paradoxical.cassieq.model.accounts;

import io.paradoxical.cassieq.model.validators.StringTypeValid;
import lombok.Value;

@Value
public class KeyCreateRequest {
    @StringTypeValid
    private KeyName keyName;
}

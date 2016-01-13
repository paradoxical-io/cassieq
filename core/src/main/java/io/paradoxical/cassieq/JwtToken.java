package io.paradoxical.cassieq;

import com.google.common.collect.ImmutableMap;
import lombok.EqualsAndHashCode;
import lombok.Value;

import javax.security.auth.Subject;

@EqualsAndHashCode
@Value
public class JwtToken implements ClaimsPrincipal {

    private final ImmutableMap<String, Object> claims;

    @Override
    public String getName() {
        return "jwt-principal";
    }

    @Override
    public ImmutableMap<String, Object> getClaims() {
        return null;
    }
}

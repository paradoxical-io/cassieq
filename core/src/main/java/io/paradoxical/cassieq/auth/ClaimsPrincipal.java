package io.paradoxical.cassieq.auth;

import com.google.common.collect.ImmutableMap;

import java.security.Principal;

public interface ClaimsPrincipal extends Principal {
    ImmutableMap<String, Object> getClaims();
}

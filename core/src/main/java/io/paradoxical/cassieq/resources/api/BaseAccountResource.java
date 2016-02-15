package io.paradoxical.cassieq.resources.api;

import io.paradoxical.cassieq.model.accounts.AccountName;
import lombok.AccessLevel;
import lombok.Getter;

import javax.ws.rs.PathParam;

public abstract class BaseAccountResource {

    @Getter(AccessLevel.PROTECTED)
    private final AccountName accountName;

    public BaseAccountResource(@PathParam("accountName") AccountName accountName) {

        this.accountName = accountName;
    }
}
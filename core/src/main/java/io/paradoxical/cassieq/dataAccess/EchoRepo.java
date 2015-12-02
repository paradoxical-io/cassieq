package io.paradoxical.cassieq.dataAccess;

import io.paradoxical.cassieq.dataAccess.interfaces.DbRepo;

public class EchoRepo implements DbRepo {
    @Override public String echo(final String data) {
        return data;
    }
}

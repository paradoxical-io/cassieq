package io.paradoxical.cassieq.model.auth;

import com.google.common.base.Joiner;

public final class SignatureJoiner {
    public static final Joiner componentJoiner = Joiner.on('\n')
                                                       .skipNulls();

}

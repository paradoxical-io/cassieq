package io.paradoxical.cassieq.model.auth;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.EnumSet;

import static java.util.stream.Collectors.toList;

public enum AuthorizationLevel {
    None("n"),
    ReadMessage("r"),
    PutMessage("p"),
    UpdateMessage("u"),
    AckMessage("a"),
    GetQueueInformation("g"),
    CreateQueue("c"),
    DeleteQueue("d");

    public static final EnumSet<AuthorizationLevel> All =
            EnumSet.of(ReadMessage, PutMessage, UpdateMessage, AckMessage, GetQueueInformation, CreateQueue, DeleteQueue);

    private static final Splitter parseSplitter = Splitter.fixedLength(1);

    @Getter
    private final String shortForm;

    AuthorizationLevel(final String shortForm) {
        this.shortForm = shortForm;
    }

    public static EnumSet<AuthorizationLevel> emptyPermissions() {
        return EnumSet.noneOf(AuthorizationLevel.class);
    }

    public static EnumSet<AuthorizationLevel> parse(final String authLevels) {

        if (Strings.isNullOrEmpty(authLevels)) {
            return emptyPermissions();
        }

        final ImmutableSet<String> levels = ImmutableSet.copyOf(parseSplitter.split(authLevels));

        final EnumSet<AuthorizationLevel> authorizationLevels = EnumSet.copyOf(levels.stream().map(AuthorizationLevel::parseLevel).collect(toList()));

        if (authorizationLevels.contains(None)) {
            return emptyPermissions();
        }

        return authorizationLevels;
    }

    private static AuthorizationLevel parseLevel(final String levelString) {

        for (final AuthorizationLevel authorizationLevel : values()) {
            if (authorizationLevel.getShortForm().equals(levelString)) {
                return authorizationLevel;
            }
        }

        return None;
    }

    public static String stringify(EnumSet<AuthorizationLevel> authorizationLevels) {
        final Joiner joiner = Joiner.on(StringUtils.EMPTY);

        return joiner.join(authorizationLevels.stream().map(AuthorizationLevel::getShortForm).iterator());
    }
}

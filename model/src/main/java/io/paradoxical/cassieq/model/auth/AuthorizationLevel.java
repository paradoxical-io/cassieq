package io.paradoxical.cassieq.model.auth;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

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

    public static final EnumSet<AuthorizationLevel> All = buildAllLevelSet();

    private static EnumSet<AuthorizationLevel> buildAllLevelSet() {
        final EnumSet<AuthorizationLevel> allLevels = EnumSet.copyOf(Arrays.asList(values()));
        allLevels.remove(None);

        return allLevels;
    }

    private static final Splitter parseSplitter = Splitter.fixedLength(1);

    @Getter
    private final String shortForm;

    AuthorizationLevel(final String shortForm) {
        this.shortForm = shortForm;
    }

    public static EnumSet<AuthorizationLevel> emptyPermissions() {
        return EnumSet.noneOf(AuthorizationLevel.class);
    }

    @JsonCreator
    public static AuthorizationLevel fromJson(String value) {
        return parseLevel(value);
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
            if (authorizationLevel.getShortForm().equals(levelString) || authorizationLevel.toString().equals(levelString)) {
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

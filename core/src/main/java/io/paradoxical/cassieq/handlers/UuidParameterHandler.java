package io.paradoxical.cassieq.handlers;

import javax.ws.rs.ext.ParamConverter;
import java.util.UUID;

public class UuidParameterHandler<T> implements ParamConverter<T> {
    private final Class<?> clazz;

    public UuidParameterHandler(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override public T fromString(final String value) {
        try {
            return (T) (clazz.getMethod("valueOf", UUID.class).invoke(null, UUID.fromString(value)));
        }
        catch (Exception ex) {
            return null;
        }
    }

    @Override public String toString(final T value) {
        return value.toString();
    }
}

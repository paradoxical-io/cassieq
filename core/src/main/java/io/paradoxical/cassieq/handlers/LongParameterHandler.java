package io.paradoxical.cassieq.handlers;

import javax.ws.rs.ext.ParamConverter;

public class LongParameterHandler<T> implements ParamConverter<T> {
    private final Class<?> clazz;

    public LongParameterHandler(Class<T> clazz) {
        this.clazz = clazz;
    }

    @Override public T fromString(final String value) {
        try {
            return (T) (clazz.getMethod("valueOf", long.class).invoke(null, Long.parseLong(value)));
        }
        catch (Exception ex) {
            return null;
        }
    }

    @Override public String toString(final T value) {
        return value.toString();
    }
}

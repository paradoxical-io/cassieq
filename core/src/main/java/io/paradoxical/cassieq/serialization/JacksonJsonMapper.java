package io.paradoxical.cassieq.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.paradoxical.cassieq.model.mappers.Mappers;

import java.io.IOException;


public class JacksonJsonMapper implements JsonMapper {
    private final ObjectMapper mapper;

    public JacksonJsonMapper() {
        this.mapper = Mappers.getJson();
    }

    @Override
    public <T> String toJson(T item) throws JsonProcessingException {
        return mapper.writeValueAsString(item);
    }

    @Override
    public <T> T fromJson(String json, Class<T> target) throws IOException {
        if (json == null) {
            return null;
        }

        return mapper.readValue(json, target);
    }

    @Override
    public <T> T fromJson(final byte[] json, final Class<T> target) throws IOException {
        if (json == null) {
            return null;
        }

        return mapper.readValue(json, target);
    }

    @Override
    public <T> byte[] toJsonBytes(final T item) throws IOException {
        return mapper.writeValueAsBytes(item);
    }

    public ObjectMapper getMapper() {
        return mapper;
    }
}

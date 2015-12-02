package io.paradoxical.cassieq.serialization;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.joda.JodaModule;

import java.io.IOException;


public class JacksonJsonMapper implements JsonMapper {
    private final ObjectMapper mapper;

    public JacksonJsonMapper() {
        this.mapper = new ObjectMapper().configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
                                        .configure(SerializationFeature.INDENT_OUTPUT, true)
                                        .configure(SerializationFeature.WRITE_NULL_MAP_VALUES, true)
                                        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
                                        .configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true)
                                        .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                                        .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                                        .configure(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS, true)
                                        .configure(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS, true)
                                        .configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);

        this.mapper.registerModule(new JodaModule());
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

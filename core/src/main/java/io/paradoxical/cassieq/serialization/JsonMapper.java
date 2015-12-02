package io.paradoxical.cassieq.serialization;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.io.IOException;

public interface JsonMapper {
    <T> String toJson(T item) throws JsonProcessingException;

    <T> T fromJson(String json, Class<T> target) throws IOException;

    <T> T fromJson(byte[] json, Class<T> target) throws IOException;

    <T> byte[] toJsonBytes(T item) throws IOException;
}

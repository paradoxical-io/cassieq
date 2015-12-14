package io.paradoxical.cassieq.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

import java.io.IOException;
import java.util.Base64;

@Data
@JsonSerialize(using = PopReceipt.JsonSerializeAdapter.class)
@JsonDeserialize(using = PopReceipt.JsonDeserializeAdapater.class)
public final class PopReceipt {
    private final MonotonicIndex messageIndex;

    private final int messageVersion;

    private final MessageTag messageTag;

    public static PopReceipt valueOf(String string) {
        return parsePopReceipt(string);
    }

    public static PopReceipt from(Message message) {
        return new PopReceipt(message.getIndex(), message.getVersion(), message.getTag());
    }

    @Override
    public String toString() {
        return getPopReceipt();
    }

    private String getPopReceipt() {
        final String receiptString = String.format("%s:%s:%s:%s", getMessageIndex(), getMessageVersion(), getMessageTag());

        return Base64.getEncoder().withoutPadding().encodeToString(receiptString.getBytes());
    }

    private static PopReceipt parsePopReceipt(String popReceipt) {
        final byte[] rawReceipt = Base64.getDecoder().decode(popReceipt);

        final String receipt = new String(rawReceipt);

        final String[] components = receipt.split(":");

        final MonotonicIndex monotonicIndex = MonotonicIndex.valueOf(Long.parseLong(components[0]));
        final Integer messageVersion = Integer.parseInt(components[1]);
        final MessageTag messageTag = MessageTag.valueOf(components[2]);

        return new PopReceipt(monotonicIndex, messageVersion, messageTag);
    }

    public static class JsonDeserializeAdapater extends JsonDeserializer<PopReceipt> {

        @Override
        public PopReceipt deserialize(final JsonParser jp, final DeserializationContext ctxt)
                throws IOException {
            return PopReceipt.valueOf(jp.getValueAsString());
        }
    }

    public static class JsonSerializeAdapter extends JsonSerializer<PopReceipt> {
        @SuppressWarnings("ConstantConditions")
        @Override
        public void serialize(final PopReceipt value, final JsonGenerator jgen, final SerializerProvider provider)
                throws IOException {
            jgen.writeString(value.getPopReceipt());
        }
    }
}

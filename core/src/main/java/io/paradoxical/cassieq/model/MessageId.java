package io.paradoxical.cassieq.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.paradoxical.common.valuetypes.StringValue;
import io.paradoxical.common.valuetypes.adapters.xml.JaxbStringValueAdapter;
import jdk.nashorn.internal.ir.annotations.Immutable;
import org.apache.commons.lang3.StringUtils;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.IOException;

@Immutable
@XmlJavaTypeAdapter(value = MessageId.XmlAdapter.class)
@JsonSerialize(using = MessageId.JsonSerializeAdapter.class)
@JsonDeserialize(using = MessageId.JsonDeserializeAdapater.class)
public final class MessageId extends StringValue {
    protected MessageId(final String value) {
        super(value);
    }

    public static MessageId valueOf(String value) {
        return new MessageId(StringUtils.trimToEmpty(value));
    }

    public static MessageId valueOf(StringValue value) {
        return MessageId.valueOf(value.get());
    }

    public static class XmlAdapter extends JaxbStringValueAdapter<MessageId> {
        @Override
        protected MessageId createNewInstance(String value) {
            return MessageId.valueOf(value);
        }
    }

    public static class JsonDeserializeAdapater extends JsonDeserializer<MessageId> {

        @Override public MessageId deserialize(
                final JsonParser jp, final DeserializationContext ctxt) throws IOException, JsonProcessingException {
            return valueOf(jp.getValueAsString());
        }
    }

    public static class JsonSerializeAdapter extends JsonSerializer<MessageId> {
        @Override public void serialize(
                final MessageId value, final JsonGenerator jgen, final SerializerProvider provider) throws
                                                                                                    IOException,
                                                                                                    JsonProcessingException {
            jgen.writeString(value.get());
        }
    }
}

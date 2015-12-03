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
import java.util.Base64;
import java.util.Random;

@Immutable
@XmlJavaTypeAdapter(value = MessageTag.XmlAdapter.class)
@JsonSerialize(using = MessageTag.JsonSerializeAdapter.class)
@JsonDeserialize(using = MessageTag.JsonDeserializeAdapater.class)
public final class MessageTag extends StringValue {
    private static final int BYTES = 4;

    protected MessageTag(final String value) {
        super(value);
    }

    public static MessageTag valueOf(String value) {
        if (value == null) {
            return null;
        }
        return new MessageTag(StringUtils.trimToEmpty(value));
    }

    public static MessageTag valueOf(StringValue value) {
        if (value == null) {
            return null;
        }
        return MessageTag.valueOf(value.get());
    }

    public static MessageTag random() {
        Random r = new Random();
        byte[] bytes = new byte[MessageTag.BYTES];
        r.nextBytes(bytes);

        final String tag = Base64.getEncoder().withoutPadding().encodeToString(bytes);
        return MessageTag.valueOf(tag);
    }

    public static class XmlAdapter extends JaxbStringValueAdapter<MessageTag> {
        @Override
        protected MessageTag createNewInstance(String value) {
            return MessageTag.valueOf(value);
        }
    }

    public static class JsonDeserializeAdapater extends JsonDeserializer<MessageTag> {

        @Override
        public MessageTag deserialize(
                final JsonParser jp, final DeserializationContext ctxt) throws IOException, JsonProcessingException {
            return MessageTag.valueOf(jp.getValueAsString());
        }
    }

    public static class JsonSerializeAdapter extends JsonSerializer<MessageTag> {
        @Override
        public void serialize(
                final MessageTag value, final JsonGenerator jgen, final SerializerProvider provider) throws
                                                                                                     IOException,
                                                                                                     JsonProcessingException {
            jgen.writeString(value.get());
        }
    }
}

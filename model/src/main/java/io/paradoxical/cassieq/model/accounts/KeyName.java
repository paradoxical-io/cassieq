package io.paradoxical.cassieq.model.accounts;

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
@XmlJavaTypeAdapter(value = KeyName.XmlAdapter.class)
@JsonSerialize(using = KeyName.JsonSerializeAdapter.class)
@JsonDeserialize(using = KeyName.JsonDeserializeAdapater.class)
public final class KeyName extends StringValue {
    protected KeyName(final String value) {
        super(value);
    }

    public static KeyName valueOf(String value) {
        return new KeyName(StringUtils.trimToEmpty(value));
    }

    public static KeyName valueOf(StringValue value) {
        return KeyName.valueOf(value.get());
    }

    public static class XmlAdapter extends JaxbStringValueAdapter<KeyName> {
        @Override
        protected KeyName createNewInstance(String value) {
            return KeyName.valueOf(value);
        }
    }

    public static class JsonDeserializeAdapater extends JsonDeserializer<KeyName> {

        @Override
        public KeyName deserialize(
                final JsonParser jp, final DeserializationContext ctxt) throws IOException, JsonProcessingException {
            return KeyName.valueOf(jp.getValueAsString());
        }
    }

    public static class JsonSerializeAdapter extends JsonSerializer<KeyName> {
        @Override
        public void serialize(
                final KeyName value, final JsonGenerator jgen, final SerializerProvider provider) throws
                                                                                                  IOException,
                                                                                                  JsonProcessingException {
            jgen.writeString(value.get());
        }
    }
}

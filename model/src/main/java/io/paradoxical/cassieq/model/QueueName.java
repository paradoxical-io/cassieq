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
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.IOException;

@Immutable
@XmlJavaTypeAdapter(value = QueueName.XmlAdapter.class)
@JsonSerialize(using = QueueName.JsonSerializeAdapter.class)
@JsonDeserialize(using = QueueName.JsonDeserializeAdapater.class)
public final class QueueName extends StringValue {
    protected QueueName(final String value) {
        super(value);
    }

    public static QueueName valueOf(@NonNull String value) {
        return new QueueName(StringUtils.trimToEmpty(value));
    }

    public static QueueName valueOf(@NonNull StringValue value) {
        return QueueName.valueOf(value.get());
    }

    public static class XmlAdapter extends JaxbStringValueAdapter<QueueName> {
        @Override
        protected QueueName createNewInstance(String value) {
            return QueueName.valueOf(value);
        }
    }

    public static class JsonDeserializeAdapater extends JsonDeserializer<QueueName> {

        @Override public QueueName deserialize(
                final JsonParser jp, final DeserializationContext ctxt) throws IOException, JsonProcessingException {
            return QueueName.valueOf(jp.getValueAsString());
        }
    }

    public static class JsonSerializeAdapter extends JsonSerializer<QueueName> {
        @Override public void serialize(
                final QueueName value, final JsonGenerator jgen, final SerializerProvider provider) throws
                                                                                                IOException,
                                                                                                JsonProcessingException {
            jgen.writeString(value.get());
        }
    }
}

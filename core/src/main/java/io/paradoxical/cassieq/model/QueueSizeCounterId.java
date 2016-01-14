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
@XmlJavaTypeAdapter(value = QueueSizeCounterId.XmlAdapter.class)
@JsonSerialize(using = QueueSizeCounterId.JsonSerializeAdapter.class)
@JsonDeserialize(using = QueueSizeCounterId.JsonDeserializeAdapater.class)
public final class QueueSizeCounterId extends StringValue {
    protected QueueSizeCounterId(final String value) {
        super(value);
    }

    public static QueueSizeCounterId valueOf(String value) {
        return new QueueSizeCounterId(StringUtils.trimToEmpty(value));
    }

    public static QueueSizeCounterId valueOf(StringValue value) {
        return QueueSizeCounterId.valueOf(value.get());
    }

    public static class XmlAdapter extends JaxbStringValueAdapter<QueueSizeCounterId> {
        @Override
        protected QueueSizeCounterId createNewInstance(String value) {
            return QueueSizeCounterId.valueOf(value);
        }
    }

    public static class JsonDeserializeAdapater extends JsonDeserializer<QueueSizeCounterId> {

        @Override
        public QueueSizeCounterId deserialize(
                final JsonParser jp, final DeserializationContext ctxt) throws IOException, JsonProcessingException {
            return QueueSizeCounterId.valueOf(jp.getValueAsString());
        }
    }

    public static class JsonSerializeAdapter extends JsonSerializer<QueueSizeCounterId> {
        @Override
        public void serialize(
                final QueueSizeCounterId value, final JsonGenerator jgen, final SerializerProvider provider) throws
                                                                                                             IOException,
                                                                                                             JsonProcessingException {
            jgen.writeString(value.get());
        }
    }
}

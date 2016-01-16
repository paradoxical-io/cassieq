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
@XmlJavaTypeAdapter(value = QueueStatsId.XmlAdapter.class)
@JsonSerialize(using = QueueStatsId.JsonSerializeAdapter.class)
@JsonDeserialize(using = QueueStatsId.JsonDeserializeAdapater.class)
public final class QueueStatsId extends StringValue {
    protected QueueStatsId(final String value) {
        super(value);
    }

    public static QueueStatsId valueOf(String value) {
        return new QueueStatsId(StringUtils.trimToEmpty(value));
    }

    public static QueueStatsId valueOf(StringValue value) {
        return QueueStatsId.valueOf(value.get());
    }

    public static class XmlAdapter extends JaxbStringValueAdapter<QueueStatsId> {
        @Override
        protected QueueStatsId createNewInstance(String value) {
            return QueueStatsId.valueOf(value);
        }
    }

    public static class JsonDeserializeAdapater extends JsonDeserializer<QueueStatsId> {

        @Override
        public QueueStatsId deserialize(
                final JsonParser jp, final DeserializationContext ctxt) throws IOException, JsonProcessingException {
            return QueueStatsId.valueOf(jp.getValueAsString());
        }
    }

    public static class JsonSerializeAdapter extends JsonSerializer<QueueStatsId> {
        @Override
        public void serialize(
                final QueueStatsId value, final JsonGenerator jgen, final SerializerProvider provider) throws
                                                                                                             IOException,
                                                                                                             JsonProcessingException {
            jgen.writeString(value.get());
        }
    }
}

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
import io.paradoxical.cassieq.model.accounts.AccountName;
import io.paradoxical.common.valuetypes.StringValue;
import io.paradoxical.common.valuetypes.adapters.xml.JaxbStringValueAdapter;
import jdk.nashorn.internal.ir.annotations.Immutable;
import org.apache.commons.lang3.StringUtils;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.IOException;

@Immutable
@XmlJavaTypeAdapter(value = QueueId.XmlAdapter.class)
@JsonSerialize(using = QueueId.JsonSerializeAdapter.class)
@JsonDeserialize(using = QueueId.JsonDeserializeAdapater.class)
public final class QueueId extends StringValue {
    protected QueueId(final String value) {
        super(value);
    }

    public static QueueId valueOf(String value) {
        return new QueueId(StringUtils.trimToEmpty(value));
    }

    public static QueueId valueOf(AccountName accountName, QueueName name, int version){
        return QueueId.valueOf(accountName + ":" + name + "_v" + version);
    }

    public static QueueId valueOf(StringValue value) {
        return QueueId.valueOf(value.get());
    }

    public int getVersion(final QueueName queueName) {
        return Integer.valueOf(get().replaceFirst("^" + queueName.get() + "_v", ""));
    }

    public static class XmlAdapter extends JaxbStringValueAdapter<QueueId> {
        @Override
        protected QueueId createNewInstance(String value) {
            return QueueId.valueOf(value);
        }
    }

    public static class JsonDeserializeAdapater extends JsonDeserializer<QueueId> {

        @Override
        public QueueId deserialize(
                final JsonParser jp, final DeserializationContext ctxt) throws IOException, JsonProcessingException {
            return QueueId.valueOf(jp.getValueAsString());
        }
    }

    public static class JsonSerializeAdapter extends JsonSerializer<QueueId> {
        @Override
        public void serialize(
                final QueueId value, final JsonGenerator jgen, final SerializerProvider provider) throws
                                                                                                  IOException,
                                                                                                  JsonProcessingException {
            jgen.writeString(value.get());
        }
    }
}

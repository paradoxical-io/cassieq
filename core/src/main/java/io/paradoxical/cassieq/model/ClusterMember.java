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
@XmlJavaTypeAdapter(value = ClusterMember.XmlAdapter.class)
@JsonSerialize(using = ClusterMember.JsonSerializeAdapter.class)
@JsonDeserialize(using = ClusterMember.JsonDeserializeAdapater.class)
public final class ClusterMember extends StringValue {
    protected ClusterMember(final String value) {
        super(value);
    }

    public static ClusterMember valueOf(String value) {
        return new ClusterMember(StringUtils.trimToEmpty(value));
    }

    public static ClusterMember valueOf(StringValue value) {
        return ClusterMember.valueOf(value.get());
    }

    public static class XmlAdapter extends JaxbStringValueAdapter<ClusterMember> {
        @Override
        protected ClusterMember createNewInstance(String value) {
            return ClusterMember.valueOf(value);
        }
    }

    public static class JsonDeserializeAdapater extends JsonDeserializer<ClusterMember> {

        @Override
        public ClusterMember deserialize(
                final JsonParser jp, final DeserializationContext ctxt) throws IOException, JsonProcessingException {
            return ClusterMember.valueOf(jp.getValueAsString());
        }
    }

    public static class JsonSerializeAdapter extends JsonSerializer<ClusterMember> {
        @Override
        public void serialize(
                final ClusterMember value, final JsonGenerator jgen, final SerializerProvider provider) throws
                                                                                                        IOException,
                                                                                                        JsonProcessingException {
            jgen.writeString(value.get());
        }
    }
}

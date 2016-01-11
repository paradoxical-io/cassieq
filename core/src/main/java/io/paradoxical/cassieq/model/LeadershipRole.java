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
@XmlJavaTypeAdapter(value = LeadershipRole.XmlAdapter.class)
@JsonSerialize(using = LeadershipRole.JsonSerializeAdapter.class)
@JsonDeserialize(using = LeadershipRole.JsonDeserializeAdapater.class)
public final class LeadershipRole extends StringValue {
    protected LeadershipRole(final String value) {
        super(value);
    }

    public static LeadershipRole valueOf(String value) {
        return new LeadershipRole(StringUtils.trimToEmpty(value));
    }

    public static LeadershipRole valueOf(StringValue value) {
        return LeadershipRole.valueOf(value.get());
    }

    public static class XmlAdapter extends JaxbStringValueAdapter<LeadershipRole> {
        @Override
        protected LeadershipRole createNewInstance(String value) {
            return LeadershipRole.valueOf(value);
        }
    }

    public static class JsonDeserializeAdapater extends JsonDeserializer<LeadershipRole> {

        @Override
        public LeadershipRole deserialize(
                final JsonParser jp, final DeserializationContext ctxt) throws IOException, JsonProcessingException {
            return LeadershipRole.valueOf(jp.getValueAsString());
        }
    }

    public static class JsonSerializeAdapter extends JsonSerializer<LeadershipRole> {
        @Override
        public void serialize(
                final LeadershipRole value, final JsonGenerator jgen, final SerializerProvider provider) throws
                                                                                                         IOException,
                                                                                                         JsonProcessingException {
            jgen.writeString(value.get());
        }
    }
}

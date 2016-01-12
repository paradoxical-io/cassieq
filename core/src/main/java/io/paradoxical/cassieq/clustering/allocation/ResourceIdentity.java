package io.paradoxical.cassieq.clustering.allocation;

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
@XmlJavaTypeAdapter(value = ResourceIdentity.XmlAdapter.class)
@JsonSerialize(using = ResourceIdentity.JsonSerializeAdapter.class)
@JsonDeserialize(using = ResourceIdentity.JsonDeserializeAdapater.class)
public final class ResourceIdentity extends StringValue {
    protected ResourceIdentity(final String value) {
        super(value);
    }

    public static ResourceIdentity valueOf(String value) {
        return new ResourceIdentity(StringUtils.trimToEmpty(value));
    }

    public static ResourceIdentity valueOf(StringValue value) {
        return ResourceIdentity.valueOf(value.get());
    }

    public static class XmlAdapter extends JaxbStringValueAdapter<ResourceIdentity> {
        @Override
        protected ResourceIdentity createNewInstance(String value) {
            return ResourceIdentity.valueOf(value);
        }
    }

    public static class JsonDeserializeAdapater extends JsonDeserializer<ResourceIdentity> {

        @Override
        public ResourceIdentity deserialize(
                final JsonParser jp, final DeserializationContext ctxt) throws IOException, JsonProcessingException {
            return ResourceIdentity.valueOf(jp.getValueAsString());
        }
    }

    public static class JsonSerializeAdapter extends JsonSerializer<ResourceIdentity> {
        @Override
        public void serialize(
                final ResourceIdentity value, final JsonGenerator jgen, final SerializerProvider provider) throws
                                                                                                           IOException,
                                                                                                           JsonProcessingException {
            jgen.writeString(value.get());
        }
    }
}

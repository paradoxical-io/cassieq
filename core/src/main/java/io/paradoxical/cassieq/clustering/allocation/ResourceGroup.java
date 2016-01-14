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
@XmlJavaTypeAdapter(value = ResourceGroup.XmlAdapter.class)
@JsonSerialize(using = ResourceGroup.JsonSerializeAdapter.class)
@JsonDeserialize(using = ResourceGroup.JsonDeserializeAdapater.class)
public final class ResourceGroup extends StringValue {
    protected ResourceGroup(final String value) {
        super(value);
    }

    public static ResourceGroup valueOf(String value) {
        return new ResourceGroup(StringUtils.trimToEmpty(value));
    }

    public static ResourceGroup valueOf(StringValue value) {
        return ResourceGroup.valueOf(value.get());
    }

    public static class XmlAdapter extends JaxbStringValueAdapter<ResourceGroup> {
        @Override
        protected ResourceGroup createNewInstance(String value) {
            return ResourceGroup.valueOf(value);
        }
    }

    public static class JsonDeserializeAdapater extends JsonDeserializer<ResourceGroup> {

        @Override
        public ResourceGroup deserialize(
                final JsonParser jp, final DeserializationContext ctxt) throws IOException, JsonProcessingException {
            return ResourceGroup.valueOf(jp.getValueAsString());
        }
    }

    public static class JsonSerializeAdapter extends JsonSerializer<ResourceGroup> {
        @Override
        public void serialize(
                final ResourceGroup value, final JsonGenerator jgen, final SerializerProvider provider) throws
                                                                                                        IOException,
                                                                                                        JsonProcessingException {
            jgen.writeString(value.get());
        }
    }
}

package io.paradoxical.cassieq.model;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.paradoxical.common.valuetypes.LongValue;
import io.paradoxical.common.valuetypes.adapters.xml.JaxbLongValueAdapter;
import jdk.nashorn.internal.ir.annotations.Immutable;

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.IOException;

@Immutable
@XmlJavaTypeAdapter(value = GenericMessagePointer.XmlAdapter.class)
@JsonSerialize(using = GenericMessagePointer.JsonSerializeAdapter.class)
@JsonDeserialize(using = GenericMessagePointer.JsonDeserializeAdapater.class)
public final class GenericMessagePointer extends LongValue implements MessagePointer {
    protected GenericMessagePointer(final Long value) {
        super(value);
    }

    public static GenericMessagePointer valueOf(long value) {
        return new GenericMessagePointer(value);
    }

    public static class XmlAdapter extends JaxbLongValueAdapter<GenericMessagePointer> {

        @Nonnull
        @Override
        protected GenericMessagePointer createNewInstance(final Long value) {
            return GenericMessagePointer.valueOf(value);
        }
    }

    public static class JsonDeserializeAdapater extends JsonDeserializer<GenericMessagePointer> {

        @Override
        public GenericMessagePointer deserialize(final JsonParser jp, final DeserializationContext ctxt)
                throws IOException {
            return GenericMessagePointer.valueOf(jp.getValueAsLong());
        }
    }

    public static class JsonSerializeAdapter extends JsonSerializer<GenericMessagePointer> {
        @SuppressWarnings("ConstantConditions")
        @Override
        public void serialize(final GenericMessagePointer value, final JsonGenerator jgen, final SerializerProvider provider)
                throws IOException {
            jgen.writeNumber(value.get());
        }
    }
}
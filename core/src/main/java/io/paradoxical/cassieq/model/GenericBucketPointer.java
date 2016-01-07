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
@XmlJavaTypeAdapter(value = GenericBucketPointer.XmlAdapter.class)
@JsonSerialize(using = GenericBucketPointer.JsonSerializeAdapter.class)
@JsonDeserialize(using = GenericBucketPointer.JsonDeserializeAdapater.class)
public final class GenericBucketPointer extends LongValue implements BucketPointer {
    protected GenericBucketPointer(final Long value) {
        super(value);
    }

    public static GenericBucketPointer valueOf(long value) {
        return new GenericBucketPointer(value);
    }

    public static class XmlAdapter extends JaxbLongValueAdapter<GenericBucketPointer> {

        @Nonnull
        @Override
        protected GenericBucketPointer createNewInstance(final Long value) {
            return GenericBucketPointer.valueOf(value);
        }
    }

    public static class JsonDeserializeAdapater extends JsonDeserializer<GenericBucketPointer> {

        @Override
        public GenericBucketPointer deserialize(final JsonParser jp, final DeserializationContext ctxt)
                throws IOException {
            return GenericBucketPointer.valueOf(jp.getValueAsLong());
        }
    }

    public static class JsonSerializeAdapter extends JsonSerializer<GenericBucketPointer> {
        @SuppressWarnings("ConstantConditions")
        @Override
        public void serialize(final GenericBucketPointer value, final JsonGenerator jgen, final SerializerProvider provider)
                throws IOException {
            jgen.writeNumber(value.get());
        }
    }
}
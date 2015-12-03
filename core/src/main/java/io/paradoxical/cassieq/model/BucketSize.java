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
@XmlJavaTypeAdapter(value = BucketSize.XmlAdapter.class)
@JsonSerialize(using = BucketSize.JsonSerializeAdapter.class)
@JsonDeserialize(using = BucketSize.JsonDeserializeAdapater.class)
public final class BucketSize extends LongValue {
    protected BucketSize(final Long value) {
        super(value);
    }

    public static BucketSize valueOf(long value) {
        return new BucketSize(value);
    }

    public static class XmlAdapter extends JaxbLongValueAdapter<BucketSize> {

        @Nonnull @Override protected BucketSize createNewInstance(final Long value) {
            return BucketSize.valueOf(value);
        }
    }

    public static class JsonDeserializeAdapater extends JsonDeserializer<BucketSize> {

        @Override public BucketSize deserialize(final JsonParser jp, final DeserializationContext ctxt)
                throws IOException {
            return BucketSize.valueOf(jp.getValueAsLong());
        }
    }

    public static class JsonSerializeAdapter extends JsonSerializer<BucketSize> {
        @SuppressWarnings("ConstantConditions") @Override
        public void serialize(final BucketSize value, final JsonGenerator jgen, final SerializerProvider provider)
                throws IOException {
            jgen.writeNumber(value.get());
        }
    }
}
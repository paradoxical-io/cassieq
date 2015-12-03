package io.paradoxical.cassieq.model;

import com.datastax.driver.core.Row;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import io.paradoxical.cassieq.dataAccess.Tables;
import io.paradoxical.common.valuetypes.LongValue;
import io.paradoxical.common.valuetypes.adapters.xml.JaxbLongValueAdapter;
import jdk.nashorn.internal.ir.annotations.Immutable;

import javax.annotation.Nonnull;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.IOException;

@Immutable
@XmlJavaTypeAdapter(value = InvisibilityMessagePointer.XmlAdapter.class)
@JsonSerialize(using = InvisibilityMessagePointer.JsonSerializeAdapter.class)
@JsonDeserialize(using = InvisibilityMessagePointer.JsonDeserializeAdapater.class)
public final class InvisibilityMessagePointer extends LongValue implements MessagePointer {
    protected InvisibilityMessagePointer(final Long value) {
        super(value);
    }

    public static InvisibilityMessagePointer valueOf(long value) {
        return new InvisibilityMessagePointer(value);
    }

    public static InvisibilityMessagePointer valueOf(MessagePointer value) {
        return new InvisibilityMessagePointer(value.get());
    }

    public ReaderBucketPointer toBucketPointer(int bucketSize) {
        if (get() <= 0) {
            return ReaderBucketPointer.valueOf(0);
        }
        return ReaderBucketPointer.valueOf(get() / bucketSize);
    }

    public static InvisibilityMessagePointer map(Row row) {
        return InvisibilityMessagePointer.valueOf(row.getLong(Tables.Pointer.VALUE));
    }

    public static class XmlAdapter extends JaxbLongValueAdapter<InvisibilityMessagePointer> {

        @Nonnull
        @Override
        protected InvisibilityMessagePointer createNewInstance(final Long value) {
            return InvisibilityMessagePointer.valueOf(value);
        }
    }

    public static class JsonDeserializeAdapater extends JsonDeserializer<InvisibilityMessagePointer> {

        @Override
        public InvisibilityMessagePointer deserialize(final JsonParser jp, final DeserializationContext ctxt)
                throws IOException {
            return valueOf(jp.getValueAsLong());
        }
    }

    public static class JsonSerializeAdapter extends JsonSerializer<InvisibilityMessagePointer> {
        @SuppressWarnings("ConstantConditions")
        @Override
        public void serialize(final InvisibilityMessagePointer value, final JsonGenerator jgen, final SerializerProvider provider)
                throws IOException {
            jgen.writeNumber(value.get());
        }
    }
}
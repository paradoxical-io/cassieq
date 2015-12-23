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
@XmlJavaTypeAdapter(value = ReaderBucketPointer.XmlAdapter.class)
@JsonSerialize(using = ReaderBucketPointer.JsonSerializeAdapter.class)
@JsonDeserialize(using = ReaderBucketPointer.JsonDeserializeAdapater.class)
public final class ReaderBucketPointer extends LongValue implements BucketPointer {
    protected ReaderBucketPointer(final Long value) {
        super(value);
    }

    public static ReaderBucketPointer valueOf(long value) {
        return new ReaderBucketPointer(value);
    }

    public ReaderBucketPointer next() {
        return new ReaderBucketPointer(get() + 1);
    }

    public static ReaderBucketPointer map(Row row) {
        return ReaderBucketPointer.valueOf(row.getLong(Tables.Pointer.VALUE));
    }

    public static class XmlAdapter extends JaxbLongValueAdapter<ReaderBucketPointer> {

        @Nonnull @Override protected ReaderBucketPointer createNewInstance(final Long value) {
            return ReaderBucketPointer.valueOf(value);
        }
    }

    public static class JsonDeserializeAdapater extends JsonDeserializer<ReaderBucketPointer> {

        @Override public ReaderBucketPointer deserialize(final JsonParser jp, final DeserializationContext ctxt)
                throws IOException {
            return ReaderBucketPointer.valueOf(jp.getValueAsLong());
        }
    }

    public static class JsonSerializeAdapter extends JsonSerializer<ReaderBucketPointer> {
        @SuppressWarnings("ConstantConditions") @Override
        public void serialize(final ReaderBucketPointer value, final JsonGenerator jgen, final SerializerProvider provider)
                throws IOException {
            jgen.writeNumber(value.get());
        }
    }
}
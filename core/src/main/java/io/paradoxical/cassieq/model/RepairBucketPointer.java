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
@XmlJavaTypeAdapter(value = RepairBucketPointer.XmlAdapter.class)
@JsonSerialize(using = RepairBucketPointer.JsonSerializeAdapter.class)
@JsonDeserialize(using = RepairBucketPointer.JsonDeserializeAdapater.class)
public final class RepairBucketPointer extends LongValue implements BucketPointer {
    protected RepairBucketPointer(final Long value) {
        super(value);
    }

    public static RepairBucketPointer valueOf(long value) {
        return new RepairBucketPointer(value);
    }

    public static RepairBucketPointer map(Row row) {
        return RepairBucketPointer.valueOf(row.getLong(Tables.Pointer.VALUE));
    }

    public RepairBucketPointer next() {
        return new RepairBucketPointer(get() + 1);
    }


    public MonotonicIndex startOf(BucketSize bucketsize) {
        return MonotonicIndex.valueOf(get() * bucketsize.get());
    }

    public static class XmlAdapter extends JaxbLongValueAdapter<RepairBucketPointer> {

        @Nonnull
        @Override
        protected RepairBucketPointer createNewInstance(final Long value) {
            return RepairBucketPointer.valueOf(value);
        }
    }

    public static class JsonDeserializeAdapater extends JsonDeserializer<RepairBucketPointer> {

        @Override
        public RepairBucketPointer deserialize(final JsonParser jp, final DeserializationContext ctxt)
                throws IOException {
            return RepairBucketPointer.valueOf(jp.getValueAsLong());
        }
    }

    public static class JsonSerializeAdapter extends JsonSerializer<RepairBucketPointer> {
        @SuppressWarnings("ConstantConditions")
        @Override
        public void serialize(final RepairBucketPointer value, final JsonGenerator jgen, final SerializerProvider provider)
                throws IOException {
            jgen.writeNumber(value.get());
        }
    }
}
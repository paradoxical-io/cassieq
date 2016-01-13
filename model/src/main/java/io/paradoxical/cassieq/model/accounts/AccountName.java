package io.paradoxical.cassieq.model.accounts;

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
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.IOException;

@Immutable
@XmlJavaTypeAdapter(value = AccountName.XmlAdapter.class)
@JsonSerialize(using = AccountName.JsonSerializeAdapter.class)
@JsonDeserialize(using = AccountName.JsonDeserializeAdapater.class)
public final class AccountName extends StringValue {
    protected AccountName(@NonNull final String value) {
        super(value);
    }

    public static AccountName valueOf(@NonNull String value) {
        return new AccountName(StringUtils.trimToEmpty(value));
    }

    public static AccountName valueOf(@NonNull StringValue value) {
        return AccountName.valueOf(value.get());
    }

    public static class XmlAdapter extends JaxbStringValueAdapter<AccountName> {
        @Override
        protected AccountName createNewInstance(String value) {
            return AccountName.valueOf(value);
        }
    }

    public static class JsonDeserializeAdapater extends JsonDeserializer<AccountName> {

        @Override
        public AccountName deserialize(
                final JsonParser jp, final DeserializationContext ctxt) throws IOException, JsonProcessingException {
            return AccountName.valueOf(jp.getValueAsString());
        }
    }

    public static class JsonSerializeAdapter extends JsonSerializer<AccountName> {
        @Override
        public void serialize(
                final AccountName value, final JsonGenerator jgen, final SerializerProvider provider) throws
                                                                                                      IOException,
                                                                                                      JsonProcessingException {
            jgen.writeString(value.get());
        }
    }
}

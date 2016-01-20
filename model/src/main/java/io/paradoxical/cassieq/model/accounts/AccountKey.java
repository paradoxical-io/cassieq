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
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;

@Immutable
@XmlJavaTypeAdapter(value = AccountKey.XmlAdapter.class)
@JsonSerialize(using = AccountKey.JsonSerializeAdapter.class)
@JsonDeserialize(using = AccountKey.JsonDeserializeAdapater.class)
public final class AccountKey extends StringValue {

    public static final int AccountKeyBitLegth = 512;
    public static final int AccountKeyByteLength = AccountKeyBitLegth / 8;

    protected AccountKey(final String value) {
        super(value);
    }

    public byte[] getBytes() {
        return Base64.getUrlDecoder().decode(get());
    }

    public static AccountKey valueOf(String value) {
        return new AccountKey(StringUtils.trimToEmpty(value));
    }

    public static AccountKey valueOf(StringValue value) {
        return AccountKey.valueOf(value.get());
    }

    public static AccountKey random(SecureRandom secureRandomSource) {

        byte[] randomBytes = new byte[AccountKeyByteLength];
        secureRandomSource.nextBytes(randomBytes);

        final Base64.Encoder encoder = Base64.getUrlEncoder().withoutPadding();
        final String base64Key = encoder.encodeToString(randomBytes);

        return AccountKey.valueOf(base64Key);
    }

    public static class XmlAdapter extends JaxbStringValueAdapter<AccountKey> {
        @Override
        protected AccountKey createNewInstance(String value) {
            return AccountKey.valueOf(value);
        }
    }

    public static class JsonDeserializeAdapater extends JsonDeserializer<AccountKey> {

        @Override
        public AccountKey deserialize(
                final JsonParser jp, final DeserializationContext ctxt) throws IOException, JsonProcessingException {
            return AccountKey.valueOf(jp.getValueAsString());
        }
    }

    public static class JsonSerializeAdapter extends JsonSerializer<AccountKey> {
        @Override
        public void serialize(
                final AccountKey value, final JsonGenerator jgen, final SerializerProvider provider) throws
                                                                                                     IOException,
                                                                                                     JsonProcessingException {
            jgen.writeString(value.get());
        }
    }
}

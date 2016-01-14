package io.paradoxical.cassieq.clustering.allocation;

import io.paradoxical.common.valuetypes.StringValue;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public final class ResourceIdentity implements Serializable {

    private String value;

    public String get() {
        return value;
    }

    public static ResourceIdentity valueOf(String value) {
        return new ResourceIdentity(value);
    }

    public static ResourceIdentity valueOf(StringValue value) {
        return new ResourceIdentity(value.get());
    }
}

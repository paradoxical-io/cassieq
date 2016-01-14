package io.paradoxical.cassieq.clustering.allocation;

import lombok.Data;

@Data
public class ResourceConfig{
    private final ResourceGroup group;

    private final int maxPerMember;
}

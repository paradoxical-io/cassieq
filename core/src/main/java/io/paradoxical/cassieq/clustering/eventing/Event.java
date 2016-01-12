package io.paradoxical.cassieq.clustering.eventing;

import java.io.Serializable;

public interface Event extends Serializable {
    default String getId(){
        return getClass().getSimpleName();
    }
}

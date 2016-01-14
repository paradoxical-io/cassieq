package io.paradoxical.cassieq.clustering.eventing;

import java.io.Serializable;

public interface Event extends Serializable {
    default String getEventId(){
        return getClass().getSimpleName();
    }
}

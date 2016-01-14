package io.paradoxical.cassieq.model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public final class ClusterMember implements Serializable {
    private String value;

    public String get(){
        return value;
    }

    public static ClusterMember valueOf(String value){
        return new ClusterMember(value);
    }
}

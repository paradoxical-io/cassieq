package io.paradoxical.cassieq.configurations;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotEmpty;

public class RepairConfig {

    @NotEmpty
    @Getter
    @Setter
    private String raftConfigPath = "docker/data/conf/raft.xml";

}
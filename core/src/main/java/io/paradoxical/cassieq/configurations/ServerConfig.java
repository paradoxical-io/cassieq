package io.paradoxical.cassieq.configurations;

import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.NotEmpty;

public class ServerConfig {

    @NotEmpty
    @Getter
    @Setter
    private String name = "localhost";

}
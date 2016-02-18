package io.paradoxical.cassieq.environment;

import com.datastax.driver.core.ConsistencyLevel;
import com.google.common.base.Strings;
import io.paradoxical.cassieq.configurations.AllocationStrategy;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public interface SystemProps {
    @Env(defaultValue = "/data/db", help = "The location of the cql scripts for bootstrapping/dev mode")
    String DB_SCRIPTS_PATH();

    @Env(defaultValue = "cassieq", help = "The cassandra cluster name")
    String CLUSTER_NAME();

    @Env(defaultValue = "192.168.99.100", help = "The cassandra cluster contact points")
    String CONTACT_POINTS();

    @Env(defaultValue = "cassieq", help = "The cassandra keyspace cassieq lives in")
    String KEYSPACE();

    @Env(defaultValue = "9042", help = "The cassandra cql client port")
    Integer CASSANDRA_PORT();

    @Env(defaultValue = "", help = "The cassandra username")
    String USERNAME();

    @Env(defaultValue = "", help = "The cassandra password")
    String PASSWORD();

    @Env(defaultValue = "500", help = "The max number of connections to a remote data center")
    Integer REMOTE_MAX_CONNECTIONS();

    @Env(defaultValue = "LOCAL_QUORUM", help = "The consistency level of queries")
    ConsistencyLevel CONSISTENCY_LEVEL();

    @Env(defaultValue = "false", help = "Enable or disable https")
    Boolean HTTPS();

    @Env(defaultValue = "PKCS12", help = "The https keystore type")
    String HTTPS_KEY_STORE_TYPE();

    @Env(defaultValue = "/data/https/serverKeys.p12", help = "The keystore path")
    String HTTPS_KEY_STORE_PATH();

    @Env(defaultValue = "", help = "The keystore password")
    String HTTPS_KEY_STORE_PASSWORD();

    @Env(defaultValue = "true", help = "Whether to do cert validation or not")
    Boolean HTTPS_VALIDATE_CERTS();

    @Env(defaultValue = "NONE", help = "The repair worker queue distribution strategy")
    AllocationStrategy ALLOCATION_STRATEGY();

    @Env(defaultValue = "false", help = "Enable graphite publishing")
    Boolean USE_METRICS_GRAPHITE();

    @Env(defaultValue = "192.168.99.100", help = "The graphite endpoint url")
    String GRAPHITE_URL();

    @Env(defaultValue = "2003", help = "The graphite port")
    Integer GRAPHITE_PORT();

    @Env(defaultValue = "", help = "The graphite prefix")
    String GRAPHITE_PREFIX();

    static List<SystemPropDiscovery> discover() {
        return Arrays.asList(SystemProps.class.getMethods())
                     .stream()
                     .map(method -> {
                         if (Modifier.isStatic(method.getModifiers())) {
                             return null;
                         }

                         final Env env = method.getAnnotation(Env.class);

                         String envVarName = method.getName();

                         try {
                             return new SystemPropDiscovery(env.help(), env.defaultValue(), envVarName, getFromMethod(method));
                         }
                         catch (Exception e) {
                             throw new RuntimeException("Error getting value!", e);
                         }
                     })
                     .filter(m -> m != null)
                     .collect(Collectors.toList());
    }

    static SystemProps instance() {
        return (SystemProps) Proxy.newProxyInstance(
                SystemProps.class.getClassLoader(),
                new Class[]{ SystemProps.class },
                (proxy, method, args) -> getFromMethod(method));
    }

    static Object getFromMethod(Method method) throws InvocationTargetException, IllegalAccessException {
        final Class<?> returnType = method.getReturnType();

        String envVar = System.getenv(method.getName());

        if (Strings.isNullOrEmpty(envVar)) {
            envVar = method.getAnnotation(Env.class).defaultValue();
        }

        if (returnType.isInstance(String.class)) {
            return envVar;
        }

        /**
         * Mistyping a boolean as "true1" just evaluates to false... why java why..
         */
        if(returnType.isAssignableFrom(Boolean.class) && !(envVar.equals("true") || envVar.equals("false"))){
            throw new RuntimeException("Boolean variable should be either true or false but was " +  envVar);
        }

        final Optional<Method> valueOfStringConverter
                = Arrays.asList(returnType.getMethods())
                        .stream()
                        .filter(i -> i.getName().equals("valueOf")
                                     && i.getParameterCount() == 1 &&
                                     i.getParameterTypes()[0].isAssignableFrom(String.class))
                        .findFirst();

        if (valueOfStringConverter.isPresent()) {
            return valueOfStringConverter.get().invoke(null, envVar);
        }

        throw new RuntimeException("Could not convert string to type: " + method.getName());
    }
}

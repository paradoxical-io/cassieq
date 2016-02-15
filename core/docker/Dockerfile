FROM java:8

# load the cassadanra loader first, since this wont change much
RUN curl -O -s http://central.maven.org/maven2/io/paradoxical/cassandra.loader/1.2/cassandra.loader-1.2-runner.jar && \
            mkdir -p /data/tools && \
            mv cassandra.loader-1.2-runner.jar /data/tools/cassandra-loader.jar

# add the new jar/configs
ADD data /data

VOLUME ["/data/logs", "/data/conf", "/data/https"]

# set the service to run
ENTRYPOINT ["/data/bin/service"]

# these are the default arguments.
# if others are passed with
#
#   docker -it run paradoxical/cassieq args...
#
# these arguments will be overwritten.
CMD ["server" "/data/conf/configuration.yml"]
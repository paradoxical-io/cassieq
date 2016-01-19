FROM java:8

# load the cassadanra loader first, since this wont change much
RUN curl -O -s https://oss.sonatype.org/content/repositories/snapshots/io/paradoxical/cassandra.loader/1.0-SNAPSHOT/cassandra.loader-1.0-20151203.021931-4-runner.jar && \
            mkdir -p /data/tools && \
            mv cassandra.loader-1.0-20151203.021931-4-runner.jar /data/tools/cassandra-loader.jar

# add the new jar/configs
ADD data /data

# set the service to run
ENTRYPOINT ["/data/bin/service"]
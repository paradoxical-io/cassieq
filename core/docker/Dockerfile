FROM java:8

ADD data /data

RUN curl -O https://oss.sonatype.org/content/repositories/snapshots/io/paradoxical/cassandra.loader/1.0-SNAPSHOT/cassandra.loader-1.0-20151203.021931-4-runner.jar && \
            mkdir -p /data/tools && \
            mv cassandra.loader-1.0-20151203.021931-4-runner.jar /data/tools/cassandra-loader.jar

# set the service to run
ENTRYPOINT ["/data/bin/service"]
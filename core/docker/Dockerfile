FROM java:8

RUN apt-get update && apt-get -y install maven

ADD data /data

RUN git clone https://github.com/paradoxical-io/cassandra-loader.git /data/tools
RUN cd /data/tools/cassandra-loader && mvn clean package -DskipTests -P shade

# set the service to run
ENTRYPOINT ["/data/bin/service"]
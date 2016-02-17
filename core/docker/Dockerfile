FROM java:8

# add the new jar/configs/dbs/libs
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
CMD ["server", "/data/conf/configuration.yml"]
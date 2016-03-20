FROM alpine:3.2

RUN echo "http://dl-4.alpinelinux.org/alpine/edge/community" >> /etc/apk/repositories \
    && apk add --update \
        bash \
        openjdk8-jre-base \
    && rm /var/cache/apk/*

RUN echo "@testing http://dl-4.alpinelinux.org/alpine/edge/testing" >> /etc/apk/repositories \
    && apk add --update dos2unix@testing \
    && rm /var/cache/apk/*


# add the new jar/configs/dbs/libs
ADD data /data

RUN chmod +x /data/bin/service; dos2unix /data/bin/service

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
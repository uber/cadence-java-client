FROM adoptopenjdk/openjdk11@sha256:13475f4be7d6701a2ebd0516c4c929f2fa78ea773b4483bb580a36a04975e9f9

# Apache Thrift version
ENV APACHE_THRIFT_VERSION=0.9.3

# Install dependencies using apk
RUN apk update && apk add --virtual wget ca-certificates wget && apk add --virtual build-dependencies build-base gcc
# Git is needed in order to update the dls submodule
RUN apk add git libstdc++

# Compile source
RUN set -ex ;\
	wget https://downloads.apache.org/thrift/${APACHE_THRIFT_VERSION}/thrift-${APACHE_THRIFT_VERSION}.tar.gz && \
	tar -xvf thrift-${APACHE_THRIFT_VERSION}.tar.gz && \
	rm thrift-${APACHE_THRIFT_VERSION}.tar.gz && \
	cd thrift-${APACHE_THRIFT_VERSION}/ && \
	./configure --enable-libs=no --enable-tests=no --enable-tutorial=no --with-cpp=no --with-c_glib=no --with-java=yes --with-ruby=no --with-erlang=no --with-go=no --with-nodejs=no --with-python=no && \
  touch config.h && \
	make && \
	make install && \
	cd .. && \
	rm -rf thrift-${APACHE_THRIFT_VERSION}

# Cleanup packages and remove cache
RUN apk del build-dependencies wget && rm -rf /var/cache/apk/*

RUN mkdir /cadence-java-client
WORKDIR /cadence-java-client

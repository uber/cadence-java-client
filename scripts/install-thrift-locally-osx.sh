#!/bin/bash -e

cd $(dirname $0)/..

if [[ ! -d .dependencies ]]; then
	mkdir .dependencies
fi

cd .dependencies
rm -rf thrift
mkdir thrift
cd thrift

APACHE_THRIFT_VERSION=0.9.3

brew install bison

# macs have an old versiion of bison hanging around typically
# so override it by using the brew version in PATH
export PATH="/opt/homebrew/opt/bison/bin:$PATH"

wget https://archive.apache.org/dist/thrift/${APACHE_THRIFT_VERSION}/thrift-${APACHE_THRIFT_VERSION}.tar.gz && \

tar -xvf thrift-${APACHE_THRIFT_VERSION}.tar.gz
rm thrift-${APACHE_THRIFT_VERSION}.tar.gz
cd thrift-${APACHE_THRIFT_VERSION}/

./configure --enable-libs=no --enable-tests=no --enable-tutorial=no --with-cpp=no --with-c_glib=no --with-java=yes --with-ruby=no --with-erlang=no --with-go=no --with-nodejs=no --with-python=no && \

make
echo ""
echo "thrift expects to be globally installed :/"
echo "asking do do with sudo to install to /usr/local/bin"
echo ""
sudo make install && \
cd .. && \
rm -rf thrift-${APACHE_THRIFT_VERSION}
thrift --version
echo "done"

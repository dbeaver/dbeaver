#!/bin/bash
cd ..
[ ! -d ../dbeaver-common ] && git clone https://github.com/dbeaver/dbeaver-common.git ../dbeaver-common
[ ! -d ../dbeaver-jdbc-libsql ] && git clone https://github.com/dbeaver/dbeaver-jdbc-libsql.git ../dbeaver-jdbc-libsql

cd product/aggregate
mvn clean install -Pall-platforms -T 1C
cd ../..


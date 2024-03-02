#!/bin/bash

[ ! -d ../dbeaver-common ] && git clone https://github.com/dbeaver/dbeaver-common.git ../dbeaver-common

cd product/aggregate
mvn clean install -T 1C -Pall-platforms
cd ../..


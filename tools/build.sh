#!/bin/bash
cd ..
[ ! -d ../dbeaver-common ] && git clone https://github.com/dbeaver/dbeaver-common.git ../dbeaver-common

cd product/aggregate
mvn clean install -Pall-platforms -T 1C
cd ../..


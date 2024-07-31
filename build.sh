#!/bin/bash

[ ! -d ../dbeaver-common ] && git clone https://github.com/dbeaver/dbeaver-common.git ../dbeaver-common

../dbeaver-common/mvnw clean install -Pall-platforms -T 1C -f "product/aggregate"

#!/bin/bash
cd contrib
mvn install:install-file -Dfile=com.ibm.icu.base_58.2.0.v20170418-1837.jar -DgroupId=com.ibm.icu -DartifactId=com.ibm.icu.base -Dversion=58.2.0 -Dpackaging=jar

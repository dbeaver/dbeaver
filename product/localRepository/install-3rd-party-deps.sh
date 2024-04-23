#!/bin/bash
cd contrib
mvn -f pom-deps.xml clean package
mvn install:install-file -Dfile=com.ibm.icu.base_58.2.0.v20170418-1837.jar -DgroupId=com.ibm.icu -DartifactId=com.ibm.icu.base -Dversion=58.2.0 -Dpackaging=jar
mvn install:install-file -Dfile=bcpkix-jdk18on-1.77.jar -DgroupId= -DartifactId=bcpkix -Dversion=1.77 -Dpackaging=jar
mvn install:install-file -Dfile=bcpg-jdk18on-1.77.jar -DgroupId= -DartifactId=bcpg -Dversion=1.77 -Dpackaging=jar
mvn install:install-file -Dfile=bcprov-jdk18on-1.77.jar -DgroupId= -DartifactId=bcprov -Dversion=1.77 -Dpackaging=jar
mvn install:install-file -Dfile=bcutil-jdk18on-1.77.jar -DgroupId= -DartifactId=bcutil -Dversion=1.77 -Dpackaging=jar

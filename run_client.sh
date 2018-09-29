#!/bin/bash

ROOT_DIR=$(pwd)
CLIENT_DIR="$ROOT_DIR/Client"
JAR_DIR="$CLIENT_DIR/target"	
JAR_CLIENT_SUFFIX="Client-1.0-SNAPSHOT-jar-with-dependencies.jar"
JAR_LOCATION="$JAR_DIR/$JAR_CLIENT_SUFFIX"
COMMON_DIR="$ROOT_DIR/Server/Common/pom.xml"

mvn -f $COMMON_DIR clean install

mvn -f "$CLIENT_DIR" clean compile assembly:single
cp java.policy "$JAR_DIR"

echo GENERATE POLICY FILE
echo "grant {" > $JAR_DIR/java.policy
echo "	permission java.security.AllPermission;" >> $JAR_DIR/java.policy
echo "};" >> $JAR_DIR/java.policy


java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$JAR_LOCATION -jar $JAR_LOCATION

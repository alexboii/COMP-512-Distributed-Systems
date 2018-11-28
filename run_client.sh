#!/bin/bash

ROOT_DIR=$(pwd)
CLIENT_DIR="$ROOT_DIR/Client"
JAR_DIR="$CLIENT_DIR/target"	
JAR_CLIENT_SUFFIX="Client-1.0-SNAPSHOT-jar-with-dependencies.jar"
JAR_LOCATION="$JAR_DIR/$JAR_CLIENT_SUFFIX"
COMMON_DIR="$ROOT_DIR/Server/Common/pom.xml"

mvn -f $COMMON_DIR clean install

mvn -f "$CLIENT_DIR" clean compile assembly:single

java -jar $JAR_LOCATION

#!/bin/bash

ROOT_DIR=$(pwd)
CLIENT_DIR="$ROOT_DIR/Client"
JAR_DIR="$CLIENT_DIR/target"	
JAR_CLIENT_SUFFIX="Client-1.0-SNAPSHOT-jar-with-dependencies.jar"
JAR_LOCATION="$JAR_DIR/$JAR_CLIENT_SUFFIX"

mvn -f "$CLIENT_DIR" clean compile assembly:single
cp java.policy "$JAR_DIR"


java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:$JAR_LOCATION -jar $JAR_LOCATION

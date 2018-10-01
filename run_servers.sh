#!/bin/bash

#usage sudo ./run_servers.sh [McGill CS username] [McGill CS password] 
#prereq: need to have sshpass installed

#username and password
username=$1
password=$2

#constants
POM_FILE_NAME="pom.xml"

#servers
COMMON_SERVER="mimi.cs.mcgill.ca"
CARS_SERVER="lab2-17.cs.mcgill.ca"
FLIGHTS_SERVER="lab2-11.cs.mcgill.ca"
ROOMS_SERVER="lab2-35.cs.mcgill.ca"
MIDDLEWARE_SERVER="lab2-37.cs.mcgill.ca"
ENTITIES_SERVERS="$CARS_SERVER $FLIGHTS_SERVER $ROOMS_SERVER $MIDDLEWARE_SERVER"

#declare entities
CARS="Cars"
FLIGHTS="Flights"
ROOMS="Rooms"
MIDDLEWARE="Middleware"

#declare directories
ROOT_DIR=$(pwd)
COMMON_DIR="$ROOT_DIR/Server/Common/"
CAR_DIR="$ROOT_DIR/Server/$CARS/"
FLIGHTS_DIR="$ROOT_DIR/Server/$FLIGHTS/"
ROOMS_DIR="$ROOT_DIR/Server/$ROOMS/"
MIDDLEWARE_DIR="$ROOT_DIR/Server/$MIDDLEWARE/"
MVN_DIRS="$CAR_DIR $FLIGHTS_DIR $ROOMS_DIR $MIDDLEWARE_DIR"

#declare jar file names
JAR_COMMON_SUFFIX="-1.0-SNAPSHOT-jar-with-dependencies.jar"
JAR_LOCATION="target/"
JAR_CARS="$CARS$JAR_COMMON_SUFFIX"
JAR_FLIGHTS="Flight$JAR_COMMON_SUFFIX"
JAR_ROOMS="$ROOMS$JAR_COMMON_SUFFIX"
JAR_MIDDLEWARE="$MIDDLEWARE$JAR_COMMON_SUFFIX"
CAR_JAR_LOCATION="$CAR_DIR$JAR_LOCATION$JAR_CARS"
FLIGHTS_JAR_LOCATION="$FLIGHTS_DIR$JAR_LOCATION$JAR_FLIGHTS"
ROOMS_JAR_LOCATION="$ROOMS_DIR$JAR_LOCATION$JAR_ROOMS"
MIDDLEWARE_JAR_LOCATION="$MIDDLEWARE_DIR$JAR_LOCATION$JAR_MIDDLEWARE"
JAR_DIRS="$CAR_JAR_LOCATION $FLIGHTS_JAR_LOCATION $ROOMS_JAR_LOCATION $MIDDLEWARE_JAR_LOCATION"

#commands
REGISTRY_COMMAND="rmiregistry -J-Djava.rmi.server.useCodebaseOnly=false"
CARS_COMMAND="java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:./$JAR_CARS -jar $JAR_CARS &"
FLIGHTS_COMMAND="java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:./$JAR_FLIGHTS -jar $JAR_FLIGHTS &"
ROOMS_COMMAND="java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:./$JAR_ROOMS -jar $JAR_ROOMS &"
MIDDLEWARE_COMMAND="java -Djava.security.policy=java.policy -Djava.rmi.server.codebase=file:./$JAR_MIDDLEWARE -jar $JAR_MIDDLEWARE &"

#ports
REGULAR_PORT="1099"
MIDDLEWARE_CLIENT_PORT="1088"

echo COMPILATION STAGE 

mvn clean install

echo COPYING FILES TO MCGILL SERVER

for JAR_DIR  in ${JAR_DIRS}; do
	# copy the files into the mcgill servers
	echo "Copying $JAR_DIR to $username@$COMMON_SERVER:~..."
	sshpass -p $password scp -o StrictHostKeyChecking=no $JAR_DIR "$username@$COMMON_SERVER:~"
done
	

echo GENERATE POLICY FILE 
echo "grant {" > $ROOT_DIR/java.policy
echo "	permission java.security.AllPermission;" >> $ROOT_DIR/java.policy
echo "};" >> $ROOT_DIR/java.policy

#copy policy file to server
sshpass -p $password scp -o StrictHostKeyChecking=no "$ROOT_DIR/java.policy" "$username@$COMMON_SERVER:~"

#remove policy file from local 
#rm -rf "$ROOT_DIR/java.policy"


#echo INITIATE REGISTRIES FOR EVERY SERVER 
#for ENTITY_SERVER in ${ENTITIES_SERVERS}; do
#	sshpass -p $password ssh -o StrictHostKeyChecking=no "$username@$ENTITY_SERVER" "$REGISTRY_COMMAND $REGULAR_PORT &"
#done

#initiate exclusive registry on another port for middleware server
sshpass -p $password ssh -o StrictHostKeyChecking=no "$username@$MIDDLEWARE_SERVER" "$REGISTRY_COMMAND $MIDDLEWARE_PORT &"

#run the different servers on different machines

echo RUNNING CARS SERVER
echo $CARS_COMMAND | sshpass -p $password ssh -o StrictHostKeyChecking=no "$username@$CARS_SERVER" "cat > ~/run_cars.sh; chmod 777 ~/run_cars.sh; nohup ./run_cars.sh > /dev/null 2>&1 &"

echo RUNNING FLIGHTS SERVER
echo $FLIGHTS_COMMAND | sshpass -p $password ssh -o StrictHostKeyChecking=no "$username@$FLIGHTS_SERVER" "cat > ~/run_flights.sh; chmod 777 ~/run_flights.sh; nohup ./run_flights.sh > /dev/null 2>&1 &"

echo RUNNING ROOMS SERVER
echo $ROOMS_COMMAND | sshpass -p $password ssh -o StrictHostKeyChecking=no "$username@$ROOMS_SERVER" "cat > ~/run_rooms.sh; chmod 777 ~/run_rooms.sh; nohup ./run_rooms.sh > /dev/null 2>&1 &"

echo RUNNING MIDDLEWARE SERVER
echo $MIDDLEWARE_COMMAND | sshpass -p $password ssh -o StrictHostKeyChecking=no "$username@$MIDDLEWARE_SERVER" "cat > ~/run_middleware.sh; chmod 777 ~/run_middleware.sh; nohup ./run_middleware.sh > /dev/null 2>&1 &"






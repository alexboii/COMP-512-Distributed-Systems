all: java.policy compile-client

java.policy: FORCE
	@echo "Creating client java policy"
	@echo "grant codebase \"file:`pwd`/\" {" > java.policy
	@echo "permission java.security.AllPermission;" >> java.policy
	@echo "};" >> java.policy

FORCE: ;

../Server/RMIInterface.jar: ../Server/Common/RM/IResourceManager.java
	make -C ../Server/RMIInterface.jar

compile-client: ../Server/RMIInterface.jar
	javac -cp ../Server/RMIInterface.jar client.Client/*.java

clean:
	rm -f client.Client/*.class
	rm -f ../Server/RMIInterface.jar
	rm -f java.policy

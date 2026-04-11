cd "$(dirname "$0")"
java -jar \
-Xms256m \
-Xmx1024m \
-XX:MaxMetaspaceSize=256m \
-XX:CompressedClassSpaceSize=128m \
-Dlogging.config=config/logback-spring.xml \
-Dspring.config.location=config/application-prod.properties \
syslog.jar
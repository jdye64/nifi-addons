# nifi-jmx-demo

##Running

```java -jar -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.port=5000 ./target/nifi-jmx-demo-1.0-SNAPSHOT.jar server application.yml```
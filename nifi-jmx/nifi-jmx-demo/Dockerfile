FROM java:8-jre
MAINTAINER Jeremy Dyer

ENV JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64

# web port
EXPOSE 9000

ADD ./target/nifi-jmx-demo-1.0-SNAPSHOT.jar .
ADD ./application.yml .
RUN chmod +x ./nifi-jmx-demo-1.0-SNAPSHOT.jar
ENTRYPOINT ["java", "-jar", "-Dcom.sun.management.jmxremote", "-Dcom.sun.management.jmxremote.ssl=false", "-Dcom.sun.management.jmxremote.authenticate=false", "-Dcom.sun.management.jmxremote.port=5000", "./nifi-jmx-demo-1.0-SNAPSHOT.jar", "server", "application.yml"]
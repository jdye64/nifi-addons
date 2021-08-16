FROM ubuntu:14.04

ENV JAVA_HOME=/usr/lib/jvm/java-8-oracle

# Update apt-get repository
RUN apt-get update

# Install system dependencies
RUN apt-get install -y unzip ant wget curl

# Install Java
RUN echo debconf shared/accepted-oracle-license-v1-1 select true | debconf-set-selections && echo debconf shared/accepted-oracle-license-v1-1 seen true | debconf-set-selections && apt-get install -y software-properties-common && add-apt-repository ppa:webupd8team/java -y && apt-get update && apt-get install -y oracle-java8-installer && apt-get install -y oracle-java8-set-default

# Install HDF 1.2
RUN wget http://public-repo-1.hortonworks.com/HDF/centos6/1.x/updates/1.2.0.0/HDF-1.2.0.0-91.tar.gz && tar -xvf HDF-1.2.0.0-91.tar.gz

# Install Maven
ADD apache-maven-3.3.9-bin.tar.gz /
RUN cp -R apache-maven-3.3.9 /usr/local
RUN ln -s /usr/local/apache-maven-3.3.9/bin/mvn /usr/bin/mvn 
RUN mvn -version

# Sphinx - Add the processor code to the image
RUN mkdir nifi-sphinx
ADD . /nifi-sphinx
RUN cd nifi-sphinx && mvn clean install package -DskipTests && cp ./nifi-sphinx-nar/target/nifi-sphinx-nar-0.5.1.nar /HDF-1.2.0.0/nifi/lib/.

# Add the test Audio file
RUN mkdir /audio
RUN cp /nifi-sphinx/nifi-sphinx-processors/src/test/resources/audio/test.wav /audio/test.wav

# Expose the needed ports
EXPOSE 8080

# Startup NiFi
CMD /HDF-1.2.0.0/nifi/bin/nifi.sh run
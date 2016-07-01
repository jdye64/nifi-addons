FROM ubuntu:14.04

# Install Tesseract dependencies and the English language support
RUN apt-get update && apt-get install -y tesseract-ocr && apt-get install -y tesseract-ocr-eng

# Install System components needed
RUN apt-get install -y curl && apt-get install -y wget

# Install HDF 1.2
RUN wget http://public-repo-1.hortonworks.com/HDF/centos6/1.x/updates/1.2.0.0/HDF-1.2.0.0-91.tar.gz && tar -xvf HDF-1.2.0.0-91.tar.gz

# Expose the needed ports
EXPOSE 8080

# Install Java
RUN echo debconf shared/accepted-oracle-license-v1-1 select true | debconf-set-selections && echo debconf shared/accepted-oracle-license-v1-1 seen true | debconf-set-selections && apt-get install -y software-properties-common && add-apt-repository ppa:webupd8team/java -y && apt-get update && apt-get install -y oracle-java8-installer && apt-get install -y oracle-java8-set-default

# Install Maven
ADD apache-maven-3.3.9-bin.tar.gz /
RUN cp -R apache-maven-3.3.9 /usr/local
RUN ln -s /usr/local/apache-maven-3.3.9/bin/mvn /usr/bin/mvn 
RUN mvn -version

# Add the processor code to the image
RUN mkdir nifi-tesseract
ADD . /nifi-tesseract
RUN cd nifi-tesseract && mvn clean install package  -DskipTests && cp ./nifi-tesseract-nar/target/nifi-tesseract-nar-0.5.1.nar /HDF-1.2.0.0/nifi/lib/.

# Add the QuickBrownFox image to the Docker image
RUN mkdir /images
ADD nifi-tesseract-processors/src/test/resources/images/QuickBrownFox.jpg /images
ADD nifi-tesseract-processors/src/test/resources/images/tesseract-test.jpg /images

# Startup NiFi
CMD /HDF-1.2.0.0/nifi/bin/nifi.sh run
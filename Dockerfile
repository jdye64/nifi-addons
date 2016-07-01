FROM ubuntu:15.10

ENV JAVA_HOME=/usr/lib/jvm/java-8-oracle
ENV LD_LIBRARY_PATH = $LD_LIBRARY_PATH:/opencv-3.1.0/build/lib/

# Update apt-get repository
RUN apt-get update

# Install system dependencies
RUN apt-get install -y unzip ant wget curl

# Install Tesseract dependencies and the English language support
RUN apt-get update && apt-get install -y tesseract-ocr && apt-get install -y tesseract-ocr-eng

# OpenCV System compiler dependencies
RUN apt-get install -y build-essential

# OpenCV required dependencies
RUN apt-get install -y cmake git libgtk2.0-dev pkg-config libavcodec-dev libavformat-dev libswscale-dev curl wget

# OpenCV optional dependencies
RUN apt-get install -y python-dev python-numpy libtbb2 libtbb-dev libjpeg-dev libpng-dev libtiff-dev libjasper-dev libdc1394-22-dev

# Download the OpenCV source code
RUN wget https://github.com/Itseez/opencv/archive/3.1.0.zip

# Install Java
RUN echo debconf shared/accepted-oracle-license-v1-1 select true | debconf-set-selections && echo debconf shared/accepted-oracle-license-v1-1 seen true | debconf-set-selections && apt-get install -y software-properties-common && add-apt-repository ppa:webupd8team/java -y && apt-get update && apt-get install -y oracle-java8-installer && apt-get install -y oracle-java8-set-default

RUN unzip 3.1.0.zip
RUN cd opencv-3.1.0 && mkdir build && cd build && cmake -D CMAKE_BUILD_TYPE=Release -D CMAKE_INSTALL_PREFIX=/usr/local -DBUILD_SHARED_LIBS=OFF .. && make -j8

# Install HDF 1.2
RUN wget http://public-repo-1.hortonworks.com/HDF/centos6/1.x/updates/1.2.0.0/HDF-1.2.0.0-91.tar.gz && tar -xvf HDF-1.2.0.0-91.tar.gz

# Install Maven
ADD apache-maven-3.3.9-bin.tar.gz /
RUN cp -R apache-maven-3.3.9 /usr/local
RUN ln -s /usr/local/apache-maven-3.3.9/bin/mvn /usr/bin/mvn 
RUN mvn -version

# Spinx - Add the processor code to the image
RUN mkdir nifi-spinx
ADD . /nifi-spinx
RUN cd nifi-spinx && mvn clean install package -DskipTests && cp ./nifi-spinx-nar/target/nifi-spinx-nar-0.5.1.nar /HDF-1.2.0.0/nifi/lib/.

# Add the test Audio file
RUN mkdir /audio
RUN cp /nifi-spinx/nifi-spinx-processors/src/test/resources/audio/test.wav /audio/test.wav

# OpenCV - Add the processor code to the image
RUN mkdir nifi-opencv
ADD . /nifi-opencv
RUN cd nifi-opencv && mvn clean install package -DskipTests && cp ./nifi-opencv-nar/target/nifi-opencv-nar-0.5.1.nar /HDF-1.2.0.0/nifi/lib/.

# Tesseract - Add the processor code to the image
RUN mkdir nifi-tesseract
ADD . /nifi-tesseract
RUN cd nifi-tesseract && mvn clean install package -DskipTests && cp ./nifi-tesseract-nar/target/nifi-tesseract-nar-0.5.1.nar /HDF-1.2.0.0/nifi/lib/.

# Add the QuickBrownFox image to the Docker image
RUN mkdir /images
ADD nifi-tesseract-processors/src/test/resources/images/QuickBrownFox.jpg /images
ADD nifi-tesseract-processors/src/test/resources/images/tesseract-test.jpg /images

# Expose the needed ports
EXPOSE 8080

# Startup NiFi
CMD /HDF-1.2.0.0/nifi/bin/nifi.sh run
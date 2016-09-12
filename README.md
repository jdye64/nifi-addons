#NiFi Addons

## Motivation/Introduction
Apache NiFi is an extremely flexible framework for data in motion. NiFi strives to provide the ability to interact 
with several external systems to seamless move data around in the modern data center. During this journey to make the
ultimate data in motion tool focus has (wisely) be centered around creating processors that are generic enough to handle
 several different use cases. For example instead of interfacing with a specific REST API (Nest for example) NiFi provides
 a set of processors that allows the end user to develop those workflows which handles the Nest API integration. While 
 this approach is certainly powerful/flexible I would like to help make things as straightforward as possible to provide
 a seamless integration with these sort of services without a deep understanding of NiFi or the external services and more 
 importantly understanding how to make them integrate with one another. Also certain processors like OpenCV for example
  require system dependencies that can make install and running a little daunting for certain users. This repository
  aims to alleviate those issues by providing pre-baked Dockerfile(s) (and public Docker Images) that allow users to quickly
  use those features without the need for a more complex setup. That in a nutshell is the motivation for this repository.
  
## Versioning
This projects versioning will attempt to align with Apache NiFi as closely as possible. The idea will be that each new
release of Apache NiFi will correlate to a new branch/tag/release within this project. Subsequently a new Docker image will be created
containing the version format of ```jdye64/nifi-addons:{ADDONS_BUILD_VERSION}```. 
  
## Docker
In an effort to make using these NiFi addons as easy as possible this project can be ran as a Docker container. Since the
project contains several system dependencies such as OpenCV and Tesseract I highly encourage you to use this image and 
save yourself the time of building it all out on your own. The Docker images are hosted on Docker Hub and can be ran using the 
commands below.

| NiFi Version        | HDF Version           | Docker Command  |
| :-------------: |:-------------:| :-----:|
| 0.6.1 | HDF 1.2 | ```docker run -d -p 8080:8080 jdye64/nifi-addons:0.6.1``` |
| 0.7.0 | NA      | ```docker run -d -p 8080:8080 jdye64/nifi-addons:0.7.0``` |
| 1.0.0 | NA | ```docker run -d -p 8080:8080 jdye64/nifi-addons:1.0.0``` |
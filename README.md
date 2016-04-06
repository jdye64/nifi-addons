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
  
## Docker


```~/Desktop/nifi/HDF/nifi-0.5.1.1.1.2.0-32/bin/nifi.sh stop && mvn clean install package && cp ./nifi-file/nifi-file-nar/target/nifi-file-nar-0.5.1.nar ~/Desktop/nifi/HDF/nifi-0.5.1.1.1.2.0-32/lib/. && cp ./nifi-salesforce/nifi-salesforce-nar/target/nifi-salesforce-nar-0.5.1.nar ~/Desktop/nifi/HDF/nifi-0.5.1.1.1.2.0-32/lib/. && cp ./nifi-addon-services/nifi-salesforce-service/nifi-salesforce.service-api-nar/target/nifi-salesforce.service-api-nar-0.5.1.nar ~/Desktop/nifi/HDF/nifi-0.5.1.1.1.2.0-32/lib/. && cp ./nifi-addon-services/nifi-salesforce-service/nifi-salesforce.service-nar/target/nifi-salesforce.service-nar-0.5.1.nar ~/Desktop/nifi/HDF/nifi-0.5.1.1.1.2.0-32/lib/. && ~/Desktop/nifi/HDF/nifi-0.5.1.1.1.2.0-32/bin/nifi.sh start```

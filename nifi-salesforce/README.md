# Apache NiFi Salesforce Processors

## Overview
The goal of this project is to provide a flexible integration point with Salesforce.com. A large number of companies use Salesforce to track trends within their business. While Salesforce is a fantastic tool there are limitations and expenses that occur from using the hosted solutions. Companies can often benefit from pulling that data down into a local Hadoop environment where more detailed analysis of that data can be performed. 

Since Salesforce operates on units they refer to as "SObjects" that naming convention will be present in most of these processors as well. The first pass at this project will focus mostly on pulling SObjects from Salesforce.com. As the project progresses the plan is to add things like updating objects, deleting objects, and streaming realtime Salesforce.com changes into the NiFi instance for downstream analysis.

## Salesforce.com Required Setup
In order to use these processors administrators much first ensure that their Salesforce.com accounts are properly setup to allow access to this project. The first step is to create a connected application. Instructions for setting up a connected application can be found at https://help.salesforce.com/apex/HTViewHelpDoc?id=connected_app_create.htm

## Installation
```nifi-salesforce``` has a runtime and compile time dependency on the ```nifi-salesforce-service```. The ```nifi-salesforce-service``` must first be compiled and present in your local Maven repository before this project will even compile successfully. ```nifi-salesforce-service``` can be located at https://github.com/jdye64/nifi-addons/tree/master/Services/nifi-salesforce-service and can be compiled and placed in your local Maven repository by running ```mvn clean install package -DskipTests```. After ```nifi-salesforce-service``` has successfully compiled you will want to place the resulting ```NIFI-ADDONS-HOME/Services/nifi-salesforce-service/nifi-salesforce-api-nar/target/nifi-salesforce-api-nar-0.7.0.nar``` into your ```NIFI_HOME/lib``` directory of the NiFi instance you plan to run this service on. Currently this project has only been validated against Apache NiFi 0.7.0.

Once the Controller Service has been built and installed in your NiFi lib directory you can continue with building the processors located here. This project can be built by running ```mvn clean install package -DskipTests``` and then copying the resulting nar from ```NIFI-ADDONS-HOME/Processors/nifi-salesforce/nifi-salesforce-nar/target/nifi-salesforce-nar-0.7.0.nar``` to ```NIFI_HOME/lib``` directory.

Once all of the nars have been built you can start NiFi as you normally would and the Salesforce controllers and processors will be available for your use.

## Capabilities
The below chart lists the processors contained in this project and their capability. Each processor has a dependency on the ```SalesforceUserPassAuthenticationService``` Controller service which maintains the authentication for the processors to the Salesforce.com REST API.

| Salesforce Processor Name | Purpose | Salesforce Documentation Link |
| :-------: |:--------------------:| :-------------:|
| DescribeGlobalProcessor | Lists all SObject availale in the context of the current Salesforce.com authentication session context. | https://developer.salesforce.com/docs/atlas.en-us.api_rest.meta/api_rest/resources_describeGlobal.htm
| SalesforceOrganizationLimitsProcessor | Lists information about limits in your organization. This resource is available in REST API version 29.0 and later for API users with the "View Setup and Configuration" permission. The resource returns these limits: | https://developer.salesforce.com/docs/atlas.en-us.api_rest.meta/api_rest/resources_limits.htm
| SalesforceQueryProcessor | Executes the specified SOQL query | https://developer.salesforce.com/docs/atlas.en-us.api_rest.meta/api_rest/resources_query.htm
| SObjectBasicInfoProcessor | Describes the individual metadata for the specified object | https://developer.salesforce.com/docs/atlas.en-us.api_rest.meta/api_rest/resources_sobject_basic_info.htm
| SObjectDescribeProcessor | Completely describes the individual metadata at all levels for the specified object. For example, this can be used to retrieve the fields, URLs, and child relationships for the Account object | https://developer.salesforce.com/docs/atlas.en-us.api_rest.meta/api_rest/resources_sobject_describe.htm
| SObjectGetDeletedProcessor | Retrieves the list of individual records that have been deleted within the given timespan for the specified object. SObject Get Deleted is available in API version 29.0 and later | https://developer.salesforce.com/docs/atlas.en-us.api_rest.meta/api_rest/resources_getdeleted.htm
| SObjectGetUpdatedProcessor | Retrieves the list of individual records that have been updated (added or changed) within the given timespan for the specified object. SObject Get Updated is available in API version 29.0 and later | https://developer.salesforce.com/docs/atlas.en-us.api_rest.meta/api_rest/resources_getupdated.htm
| SObjectRowsProcessor | Accesses records based on the specified object ID. Retrieves, updates, or deletes records. This resource can also be used to retrieve field values | https://developer.salesforce.com/docs/atlas.en-us.api_rest.meta/api_rest/resources_sobject_retrieve.htm
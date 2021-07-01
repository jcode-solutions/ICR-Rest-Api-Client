# infodebit-rest-client

Infodebit API
- API version: 1.0
    - Build date: 2021-06-28T21:39:15.682-08:00[GMT-08:00]


# Description
## Versioning: Guidelines and Formats
* This API should never be released without a version number.
* Minor changes to the API will be backwards compatible and will increase the minor version number.

RESTful URIs - General Guidelines
----------------------

## HTTP Verbs

APIs supports the full range of CRUD operations. The following table shows our implementation of CRUD via REST:

| Action      | HTTP Verb   | Context         |
 | ----------- | ----------- | --------------- |
| Create | POST      | Collection          |
| Read     | GET, HEAD | Collection/Document |
| Update | PATCH     | Document            |
| Replace   | PUT     | Document            |
| Delete | DELETE | Collection/Document |

Use PUT when you want to modify a singular resource which is already a part of resources collection. PUT replaces the resource in its entirety. Use PATCH if request updates part of the resource.

## Responses

### The Response Body

The Response Body should be compatible with but not part of the HAL Standard:

data - required - not part of HAL - data specific to the resource in question is grouped under the 'data' element to make it clear what is related to the resource.
_metadata - required - includes various elements that describe the data included in the parent object including but not limited to sorting, filtering, pagination data and field exclusion.

#### data

The data for every resource falls within the data element.
Required Fields:
* id - (required) yep - this is the only required field - this is an id that uniquely identifies this resource vs other resources of the same type.

#### _metadata

Borrowing from HAL style syntax this standard adopts the _metadata object as a means of isolating metadata from other response elements. Any object may have \"_metadata\".  _metadata at the root of the response object refers to the requested resource, while _metadata found within an _embedded object for example will refer to that object.

Example uses are to standardize resource responses including the means to page, filter, and sort resources consistently (more on this later) are of this form:

\"_metadata\": {
\"status\"     : \"200\",
\"created_at\" : \"1994-11-05T13:15:30Z\",
\"updated_at\" : \"1994-11-05T13:15:30Z\",

\"pagination\" : {
\"page\" : 1,
\"total_pages\" : 12,
\"size\" : 5,
\"offset\" : 0
},
\"sort\" : [(\"lastname\", \"asc\")],
\"where\" : {\"lastname\": \"Doe\"},
\"fields\" : { \"includes\" : [\"fullname\"], \"excludes\" : [\"birthdate\"] },
\"messages\" : {
},
},s
The metadata properties include:
* status     : (required) the HTTP status code of the response - here to allow for consistency in client logic when handling bulk operations.
* created_at : (optional) the timestamp at which the resource was created.
* updated_at : (required for modifiable objects) the timestamp at which the resource was most recently updated.

### Response Codes

Refer to the full list of [HTTP Status Codes](http://www.restapitutorial.com/httpstatuscodes.html)

* 200 OK - General status code. Most common code used to indicate success.
* 201 CREATED - Successful creation occurred (via either POST or PUT). Set the Location header to contain a link to the newly-created resource (on POST). Response body content may or may not be present.
* 204 NO CONTENT - Indicates success but nothing is in the response body, often used for DELETE and UPDATE operations.
* 400 BAD REQUEST - General error when fulfilling the request would cause an invalid state. Domain validation errors, missing data, etc. are some examples.
* 401 UNAUTHORIZED - Error code response for missing or invalid authentication token.
* 403 FORBIDDEN - Error code for user not authorized to perform the operation or the resource is unavailable for some reason (e.g. time constraints, etc.).
* 404 NOT FOUND- Used when the requested resource is not found, whether it doesn't exist or if there was a 401 or 403 that, for security reasons, the service wants to mask.
* 405 METHOD NOT ALLOWED - Used to indicate that the requested URI exists, but the requested HTTP method is not applicable. For example, POST /users/12345 where the API doesn't support creation of resources this way (with a provided ID). The Allow HTTP header must be set when returning a 405 to indicate the HTTP methods that are supported. In the previous case, the header would look like \"Allow: GET, PUT, DELETE\"
* 409 CONFLICT - Whenever a resource conflict would be caused by fulfilling the request. Duplicate entries, such as trying to create two customers with the same information, and deleting root objects when cascade-delete is not supported are a couple of examples.
* 500 INTERNAL SERVER ERROR - Never return this intentionally. The general catch-all error when the server-side throws an exception. Use this only for errors that the consumer cannot address from their end.

## API Discovery and Documentation

In order to ensure that our APIs are consistently documented, easily understood and used, in a human readable form of documentation we will use OPEN-API compliant.

## JSON Schema

OPEN-API does allow definition of models and the schema shall be provided to the client in two ways:

* for humans
* a link on the api definition page: {hostname}/documentation
* for machines
* by way of the 'response-schema-uri' and the 'data-schema-uri' in the _metadata element in every response.
* the overall structure of the api/response: {hostname}/restapi/{api-version}/{open-api-version}/api-docs.json

This will give to clients a formal definition of how requests and responses need to be structured. These formal definitions can be used for general validation routines both in production and during QA activities.

## Filtering, Sorting, Paging and more...

All of these concepts are handled by way of query parameters.

### Filtering (where)

Resource endpoints returning multiple results may require greater sophistication.  For these endpoints filtering may be supported on any given resource.  If it is the API should use a subset of the [mongo query syntax](http://docs.mongodb.org/manual/tutorial/query-documents/) within a 'where' parameter:
Example: https://{hostname}/restapi/v2/documents?where={\"lastname\":\"Doe\"}

Note: the where clauses are shown un-encoded for clarity - here's what they'd really look like encoded:

$ curl -i -g https://{hostname}/restapi/v2/documents?where={%22lastname%22:%20%22Doe%22}
HTTP/1.1 200 OK

Filters may be enabled or disabled on certain fields as necessary to prevent denial of service attacks on non-indexed fields, etc.  The subset of the syntax supported may be determined by the needs of the API.


### Sorting (sort)

Sorting is supported as well:

https://{hostname}/restapi/v2/documents?sort=[(\"lastname\",\"asc\")]

and descending...

https://{hostname}/restapi/v2/documents?sort=[(\"lastname\",\"desc\")]

If directional (asc, desc) flag is not supplied the list is sorted ascending. Multiple fields may of course be specified in the array.

If sorting is not specified the sort order may either be
* returned unsorted
* or returned using a default sort order determined by the server and if so will include the 'sort' field(s) within the metadata element.

### Pagination

We will support just Offset Based Pagination

#### Offset Based Pagination

Offset based pagination allows paging by specifying 'offset' and 'count' query parameters.

* If no count is specified, return results with a default(500) number of records. Default number of records should be secified for every resource.
* If no offset is specified, return results will start with offset=0

* To get records 51 through 75 do this:

https://{hostname}/restapi/v2/documents?offset=50&count=25

Above, offset=50 means, \"skip the first 50 records\" and size=25 means, \"return a maximum of 25 records\".

#### Pagination and the Response

If pagination is performed the _metadata element must supply pagination details as follows:

\"pagination\" : {
\"offset\" : 0,
\"count\" : 3,
\"size\" : 25
},

### Including and Excluding Fields

Minimizing the number of fields generated in the response sometimes desirable as an optimization for rich datasets. The server side of a RESTful resource may choose to (by default) exclude fields that are expensive to generate, render or calculate.
Likewise clients may choose to specify the fields they intend to use to cut down on bandwidth consumption, to include a value that is excluded by default or to make them future proof to changes in the API.

If supported by an API it is specified using by using a comma separated set of values in the 'fields' parameter which is of this form:

\"fields\" : { \"includes\" : [\"type\"], \"excludes\" : [\"articles\"] }

* includes - a list of fields that should be included in the response.
* excludes - a list of fields that should be excluded from the response.

If fields are specified _and used_ by the implementation they must be listed in a 'fields' object enclosed within the metadata object.

Here's an example: /api/magazines/1234?fields={\"includes\":[\"type\"],\"excludes\":[\"articles\"]}

Response body:

{
\"id\": \"1234\",
\"type\": \"magazine\",
\"title\": \"Public Water Systems\",

_metadata {
\"status\"     : \"200\",
\"created_at\" : \"1994-11-05T13:15:30Z\",
\"updated_at\" : \"1994-11-05T13:15:30Z\",
\"fields\" : { \"includes\" : [\"type\"], \"excludes\" : [\"articles\"] }
}
}

## Versioning: Guidelines and Formats

* API should never be released without a version number.
* Minor changes to the API should be backwards compatible and should increase the minor version number.  Examples of minor changes include:
* Fields added to JSON objects.
* New links added to responses.
* Fields deprecated.
* Breaking changes should be avoided if possible but when necessary the major version number must be updated. Generally major changes are structural and include:
* Fields removed from JSON objects (either permanently or excluded by default).
* Fields renamed in JSON objects.
* Links removed from responses.
* If the client requests a specific version the API must respond with the version requested if it is available. If it is not available it should respond with the closest available API version.
* If no version is specified the server should respond with an error detailing the problem and (as with all errors) how to correct the problem (i.e. where to find valid versions).
* API should maintain at least one major version back, but maintaining previous minor versions is not required.

## Error handling

Error responses should include a common HTTP status code, message for the developer, message for the end-user (when appropriate), internal error code (corresponding to some specific internally determined ID), links where developers can find more info.

All of this information is provided using the [vnd.error](https://github.com/blongden/vnd.error) Media Type.  Specifically the API must support the json variant of the media type: 'application/vnd.error+json'

Errors json may include non-conflicting properties beyond what are specified in vnd.error. This is especially useful for common recoverable application specific errors.

##  Data Types with the payload

### Dates and Times should be Timezone independent.

In order to comply with ISO 8601 as recommended by W3C dates and times are expected to allow for timezone independence using [W3C's Date and Time Formats](http://www.w3.org/TR/NOTE-datetime):

1994-11-05T08:15:30-05:00 corresponds to November 5, 1994, 8:15:30 am, US Eastern Standard Time.
1994-11-05T13:15:30Z corresponds to the same instant.



*Automatically generated by the [OpenAPI Generator](https://openapi-generator.tech)*


## Requirements

Building the API client library requires:
1. Java 1.7+
2. Maven/Gradle

## Installation

To install the API client library to your local Maven repository, simply execute:

```shell
mvn clean install
```

To deploy it to a remote Maven repository instead, configure the settings of the repository and execute:

```shell
mvn clean deploy
```

Refer to the [OSSRH Guide](http://central.sonatype.org/pages/ossrh-guide.html) for more information.

### Maven users

Add this dependency to your project's POM:

```xml
<dependency>
  <groupId>infodebit.rest.client</groupId>
  <artifactId>infodebit-rest-client</artifactId>
  <version>1.0</version>
  <scope>compile</scope>
</dependency>
```

### Gradle users

Add this dependency to your project's build file:

```groovy
compile "infodebit.rest.client:infodebit-rest-client:1.0"
```

### Others

At first generate the JAR by executing:

```shell
mvn clean package
```

Then manually install the following JARs:

* `target/infodebit-rest-client-1.0.jar`
* `target/lib/*.jar`

## Getting Started

Please follow the [installation](#installation) instruction and execute the following Java code:

```java

// Import classes:
import infodebit.rest.client.ApiClient;
import infodebit.rest.client.ApiException;
import infodebit.rest.client.Configuration;
import infodebit.rest.client.models.*;
import infodebit.rest.client.controllers.AddressApi;

public class Example {
  public static void main(String[] args) {
    ApiClient defaultClient = Configuration.getDefaultApiClient();
    defaultClient.setBasePath("http://31.131.2.24:8083");

    AddressApi apiInstance = new AddressApi(defaultClient);
    Long personId = 56L; // Long | 
    Pageable pageable = new Pageable(); // Pageable | 
    PagedResourcesAssemblerEntityModelAddressDTO pageAssembler = new PagedResourcesAssemblerEntityModelAddressDTO(); // PagedResourcesAssemblerEntityModelAddressDTO | 
    try {
      PagedModelEntityModelEntityModelAddressDTO result = apiInstance.allForPerson(personId, pageable, pageAssembler);
      System.out.println(result);
    } catch (ApiException e) {
      System.err.println("Exception when calling AddressApi#allForPerson");
      System.err.println("Status code: " + e.getCode());
      System.err.println("Reason: " + e.getResponseBody());
      System.err.println("Response headers: " + e.getResponseHeaders());
      e.printStackTrace();
    }
  }
}

```

## Documentation for API Endpoints

All URIs are relative to *http://31.131.2.24:8083*

Class | Method | HTTP request | Description
------------ | ------------- | ------------- | -------------
*AddressApi* | [**allForPerson**](docs/AddressApi.md#allForPerson) | **GET** /persons/{personId}/addresses | Get all persons addresses
*AddressApi* | [**deleteAddress**](docs/AddressApi.md#deleteAddress) | **DELETE** /addresses/{addressId} | Delete an address
*AddressApi* | [**getAddressById**](docs/AddressApi.md#getAddressById) | **GET** /addresses/{addressId} | Get an address by id
*AddressApi* | [**newAddress**](docs/AddressApi.md#newAddress) | **POST** /persons/{personId}/addresses | Add an address
*AddressApi* | [**updateAddress**](docs/AddressApi.md#updateAddress) | **PATCH** /addresses/{addressId} | Update an address
*AuthorizationEndpointApi* | [**authorize**](docs/AuthorizationEndpointApi.md#authorize) | **HEAD** /oauth/authorize |
*AuthorizationEndpointApi* | [**authorize11**](docs/AuthorizationEndpointApi.md#authorize11) | **POST** /oauth/authorize |
*AuthorizationEndpointApi* | [**authorize2**](docs/AuthorizationEndpointApi.md#authorize2) | **GET** /oauth/authorize |
*AuthorizationEndpointApi* | [**authorize3**](docs/AuthorizationEndpointApi.md#authorize3) | **PUT** /oauth/authorize |
*AuthorizationEndpointApi* | [**authorize4**](docs/AuthorizationEndpointApi.md#authorize4) | **DELETE** /oauth/authorize |
*AuthorizationEndpointApi* | [**authorize5**](docs/AuthorizationEndpointApi.md#authorize5) | **OPTIONS** /oauth/authorize |
*AuthorizationEndpointApi* | [**authorize6**](docs/AuthorizationEndpointApi.md#authorize6) | **PATCH** /oauth/authorize |
*CheckTokenEndpointApi* | [**checkToken**](docs/CheckTokenEndpointApi.md#checkToken) | **HEAD** /oauth/check_token |
*CheckTokenEndpointApi* | [**checkToken1**](docs/CheckTokenEndpointApi.md#checkToken1) | **POST** /oauth/check_token |
*CheckTokenEndpointApi* | [**checkToken2**](docs/CheckTokenEndpointApi.md#checkToken2) | **GET** /oauth/check_token |
*CheckTokenEndpointApi* | [**checkToken3**](docs/CheckTokenEndpointApi.md#checkToken3) | **PUT** /oauth/check_token |
*CheckTokenEndpointApi* | [**checkToken4**](docs/CheckTokenEndpointApi.md#checkToken4) | **DELETE** /oauth/check_token |
*CheckTokenEndpointApi* | [**checkToken5**](docs/CheckTokenEndpointApi.md#checkToken5) | **OPTIONS** /oauth/check_token |
*CheckTokenEndpointApi* | [**checkToken6**](docs/CheckTokenEndpointApi.md#checkToken6) | **PATCH** /oauth/check_token |
*DebtsApi* | [**allForDebtor**](docs/DebtsApi.md#allForDebtor) | **GET** /persons/{idnp}/debts | Get all debts of a person
*DebtsApi* | [**deleteDebt**](docs/DebtsApi.md#deleteDebt) | **DELETE** /debts/{id} | Delete a debt
*DebtsApi* | [**getDebtById**](docs/DebtsApi.md#getDebtById) | **GET** /debts/{id} | Get a debt by id
*DebtsApi* | [**newDebt**](docs/DebtsApi.md#newDebt) | **POST** /persons/{idnp}/debts | Add a debt
*DebtsApi* | [**updateDebt**](docs/DebtsApi.md#updateDebt) | **PATCH** /debts/{id} | Update a debt
*PaymentsApi* | [**allForDebt**](docs/PaymentsApi.md#allForDebt) | **GET** /debts/{debtId}/payments | Get all payments for a debt
*PaymentsApi* | [**deleteDebtPayment**](docs/PaymentsApi.md#deleteDebtPayment) | **DELETE** /payments/{paymentId} | Delete a payment
*PaymentsApi* | [**getDebtPaymentById**](docs/PaymentsApi.md#getDebtPaymentById) | **GET** /payments/{paymentId} | Get a payment by id
*PaymentsApi* | [**newDebtPayment**](docs/PaymentsApi.md#newDebtPayment) | **POST** /debts/{debtId}/payments | Add a payment
*PaymentsApi* | [**updateDebtPayment**](docs/PaymentsApi.md#updateDebtPayment) | **PATCH** /payments/{paymentId} | Update a payment
*PersonsApi* | [**allPerson**](docs/PersonsApi.md#allPerson) | **GET** /persons | Get the list of all persons
*PersonsApi* | [**getPersonByIdnp**](docs/PersonsApi.md#getPersonByIdnp) | **GET** /persons/{idnp} | Get a person by idnp
*PersonsApi* | [**newPerson**](docs/PersonsApi.md#newPerson) | **POST** /persons | Add a person
*PersonsApi* | [**updatePerson**](docs/PersonsApi.md#updatePerson) | **PATCH** /persons/{idnp} | Update a person&#39;s information
*TokenEndpointApi* | [**getAccessToken**](docs/TokenEndpointApi.md#getAccessToken) | **GET** /oauth/token |
*TokenEndpointApi* | [**postAccessToken**](docs/TokenEndpointApi.md#postAccessToken) | **POST** /oauth/token |
*UniuneaExecutorilorJudecatorestiApi* | [**getInformationFromUNEJ**](docs/UniuneaExecutorilorJudecatorestiApi.md#getInformationFromUNEJ) | **POST** /unej | Get information from UNEJ
*WhitelabelApprovalEndpointApi* | [**getAccessConfirmation**](docs/WhitelabelApprovalEndpointApi.md#getAccessConfirmation) | **HEAD** /oauth/confirm_access |
*WhitelabelApprovalEndpointApi* | [**getAccessConfirmation1**](docs/WhitelabelApprovalEndpointApi.md#getAccessConfirmation1) | **POST** /oauth/confirm_access |
*WhitelabelApprovalEndpointApi* | [**getAccessConfirmation2**](docs/WhitelabelApprovalEndpointApi.md#getAccessConfirmation2) | **GET** /oauth/confirm_access |
*WhitelabelApprovalEndpointApi* | [**getAccessConfirmation3**](docs/WhitelabelApprovalEndpointApi.md#getAccessConfirmation3) | **PUT** /oauth/confirm_access |
*WhitelabelApprovalEndpointApi* | [**getAccessConfirmation4**](docs/WhitelabelApprovalEndpointApi.md#getAccessConfirmation4) | **DELETE** /oauth/confirm_access |
*WhitelabelApprovalEndpointApi* | [**getAccessConfirmation5**](docs/WhitelabelApprovalEndpointApi.md#getAccessConfirmation5) | **OPTIONS** /oauth/confirm_access |
*WhitelabelApprovalEndpointApi* | [**getAccessConfirmation6**](docs/WhitelabelApprovalEndpointApi.md#getAccessConfirmation6) | **PATCH** /oauth/confirm_access |
*WhitelabelErrorEndpointApi* | [**handleError**](docs/WhitelabelErrorEndpointApi.md#handleError) | **HEAD** /oauth/error |
*WhitelabelErrorEndpointApi* | [**handleError1**](docs/WhitelabelErrorEndpointApi.md#handleError1) | **POST** /oauth/error |
*WhitelabelErrorEndpointApi* | [**handleError2**](docs/WhitelabelErrorEndpointApi.md#handleError2) | **GET** /oauth/error |
*WhitelabelErrorEndpointApi* | [**handleError3**](docs/WhitelabelErrorEndpointApi.md#handleError3) | **PUT** /oauth/error |
*WhitelabelErrorEndpointApi* | [**handleError4**](docs/WhitelabelErrorEndpointApi.md#handleError4) | **DELETE** /oauth/error |
*WhitelabelErrorEndpointApi* | [**handleError5**](docs/WhitelabelErrorEndpointApi.md#handleError5) | **OPTIONS** /oauth/error |
*WhitelabelErrorEndpointApi* | [**handleError6**](docs/WhitelabelErrorEndpointApi.md#handleError6) | **PATCH** /oauth/error |


## Documentation for Models

- [AddressDTO](docs/AddressDTO.md)
- [DebtDTO](docs/DebtDTO.md)
- [DebtPaymentDTO](docs/DebtPaymentDTO.md)
- [EntityModelAddressDTO](docs/EntityModelAddressDTO.md)
- [EntityModelDebtDTO](docs/EntityModelDebtDTO.md)
- [EntityModelDebtPaymentDTO](docs/EntityModelDebtPaymentDTO.md)
- [EntityModelEntityModelAddressDTO](docs/EntityModelEntityModelAddressDTO.md)
- [EntityModelEntityModelDebtDTO](docs/EntityModelEntityModelDebtDTO.md)
- [EntityModelEntityModelDebtPaymentDTO](docs/EntityModelEntityModelDebtPaymentDTO.md)
- [EntityModelEntityModelPersonDTO](docs/EntityModelEntityModelPersonDTO.md)
- [EntityModelPersonDTO](docs/EntityModelPersonDTO.md)
- [ExecutionProcedureDTO](docs/ExecutionProcedureDTO.md)
- [Link](docs/Link.md)
- [ModelAndView](docs/ModelAndView.md)
- [OAuth2AccessToken](docs/OAuth2AccessToken.md)
- [OAuth2RefreshToken](docs/OAuth2RefreshToken.md)
- [Obligation](docs/Obligation.md)
- [PageMetadata](docs/PageMetadata.md)
- [Pageable](docs/Pageable.md)
- [PagedModelEntityModelEntityModelAddressDTO](docs/PagedModelEntityModelEntityModelAddressDTO.md)
- [PagedModelEntityModelEntityModelDebtDTO](docs/PagedModelEntityModelEntityModelDebtDTO.md)
- [PagedModelEntityModelEntityModelDebtPaymentDTO](docs/PagedModelEntityModelEntityModelDebtPaymentDTO.md)
- [PagedModelEntityModelEntityModelPersonDTO](docs/PagedModelEntityModelEntityModelPersonDTO.md)
- [PagedResourcesAssemblerEntityModelAddressDTO](docs/PagedResourcesAssemblerEntityModelAddressDTO.md)
- [PagedResourcesAssemblerEntityModelDebtDTO](docs/PagedResourcesAssemblerEntityModelDebtDTO.md)
- [PagedResourcesAssemblerEntityModelDebtPaymentDTO](docs/PagedResourcesAssemblerEntityModelDebtPaymentDTO.md)
- [PagedResourcesAssemblerEntityModelPersonDTO](docs/PagedResourcesAssemblerEntityModelPersonDTO.md)
- [PersonDTO](docs/PersonDTO.md)
- [Sort](docs/Sort.md)
- [UnejRequest](docs/UnejRequest.md)
- [View](docs/View.md)


## Documentation for Authorization

All endpoints do not require authorization.
Authentication schemes defined for the API:

## Recommendation

It's recommended to create an instance of `ApiClient` per thread in a multithreaded environment to avoid any potential issues.

## Author

info[@]infodebit.md


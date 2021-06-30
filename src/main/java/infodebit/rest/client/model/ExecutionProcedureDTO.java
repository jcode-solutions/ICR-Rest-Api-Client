/*
 * Infodebit API
 *   # Description  ## Versioning: Guidelines and Formats  * This API should never be released without a version number.  * Minor changes to the API will be backwards compatible and will increase the minor version number.    RESTful URIs - General Guidelines  ----------------------    ## HTTP Verbs    APIs supports the full range of CRUD operations. The following table shows our implementation of CRUD via REST:    | Action      | HTTP Verb   | Context         |  | ----------- | ----------- | --------------- |  | Create | POST      | Collection          |  | Read     | GET, HEAD | Collection/Document |  | Update | PATCH     | Document            |  | Replace   | PUT     | Document            |  | Delete | DELETE | Collection/Document |    Use PUT when you want to modify a singular resource which is already a part of resources collection. PUT replaces the resource in its entirety. Use PATCH if request updates part of the resource.    ## Responses    ### The Response Body    The Response Body should be compatible with but not part of the HAL Standard:    data - required - not part of HAL - data specific to the resource in question is grouped under the 'data' element to make it clear what is related to the resource.  _metadata - required - includes various elements that describe the data included in the parent object including but not limited to sorting, filtering, pagination data and field exclusion.    #### data    The data for every resource falls within the data element.  Required Fields:  * id - (required) yep - this is the only required field - this is an id that uniquely identifies this resource vs other resources of the same type.    #### _metadata    Borrowing from HAL style syntax this standard adopts the _metadata object as a means of isolating metadata from other response elements. Any object may have \"_metadata\".  _metadata at the root of the response object refers to the requested resource, while _metadata found within an _embedded object for example will refer to that object.    Example uses are to standardize resource responses including the means to page, filter, and sort resources consistently (more on this later) are of this form:    \"_metadata\": {  \"status\"     : \"200\",  \"created_at\" : \"1994-11-05T13:15:30Z\",  \"updated_at\" : \"1994-11-05T13:15:30Z\",    \"pagination\" : {  \"page\" : 1,  \"total_pages\" : 12,  \"size\" : 5,  \"offset\" : 0  },  \"sort\" : [(\"lastname\", \"asc\")],  \"where\" : {\"lastname\": \"Doe\"},  \"fields\" : { \"includes\" : [\"fullname\"], \"excludes\" : [\"birthdate\"] },  \"messages\" : {  },  },s  The metadata properties include:  * status     : (required) the HTTP status code of the response - here to allow for consistency in client logic when handling bulk operations.  * created_at : (optional) the timestamp at which the resource was created.  * updated_at : (required for modifiable objects) the timestamp at which the resource was most recently updated.    ### Response Codes    Refer to the full list of [HTTP Status Codes](http://www.restapitutorial.com/httpstatuscodes.html)    * 200 OK - General status code. Most common code used to indicate success.  * 201 CREATED - Successful creation occurred (via either POST or PUT). Set the Location header to contain a link to the newly-created resource (on POST). Response body content may or may not be present.  * 204 NO CONTENT - Indicates success but nothing is in the response body, often used for DELETE and UPDATE operations.  * 400 BAD REQUEST - General error when fulfilling the request would cause an invalid state. Domain validation errors, missing data, etc. are some examples.  * 401 UNAUTHORIZED - Error code response for missing or invalid authentication token.  * 403 FORBIDDEN - Error code for user not authorized to perform the operation or the resource is unavailable for some reason (e.g. time constraints, etc.).  * 404 NOT FOUND- Used when the requested resource is not found, whether it doesn't exist or if there was a 401 or 403 that, for security reasons, the service wants to mask.  * 405 METHOD NOT ALLOWED - Used to indicate that the requested URI exists, but the requested HTTP method is not applicable. For example, POST /users/12345 where the API doesn't support creation of resources this way (with a provided ID). The Allow HTTP header must be set when returning a 405 to indicate the HTTP methods that are supported. In the previous case, the header would look like \"Allow: GET, PUT, DELETE\"  * 409 CONFLICT - Whenever a resource conflict would be caused by fulfilling the request. Duplicate entries, such as trying to create two customers with the same information, and deleting root objects when cascade-delete is not supported are a couple of examples.  * 500 INTERNAL SERVER ERROR - Never return this intentionally. The general catch-all error when the server-side throws an exception. Use this only for errors that the consumer cannot address from their end.    ## API Discovery and Documentation    In order to ensure that our APIs are consistently documented, easily understood and used, in a human readable form of documentation we will use OPEN-API compliant.    ## JSON Schema    OPEN-API does allow definition of models and the schema shall be provided to the client in two ways:    * for humans  * a link on the api definition page: {hostname}/documentation  * for machines  * by way of the 'response-schema-uri' and the 'data-schema-uri' in the _metadata element in every response.  * the overall structure of the api/response: {hostname}/restapi/{api-version}/{open-api-version}/api-docs.json    This will give to clients a formal definition of how requests and responses need to be structured. These formal definitions can be used for general validation routines both in production and during QA activities.    ## Filtering, Sorting, Paging and more...    All of these concepts are handled by way of query parameters.    ### Filtering (where)    Resource endpoints returning multiple results may require greater sophistication.  For these endpoints filtering may be supported on any given resource.  If it is the API should use a subset of the [mongo query syntax](http://docs.mongodb.org/manual/tutorial/query-documents/) within a 'where' parameter:  Example: https://{hostname}/restapi/v2/documents?where={\"lastname\":\"Doe\"}    Note: the where clauses are shown un-encoded for clarity - here's what they'd really look like encoded:    $ curl -i -g https://{hostname}/restapi/v2/documents?where={%22lastname%22:%20%22Doe%22}  HTTP/1.1 200 OK    Filters may be enabled or disabled on certain fields as necessary to prevent denial of service attacks on non-indexed fields, etc.  The subset of the syntax supported may be determined by the needs of the API.      ### Sorting (sort)    Sorting is supported as well:    https://{hostname}/restapi/v2/documents?sort=[(\"lastname\",\"asc\")]    and descending...    https://{hostname}/restapi/v2/documents?sort=[(\"lastname\",\"desc\")]    If directional (asc, desc) flag is not supplied the list is sorted ascending. Multiple fields may of course be specified in the array.    If sorting is not specified the sort order may either be  * returned unsorted  * or returned using a default sort order determined by the server and if so will include the 'sort' field(s) within the metadata element.    ### Pagination    We will support just Offset Based Pagination    #### Offset Based Pagination    Offset based pagination allows paging by specifying 'offset' and 'count' query parameters.    * If no count is specified, return results with a default(500) number of records. Default number of records should be secified for every resource.  * If no offset is specified, return results will start with offset=0    * To get records 51 through 75 do this:    https://{hostname}/restapi/v2/documents?offset=50&count=25    Above, offset=50 means, \"skip the first 50 records\" and size=25 means, \"return a maximum of 25 records\".    #### Pagination and the Response    If pagination is performed the _metadata element must supply pagination details as follows:    \"pagination\" : {  \"offset\" : 0,  \"count\" : 3,  \"size\" : 25  },    ### Including and Excluding Fields    Minimizing the number of fields generated in the response sometimes desirable as an optimization for rich datasets. The server side of a RESTful resource may choose to (by default) exclude fields that are expensive to generate, render or calculate.  Likewise clients may choose to specify the fields they intend to use to cut down on bandwidth consumption, to include a value that is excluded by default or to make them future proof to changes in the API.    If supported by an API it is specified using by using a comma separated set of values in the 'fields' parameter which is of this form:    \"fields\" : { \"includes\" : [\"type\"], \"excludes\" : [\"articles\"] }    * includes - a list of fields that should be included in the response.  * excludes - a list of fields that should be excluded from the response.    If fields are specified _and used_ by the implementation they must be listed in a 'fields' object enclosed within the metadata object.    Here's an example: /api/magazines/1234?fields={\"includes\":[\"type\"],\"excludes\":[\"articles\"]}    Response body:    {  \"id\": \"1234\",  \"type\": \"magazine\",  \"title\": \"Public Water Systems\",    _metadata {  \"status\"     : \"200\",  \"created_at\" : \"1994-11-05T13:15:30Z\",  \"updated_at\" : \"1994-11-05T13:15:30Z\",  \"fields\" : { \"includes\" : [\"type\"], \"excludes\" : [\"articles\"] }  }  }    ## Versioning: Guidelines and Formats    * API should never be released without a version number.  * Minor changes to the API should be backwards compatible and should increase the minor version number.  Examples of minor changes include:  * Fields added to JSON objects.  * New links added to responses.  * Fields deprecated.  * Breaking changes should be avoided if possible but when necessary the major version number must be updated. Generally major changes are structural and include:  * Fields removed from JSON objects (either permanently or excluded by default).  * Fields renamed in JSON objects.  * Links removed from responses.  * If the client requests a specific version the API must respond with the version requested if it is available. If it is not available it should respond with the closest available API version.  * If no version is specified the server should respond with an error detailing the problem and (as with all errors) how to correct the problem (i.e. where to find valid versions).  * API should maintain at least one major version back, but maintaining previous minor versions is not required.    ## Error handling    Error responses should include a common HTTP status code, message for the developer, message for the end-user (when appropriate), internal error code (corresponding to some specific internally determined ID), links where developers can find more info.    All of this information is provided using the [vnd.error](https://github.com/blongden/vnd.error) Media Type.  Specifically the API must support the json variant of the media type: 'application/vnd.error+json'    Errors json may include non-conflicting properties beyond what are specified in vnd.error. This is especially useful for common recoverable application specific errors.    ##  Data Types with the payload    ### Dates and Times should be Timezone independent.    In order to comply with ISO 8601 as recommended by W3C dates and times are expected to allow for timezone independence using [W3C's Date and Time Formats](http://www.w3.org/TR/NOTE-datetime):    1994-11-05T08:15:30-05:00 corresponds to November 5, 1994, 8:15:30 am, US Eastern Standard Time.  1994-11-05T13:15:30Z corresponds to the same instant.  
 *
 * The version of the OpenAPI document: 1.0
 * Contact: info[@]infodebit.md
 *
 * NOTE: This class is auto generated by OpenAPI Generator (https://openapi-generator.tech).
 * https://openapi-generator.tech
 * Do not edit the class manually.
 */


package infodebit.rest.client.model;

import com.google.gson.annotations.SerializedName;
import io.swagger.annotations.ApiModelProperty;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * ExecutionProcedureDTO
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2021-06-28T21:39:15.682-08:00[GMT-08:00]")
public class ExecutionProcedureDTO {
  public static final String SERIALIZED_NAME_ID = "id";
  @SerializedName(SERIALIZED_NAME_ID)
  private Long id;

  public static final String SERIALIZED_NAME_SYSTEM_NR = "systemNr";
  @SerializedName(SERIALIZED_NAME_SYSTEM_NR)
  private String systemNr;

  public static final String SERIALIZED_NAME_INITIATION_DATE = "initiationDate";
  @SerializedName(SERIALIZED_NAME_INITIATION_DATE)
  private String initiationDate;

  public static final String SERIALIZED_NAME_EXECUTION_DOCUMENT_NR = "executionDocumentNr";
  @SerializedName(SERIALIZED_NAME_EXECUTION_DOCUMENT_NR)
  private String executionDocumentNr;

  public static final String SERIALIZED_NAME_ISSUER = "issuer";
  @SerializedName(SERIALIZED_NAME_ISSUER)
  private String issuer;

  public static final String SERIALIZED_NAME_DESCRIPTION = "description";
  @SerializedName(SERIALIZED_NAME_DESCRIPTION)
  private String description;

  public static final String SERIALIZED_NAME_DEBTOR_TYPE = "debtorType";
  @SerializedName(SERIALIZED_NAME_DEBTOR_TYPE)
  private String debtorType;

  public static final String SERIALIZED_NAME_DEBTOR_IDENTITY_NR = "debtorIdentityNr";
  @SerializedName(SERIALIZED_NAME_DEBTOR_IDENTITY_NR)
  private String debtorIdentityNr;

  public static final String SERIALIZED_NAME_DEBTOR_NAME = "debtorName";
  @SerializedName(SERIALIZED_NAME_DEBTOR_NAME)
  private String debtorName;

  public static final String SERIALIZED_NAME_DEBTOR_SURNAME = "debtorSurname";
  @SerializedName(SERIALIZED_NAME_DEBTOR_SURNAME)
  private String debtorSurname;

  public static final String SERIALIZED_NAME_DEBTOR_ADDRESS = "debtorAddress";
  @SerializedName(SERIALIZED_NAME_DEBTOR_ADDRESS)
  private String debtorAddress;

  public static final String SERIALIZED_NAME_PROCEDURE_EXECUTOR = "procedureExecutor";
  @SerializedName(SERIALIZED_NAME_PROCEDURE_EXECUTOR)
  private String procedureExecutor;

  public static final String SERIALIZED_NAME_PROCEDURAL_STATUS = "proceduralStatus";
  @SerializedName(SERIALIZED_NAME_PROCEDURAL_STATUS)
  private String proceduralStatus;

  public static final String SERIALIZED_NAME_PROCEDURE_CLASSIFICATION = "procedureClassification";
  @SerializedName(SERIALIZED_NAME_PROCEDURE_CLASSIFICATION)
  private String procedureClassification;

  public static final String SERIALIZED_NAME_OBLIGATIONS = "obligations";
  @SerializedName(SERIALIZED_NAME_OBLIGATIONS)
  private List<Obligation> obligations = null;


  public ExecutionProcedureDTO id(Long id) {
    
    this.id = id;
    return this;
  }

   /**
   * ID dosarului de executare in sistem
   * @return id
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "ID dosarului de executare in sistem")

  public Long getId() {
    return id;
  }


  public void setId(Long id) {
    this.id = id;
  }


  public ExecutionProcedureDTO systemNr(String systemNr) {
    
    this.systemNr = systemNr;
    return this;
  }

   /**
   * Listă parametrilor unei solicitari
   * @return systemNr
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "Listă parametrilor unei solicitari")

  public String getSystemNr() {
    return systemNr;
  }


  public void setSystemNr(String systemNr) {
    this.systemNr = systemNr;
  }


  public ExecutionProcedureDTO initiationDate(String initiationDate) {
    
    this.initiationDate = initiationDate;
    return this;
  }

   /**
   * Data intentare a dosarului
   * @return initiationDate
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "Data intentare a dosarului")

  public String getInitiationDate() {
    return initiationDate;
  }


  public void setInitiationDate(String initiationDate) {
    this.initiationDate = initiationDate;
  }


  public ExecutionProcedureDTO executionDocumentNr(String executionDocumentNr) {
    
    this.executionDocumentNr = executionDocumentNr;
    return this;
  }

   /**
   * Numărul unic al dosarului de executare
   * @return executionDocumentNr
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "Numărul unic al dosarului de executare")

  public String getExecutionDocumentNr() {
    return executionDocumentNr;
  }


  public void setExecutionDocumentNr(String executionDocumentNr) {
    this.executionDocumentNr = executionDocumentNr;
  }


  public ExecutionProcedureDTO issuer(String issuer) {
    
    this.issuer = issuer;
    return this;
  }

   /**
   * Emitentul documentului executoriu
   * @return issuer
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "Emitentul documentului executoriu")

  public String getIssuer() {
    return issuer;
  }


  public void setIssuer(String issuer) {
    this.issuer = issuer;
  }


  public ExecutionProcedureDTO description(String description) {
    
    this.description = description;
    return this;
  }

   /**
   * Obiectul executării text
   * @return description
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "Obiectul executării text")

  public String getDescription() {
    return description;
  }


  public void setDescription(String description) {
    this.description = description;
  }


  public ExecutionProcedureDTO debtorType(String debtorType) {
    
    this.debtorType = debtorType;
    return this;
  }

   /**
   * Tipul persoanei(Fizică/Juridică/Instituție publică)
   * @return debtorType
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "Tipul persoanei(Fizică/Juridică/Instituție publică)")

  public String getDebtorType() {
    return debtorType;
  }


  public void setDebtorType(String debtorType) {
    this.debtorType = debtorType;
  }


  public ExecutionProcedureDTO debtorIdentityNr(String debtorIdentityNr) {
    
    this.debtorIdentityNr = debtorIdentityNr;
    return this;
  }

   /**
   * IDN entității solicitate
   * @return debtorIdentityNr
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "IDN entității solicitate")

  public String getDebtorIdentityNr() {
    return debtorIdentityNr;
  }


  public void setDebtorIdentityNr(String debtorIdentityNr) {
    this.debtorIdentityNr = debtorIdentityNr;
  }


  public ExecutionProcedureDTO debtorName(String debtorName) {
    
    this.debtorName = debtorName;
    return this;
  }

   /**
   * Numele/Denumirea entității
   * @return debtorName
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "Numele/Denumirea entității")

  public String getDebtorName() {
    return debtorName;
  }


  public void setDebtorName(String debtorName) {
    this.debtorName = debtorName;
  }


  public ExecutionProcedureDTO debtorSurname(String debtorSurname) {
    
    this.debtorSurname = debtorSurname;
    return this;
  }

   /**
   * Prenumele/Denumirea entității
   * @return debtorSurname
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "Prenumele/Denumirea entității")

  public String getDebtorSurname() {
    return debtorSurname;
  }


  public void setDebtorSurname(String debtorSurname) {
    this.debtorSurname = debtorSurname;
  }


  public ExecutionProcedureDTO debtorAddress(String debtorAddress) {
    
    this.debtorAddress = debtorAddress;
    return this;
  }

   /**
   * Adresa entității verificate
   * @return debtorAddress
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "Adresa entității verificate")

  public String getDebtorAddress() {
    return debtorAddress;
  }


  public void setDebtorAddress(String debtorAddress) {
    this.debtorAddress = debtorAddress;
  }


  public ExecutionProcedureDTO procedureExecutor(String procedureExecutor) {
    
    this.procedureExecutor = procedureExecutor;
    return this;
  }

   /**
   * Executorul Judecătoresc care gestionează dosarul
   * @return procedureExecutor
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "Executorul Judecătoresc care gestionează dosarul")

  public String getProcedureExecutor() {
    return procedureExecutor;
  }


  public void setProcedureExecutor(String procedureExecutor) {
    this.procedureExecutor = procedureExecutor;
  }


  public ExecutionProcedureDTO proceduralStatus(String proceduralStatus) {
    
    this.proceduralStatus = proceduralStatus;
    return this;
  }

   /**
   * Starea procesuală a dosarului
   * @return proceduralStatus
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "Starea procesuală a dosarului")

  public String getProceduralStatus() {
    return proceduralStatus;
  }


  public void setProceduralStatus(String proceduralStatus) {
    this.proceduralStatus = proceduralStatus;
  }


  public ExecutionProcedureDTO procedureClassification(String procedureClassification) {
    
    this.procedureClassification = procedureClassification;
    return this;
  }

   /**
   * Clasificarea procedurii de executare
   * @return procedureClassification
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "Clasificarea procedurii de executare")

  public String getProcedureClassification() {
    return procedureClassification;
  }


  public void setProcedureClassification(String procedureClassification) {
    this.procedureClassification = procedureClassification;
  }


  public ExecutionProcedureDTO obligations(List<Obligation> obligations) {
    
    this.obligations = obligations;
    return this;
  }

  public ExecutionProcedureDTO addObligationsItem(Obligation obligationsItem) {
    if (this.obligations == null) {
      this.obligations = new ArrayList<Obligation>();
    }
    this.obligations.add(obligationsItem);
    return this;
  }

   /**
   * Obligație detaliat
   * @return obligations
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "Obligație detaliat")

  public List<Obligation> getObligations() {
    return obligations;
  }


  public void setObligations(List<Obligation> obligations) {
    this.obligations = obligations;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExecutionProcedureDTO executionProcedureDTO = (ExecutionProcedureDTO) o;
    return Objects.equals(this.id, executionProcedureDTO.id) &&
        Objects.equals(this.systemNr, executionProcedureDTO.systemNr) &&
        Objects.equals(this.initiationDate, executionProcedureDTO.initiationDate) &&
        Objects.equals(this.executionDocumentNr, executionProcedureDTO.executionDocumentNr) &&
        Objects.equals(this.issuer, executionProcedureDTO.issuer) &&
        Objects.equals(this.description, executionProcedureDTO.description) &&
        Objects.equals(this.debtorType, executionProcedureDTO.debtorType) &&
        Objects.equals(this.debtorIdentityNr, executionProcedureDTO.debtorIdentityNr) &&
        Objects.equals(this.debtorName, executionProcedureDTO.debtorName) &&
        Objects.equals(this.debtorSurname, executionProcedureDTO.debtorSurname) &&
        Objects.equals(this.debtorAddress, executionProcedureDTO.debtorAddress) &&
        Objects.equals(this.procedureExecutor, executionProcedureDTO.procedureExecutor) &&
        Objects.equals(this.proceduralStatus, executionProcedureDTO.proceduralStatus) &&
        Objects.equals(this.procedureClassification, executionProcedureDTO.procedureClassification) &&
        Objects.equals(this.obligations, executionProcedureDTO.obligations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, systemNr, initiationDate, executionDocumentNr, issuer, description, debtorType, debtorIdentityNr, debtorName, debtorSurname, debtorAddress, procedureExecutor, proceduralStatus, procedureClassification, obligations);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ExecutionProcedureDTO {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    systemNr: ").append(toIndentedString(systemNr)).append("\n");
    sb.append("    initiationDate: ").append(toIndentedString(initiationDate)).append("\n");
    sb.append("    executionDocumentNr: ").append(toIndentedString(executionDocumentNr)).append("\n");
    sb.append("    issuer: ").append(toIndentedString(issuer)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    debtorType: ").append(toIndentedString(debtorType)).append("\n");
    sb.append("    debtorIdentityNr: ").append(toIndentedString(debtorIdentityNr)).append("\n");
    sb.append("    debtorName: ").append(toIndentedString(debtorName)).append("\n");
    sb.append("    debtorSurname: ").append(toIndentedString(debtorSurname)).append("\n");
    sb.append("    debtorAddress: ").append(toIndentedString(debtorAddress)).append("\n");
    sb.append("    procedureExecutor: ").append(toIndentedString(procedureExecutor)).append("\n");
    sb.append("    proceduralStatus: ").append(toIndentedString(proceduralStatus)).append("\n");
    sb.append("    procedureClassification: ").append(toIndentedString(procedureClassification)).append("\n");
    sb.append("    obligations: ").append(toIndentedString(obligations)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }

}


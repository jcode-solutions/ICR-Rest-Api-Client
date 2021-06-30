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

import com.google.gson.TypeAdapter;
import com.google.gson.annotations.JsonAdapter;
import com.google.gson.annotations.SerializedName;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import io.swagger.annotations.ApiModelProperty;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * ModelAndView
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2021-06-28T21:39:15.682-08:00[GMT-08:00]")
public class ModelAndView {
  public static final String SERIALIZED_NAME_VIEW = "view";
  @SerializedName(SERIALIZED_NAME_VIEW)
  private View view;

  public static final String SERIALIZED_NAME_MODEL = "model";
  @SerializedName(SERIALIZED_NAME_MODEL)
  private Map<String, Object> model = null;

  /**
   * Gets or Sets status
   */
  @JsonAdapter(StatusEnum.Adapter.class)
  public enum StatusEnum {
    _100_CONTINUE("100 CONTINUE"),
    
    _101_SWITCHING_PROTOCOLS("101 SWITCHING_PROTOCOLS"),
    
    _102_PROCESSING("102 PROCESSING"),
    
    _103_CHECKPOINT("103 CHECKPOINT"),
    
    _200_OK("200 OK"),
    
    _201_CREATED("201 CREATED"),
    
    _202_ACCEPTED("202 ACCEPTED"),
    
    _203_NON_AUTHORITATIVE_INFORMATION("203 NON_AUTHORITATIVE_INFORMATION"),
    
    _204_NO_CONTENT("204 NO_CONTENT"),
    
    _205_RESET_CONTENT("205 RESET_CONTENT"),
    
    _206_PARTIAL_CONTENT("206 PARTIAL_CONTENT"),
    
    _207_MULTI_STATUS("207 MULTI_STATUS"),
    
    _208_ALREADY_REPORTED("208 ALREADY_REPORTED"),
    
    _226_IM_USED("226 IM_USED"),
    
    _300_MULTIPLE_CHOICES("300 MULTIPLE_CHOICES"),
    
    _301_MOVED_PERMANENTLY("301 MOVED_PERMANENTLY"),
    
    _302_FOUND("302 FOUND"),
    
    _302_MOVED_TEMPORARILY("302 MOVED_TEMPORARILY"),
    
    _303_SEE_OTHER("303 SEE_OTHER"),
    
    _304_NOT_MODIFIED("304 NOT_MODIFIED"),
    
    _305_USE_PROXY("305 USE_PROXY"),
    
    _307_TEMPORARY_REDIRECT("307 TEMPORARY_REDIRECT"),
    
    _308_PERMANENT_REDIRECT("308 PERMANENT_REDIRECT"),
    
    _400_BAD_REQUEST("400 BAD_REQUEST"),
    
    _401_UNAUTHORIZED("401 UNAUTHORIZED"),
    
    _402_PAYMENT_REQUIRED("402 PAYMENT_REQUIRED"),
    
    _403_FORBIDDEN("403 FORBIDDEN"),
    
    _404_NOT_FOUND("404 NOT_FOUND"),
    
    _405_METHOD_NOT_ALLOWED("405 METHOD_NOT_ALLOWED"),
    
    _406_NOT_ACCEPTABLE("406 NOT_ACCEPTABLE"),
    
    _407_PROXY_AUTHENTICATION_REQUIRED("407 PROXY_AUTHENTICATION_REQUIRED"),
    
    _408_REQUEST_TIMEOUT("408 REQUEST_TIMEOUT"),
    
    _409_CONFLICT("409 CONFLICT"),
    
    _410_GONE("410 GONE"),
    
    _411_LENGTH_REQUIRED("411 LENGTH_REQUIRED"),
    
    _412_PRECONDITION_FAILED("412 PRECONDITION_FAILED"),
    
    _413_PAYLOAD_TOO_LARGE("413 PAYLOAD_TOO_LARGE"),
    
    _413_REQUEST_ENTITY_TOO_LARGE("413 REQUEST_ENTITY_TOO_LARGE"),
    
    _414_URI_TOO_LONG("414 URI_TOO_LONG"),
    
    _414_REQUEST_URI_TOO_LONG("414 REQUEST_URI_TOO_LONG"),
    
    _415_UNSUPPORTED_MEDIA_TYPE("415 UNSUPPORTED_MEDIA_TYPE"),
    
    _416_REQUESTED_RANGE_NOT_SATISFIABLE("416 REQUESTED_RANGE_NOT_SATISFIABLE"),
    
    _417_EXPECTATION_FAILED("417 EXPECTATION_FAILED"),
    
    _418_I_AM_A_TEAPOT("418 I_AM_A_TEAPOT"),
    
    _419_INSUFFICIENT_SPACE_ON_RESOURCE("419 INSUFFICIENT_SPACE_ON_RESOURCE"),
    
    _420_METHOD_FAILURE("420 METHOD_FAILURE"),
    
    _421_DESTINATION_LOCKED("421 DESTINATION_LOCKED"),
    
    _422_UNPROCESSABLE_ENTITY("422 UNPROCESSABLE_ENTITY"),
    
    _423_LOCKED("423 LOCKED"),
    
    _424_FAILED_DEPENDENCY("424 FAILED_DEPENDENCY"),
    
    _425_TOO_EARLY("425 TOO_EARLY"),
    
    _426_UPGRADE_REQUIRED("426 UPGRADE_REQUIRED"),
    
    _428_PRECONDITION_REQUIRED("428 PRECONDITION_REQUIRED"),
    
    _429_TOO_MANY_REQUESTS("429 TOO_MANY_REQUESTS"),
    
    _431_REQUEST_HEADER_FIELDS_TOO_LARGE("431 REQUEST_HEADER_FIELDS_TOO_LARGE"),
    
    _451_UNAVAILABLE_FOR_LEGAL_REASONS("451 UNAVAILABLE_FOR_LEGAL_REASONS"),
    
    _500_INTERNAL_SERVER_ERROR("500 INTERNAL_SERVER_ERROR"),
    
    _501_NOT_IMPLEMENTED("501 NOT_IMPLEMENTED"),
    
    _502_BAD_GATEWAY("502 BAD_GATEWAY"),
    
    _503_SERVICE_UNAVAILABLE("503 SERVICE_UNAVAILABLE"),
    
    _504_GATEWAY_TIMEOUT("504 GATEWAY_TIMEOUT"),
    
    _505_HTTP_VERSION_NOT_SUPPORTED("505 HTTP_VERSION_NOT_SUPPORTED"),
    
    _506_VARIANT_ALSO_NEGOTIATES("506 VARIANT_ALSO_NEGOTIATES"),
    
    _507_INSUFFICIENT_STORAGE("507 INSUFFICIENT_STORAGE"),
    
    _508_LOOP_DETECTED("508 LOOP_DETECTED"),
    
    _509_BANDWIDTH_LIMIT_EXCEEDED("509 BANDWIDTH_LIMIT_EXCEEDED"),
    
    _510_NOT_EXTENDED("510 NOT_EXTENDED"),
    
    _511_NETWORK_AUTHENTICATION_REQUIRED("511 NETWORK_AUTHENTICATION_REQUIRED");

    private String value;

    StatusEnum(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    public static StatusEnum fromValue(String value) {
      for (StatusEnum b : StatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }

    public static class Adapter extends TypeAdapter<StatusEnum> {
      @Override
      public void write(final JsonWriter jsonWriter, final StatusEnum enumeration) throws IOException {
        jsonWriter.value(enumeration.getValue());
      }

      @Override
      public StatusEnum read(final JsonReader jsonReader) throws IOException {
        String value =  jsonReader.nextString();
        return StatusEnum.fromValue(value);
      }
    }
  }

  public static final String SERIALIZED_NAME_STATUS = "status";
  @SerializedName(SERIALIZED_NAME_STATUS)
  private StatusEnum status;

  public static final String SERIALIZED_NAME_EMPTY = "empty";
  @SerializedName(SERIALIZED_NAME_EMPTY)
  private Boolean empty;

  public static final String SERIALIZED_NAME_VIEW_NAME = "viewName";
  @SerializedName(SERIALIZED_NAME_VIEW_NAME)
  private String viewName;

  public static final String SERIALIZED_NAME_MODEL_MAP = "modelMap";
  @SerializedName(SERIALIZED_NAME_MODEL_MAP)
  private Map<String, Object> modelMap = null;

  public static final String SERIALIZED_NAME_REFERENCE = "reference";
  @SerializedName(SERIALIZED_NAME_REFERENCE)
  private Boolean reference;


  public ModelAndView view(View view) {
    
    this.view = view;
    return this;
  }

   /**
   * Get view
   * @return view
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public View getView() {
    return view;
  }


  public void setView(View view) {
    this.view = view;
  }


  public ModelAndView model(Map<String, Object> model) {
    
    this.model = model;
    return this;
  }

  public ModelAndView putModelItem(String key, Object modelItem) {
    if (this.model == null) {
      this.model = new HashMap<String, Object>();
    }
    this.model.put(key, modelItem);
    return this;
  }

   /**
   * Get model
   * @return model
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public Map<String, Object> getModel() {
    return model;
  }


  public void setModel(Map<String, Object> model) {
    this.model = model;
  }


  public ModelAndView status(StatusEnum status) {
    
    this.status = status;
    return this;
  }

   /**
   * Get status
   * @return status
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public StatusEnum getStatus() {
    return status;
  }


  public void setStatus(StatusEnum status) {
    this.status = status;
  }


  public ModelAndView empty(Boolean empty) {
    
    this.empty = empty;
    return this;
  }

   /**
   * Get empty
   * @return empty
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public Boolean getEmpty() {
    return empty;
  }


  public void setEmpty(Boolean empty) {
    this.empty = empty;
  }


  public ModelAndView viewName(String viewName) {
    
    this.viewName = viewName;
    return this;
  }

   /**
   * Get viewName
   * @return viewName
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public String getViewName() {
    return viewName;
  }


  public void setViewName(String viewName) {
    this.viewName = viewName;
  }


  public ModelAndView modelMap(Map<String, Object> modelMap) {
    
    this.modelMap = modelMap;
    return this;
  }

  public ModelAndView putModelMapItem(String key, Object modelMapItem) {
    if (this.modelMap == null) {
      this.modelMap = new HashMap<String, Object>();
    }
    this.modelMap.put(key, modelMapItem);
    return this;
  }

   /**
   * Get modelMap
   * @return modelMap
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public Map<String, Object> getModelMap() {
    return modelMap;
  }


  public void setModelMap(Map<String, Object> modelMap) {
    this.modelMap = modelMap;
  }


  public ModelAndView reference(Boolean reference) {
    
    this.reference = reference;
    return this;
  }

   /**
   * Get reference
   * @return reference
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public Boolean getReference() {
    return reference;
  }


  public void setReference(Boolean reference) {
    this.reference = reference;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ModelAndView modelAndView = (ModelAndView) o;
    return Objects.equals(this.view, modelAndView.view) &&
        Objects.equals(this.model, modelAndView.model) &&
        Objects.equals(this.status, modelAndView.status) &&
        Objects.equals(this.empty, modelAndView.empty) &&
        Objects.equals(this.viewName, modelAndView.viewName) &&
        Objects.equals(this.modelMap, modelAndView.modelMap) &&
        Objects.equals(this.reference, modelAndView.reference);
  }

  @Override
  public int hashCode() {
    return Objects.hash(view, model, status, empty, viewName, modelMap, reference);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ModelAndView {\n");
    sb.append("    view: ").append(toIndentedString(view)).append("\n");
    sb.append("    model: ").append(toIndentedString(model)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    empty: ").append(toIndentedString(empty)).append("\n");
    sb.append("    viewName: ").append(toIndentedString(viewName)).append("\n");
    sb.append("    modelMap: ").append(toIndentedString(modelMap)).append("\n");
    sb.append("    reference: ").append(toIndentedString(reference)).append("\n");
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


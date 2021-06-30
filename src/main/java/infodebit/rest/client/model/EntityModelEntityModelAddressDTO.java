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
 * EntityModelEntityModelAddressDTO
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2021-06-28T21:39:15.682-08:00[GMT-08:00]")
public class EntityModelEntityModelAddressDTO {
  public static final String SERIALIZED_NAME_ID = "id";
  @SerializedName(SERIALIZED_NAME_ID)
  private Long id;

  public static final String SERIALIZED_NAME_CONTACT_ID = "contactId";
  @SerializedName(SERIALIZED_NAME_CONTACT_ID)
  private Long contactId;

  public static final String SERIALIZED_NAME_TYPE = "type";
  @SerializedName(SERIALIZED_NAME_TYPE)
  private String type;

  public static final String SERIALIZED_NAME_COUNTRY = "country";
  @SerializedName(SERIALIZED_NAME_COUNTRY)
  private String country;

  public static final String SERIALIZED_NAME_TOWN = "town";
  @SerializedName(SERIALIZED_NAME_TOWN)
  private String town;

  public static final String SERIALIZED_NAME_VILLAGE = "village";
  @SerializedName(SERIALIZED_NAME_VILLAGE)
  private String village;

  public static final String SERIALIZED_NAME_POSTAL_CODE = "postalCode";
  @SerializedName(SERIALIZED_NAME_POSTAL_CODE)
  private String postalCode;

  public static final String SERIALIZED_NAME_STREET_TYPE = "streetType";
  @SerializedName(SERIALIZED_NAME_STREET_TYPE)
  private String streetType;

  public static final String SERIALIZED_NAME_STREET = "street";
  @SerializedName(SERIALIZED_NAME_STREET)
  private String street;

  public static final String SERIALIZED_NAME_HOUSE = "house";
  @SerializedName(SERIALIZED_NAME_HOUSE)
  private String house;

  public static final String SERIALIZED_NAME_APARTMENT = "apartment";
  @SerializedName(SERIALIZED_NAME_APARTMENT)
  private String apartment;

  public static final String SERIALIZED_NAME_ACTIVE = "active";
  @SerializedName(SERIALIZED_NAME_ACTIVE)
  private Boolean active;

  public static final String SERIALIZED_NAME_PRIMARILY = "primarily";
  @SerializedName(SERIALIZED_NAME_PRIMARILY)
  private Boolean primarily;

  public static final String SERIALIZED_NAME_CLIENT_ID = "clientId";
  @SerializedName(SERIALIZED_NAME_CLIENT_ID)
  private Long clientId;

  public static final String SERIALIZED_NAME_IMPORTED_BY = "importedBy";
  @SerializedName(SERIALIZED_NAME_IMPORTED_BY)
  private Integer importedBy;

  public static final String SERIALIZED_NAME_LINKS = "links";
  @SerializedName(SERIALIZED_NAME_LINKS)
  private List<Link> links = null;


  public EntityModelEntityModelAddressDTO id(Long id) {
    
    this.id = id;
    return this;
  }

   /**
   * Get id
   * @return id
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public Long getId() {
    return id;
  }


  public void setId(Long id) {
    this.id = id;
  }


  public EntityModelEntityModelAddressDTO contactId(Long contactId) {
    
    this.contactId = contactId;
    return this;
  }

   /**
   * Get contactId
   * @return contactId
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public Long getContactId() {
    return contactId;
  }


  public void setContactId(Long contactId) {
    this.contactId = contactId;
  }


  public EntityModelEntityModelAddressDTO type(String type) {
    
    this.type = type;
    return this;
  }

   /**
   * Get type
   * @return type
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public String getType() {
    return type;
  }


  public void setType(String type) {
    this.type = type;
  }


  public EntityModelEntityModelAddressDTO country(String country) {
    
    this.country = country;
    return this;
  }

   /**
   * Get country
   * @return country
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public String getCountry() {
    return country;
  }


  public void setCountry(String country) {
    this.country = country;
  }


  public EntityModelEntityModelAddressDTO town(String town) {
    
    this.town = town;
    return this;
  }

   /**
   * Get town
   * @return town
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public String getTown() {
    return town;
  }


  public void setTown(String town) {
    this.town = town;
  }


  public EntityModelEntityModelAddressDTO village(String village) {
    
    this.village = village;
    return this;
  }

   /**
   * Get village
   * @return village
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public String getVillage() {
    return village;
  }


  public void setVillage(String village) {
    this.village = village;
  }


  public EntityModelEntityModelAddressDTO postalCode(String postalCode) {
    
    this.postalCode = postalCode;
    return this;
  }

   /**
   * Get postalCode
   * @return postalCode
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public String getPostalCode() {
    return postalCode;
  }


  public void setPostalCode(String postalCode) {
    this.postalCode = postalCode;
  }


  public EntityModelEntityModelAddressDTO streetType(String streetType) {
    
    this.streetType = streetType;
    return this;
  }

   /**
   * Get streetType
   * @return streetType
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public String getStreetType() {
    return streetType;
  }


  public void setStreetType(String streetType) {
    this.streetType = streetType;
  }


  public EntityModelEntityModelAddressDTO street(String street) {
    
    this.street = street;
    return this;
  }

   /**
   * Get street
   * @return street
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public String getStreet() {
    return street;
  }


  public void setStreet(String street) {
    this.street = street;
  }


  public EntityModelEntityModelAddressDTO house(String house) {
    
    this.house = house;
    return this;
  }

   /**
   * Get house
   * @return house
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public String getHouse() {
    return house;
  }


  public void setHouse(String house) {
    this.house = house;
  }


  public EntityModelEntityModelAddressDTO apartment(String apartment) {
    
    this.apartment = apartment;
    return this;
  }

   /**
   * Get apartment
   * @return apartment
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public String getApartment() {
    return apartment;
  }


  public void setApartment(String apartment) {
    this.apartment = apartment;
  }


  public EntityModelEntityModelAddressDTO active(Boolean active) {
    
    this.active = active;
    return this;
  }

   /**
   * Get active
   * @return active
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public Boolean getActive() {
    return active;
  }


  public void setActive(Boolean active) {
    this.active = active;
  }


  public EntityModelEntityModelAddressDTO primarily(Boolean primarily) {
    
    this.primarily = primarily;
    return this;
  }

   /**
   * Get primarily
   * @return primarily
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public Boolean getPrimarily() {
    return primarily;
  }


  public void setPrimarily(Boolean primarily) {
    this.primarily = primarily;
  }


  public EntityModelEntityModelAddressDTO clientId(Long clientId) {
    
    this.clientId = clientId;
    return this;
  }

   /**
   * Get clientId
   * @return clientId
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public Long getClientId() {
    return clientId;
  }


  public void setClientId(Long clientId) {
    this.clientId = clientId;
  }


  public EntityModelEntityModelAddressDTO importedBy(Integer importedBy) {
    
    this.importedBy = importedBy;
    return this;
  }

   /**
   * Get importedBy
   * @return importedBy
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public Integer getImportedBy() {
    return importedBy;
  }


  public void setImportedBy(Integer importedBy) {
    this.importedBy = importedBy;
  }


  public EntityModelEntityModelAddressDTO links(List<Link> links) {
    
    this.links = links;
    return this;
  }

  public EntityModelEntityModelAddressDTO addLinksItem(Link linksItem) {
    if (this.links == null) {
      this.links = new ArrayList<Link>();
    }
    this.links.add(linksItem);
    return this;
  }

   /**
   * Get links
   * @return links
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public List<Link> getLinks() {
    return links;
  }


  public void setLinks(List<Link> links) {
    this.links = links;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EntityModelEntityModelAddressDTO entityModelEntityModelAddressDTO = (EntityModelEntityModelAddressDTO) o;
    return Objects.equals(this.id, entityModelEntityModelAddressDTO.id) &&
        Objects.equals(this.contactId, entityModelEntityModelAddressDTO.contactId) &&
        Objects.equals(this.type, entityModelEntityModelAddressDTO.type) &&
        Objects.equals(this.country, entityModelEntityModelAddressDTO.country) &&
        Objects.equals(this.town, entityModelEntityModelAddressDTO.town) &&
        Objects.equals(this.village, entityModelEntityModelAddressDTO.village) &&
        Objects.equals(this.postalCode, entityModelEntityModelAddressDTO.postalCode) &&
        Objects.equals(this.streetType, entityModelEntityModelAddressDTO.streetType) &&
        Objects.equals(this.street, entityModelEntityModelAddressDTO.street) &&
        Objects.equals(this.house, entityModelEntityModelAddressDTO.house) &&
        Objects.equals(this.apartment, entityModelEntityModelAddressDTO.apartment) &&
        Objects.equals(this.active, entityModelEntityModelAddressDTO.active) &&
        Objects.equals(this.primarily, entityModelEntityModelAddressDTO.primarily) &&
        Objects.equals(this.clientId, entityModelEntityModelAddressDTO.clientId) &&
        Objects.equals(this.importedBy, entityModelEntityModelAddressDTO.importedBy) &&
        Objects.equals(this.links, entityModelEntityModelAddressDTO.links);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, contactId, type, country, town, village, postalCode, streetType, street, house, apartment, active, primarily, clientId, importedBy, links);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EntityModelEntityModelAddressDTO {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    contactId: ").append(toIndentedString(contactId)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    country: ").append(toIndentedString(country)).append("\n");
    sb.append("    town: ").append(toIndentedString(town)).append("\n");
    sb.append("    village: ").append(toIndentedString(village)).append("\n");
    sb.append("    postalCode: ").append(toIndentedString(postalCode)).append("\n");
    sb.append("    streetType: ").append(toIndentedString(streetType)).append("\n");
    sb.append("    street: ").append(toIndentedString(street)).append("\n");
    sb.append("    house: ").append(toIndentedString(house)).append("\n");
    sb.append("    apartment: ").append(toIndentedString(apartment)).append("\n");
    sb.append("    active: ").append(toIndentedString(active)).append("\n");
    sb.append("    primarily: ").append(toIndentedString(primarily)).append("\n");
    sb.append("    clientId: ").append(toIndentedString(clientId)).append("\n");
    sb.append("    importedBy: ").append(toIndentedString(importedBy)).append("\n");
    sb.append("    links: ").append(toIndentedString(links)).append("\n");
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


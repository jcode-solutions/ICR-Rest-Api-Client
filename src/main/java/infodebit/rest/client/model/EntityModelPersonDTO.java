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
import org.threeten.bp.OffsetDateTime;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * EntityModelPersonDTO
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2021-06-28T21:39:15.682-08:00[GMT-08:00]")
public class EntityModelPersonDTO {
  public static final String SERIALIZED_NAME_ID = "id";
  @SerializedName(SERIALIZED_NAME_ID)
  private Long id;

  public static final String SERIALIZED_NAME_NAME = "name";
  @SerializedName(SERIALIZED_NAME_NAME)
  private String name;

  public static final String SERIALIZED_NAME_SURNAME = "surname";
  @SerializedName(SERIALIZED_NAME_SURNAME)
  private String surname;

  public static final String SERIALIZED_NAME_PATRONIMIC = "patronimic";
  @SerializedName(SERIALIZED_NAME_PATRONIMIC)
  private String patronimic;

  /**
   * Gets or Sets gender
   */
  @JsonAdapter(GenderEnum.Adapter.class)
  public enum GenderEnum {
    MALE("MALE"),
    
    FEMALE("FEMALE"),
    
    NO_DATA("NO_DATA");

    private String value;

    GenderEnum(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    public static GenderEnum fromValue(String value) {
      for (GenderEnum b : GenderEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }

    public static class Adapter extends TypeAdapter<GenderEnum> {
      @Override
      public void write(final JsonWriter jsonWriter, final GenderEnum enumeration) throws IOException {
        jsonWriter.value(enumeration.getValue());
      }

      @Override
      public GenderEnum read(final JsonReader jsonReader) throws IOException {
        String value =  jsonReader.nextString();
        return GenderEnum.fromValue(value);
      }
    }
  }

  public static final String SERIALIZED_NAME_GENDER = "gender";
  @SerializedName(SERIALIZED_NAME_GENDER)
  private GenderEnum gender;

  public static final String SERIALIZED_NAME_BIRTH_DATE = "birthDate";
  @SerializedName(SERIALIZED_NAME_BIRTH_DATE)
  private OffsetDateTime birthDate;

  /**
   * Gets or Sets language
   */
  @JsonAdapter(LanguageEnum.Adapter.class)
  public enum LanguageEnum {
    ROMANIAN("ROMANIAN"),
    
    RUSSIAN("RUSSIAN"),
    
    OTHER("OTHER"),
    
    NO_DATA("NO_DATA");

    private String value;

    LanguageEnum(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    public static LanguageEnum fromValue(String value) {
      for (LanguageEnum b : LanguageEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }

    public static class Adapter extends TypeAdapter<LanguageEnum> {
      @Override
      public void write(final JsonWriter jsonWriter, final LanguageEnum enumeration) throws IOException {
        jsonWriter.value(enumeration.getValue());
      }

      @Override
      public LanguageEnum read(final JsonReader jsonReader) throws IOException {
        String value =  jsonReader.nextString();
        return LanguageEnum.fromValue(value);
      }
    }
  }

  public static final String SERIALIZED_NAME_LANGUAGE = "language";
  @SerializedName(SERIALIZED_NAME_LANGUAGE)
  private LanguageEnum language;

  public static final String SERIALIZED_NAME_IDNP = "idnp";
  @SerializedName(SERIALIZED_NAME_IDNP)
  private String idnp;

  /**
   * Gets or Sets maritalStatus
   */
  @JsonAdapter(MaritalStatusEnum.Adapter.class)
  public enum MaritalStatusEnum {
    NO_DATA("NO_DATA"),
    
    FIRST_TIME_MARRIED("FIRST_TIME_MARRIED"),
    
    DIVORCED("DIVORCED"),
    
    SECOND_TIME_MARRIED("SECOND_TIME_MARRIED"),
    
    WIDOWER("WIDOWER"),
    
    BACHELOR("BACHELOR"),
    
    OTHER("OTHER");

    private String value;

    MaritalStatusEnum(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    public static MaritalStatusEnum fromValue(String value) {
      for (MaritalStatusEnum b : MaritalStatusEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }

    public static class Adapter extends TypeAdapter<MaritalStatusEnum> {
      @Override
      public void write(final JsonWriter jsonWriter, final MaritalStatusEnum enumeration) throws IOException {
        jsonWriter.value(enumeration.getValue());
      }

      @Override
      public MaritalStatusEnum read(final JsonReader jsonReader) throws IOException {
        String value =  jsonReader.nextString();
        return MaritalStatusEnum.fromValue(value);
      }
    }
  }

  public static final String SERIALIZED_NAME_MARITAL_STATUS = "maritalStatus";
  @SerializedName(SERIALIZED_NAME_MARITAL_STATUS)
  private MaritalStatusEnum maritalStatus;

  public static final String SERIALIZED_NAME_OCCUPATION = "occupation";
  @SerializedName(SERIALIZED_NAME_OCCUPATION)
  private String occupation;

  public static final String SERIALIZED_NAME_LINKS = "links";
  @SerializedName(SERIALIZED_NAME_LINKS)
  private List<Link> links = null;


  public EntityModelPersonDTO id(Long id) {
    
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


  public EntityModelPersonDTO name(String name) {
    
    this.name = name;
    return this;
  }

   /**
   * Get name
   * @return name
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public String getName() {
    return name;
  }


  public void setName(String name) {
    this.name = name;
  }


  public EntityModelPersonDTO surname(String surname) {
    
    this.surname = surname;
    return this;
  }

   /**
   * Get surname
   * @return surname
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public String getSurname() {
    return surname;
  }


  public void setSurname(String surname) {
    this.surname = surname;
  }


  public EntityModelPersonDTO patronimic(String patronimic) {
    
    this.patronimic = patronimic;
    return this;
  }

   /**
   * Get patronimic
   * @return patronimic
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public String getPatronimic() {
    return patronimic;
  }


  public void setPatronimic(String patronimic) {
    this.patronimic = patronimic;
  }


  public EntityModelPersonDTO gender(GenderEnum gender) {
    
    this.gender = gender;
    return this;
  }

   /**
   * Get gender
   * @return gender
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public GenderEnum getGender() {
    return gender;
  }


  public void setGender(GenderEnum gender) {
    this.gender = gender;
  }


  public EntityModelPersonDTO birthDate(OffsetDateTime birthDate) {
    
    this.birthDate = birthDate;
    return this;
  }

   /**
   * Get birthDate
   * @return birthDate
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public OffsetDateTime getBirthDate() {
    return birthDate;
  }


  public void setBirthDate(OffsetDateTime birthDate) {
    this.birthDate = birthDate;
  }


  public EntityModelPersonDTO language(LanguageEnum language) {
    
    this.language = language;
    return this;
  }

   /**
   * Get language
   * @return language
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public LanguageEnum getLanguage() {
    return language;
  }


  public void setLanguage(LanguageEnum language) {
    this.language = language;
  }


  public EntityModelPersonDTO idnp(String idnp) {
    
    this.idnp = idnp;
    return this;
  }

   /**
   * Get idnp
   * @return idnp
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public String getIdnp() {
    return idnp;
  }


  public void setIdnp(String idnp) {
    this.idnp = idnp;
  }


  public EntityModelPersonDTO maritalStatus(MaritalStatusEnum maritalStatus) {
    
    this.maritalStatus = maritalStatus;
    return this;
  }

   /**
   * Get maritalStatus
   * @return maritalStatus
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public MaritalStatusEnum getMaritalStatus() {
    return maritalStatus;
  }


  public void setMaritalStatus(MaritalStatusEnum maritalStatus) {
    this.maritalStatus = maritalStatus;
  }


  public EntityModelPersonDTO occupation(String occupation) {
    
    this.occupation = occupation;
    return this;
  }

   /**
   * Get occupation
   * @return occupation
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public String getOccupation() {
    return occupation;
  }


  public void setOccupation(String occupation) {
    this.occupation = occupation;
  }


  public EntityModelPersonDTO links(List<Link> links) {
    
    this.links = links;
    return this;
  }

  public EntityModelPersonDTO addLinksItem(Link linksItem) {
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
    EntityModelPersonDTO entityModelPersonDTO = (EntityModelPersonDTO) o;
    return Objects.equals(this.id, entityModelPersonDTO.id) &&
        Objects.equals(this.name, entityModelPersonDTO.name) &&
        Objects.equals(this.surname, entityModelPersonDTO.surname) &&
        Objects.equals(this.patronimic, entityModelPersonDTO.patronimic) &&
        Objects.equals(this.gender, entityModelPersonDTO.gender) &&
        Objects.equals(this.birthDate, entityModelPersonDTO.birthDate) &&
        Objects.equals(this.language, entityModelPersonDTO.language) &&
        Objects.equals(this.idnp, entityModelPersonDTO.idnp) &&
        Objects.equals(this.maritalStatus, entityModelPersonDTO.maritalStatus) &&
        Objects.equals(this.occupation, entityModelPersonDTO.occupation) &&
        Objects.equals(this.links, entityModelPersonDTO.links);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, surname, patronimic, gender, birthDate, language, idnp, maritalStatus, occupation, links);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EntityModelPersonDTO {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    surname: ").append(toIndentedString(surname)).append("\n");
    sb.append("    patronimic: ").append(toIndentedString(patronimic)).append("\n");
    sb.append("    gender: ").append(toIndentedString(gender)).append("\n");
    sb.append("    birthDate: ").append(toIndentedString(birthDate)).append("\n");
    sb.append("    language: ").append(toIndentedString(language)).append("\n");
    sb.append("    idnp: ").append(toIndentedString(idnp)).append("\n");
    sb.append("    maritalStatus: ").append(toIndentedString(maritalStatus)).append("\n");
    sb.append("    occupation: ").append(toIndentedString(occupation)).append("\n");
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


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
import java.util.Objects;

/**
 * DebtDTO
 */
@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2021-06-28T21:39:15.682-08:00[GMT-08:00]")
public class DebtDTO {
  public static final String SERIALIZED_NAME_ID = "id";
  @SerializedName(SERIALIZED_NAME_ID)
  private Long id;

  public static final String SERIALIZED_NAME_REFERENCE_NUMBER = "referenceNumber";
  @SerializedName(SERIALIZED_NAME_REFERENCE_NUMBER)
  private String referenceNumber;

  public static final String SERIALIZED_NAME_CLIENT_I_D_N_P = "clientIDNP";
  @SerializedName(SERIALIZED_NAME_CLIENT_I_D_N_P)
  private String clientIDNP;

  public static final String SERIALIZED_NAME_DEBTOR_I_D_N_P = "debtorIDNP";
  @SerializedName(SERIALIZED_NAME_DEBTOR_I_D_N_P)
  private String debtorIDNP;

  /**
   * Gets or Sets debtorType
   */
  @JsonAdapter(DebtorTypeEnum.Adapter.class)
  public enum DebtorTypeEnum {
    NATURAL_PERSON("NATURAL_PERSON"),
    
    LEGAL_PERSON("LEGAL_PERSON"),
    
    ENTREPRENEUR("ENTREPRENEUR");

    private String value;

    DebtorTypeEnum(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    public static DebtorTypeEnum fromValue(String value) {
      for (DebtorTypeEnum b : DebtorTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }

    public static class Adapter extends TypeAdapter<DebtorTypeEnum> {
      @Override
      public void write(final JsonWriter jsonWriter, final DebtorTypeEnum enumeration) throws IOException {
        jsonWriter.value(enumeration.getValue());
      }

      @Override
      public DebtorTypeEnum read(final JsonReader jsonReader) throws IOException {
        String value =  jsonReader.nextString();
        return DebtorTypeEnum.fromValue(value);
      }
    }
  }

  public static final String SERIALIZED_NAME_DEBTOR_TYPE = "debtorType";
  @SerializedName(SERIALIZED_NAME_DEBTOR_TYPE)
  private DebtorTypeEnum debtorType;

  public static final String SERIALIZED_NAME_CONTRACT_NUMBER = "contractNumber";
  @SerializedName(SERIALIZED_NAME_CONTRACT_NUMBER)
  private String contractNumber;

  public static final String SERIALIZED_NAME_CREDIT_DURATION = "creditDuration";
  @SerializedName(SERIALIZED_NAME_CREDIT_DURATION)
  private Integer creditDuration;

  public static final String SERIALIZED_NAME_INITIAL_AMOUNT = "initialAmount";
  @SerializedName(SERIALIZED_NAME_INITIAL_AMOUNT)
  private Float initialAmount;

  public static final String SERIALIZED_NAME_INTEREST_RATE = "interestRate";
  @SerializedName(SERIALIZED_NAME_INTEREST_RATE)
  private Float interestRate;

  /**
   * Gets or Sets currency
   */
  @JsonAdapter(CurrencyEnum.Adapter.class)
  public enum CurrencyEnum {
    NO_DATA("NO_DATA"),
    
    MDL("MDL"),
    
    EUR("EUR"),
    
    USD("USD");

    private String value;

    CurrencyEnum(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    public static CurrencyEnum fromValue(String value) {
      for (CurrencyEnum b : CurrencyEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }

    public static class Adapter extends TypeAdapter<CurrencyEnum> {
      @Override
      public void write(final JsonWriter jsonWriter, final CurrencyEnum enumeration) throws IOException {
        jsonWriter.value(enumeration.getValue());
      }

      @Override
      public CurrencyEnum read(final JsonReader jsonReader) throws IOException {
        String value =  jsonReader.nextString();
        return CurrencyEnum.fromValue(value);
      }
    }
  }

  public static final String SERIALIZED_NAME_CURRENCY = "currency";
  @SerializedName(SERIALIZED_NAME_CURRENCY)
  private CurrencyEnum currency;

  public static final String SERIALIZED_NAME_TOTAL = "total";
  @SerializedName(SERIALIZED_NAME_TOTAL)
  private Float total;

  public static final String SERIALIZED_NAME_FINES = "fines";
  @SerializedName(SERIALIZED_NAME_FINES)
  private Float fines;

  public static final String SERIALIZED_NAME_PAID = "paid";
  @SerializedName(SERIALIZED_NAME_PAID)
  private Float paid;

  public static final String SERIALIZED_NAME_AGREEMENT_NUMBER = "agreementNumber";
  @SerializedName(SERIALIZED_NAME_AGREEMENT_NUMBER)
  private String agreementNumber;

  public static final String SERIALIZED_NAME_AGREEMENT_DATE = "agreementDate";
  @SerializedName(SERIALIZED_NAME_AGREEMENT_DATE)
  private OffsetDateTime agreementDate;

  public static final String SERIALIZED_NAME_INVOICE_DATE = "invoiceDate";
  @SerializedName(SERIALIZED_NAME_INVOICE_DATE)
  private OffsetDateTime invoiceDate;

  public static final String SERIALIZED_NAME_INVOICE_AMOUNT = "invoiceAmount";
  @SerializedName(SERIALIZED_NAME_INVOICE_AMOUNT)
  private Float invoiceAmount;

  public static final String SERIALIZED_NAME_DEBT_AMOUNT = "debtAmount";
  @SerializedName(SERIALIZED_NAME_DEBT_AMOUNT)
  private Float debtAmount;

  /**
   * Gets or Sets debtStage
   */
  @JsonAdapter(DebtStageEnum.Adapter.class)
  public enum DebtStageEnum {
    NEW_ACCOUNT("NEW_ACCOUNT"),
    
    CREDITEBIT_WITHOUT_OUTSTANDING("CREDITEBIT_WITHOUT_OUTSTANDING"),
    
    CPO_BALANCE_IS_ZERO("CPO_BALANCE_IS_ZERO"),
    
    CPO_FULLY_PAID_IN_ADVANCE("CPO_FULLY_PAID_IN_ADVANCE"),
    
    CPO_PAID_IN_COLLECTION("CPO_PAID_IN_COLLECTION"),
    
    CPO_RECEIVED_BY_VOLUNTARY_TRANSFER("CPO_RECEIVED_BY_VOLUNTARY_TRANSFER"),
    
    CPO_PREVIOUS_CODE_WAS_90("CPO_PREVIOUS_CODE_WAS_90"),
    
    CLOSED_PAID_IN_THE_EXECUTION_PROCEDURE("CLOSED_PAID_IN_THE_EXECUTION_PROCEDURE"),
    
    CLOSED_PAID_BY_THE_INSURANCE_COMPANY("CLOSED_PAID_BY_THE_INSURANCE_COMPANY"),
    
    CLOSED_DEBT_RECOVERED_BY_CESSION("CLOSED_DEBT_RECOVERED_BY_CESSION"),
    
    OUTSTANDING_DEBT_FOR_31_60_DAYS("OUTSTANDING_DEBT_FOR_31_60_DAYS"),
    
    OUTSTANDING_DEBT_FOR_61_90_DAYS("OUTSTANDING_DEBT_FOR_61_90_DAYS"),
    
    OUTSTANDING_DEBT_FOR_91_120_DAYS("OUTSTANDING_DEBT_FOR_91_120_DAYS"),
    
    OUTSTANDING_DEBT_FOR_121_150_DAYS("OUTSTANDING_DEBT_FOR_121_150_DAYS"),
    
    OUTSTANDING_DEBT_FOR_151_180_DAYS("OUTSTANDING_DEBT_FOR_151_180_DAYS"),
    
    OUTSTANDING_DEBT_FOR_MORE_THAN_181_DAYS("OUTSTANDING_DEBT_FOR_MORE_THAN_181_DAYS"),
    
    CHARGED_OFF("CHARGED_OFF"),
    
    SENT_TO_EXTERNAL_COLLECTION("SENT_TO_EXTERNAL_COLLECTION"),
    
    SENT_TO_EXECUTION_OR_BAILIFFS("SENT_TO_EXECUTION_OR_BAILIFFS"),
    
    SENT_TO_COURT("SENT_TO_COURT"),
    
    CLOSED_VOLUNTARY_RETURNING_THE_GOOD("CLOSED_VOLUNTARY_RETURNING_THE_GOOD"),
    
    VOLUNTARY_RETURNING_THE_GOOD_BUT_THERE_IS_A_BALANCE_DUE("VOLUNTARY_RETURNING_THE_GOOD_BUT_THERE_IS_A_BALANCE_DUE"),
    
    CLOSED_UNPAID_BALANCE_REPORTED_AS_A_LOSS("CLOSED_UNPAID_BALANCE_REPORTED_AS_A_LOSS"),
    
    CLOSED_NOT_UPDATE_OR_CONFIRM_DATA_IN_INFODEBIT_BY_CREDITOR_FOR_MORE_THAN_30_DAYS("CLOSED_NOT_UPDATE_OR_CONFIRM_DATA_IN_INFODEBIT_BY_CREDITOR_FOR_MORE_THAN_30_DAYS");

    private String value;

    DebtStageEnum(String value) {
      this.value = value;
    }

    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    public static DebtStageEnum fromValue(String value) {
      for (DebtStageEnum b : DebtStageEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }

    public static class Adapter extends TypeAdapter<DebtStageEnum> {
      @Override
      public void write(final JsonWriter jsonWriter, final DebtStageEnum enumeration) throws IOException {
        jsonWriter.value(enumeration.getValue());
      }

      @Override
      public DebtStageEnum read(final JsonReader jsonReader) throws IOException {
        String value =  jsonReader.nextString();
        return DebtStageEnum.fromValue(value);
      }
    }
  }

  public static final String SERIALIZED_NAME_DEBT_STAGE = "debtStage";
  @SerializedName(SERIALIZED_NAME_DEBT_STAGE)
  private DebtStageEnum debtStage;

  public static final String SERIALIZED_NAME_GUARANTORS_COUNT = "guarantorsCount";
  @SerializedName(SERIALIZED_NAME_GUARANTORS_COUNT)
  private Long guarantorsCount;

  public static final String SERIALIZED_NAME_CREDITOR_IDNP = "creditorIdnp";
  @SerializedName(SERIALIZED_NAME_CREDITOR_IDNP)
  private String creditorIdnp;

  public static final String SERIALIZED_NAME_CREDITOR_NAME = "creditorName";
  @SerializedName(SERIALIZED_NAME_CREDITOR_NAME)
  private String creditorName;


  public DebtDTO id(Long id) {
    
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


  public DebtDTO referenceNumber(String referenceNumber) {
    
    this.referenceNumber = referenceNumber;
    return this;
  }

   /**
   * Get referenceNumber
   * @return referenceNumber
  **/
  @ApiModelProperty(required = true, value = "")

  public String getReferenceNumber() {
    return referenceNumber;
  }


  public void setReferenceNumber(String referenceNumber) {
    this.referenceNumber = referenceNumber;
  }


  public DebtDTO clientIDNP(String clientIDNP) {
    
    this.clientIDNP = clientIDNP;
    return this;
  }

   /**
   * Get clientIDNP
   * @return clientIDNP
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public String getClientIDNP() {
    return clientIDNP;
  }


  public void setClientIDNP(String clientIDNP) {
    this.clientIDNP = clientIDNP;
  }


  public DebtDTO debtorIDNP(String debtorIDNP) {
    
    this.debtorIDNP = debtorIDNP;
    return this;
  }

   /**
   * Get debtorIDNP
   * @return debtorIDNP
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public String getDebtorIDNP() {
    return debtorIDNP;
  }


  public void setDebtorIDNP(String debtorIDNP) {
    this.debtorIDNP = debtorIDNP;
  }


  public DebtDTO debtorType(DebtorTypeEnum debtorType) {
    
    this.debtorType = debtorType;
    return this;
  }

   /**
   * Get debtorType
   * @return debtorType
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public DebtorTypeEnum getDebtorType() {
    return debtorType;
  }


  public void setDebtorType(DebtorTypeEnum debtorType) {
    this.debtorType = debtorType;
  }


  public DebtDTO contractNumber(String contractNumber) {
    
    this.contractNumber = contractNumber;
    return this;
  }

   /**
   * Get contractNumber
   * @return contractNumber
  **/
  @ApiModelProperty(required = true, value = "")

  public String getContractNumber() {
    return contractNumber;
  }


  public void setContractNumber(String contractNumber) {
    this.contractNumber = contractNumber;
  }


  public DebtDTO creditDuration(Integer creditDuration) {
    
    this.creditDuration = creditDuration;
    return this;
  }

   /**
   * Get creditDuration
   * @return creditDuration
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public Integer getCreditDuration() {
    return creditDuration;
  }


  public void setCreditDuration(Integer creditDuration) {
    this.creditDuration = creditDuration;
  }


  public DebtDTO initialAmount(Float initialAmount) {
    
    this.initialAmount = initialAmount;
    return this;
  }

   /**
   * Get initialAmount
   * @return initialAmount
  **/
  @ApiModelProperty(required = true, value = "")

  public Float getInitialAmount() {
    return initialAmount;
  }


  public void setInitialAmount(Float initialAmount) {
    this.initialAmount = initialAmount;
  }


  public DebtDTO interestRate(Float interestRate) {
    
    this.interestRate = interestRate;
    return this;
  }

   /**
   * Get interestRate
   * @return interestRate
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public Float getInterestRate() {
    return interestRate;
  }


  public void setInterestRate(Float interestRate) {
    this.interestRate = interestRate;
  }


  public DebtDTO currency(CurrencyEnum currency) {
    
    this.currency = currency;
    return this;
  }

   /**
   * Get currency
   * @return currency
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public CurrencyEnum getCurrency() {
    return currency;
  }


  public void setCurrency(CurrencyEnum currency) {
    this.currency = currency;
  }


  public DebtDTO total(Float total) {
    
    this.total = total;
    return this;
  }

   /**
   * Get total
   * @return total
  **/
  @ApiModelProperty(required = true, value = "")

  public Float getTotal() {
    return total;
  }


  public void setTotal(Float total) {
    this.total = total;
  }


  public DebtDTO fines(Float fines) {
    
    this.fines = fines;
    return this;
  }

   /**
   * Get fines
   * @return fines
  **/
  @ApiModelProperty(required = true, value = "")

  public Float getFines() {
    return fines;
  }


  public void setFines(Float fines) {
    this.fines = fines;
  }


  public DebtDTO paid(Float paid) {
    
    this.paid = paid;
    return this;
  }

   /**
   * Get paid
   * @return paid
  **/
  @ApiModelProperty(required = true, value = "")

  public Float getPaid() {
    return paid;
  }


  public void setPaid(Float paid) {
    this.paid = paid;
  }


  public DebtDTO agreementNumber(String agreementNumber) {
    
    this.agreementNumber = agreementNumber;
    return this;
  }

   /**
   * Get agreementNumber
   * @return agreementNumber
  **/
  @ApiModelProperty(required = true, value = "")

  public String getAgreementNumber() {
    return agreementNumber;
  }


  public void setAgreementNumber(String agreementNumber) {
    this.agreementNumber = agreementNumber;
  }


  public DebtDTO agreementDate(OffsetDateTime agreementDate) {
    
    this.agreementDate = agreementDate;
    return this;
  }

   /**
   * Get agreementDate
   * @return agreementDate
  **/
  @ApiModelProperty(required = true, value = "")

  public OffsetDateTime getAgreementDate() {
    return agreementDate;
  }


  public void setAgreementDate(OffsetDateTime agreementDate) {
    this.agreementDate = agreementDate;
  }


  public DebtDTO invoiceDate(OffsetDateTime invoiceDate) {
    
    this.invoiceDate = invoiceDate;
    return this;
  }

   /**
   * Get invoiceDate
   * @return invoiceDate
  **/
  @ApiModelProperty(required = true, value = "")

  public OffsetDateTime getInvoiceDate() {
    return invoiceDate;
  }


  public void setInvoiceDate(OffsetDateTime invoiceDate) {
    this.invoiceDate = invoiceDate;
  }


  public DebtDTO invoiceAmount(Float invoiceAmount) {
    
    this.invoiceAmount = invoiceAmount;
    return this;
  }

   /**
   * Get invoiceAmount
   * @return invoiceAmount
  **/
  @ApiModelProperty(required = true, value = "")

  public Float getInvoiceAmount() {
    return invoiceAmount;
  }


  public void setInvoiceAmount(Float invoiceAmount) {
    this.invoiceAmount = invoiceAmount;
  }


  public DebtDTO debtAmount(Float debtAmount) {
    
    this.debtAmount = debtAmount;
    return this;
  }

   /**
   * Get debtAmount
   * @return debtAmount
  **/
  @ApiModelProperty(required = true, value = "")

  public Float getDebtAmount() {
    return debtAmount;
  }


  public void setDebtAmount(Float debtAmount) {
    this.debtAmount = debtAmount;
  }


  public DebtDTO debtStage(DebtStageEnum debtStage) {
    
    this.debtStage = debtStage;
    return this;
  }

   /**
   * Get debtStage
   * @return debtStage
  **/
  @javax.annotation.Nullable
  @ApiModelProperty(value = "")

  public DebtStageEnum getDebtStage() {
    return debtStage;
  }


  public void setDebtStage(DebtStageEnum debtStage) {
    this.debtStage = debtStage;
  }


  public DebtDTO guarantorsCount(Long guarantorsCount) {
    
    this.guarantorsCount = guarantorsCount;
    return this;
  }

   /**
   * Get guarantorsCount
   * @return guarantorsCount
  **/
  @ApiModelProperty(required = true, value = "")

  public Long getGuarantorsCount() {
    return guarantorsCount;
  }


  public void setGuarantorsCount(Long guarantorsCount) {
    this.guarantorsCount = guarantorsCount;
  }


  public DebtDTO creditorIdnp(String creditorIdnp) {
    
    this.creditorIdnp = creditorIdnp;
    return this;
  }

   /**
   * Get creditorIdnp
   * @return creditorIdnp
  **/
  @ApiModelProperty(required = true, value = "")

  public String getCreditorIdnp() {
    return creditorIdnp;
  }


  public void setCreditorIdnp(String creditorIdnp) {
    this.creditorIdnp = creditorIdnp;
  }


  public DebtDTO creditorName(String creditorName) {
    
    this.creditorName = creditorName;
    return this;
  }

   /**
   * Get creditorName
   * @return creditorName
  **/
  @ApiModelProperty(required = true, value = "")

  public String getCreditorName() {
    return creditorName;
  }


  public void setCreditorName(String creditorName) {
    this.creditorName = creditorName;
  }


  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DebtDTO debtDTO = (DebtDTO) o;
    return Objects.equals(this.id, debtDTO.id) &&
        Objects.equals(this.referenceNumber, debtDTO.referenceNumber) &&
        Objects.equals(this.clientIDNP, debtDTO.clientIDNP) &&
        Objects.equals(this.debtorIDNP, debtDTO.debtorIDNP) &&
        Objects.equals(this.debtorType, debtDTO.debtorType) &&
        Objects.equals(this.contractNumber, debtDTO.contractNumber) &&
        Objects.equals(this.creditDuration, debtDTO.creditDuration) &&
        Objects.equals(this.initialAmount, debtDTO.initialAmount) &&
        Objects.equals(this.interestRate, debtDTO.interestRate) &&
        Objects.equals(this.currency, debtDTO.currency) &&
        Objects.equals(this.total, debtDTO.total) &&
        Objects.equals(this.fines, debtDTO.fines) &&
        Objects.equals(this.paid, debtDTO.paid) &&
        Objects.equals(this.agreementNumber, debtDTO.agreementNumber) &&
        Objects.equals(this.agreementDate, debtDTO.agreementDate) &&
        Objects.equals(this.invoiceDate, debtDTO.invoiceDate) &&
        Objects.equals(this.invoiceAmount, debtDTO.invoiceAmount) &&
        Objects.equals(this.debtAmount, debtDTO.debtAmount) &&
        Objects.equals(this.debtStage, debtDTO.debtStage) &&
        Objects.equals(this.guarantorsCount, debtDTO.guarantorsCount) &&
        Objects.equals(this.creditorIdnp, debtDTO.creditorIdnp) &&
        Objects.equals(this.creditorName, debtDTO.creditorName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, referenceNumber, clientIDNP, debtorIDNP, debtorType, contractNumber, creditDuration, initialAmount, interestRate, currency, total, fines, paid, agreementNumber, agreementDate, invoiceDate, invoiceAmount, debtAmount, debtStage, guarantorsCount, creditorIdnp, creditorName);
  }


  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DebtDTO {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    referenceNumber: ").append(toIndentedString(referenceNumber)).append("\n");
    sb.append("    clientIDNP: ").append(toIndentedString(clientIDNP)).append("\n");
    sb.append("    debtorIDNP: ").append(toIndentedString(debtorIDNP)).append("\n");
    sb.append("    debtorType: ").append(toIndentedString(debtorType)).append("\n");
    sb.append("    contractNumber: ").append(toIndentedString(contractNumber)).append("\n");
    sb.append("    creditDuration: ").append(toIndentedString(creditDuration)).append("\n");
    sb.append("    initialAmount: ").append(toIndentedString(initialAmount)).append("\n");
    sb.append("    interestRate: ").append(toIndentedString(interestRate)).append("\n");
    sb.append("    currency: ").append(toIndentedString(currency)).append("\n");
    sb.append("    total: ").append(toIndentedString(total)).append("\n");
    sb.append("    fines: ").append(toIndentedString(fines)).append("\n");
    sb.append("    paid: ").append(toIndentedString(paid)).append("\n");
    sb.append("    agreementNumber: ").append(toIndentedString(agreementNumber)).append("\n");
    sb.append("    agreementDate: ").append(toIndentedString(agreementDate)).append("\n");
    sb.append("    invoiceDate: ").append(toIndentedString(invoiceDate)).append("\n");
    sb.append("    invoiceAmount: ").append(toIndentedString(invoiceAmount)).append("\n");
    sb.append("    debtAmount: ").append(toIndentedString(debtAmount)).append("\n");
    sb.append("    debtStage: ").append(toIndentedString(debtStage)).append("\n");
    sb.append("    guarantorsCount: ").append(toIndentedString(guarantorsCount)).append("\n");
    sb.append("    creditorIdnp: ").append(toIndentedString(creditorIdnp)).append("\n");
    sb.append("    creditorName: ").append(toIndentedString(creditorName)).append("\n");
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


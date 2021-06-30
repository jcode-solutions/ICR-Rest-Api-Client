package infodebit.rest.client.auth;

import infodebit.rest.client.Pair;

import java.util.List;
import java.util.Map;

@javax.annotation.Generated(value = "org.openapitools.codegen.languages.JavaClientCodegen", date = "2020-06-23T13:09:16.929+03:00[EET]")
public class OAuth implements Authentication {
  private String accessToken;

  public String getAccessToken() {
    return accessToken;
  }

  public void setAccessToken(String accessToken) {
    this.accessToken = accessToken;
  }

  @Override
  public void applyToParams(List<Pair> queryParams, Map<String, String> headerParams, Map<String, String> cookieParams) {
    if (accessToken != null) {
      headerParams.put("Authorization", "Bearer " + accessToken);
    }
  }

}
package io.virtualan.cucumblan.core;

import static io.restassured.RestAssured.given;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.virtualan.cucumblan.exception.ParserError;
import io.virtualan.cucumblan.parser.OpenAPIParser;
import io.virtualan.cucumblan.props.ApplicationConfiguration;
import io.virtualan.cucumblan.props.EndpointConfiguration;
import io.virtualan.cucumblan.props.ExcludeConfiguration;
import io.virtualan.cucumblan.props.util.HelperUtil;
import io.virtualan.cucumblan.props.util.ScenarioContext;
import io.virtualan.cucumblan.props.util.StepDefinitionHelper;
import io.virtualan.cucumblan.script.ExcelAndMathHelper;
import io.virtualan.mapson.Mapson;
import io.virtualan.util.Helper;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.xmlbeans.impl.util.Base64;
import org.junit.Assert;


/**
 * The type Base step definition.
 *
 * @author Elan Thangamani
 */
public class BaseStepDefinition {

  private final static Logger LOGGER = Logger.getLogger(BaseStepDefinition.class.getName());

  static {
    try {
      OpenAPIParser.loader();
      EndpointConfiguration.getInstance().loadEndpoints();
    } catch (ParserError parserError) {
      LOGGER
          .warning("Unable to start the process - see if conf folder and endpoints are generated");
      System.exit(-1);
    }
  }

  private Response response;
  private ValidatableResponse json;
  private String jsonBody;
  private RequestSpecification request = given();

  /**
   * Read request by path param.
   *
   * @param dummy      the dummy
   * @param identifier the identifier
   * @param value      the value
   */
  @Given("^(.*) with an path param (.*) of (.*)")
  public void readRequestByPathParam(String dummy, String identifier, String value) {
    request = given().pathParam(identifier, StepDefinitionHelper.getActualValue(value));
  }

  /**
   * Read request by path param.
   *
   * @param identifier the identifier
   * @param value      the value
   */
  @Given("^enable cert for (.*) of (.*)")
  public void cert(String identifier, String value) {
    RestAssured.authentication = RestAssured.certificate(identifier, value);
  }


  /**
   * Read request by path param.
   *
   * @param username the identifier
   * @param password the value
   */
  @Given("^basic authentication with (.*) and (.*)")
  public void auth(String username, String password) {
    byte[] authBasic = Base64.encode(String
        .format("%s:%s", StepDefinitionHelper.getActualValue(username),
            StepDefinitionHelper.getActualValue(password)).getBytes());
    request.header("Authorization", String.format("Basic %s", new String(authBasic)));
  }

  /**
   * Read request by path param.
   *
   * @param auth  the identifier
   * @param token the value
   */
  @Given("^(.*) auth with (.*) token$")
  public void bearer(String auth, String token) {
    request.header("Authorization", String
        .format("%s %s", auth, Helper.getActualValueForAll(token, ScenarioContext.getContext())));
  }

  /**
   * Read request by path param.
   *
   * @param dummy the dummy
   */
  @Given("^(.*) perform a api action")
  public void readRequestByPathParam(String dummy) {
    request = given();
  }

  /**
   * Read request by header param.
   *
   * @param dummy      the dummy
   * @param identifier the identifier
   * @param value      the value
   */
  @Given("^(.*) with an header param (.*) of (.*)")
  public void readRequestByHeaderParam(String dummy, String identifier, String value) {
    request = request.header(identifier, StepDefinitionHelper.getActualValue(value));
  }


  /**
   * Read request.
   *
   * @param nameIgnore   the name ignore
   * @param parameterMap the parameter map
   */
  @Given("add (.*) with given header params$")
  public void readAllHeaderParams(String nameIgnore, Map<String, String> parameterMap) {
    for (Map.Entry<String, String> params : parameterMap.entrySet()) {
      request = request
          .header(params.getKey(), StepDefinitionHelper.getActualValue(params.getValue()));
    }
  }

  /**
   * Read request by query param.
   *
   * @param dummy      the dummy
   * @param identifier the identifier
   * @param value      the value
   */
  @Given("^(.*) with an query param (.*) of (.*)")
  public void readRequestByQueryParam(String dummy, String identifier, String value) {
    request = given().queryParam(identifier, StepDefinitionHelper.getActualValue(value));
  }

  /**
   * Load global param.
   *
   * @param globalParams the global params
   * @throws IOException the io exception
   */
  @Given("^Provided all the feature level parameters$")
  public void loadGlobalParam(Map<String, String> globalParams) throws IOException {
    ScenarioContext.setContext(globalParams);
  }

  /**
   * Load global param.
   *
   * @throws IOException the io exception
   */
  @Given("^Provided all the feature level parameters from file$")
  public void loadGlobalParamFromFile() throws IOException {
    Properties properties = new Properties();
    InputStream stream = ApplicationConfiguration.class.getClassLoader()
        .getResourceAsStream("cucumblan-env.properties");
    if (stream != null) {
      properties.load(stream);
      ScenarioContext.setContext((Map) properties);
    } else {
      LOGGER.warning(
          "cucumblan-env.properties is not configured. Need to add if default data loaded");
    }
  }

  /**
   * Validate global param.
   */
  @Then("^Verify all the feature level parameters exists")
  public void validateGlobalParam() {
    assertTrue("Valid Global Parameters are present ", ScenarioContext.hasContextValues());
  }


  /**
   * Add variable.
   *
   * @param responseValue the response value
   * @param key           the key
   */
  @Given("^Add the (.*) value of the key as (.*)")
  public void addVariable(String responseValue, String key) {
    ScenarioContext.setContext(key,
        Helper.getActualValueForAll(responseValue, ScenarioContext.getContext()).toString());
  }

  /**
   * Modify variable.
   *
   * @param responseValue the response value
   * @param key           the key
   * @throws IOException the io exception
   */
  @Given("^evaluate the (.*) decimal value of the key as (.*)")
  public void modifyDecimalVariable(String responseValue, String key) throws IOException {
    ScenarioContext.setContext(key, ExcelAndMathHelper.evaluateWithVariables(Double.class,
        responseValue, ScenarioContext.getContext()).toString());
  }


  /**
   * Modify variable.
   *
   * @param responseValue the response value
   * @param key           the key
   * @throws IOException the io exception
   */
  @Given("^evaluate the (.*) integer value of the key as (.*)")
  public void modifyIntVariable(String responseValue, String key) throws IOException {
    ScenarioContext.setContext(key, ExcelAndMathHelper.evaluateWithVariables(Integer.class,
        responseValue, ScenarioContext.getContext()).toString());
  }

  /**
   * Modify variable.
   *
   * @param responseValue the response value
   * @param key           the key
   * @throws IOException the io exception
   */
  @Given("^evaluate the (.*) boolean value of the key as (.*)")
  public void modifyBooleanVariable(String responseValue, String key) throws IOException {
    ScenarioContext.setContext(key, ExcelAndMathHelper.evaluateWithVariables(Boolean.class,
        responseValue, ScenarioContext.getContext()).toString());
  }


  /**
   * Modify variable.
   *
   * @param responseValue the response value
   * @param key           the key
   * @throws IOException the io exception
   */
  @Given("^Modify the (.*) value of the key as (.*)")
  public void modifyStringVariable(String responseValue, String key) throws IOException {
    ScenarioContext.setContext(key,
        Helper.getActualValueForAll(responseValue, ScenarioContext.getContext()).toString());
  }

  /**
   * Load as global param.
   *
   * @param responseKey the response key
   * @param key         the key
   */
  @Given("^Store the (.*) value of the key as (.*)")
  public void loadAsGlobalParam(String responseKey, String key) {
    ScenarioContext.setContext(key, json.extract().body().jsonPath().getString(responseKey));
  }


  /**
   * Read request.
   *
   * @param nameIgnore   the name ignore
   * @param parameterMap the parameter map
   */
  @Given("^add (.*) with given path params$")
  public void readPathParamsRequest(String nameIgnore, Map<String, String> parameterMap)
       {
    request = request.contentType("application/json");
    for (Map.Entry<String, String> params : parameterMap.entrySet()) {
      request = request
          .pathParam(params.getKey(), StepDefinitionHelper.getActualValue(params.getValue()));
    }
  }


  /**
   * Read request.
   *
   * @param nameIgnore   the name ignore
   * @param parameterMap the parameter map
   */
  @Given("add (.*) with given query params$")
  public void readRequest(String nameIgnore, Map<String, String> parameterMap) {
    request = request.contentType("application/json");
    for (Map.Entry<String, String> params : parameterMap.entrySet()) {
      request = request
          .queryParam(params.getKey(), StepDefinitionHelper.getActualValue(params.getValue()));
    }
  }

  /**
   * Read request.
   *
   * @param nameIgnore   the name ignore
   * @param contentType  the content type
   * @param parameterMap the parameter map
   */
  @Given("add (.*) with contentType (.*) given query params$")
  public void readRequest(String nameIgnore, String contentType, Map<String, String> parameterMap) {
    request = request.contentType(contentType);
    for (Map.Entry<String, String> params : parameterMap.entrySet()) {
      request = request
          .queryParam(params.getKey(), StepDefinitionHelper.getActualValue(params.getValue()));
    }
  }

  /**
   * Load request.
   *
   * @param nameIgnore   the name ignore
   * @param contentType  the content type
   * @param parameterMap the parameter map
   * @throws Exception the exception
   */
  @Given("^Populate (.*) with contentType(.*) given input$")
  public void loadRequest(String nameIgnore, String contentType, Map<String, String> parameterMap) {
    request = request.contentType(contentType);
    for (Map.Entry<String, String> params : parameterMap.entrySet()) {
      request = request
          .queryParam(params.getKey(), StepDefinitionHelper.getActualValue(params.getValue()));
    }
  }

  /**
   * Load request.
   *
   * @param nameIgnore   the name ignore
   * @param parameterMap the parameter map
   * @throws Exception the exception
   */
  @Given("^Populate (.*) with given input$")
  public void loadRequest(String nameIgnore, Map<String, String> parameterMap) {
    request = request.contentType("application/json");
    for (Map.Entry<String, String> params : parameterMap.entrySet()) {
      request = request
          .queryParam(params.getKey(), StepDefinitionHelper.getActualValue(params.getValue()));
    }
  }

  /**
   * Create request.
   *
   * @param body        the body
   * @param contentType the content type
   */
  @Given("^add (.*) data with (.*) given input$")
  public void createRequest(String body, String contentType) {
    request = request.contentType(contentType).body(body);
  }


  /**
   * Create request.
   *
   * @param fileBody    the body
   * @param contentType the content type
   * @throws IOException the io exception
   */
  @Given("add (.*) data file with (.*) given input$")
  public void createFileRequest(String fileBody, String contentType) throws IOException {
    String body = HelperUtil.readFileAsString(fileBody);
    if (body != null) {
      Map<String, String> mapHeader = new HashMap();
      mapHeader.put("content-type", contentType);
      request = request.headers(mapHeader).contentType(contentType).body(body);
    } else {
      Assert.assertTrue(fileBody +" input file is missing ", false);
    }
  }

  /**
   * Create request.
   *
   * @param fileBody    the body
   * @param contentType the content type
   * @param input       the input
   * @throws IOException the io exception
   */
  @Given("add (.*) data inline with (.*) given input$")
  public void createInlineRequest(String fileBody, String contentType, List<String> input) throws IOException {
    if (input != null && !input.isEmpty()) {
      Map<String, String> mapHeader = new HashMap();
      mapHeader.put("content-type", contentType);
      String listString = input.stream().map(Object::toString)
          .collect(Collectors.joining());
      request = request.headers(mapHeader).contentType(contentType).body(listString);
    } else {
      Assert.assertTrue(fileBody +" input inline is missing ", false);
    }
  }


  /**
   * Create request.
   *
   * @param nameIgnore   the name ignore
   * @param contentType  the content type
   * @param parameterMap the parameter map
   * @throws Exception the exception
   */
  @Given("^Create (.*) with contentType (.*) given input$")
  public void createRequest(String nameIgnore, String contentType, Map<String, String> parameterMap)
      throws Exception {
    jsonBody = Mapson.buildMAPsonAsJson(parameterMap, ScenarioContext.getContext());
    request = request.contentType(contentType).body(jsonBody);
  }


  /**
   * Create request.
   *
   * @param nameIgnore   the name ignore
   * @param parameterMap the parameter map
   * @throws Exception the exception
   */
  @Given("^Create (.*) with given input$")
  public void createRequest(String nameIgnore, Map<String, String> parameterMap) throws Exception {
    jsonBody = Mapson.buildMAPsonAsJson(parameterMap, ScenarioContext.getContext());
    request = request.contentType("application/json").body(jsonBody);
  }

  /**
   * Update request.
   *
   * @param nameIgnore   the name ignore
   * @param parameterMap the parameter map
   * @throws Exception the exception
   */
  @Given("^Update (.*) with given input$")
  public void updateRequest(String nameIgnore, Map<String, String> parameterMap) throws Exception {
    jsonBody = Mapson.buildMAPsonAsJson(parameterMap, ScenarioContext.getContext());
    request = request.contentType("application/json").body(jsonBody);
  }

  /**
   * Update request.
   *
   * @param nameIgnore   the name ignore
   * @param contentType  the content type
   * @param parameterMap the parameter map
   * @throws Exception the exception
   */
  @Given("^Update (.*) with contentType (.*) given input$")
  public void updateRequest(String nameIgnore, String contentType, Map<String, String> parameterMap)
      throws Exception {
    jsonBody = Mapson.buildMAPsonAsJson(parameterMap, ScenarioContext.getContext());
    request = request.contentType(contentType).body(jsonBody);
  }


  /**
   * Create request.
   *
   * @param dummyString       the dummy string
   * @param acceptContentType the accept content type
   * @param resource          the resource
   * @param system            the system
   */
  @When("^(.*) post (.*) in (.*) resource on (.*)")
  public void createRequest(String dummyString, String acceptContentType, String resource,
      String system) {
    response = request.baseUri(StepDefinitionHelper.getHostName(resource, system)).when()
        .log().all()
        .accept(acceptContentType)
        .post(StepDefinitionHelper.getActualResource(resource, system));
  }

  /**
   * Read request.
   *
   * @param dummyString       the dummy string
   * @param acceptContentType the accept content type
   * @param resource          the resource
   * @param system            the system
   */
  @When("^(.*) get (.*) in (.*) resource on (.*)")
  public void readRequest(String dummyString, String acceptContentType, String resource,
      String system) {
    response = request.baseUri(StepDefinitionHelper.getHostName(resource, system)).when()
        .log().all().accept(acceptContentType)
        .get(StepDefinitionHelper.getActualResource(resource, system));
  }

  /**
   * Modify request.
   *
   * @param dummyString       the dummy string
   * @param acceptContentType the accept content type
   * @param resource          the resource
   * @param system            the system
   */
  @When("^(.*) put (.*) in (.*) resource on (.*)")
  public void modifyRequest(String dummyString, String acceptContentType, String resource,
      String system) {
    response = request.baseUri(StepDefinitionHelper.getHostName(resource, system)).when()
        .log().all().accept(acceptContentType)
        .put(StepDefinitionHelper.getActualResource(resource, system));
  }

  /**
   * Pathch request.
   *
   * @param dummyString       the dummy string
   * @param acceptContentType the accept content type
   * @param resource          the resource
   * @param system            the system
   */
  @When("^(.*) patch (.*) in (.*) resource on (.*)")
  public void patchRequest(String dummyString, String acceptContentType, String resource,
      String system) {
    response = request.baseUri(StepDefinitionHelper.getHostName(resource, system)).when()
        .log().all().accept(acceptContentType)
        .patch(StepDefinitionHelper.getActualResource(resource, system));
  }

  /**
   * Delete by id.
   *
   * @param dummyString       the dummy string
   * @param acceptContentType the accept content type
   * @param resource          the resource
   * @param system            the system
   */
  @When("^(.*) delete (.*) in (.*) resource on (.*)")
  public void deleteById(String dummyString, String acceptContentType, String resource,
      String system) {
    response = request.baseUri(StepDefinitionHelper.getHostName(resource, system)).when()
        .log().all().accept(acceptContentType)
        .delete(StepDefinitionHelper.getActualResource(resource, system));
  }


  /**
   * Verify status code.
   *
   * @param statusCode the status code
   */
  @Then("^Verify the status code is (\\d+)")
  public void verifyStatusCode(int statusCode) {
    json = response.then().log().ifValidationFails().statusCode(statusCode);
    LOGGER.info(ScenarioContext.getContext().toString());
    LOGGER.info(json.extract().body().asString());
  }

  /**
   * Verify response.
   *
   * @param resource the resource
   * @param data     the data
   * @throws Throwable the throwable
   */
  @And("^Verify-all (.*) api includes following in the response$")
  public void verifyResponseMapson(String resource, DataTable data) throws Throwable {
    data.asMap(String.class, String.class).forEach((k, v) -> {
      if (!ExcludeConfiguration.shouldSkip(resource, (String) k)) {
        Map<String, String> mapson = Mapson.buildMAPsonFromJson(json.extract().body().asString());
        if (v == null) {
          if (mapson.get(k) == null) {
            assertNull(mapson.get(k));
          } else {
            assertEquals(" ", mapson.get(k));
          }
        } else {
          LOGGER.info("Key: " + k + "  Expected : " + v + " ==> Actual " + mapson.get(k));
          assertEquals(v, mapson.get(k));
        }
      }
    });
  }

  /**
   * Mock single response.
   *
   * @param resource  the resource
   * @param xmlString the xml string
   * @throws Throwable the throwable
   */
  @And("^Verify (.*) response inline includes in the response$")
  public void verifyFileResponse(String resource, List<String> xmlString) throws Throwable {
    String listString = xmlString.stream().map(Object::toString)
        .collect(Collectors.joining());
    HelperUtil.assertXMLEquals(listString, response.asString());
  }

  /**
   * Mock single response.
   *
   * @param resource the resource
   * @param fileBody the file body
   * @throws Throwable the throwable
   */
  @And("^Verify (.*) response XML File (.*) includes in the response$")
  public void verifyXMLResponse(String resource, String fileBody)
      throws Throwable {
    String body = HelperUtil.readFileAsString(fileBody);
    if (body != null) {
      HelperUtil.assertXMLEquals(body, response.asString());
    } else {
			Assert.assertTrue(fileBody + "  file is missing :", false );
    }
  }


  /**
   * Mock single response.
   *
   * @param resource the resource
   * @param context  the context
   * @throws Throwable the throwable
   */
  @And("^Verify (.*) response with (.*) includes in the response$")
  public void verifySingleResponse(String resource, String context) throws Throwable {
    assertEquals(context, json.extract().body().asString());
  }


  /**
   * Verify response.
   *
   * @param dummyString the dummy string
   * @param data        the data
   * @throws Throwable the throwable
   */
  @And("^Verify (.*) includes following in the response$")
  public void verifyResponse(String dummyString, DataTable data) throws Throwable {
    data.asMap(String.class, String.class).forEach((k, v) -> {
      System.out.println(v + " : " + json.extract().body().jsonPath().getString((String) k));
      assertEquals(StepDefinitionHelper.getActualValue((String) v),
          json.extract().body().jsonPath().getString((String) k));
    });
  }
}

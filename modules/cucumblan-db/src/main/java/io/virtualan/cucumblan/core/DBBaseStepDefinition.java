/*
 *
 *
 *    Copyright (c) 2022.  Virtualan Contributors (https://virtualan.io)
 *
 *     Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *     in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software distributed under the License
 *     is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *     or implied. See the License for the specific language governing permissions and limitations under
 *     the License.
 *
 *
 *
 */
package io.virtualan.cucumblan.core;

import io.cucumber.java.Before;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.virtualan.csvson.Csvson;
import io.virtualan.cucumblan.jdbc.util.StreamingJsonResultSetExtractor;
import io.virtualan.cucumblan.props.ApplicationConfiguration;
import io.virtualan.cucumblan.props.util.ScenarioContext;
import io.virtualan.cucumblan.props.util.StepDefinitionHelper;
import io.virtualan.cucumblan.props.util.UtilHelper;
import io.virtualan.mapson.Mapson;
import org.apache.commons.dbcp2.BasicDataSource;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.skyscreamer.jsonassert.JSONCompare;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.springframework.jdbc.core.JdbcTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Collectors;


/*
 *
 *
 *    Copyright (c) 2022.  Virtualan Contributors (https://virtualan.io)
 *
 *     Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *     in compliance with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software distributed under the License
 *     is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 *     or implied. See the License for the specific language governing permissions and limitations under
 *     the License.
 *
 *
 *
 */


/**
 * The type JDBC base step definition.
 *
 * @author Elan Thangamani
 */
public class DBBaseStepDefinition {

    private final static Logger LOGGER = Logger.getLogger(DBBaseStepDefinition.class.getName());
    /**
     * The Jdbc template map.
     */
    static Map<String, JdbcTemplate> jdbcTemplateMap = new HashMap<String, JdbcTemplate>();

    String sqlJson = null;
    /**
     * The Scenario.
     */
    Scenario scenario;
    private boolean skipScenario = false;

    public static void loadAllDataSource() {
        try {
            for (String key : ApplicationConfiguration.getProperties().keySet()) {
                if (key.contains(".cucumblan.jdbc.driver-class-name")) {
                    String source = key.replaceAll(".cucumblan.jdbc.driver-class-name", "");
                    if (!jdbcTemplateMap.containsKey(source)) {
                        BasicDataSource dataSource = new BasicDataSource();
                        dataSource.setDriverClassName(
                                StepDefinitionHelper.getActualValue(ApplicationConfiguration
                                        .getProperty(source + ".cucumblan.jdbc.driver-class-name")));
                        dataSource
                                .setUsername(
                                        StepDefinitionHelper.getActualValue(
                                                ApplicationConfiguration.getProperty(source + ".cucumblan.jdbc.username")));
                        dataSource
                                .setPassword(
                                        StepDefinitionHelper.getActualValue(
                                                ApplicationConfiguration.getProperty(source + ".cucumblan.jdbc.password")));
                        dataSource.setUrl(StepDefinitionHelper.getActualValue(
                                ApplicationConfiguration.getProperty(source + ".cucumblan.jdbc.url")));
                        dataSource.setMaxIdle(5);
                        dataSource.setInitialSize(5);
                        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);
                        jdbcTemplateMap.put(source, jdbcTemplate);
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.severe("Unable to load properties :" + e.getMessage());
        }
    }

    /**
     * Before.
     *
     * @param scenario the scenario
     */
    @Before
    public void before(Scenario scenario) {
        this.scenario = scenario;
        this.skipScenario = false;
        this.sqlJson = null;
        if (jdbcTemplateMap.isEmpty()) {
            loadAllDataSource();
        }
    }

    /**
     * given sql.
     *
     * @param dummy the dummy
     * @throws Exception the exception
     */
    @Given("as a user perform query (.*) action$")
    @Given("as a user perform sql (.*) action$")
    public void dummyGiven(String dummy) throws Exception {
    }

    /**
     * Insert sql.
     *
     * @param dummy    the dummy
     * @param resource the resource
     * @param sqls     the sqls
     * @throws Exception the exception
     */
    @Given("execute DDL for the given query (.*) on (.*)$")
    @Given("execute UPDATE for the given query (.*) on (.*)$")
    @Given("execute DELETE for the given query (.*) on (.*)$")
    @Given("execute INSERT for the given query (.*) on (.*)$")
    @Given("execute DDL for the given sql (.*) on (.*)$")
    @Given("execute UPDATE for the given (.*) on (.*)$")
    @Given("execute DELETE for the given (.*) on (.*)$")
    @Given("execute INSERT for the given (.*) on (.*)$")
    public void insertSql(String dummy, String resource, List<String> sqls) throws Exception {
        if (!this.skipScenario) {
            JdbcTemplate jdbcTemplate = getJdbcTemplate(resource);
            for (String sql : sqls) {
                try {
                    jdbcTemplate.execute(StepDefinitionHelper.getActualValue(sql));
                } catch (Exception e) {
                    LOGGER.warning("Unable to load " + dummy + " this sqls " + sql + " : " + e.getMessage());
                    scenario.log("Unable to load " + dummy + " this sqls " + sql + " : " + e.getMessage());
                    Assert.assertTrue(dummy + "  sqls are not inserted : (" + e.getMessage() + ")", false);
                }
            }
            Assert.assertTrue("All sqls are executed successfully", true);
        }
    }

    /**
     * perform the skip scenario
     *
     * @param condition the response value excel based
     * @throws IOException the io exception
     */
    @Given("^perform-db the (.*) condition to skip scenario")
    public void modifyBooleanVariable(String condition) throws Exception {
        skipScenario = (Boolean) io.virtualan.cucumblan.script.ExcelAndMathHelper
                .evaluateWithVariables(Boolean.class, condition, ScenarioContext
                        .getContext(String.valueOf(Thread.currentThread().getId())));
        scenario.log("condition :" + condition + " : is Skipped : " + skipScenario);
    }


    private JdbcTemplate getJdbcTemplate(String resource) throws Exception {
        if (jdbcTemplateMap.containsKey(resource)) {
            return jdbcTemplateMap.get(resource);
        } else {
            Assert.assertTrue("Jdbc sources are not defined in configuration for : " + resource, false);
            throw new Exception("Jdbc sources are not defined in configuration : ");
        }
    }

    @Given("^store (.*) as key and query's (.*) as value")
    public void storeSqlResponseAskeySwap(String key, String responseKey) throws JSONException {
        storeSqlResponseAskey(responseKey, key);
    }

    @Given("^store-sql's (.*) value of the key as (.*)")
    public void storeSqlResponseAskey(String responseKey, String key) throws JSONException {
        if (!this.skipScenario) {
            if (sqlJson != null) {
                Map<String, String> mapson = Mapson.buildMAPsonFromJson(sqlJson);
                if (mapson.get(responseKey) != null) {
                    ScenarioContext
                            .setContext(String.valueOf(Thread.currentThread().getId()), key,
                                    UtilHelper.getObject(mapson.get(responseKey)));
                } else {
                    Assert.assertTrue(responseKey + " not found in the sql ", false);
                }
            } else {
                Assert.assertTrue(" Sql query response not found for the executed query?  ", false);
            }
        }
    }


    /**
     * Verify.
     *
     * @param dummy1    the dummy 1
     * @param dummy     the dummy
     * @param resource  the resource
     * @param selectSql the select sql
     * @throws Exception the exception
     */
    @Given("verify (.*) with the given sql (.*) on (.*)$")
    @Given("validate (.*) given (.*) on (.*)$")
    public void verify(String dummy1, String dummy, String resource, List<String> selectSql)
            throws Exception {
        if (!this.skipScenario) {
            int index = 0;
            JdbcTemplate jdbcTemplate = getJdbcTemplate(resource);
            if (selectSql.size() >= 1 && selectSql.get(0).toLowerCase().startsWith("select")) {
                try {
                    sqlJson = getJson(resource,
                            StepDefinitionHelper.getActualValue(selectSql.get(0)));
                    index = 1;
                } catch (Exception e) {
                    Assert.assertTrue(" Invalid query?? " + e.getMessage(), false);
                }
            } else if (sqlJson != null) {
                index = 0;
            } else {
                Assert.assertTrue(" select query missing ", false);
            }
            scenario.attach(new JSONArray(sqlJson).toString(4), "application/json", "ActualQueryResponse");
            if (selectSql.size() == 1) {
                Assert.assertNull(sqlJson);
            } else {
                List<String> csvons = selectSql.subList(index, selectSql.size());
                JSONArray expectedArray = Csvson
                        .buildCSVson(csvons,
                                ScenarioContext.getContext(String.valueOf(Thread.currentThread().getId())));
                JSONArray actualArray = new JSONArray(sqlJson);
                JSONCompareResult result = JSONCompare
                        .compareJSON(actualArray, expectedArray, JSONCompareMode.LENIENT);
                scenario.attach(expectedArray.toString(4), "application/json", "ExpectedResponse:");
                if (result.failed()) {
                    scenario.log(result.getMessage());
                }
                Assert.assertTrue( " select sql and csvson record matches", result.passed());
            }
        }
    }

    /**
     * SELECT.
     *
     * @param dummy1    the dummy 1
     * @param dummy     the dummy
     * @param resource  the resource
     * @param selectSql the select sql
     * @throws Exception the exception
     */
    @Given("select (.*) with the given sql (.*) on (.*)$")
    @Given("read (.*) given (.*) on (.*)$")
    public void select(String dummy1, String dummy, String resource, List<String> selectSql)
            throws Exception {
        if (!this.skipScenario) {

            JdbcTemplate jdbcTemplate = getJdbcTemplate(resource);
            if (selectSql.size() >= 1) {
                try {
                    scenario.attach(
                            new JSONObject("{\"sql\" : \"" + StepDefinitionHelper.getActualValue(selectSql.stream().collect(Collectors.joining("\n"))) + "\", \"resource\" : \"" + resource + "\" }")
                                    .toString(4), "application/json", "SelectSql:");
                    sqlJson = getJson(resource,
                            StepDefinitionHelper.getActualValue(selectSql.stream().collect(Collectors.joining("\n"))));
                    scenario.attach(sqlJson, "application/json", "SqlResponse:");
                } catch (Exception e) {
                    Assert.assertTrue(" Invalid sql? " + e.getMessage(), false);
                }
            } else {
                Assert.assertTrue(" select sql missing ", false);
            }
        }
    }

    private String getJson(String resource, String sql) throws Exception {
        JdbcTemplate jdbcTemplate = getJdbcTemplate(resource);
        OutputStream os = new ByteArrayOutputStream();
        jdbcTemplate.query(sql, new StreamingJsonResultSetExtractor(os));
        return os.toString();
    }

}
/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.model.impl.jdbc;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DatabaseURL;
import org.jkiss.dbeaver.model.StringTemplate;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.stream.Collectors;

public class StringTemplateTest extends DBeaverUnitTest {

    static final String[] ALL_URL_TEMPLATES = new String[] {
        "jdbc:Altibase://{host}:{port}/{database}",
        "jdbc:athena://Region={region};",
        "jdbc:bigquery://{host}:{port}",
        "jdbc:clickhouse://{host}:{port}[/{database}]",
        "jdbc:CUBRID:{host}:{port}:{database}:::",
        "jdbc:dm://{host}[:{port}]",
        "jdbc:databend://{username}:{password}@{host}:{port}[/{database}]",
        "jdbc:databricks://{host}[:{port}][/{database}]",
        "jdbc:db2://{host}[:{port}]/{database}",
        "jdbc:as400://{host};[libraries={database};]",
        "jdbc:db2://{host}[:{port}]/{database}",
        "jdbc:denodo://{host}:{port}/{database}",
        "jdbc:mysql://{host}[:{port}]/[{database}]",
        "jdbc:duckdb:{file}",
        "jdbc:exa:{host}[:{port}][;schema={database}]",
        "jdbc:firebirdsql://{host}:{port}/{file}",
        "jdbc:postgresql://{host}[:{port}]/[{database}]",
        "jdbc:gbasedbt-sqli://{host}:{port}/{database}:GBASEDBTSERVER={server}",
        "jdbc:sapdb://{host}[:{port}]/{database}",
        "jdbc:postgresql://{host}[:{port}]/[{database}]",
        "jdbc:h2:tcp://{host}[:{port}]/{database}",
        "jdbc:sap://{host}[:{port}]",
        "jdbc:hive2://{host}[:{port}][/{database}]",
        "jdbc:iotdb://{host}:{port}/?sql_dialect={sqlDialect}",
        "jdbc:kingbase8://{host}[:{port}]/[{database}]",
        "jdbc:sqlserver://{host}[:{port}][;databaseName={database}]",
        "jdbc:mysql://{host}[:{port}]/[{database}]",
        "jdbc:mysql://{host}:{port}[/{database}]",
        "jdbc:ocient://{host}:{port}/{database}",
        "jdbc:oracle:thin:@{host}[:{port}]/{database}",
        "jdbc:postgresql://{host}[:{port}]/[{database}]",
        "jdbc:snowflake://{host}[:{port}]/?[db={database}]",
        "jdbc:cloudspanner:/projects/my-project/instances/my-instance/databases/my-database",
        "jdbc:sqlite:{file}",
        "jdbc:starrocks://{host}[:{port}]/[{database}]",
        "jdbc:mysql://{host}[:{port}]/[{database}]",
        "jdbc:timeplus://{host}:{port}[/{database}]",
        "jdbc:vertica://{host}:{port}/[{database}]",
        "jdbc:athena://Region={region};",
        "jdbc:bigquery://{host}:{port}",
        "http://{host}[:{port}]/[{database}]",
        "cql://{host}[:{port}]/{database}",
        "mongo://{host}[:{port}]/{database}",
        "AccountEndpoint={server}",
        "couchbase://{host}[:{port}]/[{database}]",
        "couchdb://{host}[:{port}]/[{database}]",
        "jdbc:postgresql://[{host}]:5432/postgres",
        "http://{host}[:{port}]/[{database}]",
        "jdbc:edb://{host}[:{port}]/[{database}]",
        "jdbc:dbeaver:file:csv:{file}",
        "firestore://{host}[:{port}]/{database}",
        "jdbc:postgresql:///[{database}]?cloudSqlInstance={host}",
        "jdbc:drill:zk={host}/{server}[;schema={database}]",
        "http://{host}[:{port}]/[{database}]",
        "http://{host}[:{port}]/[{database}]",
        "jdbc:informix-sqli://{host}:{port}/{database}:INFORMIXSERVER={server}",
        "kafka://{host}[:{port}]/[{database}]",
        "keyspaces://{host}",
        "mongo://{host}[:{port}]/{database}",
        "jdbc:sqlserver://{host}[:{port}][;databaseName={database}]",
        "jdbc:mysql://{host}[:{port}]/[{database}]",
        "jdbc:neo4j:bolt://{host}[:{port}]/",
        "jdbc:neptune:sqlgremlin://{host}[;port={port}]",
        "jdbc:netezza://{host}:{port}/{database}",
        "jdbc:ns://{host}:{port};ServerDataSource=NetSuite.com;Encrypted=1;NegotiateSSLClose=false",
        "jdbc:dbodbc:{database}",
        "jdbc:oracle:thin:@{host}[:{port}]/{database}",
        "jdbc:postgresql://{host}[:{port}]/[{database}]",
        "redis://{host}[:{port}]/",
        "jdbc:redshift://{host}:{port}/{database}",
        "jdbc:dbeaver:salesforce:{host}",
        "jdbc:snowflake://{host}[:{port}]/?[db={database}]",
        "jdbc:sqlite:{file}",
        "jdbc:sqlite:{file}",
        "jdbc:sybase:Tds:{host}[:{port}][?ServiceName={database}]",
        "jdbc:TAOS-RS://{host}:{port}/[{db}]",
        "jdbc:teradata://{host}/DATABASE={database},DBS_PORT={port}",
        "jdbc:timestream://",
        "jdbc:vertica://{host}:{port}/[{database}]",
        "cql://{host}[:{port}]/{database}",
        "jdbc:qmdb",
        "jdbc:jkiss:cassandra://{host}[:{port}]/{database}"
    };

    @Test
    public void testOptionals() throws StringTemplate.StringTemplateException {
        evaluatePlainUrl(
            DatabaseURL.Generic.TEMPLATE, "wtf://user:pwd@myhost:1234", Map.of(
                DBConstants.PROP_USER, DBConstants.PROP_USER,
                DBConstants.PROP_PASSWORD, "pwd",
                "driver", "wtf",
                DBConstants.PROP_HOST, "myhost",
                DBConstants.PROP_PORT, "1234"
            )
        );
        evaluatePlainUrl(
            DatabaseURL.Generic.TEMPLATE, "wtf://myhost/dbname", Map.of(
                "driver", "wtf",
                DBConstants.PROP_HOST, "myhost",
                DBConstants.PROP_DATABASE, "dbname"
            )
        );
    }

    @Test
    public void testBranching() throws StringTemplate.StringTemplateException {
        evaluatePlainUrl("abc{x{x}|y{y}}def", "abcx134def", Map.of("x", "134"));
        evaluatePlainUrl("abc{x{x}|y{y}}def", "abcy456def", Map.of("y", "456"));
    }

    @Test
    public void testRepeatingsFlat() throws StringTemplate.StringTemplateException {
        evaluatePlainUrl(
            DatabaseURL.Generic.TEMPLATE_WITH_PARAMS,
            "jdbc:mysql://mysql.db.server:3306/my_database?1useSSL=1false&2serverTimezone=2UTC",
            Map.of(
                "driver", "mysql",
                DBConstants.PROP_HOST, "mysql.db.server",
                DBConstants.PROP_PORT, "3306",
                DBConstants.PROP_DATABASE, "my_database",
                DatabaseURL.Generic.PARAM_PROP, List.of("1useSSL", "2serverTimezone"),
                DatabaseURL.Generic.PARAM_VALUE, List.of("1false", "2UTC")
            )
        );
        evaluateFlat(
            DatabaseURL.Generic.TEMPLATE_WITH_PARAMS,
            "jdbc:mysql://mysql.db.server:3306/my_database?1useSSL=1false&2serverTimezone=2UTC",
            List.of(
                Map.entry("driver", "mysql"),
                Map.entry(DBConstants.PROP_HOST, "mysql.db.server"),
                Map.entry(DBConstants.PROP_PORT, "3306"),
                Map.entry(DBConstants.PROP_DATABASE, "my_database"),
                Map.entry(DatabaseURL.Generic.PARAM_PROP, "1useSSL"),
                Map.entry(DatabaseURL.Generic.PARAM_VALUE, "1false"),
                Map.entry(DatabaseURL.Generic.PARAM_PROP, "2serverTimezone"),
                Map.entry(DatabaseURL.Generic.PARAM_VALUE, "2UTC")
            )
        );
    }

    @Test
    public void testRepeatingsWithGroups() throws StringTemplate.StringTemplateException {
        evaluateHierarchicalUrl(
            DatabaseURL.Generic.TEMPLATE_WITH_PARAM_GROUPS,
            "jdbc:mysql://mysql.db.server:3306/my_database?1useSSL=1false&2serverTimezone=2UTC",
            Map.of(
                "driver", "mysql",
                DBConstants.PROP_HOST, "mysql.db.server",
                DBConstants.PROP_PORT, "3306",
                DBConstants.PROP_DATABASE, "my_database",
                DatabaseURL.Generic.PARAM_GROUP, List.of(
                    Map.of(
                        DatabaseURL.Generic.PARAM_PROP, "1useSSL",
                        DatabaseURL.Generic.PARAM_VALUE, "1false"
                    ),
                    Map.of(
                        DatabaseURL.Generic.PARAM_PROP, "2serverTimezone",
                        DatabaseURL.Generic.PARAM_VALUE, "2UTC"
                    )
                )
            )
        );
    }

    @Test
    public void testBranchingFlat() throws StringTemplate.StringTemplateException {
        final String mysqlTemplateFlat
            = "[jdbc:]{driver}://{[{user}:{password}]\\[{{host}[:{port}]}[,{{host}[:{port}]}...]\\]|[{user}:{password}@]{host}[:{port}]}[/{database}]";

        evaluateFlat(
            mysqlTemplateFlat, "jdbc:mysql://root:mypass@myhost1:3306/db_name", List.of(
                Map.entry("driver", "mysql"),
                Map.entry(DBConstants.PROP_USER, "root"),
                Map.entry(DBConstants.PROP_PASSWORD, "mypass"),
                Map.entry(DBConstants.PROP_HOST, "myhost1"),
                Map.entry(DBConstants.PROP_PORT, "3306"),
                Map.entry(DBConstants.PROP_DATABASE, "db_name")
            )
        );

        evaluateFlat(
            mysqlTemplateFlat, "jdbc:mysql://root:mypass[myhost1:3306,myhost2,myhost3:3307]/db_name", List.of(
                Map.entry("driver", "mysql"),
                Map.entry(DBConstants.PROP_USER, "root"),
                Map.entry(DBConstants.PROP_PASSWORD, "mypass"),
                Map.entry(DBConstants.PROP_DATABASE, "db_name"),
                Map.entry(DBConstants.PROP_HOST, "myhost1"),
                Map.entry(DBConstants.PROP_PORT, "3306"),
                Map.entry(DBConstants.PROP_HOST, "myhost2"),
                Map.entry(DBConstants.PROP_HOST, "myhost3"),
                Map.entry(DBConstants.PROP_PORT, "3307")
            )
        );
    }

    @Test
    public void testBranchingWithGroups() throws StringTemplate.StringTemplateException {
        final String mysqlTemplateHeirarhical
            = "[jdbc:]{driver}://{[{user}:{password}]\\[{ep:{host}[:{port}]}[,{ep:{host}[:{port}]}...]\\]|[{user}:{password}@]{host}[:{port}]}[/{database}]";

        evaluateHierarchicalUrl(
            mysqlTemplateHeirarhical, "jdbc:mysql://root:mypass@myhost1:3306/db_name", Map.of(
                "driver", "mysql",
                DBConstants.PROP_USER, "root",
                DBConstants.PROP_PASSWORD, "mypass",
                DBConstants.PROP_DATABASE, "db_name",
                DBConstants.PROP_HOST, "myhost1",
                DBConstants.PROP_PORT, "3306"
            )
        );

        evaluateHierarchicalUrl(
            mysqlTemplateHeirarhical, "jdbc:mysql://root:mypass[myhost1:3306,myhost2,myhost3:3307]/db_name", Map.of(
                "driver", "mysql",
                DBConstants.PROP_USER, "root",
                DBConstants.PROP_PASSWORD, "mypass",
                DBConstants.PROP_DATABASE, "db_name",
                "ep", List.of(
                    Map.of(
                        DBConstants.PROP_HOST, "myhost1",
                        DBConstants.PROP_PORT, "3306"
                    ),
                    Map.of(
                        DBConstants.PROP_HOST, "myhost2"
                    ),
                    Map.of(
                        DBConstants.PROP_HOST, "myhost3",
                        DBConstants.PROP_PORT, "3307"
                    )
                )
            )
        );
    }

    @Test
    public void testRandomParametrization() throws StringTemplate.StringTemplateException {
        Random rnd = new Random(1); // fixed seed for reproducibility
        for (String templateString : ALL_URL_TEMPLATES) {
            StringTemplate template = StringTemplate.parseTemplate(templateString);

            List<StringTemplate.ParameterInfo> mandatories = template.getParametersInfo().values().stream()
                .filter(StringTemplate.ParameterInfo::isMandatory)
                .toList();
            List<StringTemplate.ParameterInfo> optionals = template.getParametersInfo().values().stream()
                .filter(p -> !p.isMandatory())
                .toList();

            for (int i = 0; i < optionals.size() + 1; i++) {
                Map<String, Object> params = new HashMap<>();
                for (StringTemplate.ParameterInfo p : mandatories) {
                    params.put(p.name(), generateString(rnd));
                }

                if (i > 0) {
                    // TODO full combinations traversing
                    int optionalsToUse = rnd.nextInt(0, optionals.size());
                    for (int j = 0; j < optionalsToUse; j++) {
                        StringTemplate.ParameterInfo p = optionals.get(rnd.nextInt(0, optionals.size()));
                        params.put(p.name(), generateString(rnd));
                    }
                }
                System.out.println(templateString + " " + jsonify(params));
                evaluatePlainUrl(templateString, null, params);
            }
        }
    }

    @NotNull
    public String generateString(@NotNull Random rnd) {
        StringBuilder sb = new StringBuilder();
        int len = rnd.nextInt(3, 10);
        for (int i = 0; i < len; i++) {
            sb.append((char) rnd.nextInt('a', 'z'));
        }
        return sb.toString();
    }

    static void evaluatePlainUrl(
        @NotNull String templateString, @Nullable String givenUrl, @Nullable Map<String, ?> givenParams
    ) throws StringTemplate.StringTemplateException {
        if (givenParams == null && givenUrl == null) {
            throw new IllegalArgumentException("At least one of givenUrl and givenParams should be specified");
        }

        StringTemplate template = StringTemplate.parseTemplate(templateString);

        Map<String, ?> givenUrlParams = givenUrl == null ? null : template.extractAllParametersMap(givenUrl);

        if (givenParams == null) {
            String preparedUrl = template.prepareString(givenUrlParams);
            Map<String, ?> extractedParams = template.extractAllParametersMap(preparedUrl);
            assertParams(givenUrlParams, extractedParams);
        } else {
            String preparedUrl = template.prepareString(givenParams);
            Map<String, ?> extractedParams = template.extractAllParametersMap(preparedUrl);
            assertParams(givenParams, extractedParams);
            if (givenUrlParams != null) {
                assertParams(givenParams, givenUrlParams);
            }
        }
    }

    static void assertParams(@Nullable Map<String, ?> given, @Nullable Map<String, ?> actual) {
        String givenParams = jsonify(given);
        String actualParams = jsonify(actual);
        Assertions.assertEquals(givenParams, actualParams);
    }

    static void assertParamsFlat(@Nullable List<Map.Entry<String, String>> given, @Nullable List<Map.Entry<String, String>> actual) {
        String givenParams = jsonify(given);
        String actualParams = jsonify(actual);
        Assertions.assertEquals(givenParams, actualParams);
    }

    @NotNull
    static String jsonify(@Nullable Object o) {
        return switch (o) {
            case null -> "null";
            case String s -> "\"" + s.replace("\"", "\\\"") + "\"";
            case List<?> l -> l.stream().map(StringTemplateTest::jsonify).sorted().collect(Collectors.joining(", ", "[", "]"));
            case Map<?, ?> m -> m.entrySet().stream().map(kv -> jsonify(kv.getKey()) + ": " + (
                    kv.getValue() instanceof List<?> ? jsonify(kv.getValue()) : ("[" + jsonify(kv.getValue()) + "]")
                )).sorted().collect(Collectors.joining(", ", "{", "}"));
            case Map.Entry<?, ?> kv -> "{" + jsonify(kv.getKey()) + ": " + jsonify(kv.getValue()) + "}";
            default -> throw new IllegalStateException();
        };
    }

    static void evaluateFlat(
        @NotNull String templateString, @Nullable String givenUrl, @Nullable List<Map.Entry<String, String>> givenParams
    ) throws StringTemplate.StringTemplateException {
        if (givenParams == null && givenUrl == null) {
            throw new IllegalArgumentException("At least one of givenUrl and givenParams should be spefified");
        }

        StringTemplate template = StringTemplate.parseTemplate(templateString);

        List<Map.Entry<String, String>> givenUrlParams = givenUrl == null ? null : template.extractAllParametersFlat(givenUrl);

        if (givenParams == null) {
            String preparedUrl = template.prepareString(givenUrlParams);
            List<Map.Entry<String, String>> extractedParams = template.extractAllParametersFlat(preparedUrl);
            assertParamsFlat(givenUrlParams, extractedParams);
        } else {
            String preparedUrl = template.prepareString(givenParams);
            List<Map.Entry<String, String>> extractedParams = template.extractAllParametersFlat(preparedUrl);
            assertParamsFlat(givenParams, extractedParams);
            if (givenUrlParams != null) {
                assertParamsFlat(givenParams, givenUrlParams);
            }
        }
    }

    static void evaluateHierarchicalUrl(
        @NotNull String templateString, @Nullable String givenUrl, @Nullable Map<String, ?> givenParams
    ) throws StringTemplate.StringTemplateException {
        if (givenParams == null && givenUrl == null) {
            throw new IllegalArgumentException("At least one of givenUrl and givenParams should be spefified");
        }

        StringTemplate template = StringTemplate.parseTemplate(templateString);

        Map<String, ?> givenUrlParams = givenUrl == null ? null : template.extractAllParametersTree(givenUrl).toMap();

        if (givenParams == null) {
            String preparedUrl = template.prepareString(givenUrlParams);
            Map<String, ?> extractedParams = template.extractAllParametersTree(preparedUrl).toMap();
            assertParams(givenUrlParams, extractedParams);
        } else {
            String preparedUrl = template.prepareString(givenParams);
            Map<String, ?> extractedParams = template.extractAllParametersTree(preparedUrl).toMap();
            assertParams(givenParams, extractedParams);
            if (givenUrlParams != null) {
                assertParams(givenParams, givenUrlParams);
            }
        }
    }
}

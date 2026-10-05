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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DatabaseURL;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DatabaseURLTest extends DBeaverUnitTest {
    @Test
    public void recognizesQueryDelimitersAndEmptyValuesInBothGenericPatterns() {
        String url = "jdbc:mysql://localhost/demo?empty=&custom.option[0]=a%20b&expression=a=b=c&path=/tmp/данные; x&other=last";
        Map<String, String> expected = Map.of(
            "empty", "", "custom.option[0]", "a%20b", "expression", "a=b=c", "path", "/tmp/данные; x", "other", "last");
        var grouped = DatabaseURL.Generic.getUrlPatternWithParamGroups().tryRecognizeHierarchical(url, true);
        Assertions.assertNotNull(grouped);
        Assertions.assertEquals(expected, DatabaseURL.Generic.extractExtraParams(grouped));

        var flat = DatabaseURL.Generic.getUrlPatternWithParams().tryRecognizeHierarchical(url, true);
        Assertions.assertNotNull(flat);
        var names = flat.getParameters().get(DatabaseURL.Generic.PARAM_PROP);
        var values = flat.getParameters().get(DatabaseURL.Generic.PARAM_VALUE);
        Map<String, String> properties = new HashMap<>();
        for (int index = 0; index < names.size(); index++) {
            properties.put(names.get(index), values.get(index));
        }
        Assertions.assertEquals(expected, properties);
    }

    @Test
    public void rejectsMalformedGenericQueryParameters() {
        for (var pattern : List.of(DatabaseURL.Generic.getUrlPatternWithParams(), DatabaseURL.Generic.getUrlPatternWithParamGroups())) {
            for (String query : List.of("=value", "valid=value&broken", "valid=value&=missing")) {
                Assertions.assertNull(pattern.tryRecognizeHierarchical("jdbc:mysql://localhost/demo?" + query, true));
            }
            Assertions.assertNotNull(pattern.tryRecognizeHierarchical("jdbc:mysql://localhost/demo", true));
        }
    }

    @Test
    public void extractsConfigurationWithGenericPattern() {
        var configuration = DatabaseURL.extractConfigurationFromUrl(
            DatabaseURL.Generic.getUrlPattern(), "jdbc:mysql://username:password@localhost:3306/demo?empty=");
        Assertions.assertNotNull(configuration);
        Assertions.assertEquals("localhost", configuration.getHostName());
        Assertions.assertEquals("3306", configuration.getHostPort());
        Assertions.assertEquals("demo", configuration.getDatabaseName());
        Assertions.assertEquals("username", configuration.getUserName());
        Assertions.assertEquals("password", configuration.getUserPassword());
    }

    @Test
    public void preservesDefaultHierarchicalUrlRecognition() throws DBException {
        var pattern = DatabaseURL.Generic.getUrlPatternWithParamGroups();
        var entries = pattern.tryRecognizeHierarchical("jdbc:mysql://localhost/demo?option=a%20b&other=c d", true);
        Assertions.assertNotNull(entries);
        Assertions.assertEquals(Map.of("option", "a%20b", "other", "c d"), DatabaseURL.Generic.extractExtraParams(entries));
    }

    @Test
    public void recognizesRepeatedPropertiesUsingCustomPatterns() throws DBException {
        var pattern = DatabaseURL.getUrlPattern("jdbc:test:[{param:{prop}={value};}...]", param -> switch (param.name()) {
            case "prop" -> "[^=;]+";
            case "value" -> "[^;]*";
            default -> throw new IllegalArgumentException(param.name());
        });
        var entries = pattern.tryRecognizeHierarchical("jdbc:test:Host=::1;Empty=;Path=/tmp/данные;", true);
        Assertions.assertNotNull(entries);
        var groups = entries.getGroups().get("param").reversed();
        Assertions.assertEquals("::1", groups.get(0).getFirstParamValue("value"));
        Assertions.assertEquals("", groups.get(1).getFirstParamValue("value"));
        Assertions.assertEquals("/tmp/данные", groups.get(2).getFirstParamValue("value"));
        Assertions.assertNull(pattern.tryRecognizeHierarchical("jdbc:test:Host=::1;invalid", true));
        Assertions.assertNotNull(pattern.tryRecognizeHierarchical("jdbc:test:Host=::1;invalid"));
        Assertions.assertNotNull(pattern.tryRecognizeHierarchical("jdbc:test:", true));
    }

    @Test
    public void testMatchPattern() throws DBException {
        assertRecognition(
            "jdbc:postgresql://{host}[:{port}]/[{database}]",
            "jdbc:postgresql://localhost:5432/dvdrental",
            new String[][]{
                {"host", "localhost"},
                {"port", "5432"},
                {"database", "dvdrental"}
            });

        assertRecognition(
            "jdbc:teradata://{host}/DATABASE={database},DBS_PORT={port}",
            "jdbc:teradata://localhost/DATABASE=test,DBS_PORT=1234",
            new String[][]{
                {"host", "localhost"},
                {"database", "test"},
                {"port", "1234"}
            });

        assertRecognition(
            "jdbc:oracle:thin:@{host}[:{port}]/{database}",
            "jdbc:oracle:thin:@localhost/orcl",
            new String[][]{
                {"host", "localhost"},
                {"database", "orcl"}
            });

        assertRecognition(
            "jdbc:sqlserver://{host}[:{port}][;databaseName={database}]",
            "jdbc:sqlserver://localhost:1433;databaseName=master",
            new String[][]{
                {"host", "localhost"},
                {"port", "1433"},
                {"database", "master"}
            });

        assertRecognition(
            "jdbc:sqlserver://{host}[:{port}][;databaseName={database}]",
            "jdbc:sqlserver://localhost",
            new String[][]{
                {"host", "localhost"}
            });

        assertRecognition(
            "jdbc:sqlite:{file}",
            "jdbc:sqlite:C:\\Users\\%USERNAME%\\Documents\\Chinook.db",
            new String[][]{
                {"file", "C:\\Users\\%USERNAME%\\Documents\\Chinook.db"}
            });

        assertRecognition(
            "jdbc:mysql://{host}[:{port}]/[{database}]",
            "jdbc:mysql://mysql-rfam-public.ebi.ac.uk:4497/Rfam?useSSL=false&serverTimezone=UTC",
            new String[][]{
                {"host", "mysql-rfam-public.ebi.ac.uk"},
                {"port", "4497"},
                {"database", "Rfam"}
            });
    }

    private void assertRecognition(
        @NotNull String sampleUrl, @NotNull String targetUrl, @NotNull String[][] properties
    ) throws DBException {
        final Map<String, String> params = DatabaseURL.getUrlPattern(sampleUrl).tryRecognize(targetUrl);
        Assertions.assertTrue(params != null, sampleUrl);
        for (String[] property : properties) {
            Assertions.assertEquals(property[1], params.get(property[0]), sampleUrl);
        }
    }
}

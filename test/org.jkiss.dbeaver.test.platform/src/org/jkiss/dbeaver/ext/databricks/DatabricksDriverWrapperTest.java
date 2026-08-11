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
package org.jkiss.dbeaver.ext.databricks;

import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Properties;

public class DatabricksDriverWrapperTest extends DBeaverUnitTest {

    private static final String URL_WITH_PARAMS =
        "jdbc:databricks://adb-1234567890123456.7.azuredatabricks.net:443/default" +
            ";transportMode=http;ssl=1;httpPath=/sql/1.0/warehouses/abcd1234;AuthMech=3;UID=token;PWD=dapiOldToken";

    @Test
    public void testRemovesUrlParametersOverriddenByProperties() {
        Properties info = new Properties();
        info.setProperty("httpPath", "/sql/1.0/warehouses/other");
        info.setProperty("PWD", "dapiNewToken");

        String result = DatabricksDriverWrapper.removeDuplicatedUrlParameters(URL_WITH_PARAMS, info);

        Assertions.assertEquals(
            "jdbc:databricks://adb-1234567890123456.7.azuredatabricks.net:443/default" +
                ";transportMode=http;ssl=1;AuthMech=3;UID=token",
            result);
        // properties are left intact, so the form values win
        Assertions.assertEquals("/sql/1.0/warehouses/other", info.getProperty("httpPath"));
        Assertions.assertEquals("dapiNewToken", info.getProperty("PWD"));
    }

    @Test
    public void testMatchesParametersCaseInsensitively() {
        Properties info = new Properties();
        info.setProperty("HTTPPath", "/sql/1.0/warehouses/other");
        info.setProperty("authmech", "3");

        String result = DatabricksDriverWrapper.removeDuplicatedUrlParameters(URL_WITH_PARAMS, info);

        Assertions.assertEquals(
            "jdbc:databricks://adb-1234567890123456.7.azuredatabricks.net:443/default" +
                ";transportMode=http;ssl=1;UID=token;PWD=dapiOldToken",
            result);
    }

    @Test
    public void testKeepsUrlWithoutParameters() {
        String url = "jdbc:databricks://adb-1234567890123456.7.azuredatabricks.net:443/default";
        Properties info = new Properties();
        info.setProperty("httpPath", "/sql/1.0/warehouses/abcd1234");

        Assertions.assertEquals(url, DatabricksDriverWrapper.removeDuplicatedUrlParameters(url, info));
    }

    @Test
    public void testKeepsUrlForEmptyProperties() {
        Assertions.assertEquals(
            URL_WITH_PARAMS,
            DatabricksDriverWrapper.removeDuplicatedUrlParameters(URL_WITH_PARAMS, new Properties()));
    }

    @Test
    public void testTreatsTextBeforeFirstEqualsSignAsParameterName() {
        // Cluster httpPath copied from the Azure console may contain '=' inside the value
        // (e.g. "...?o=123"). The parameter name is the text before the FIRST '=' only,
        // and the segment must be removed as a whole - same way the driver parses the URL
        Properties info = new Properties();
        info.setProperty("httpPath", "sql/protocolv1/o/123/cluster");

        String result = DatabricksDriverWrapper.removeDuplicatedUrlParameters(
            "jdbc:databricks://host:443/default;ssl=1;httpPath=sql/protocolv1/o/123/cluster?o=123=456", info);

        Assertions.assertEquals("jdbc:databricks://host:443/default;ssl=1", result);
    }
}

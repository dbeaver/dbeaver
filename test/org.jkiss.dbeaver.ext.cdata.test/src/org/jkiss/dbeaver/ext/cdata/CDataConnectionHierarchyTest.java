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
package org.jkiss.dbeaver.ext.cdata;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cdata.model.CDataConnectionHierarchy;
import org.jkiss.dbeaver.ext.cdata.model.CDataConnectionHierarchy.Property;
import org.jkiss.dbeaver.ext.cdata.model.CDataConnectionHierarchyReader;
import org.jkiss.dbeaver.ext.cdata.model.CDataConnectionUrl;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;

public class CDataConnectionHierarchyTest extends DBeaverUnitTest {
    private static final String DEFINITION = """
        {
          "basic": [
            {"propertyName":"Edition", "name":"Edition", "default":"Local", "enum":["Local","Cloud"],
             "hierarchyRules": {
               "Local": [{"propertyName":"AuthScheme", "default":"Password", "enum":["Password"],
                          "hierarchyRules":{"Password":[{"propertyName":"Password", "sensitivity":"PASSWORD"}]}}],
               "Cloud": [{"propertyName":"AuthScheme", "default":"OAuth", "enum":["OAuth","Token"],
                          "hierarchyRules":{"OAuth":[{"propertyName":"ClientId", "default":"built-in"}],
                                            "Token":[{"propertyName":"Token"}]}}]
             }},
            {"propertyName":"Server", "default":"", "display":"RequiredBasic"}
          ],
          "advanced": [{"name":"Connection", "properties":[
            {"propertyName":"AuthScheme", "default":"Password"},
            {"propertyName":"Password", "sensitivity":"PASSWORD"},
            {"propertyName":"ClientId"},
            {"propertyName":"Token"},
            {"propertyName":"Timeout", "default":30, "type":"Number"},
            {"propertyName":"UseSSL", "default":false, "type":"Boolean"}
          ]}]
        }
        """;

    @Test
    public void separatesGeneralFieldsFromAuthenticationModels() throws Exception {
        var hierarchy = CDataConnectionHierarchy.parse(DEFINITION);
        Assertions.assertEquals(java.util.List.of("Edition", "Server"),
            hierarchy.getGeneralProperties().stream().map(Property::name).toList());
        Assertions.assertEquals(java.util.List.of("AuthScheme", "Password"),
            hierarchy.getAuthenticationProperties().stream().map(Property::name).toList());
        hierarchy.setValue(property(hierarchy, "Edition"), "Cloud", false);
        Assertions.assertEquals(java.util.List.of("Edition", "Server"),
            hierarchy.getGeneralProperties().stream().map(Property::name).toList());
        Assertions.assertEquals(java.util.List.of("AuthScheme", "ClientId"),
            hierarchy.getAuthenticationProperties().stream().map(Property::name).toList());
    }

    @Test
    public void includesBasicDefaultsAndEmptyValues() throws Exception {
        var hierarchy = CDataConnectionHierarchy.parse(DEFINITION);
        Assertions.assertEquals(Map.of("Edition", "Local", "AuthScheme", "Password", "Password", "", "Server", ""),
            hierarchy.getConnectionProperties());
        Assertions.assertTrue(property(hierarchy, "Password").password());
        Assertions.assertTrue(property(hierarchy, "Server").required());
    }

    @Test
    public void changingParentResetsAllDescendantsAndUsesBranchDefaults() throws Exception {
        var hierarchy = CDataConnectionHierarchy.parse(DEFINITION);
        hierarchy.loadValues(Map.of("Password", "old-secret", "AuthScheme", "Password", "Timeout", "45"));
        hierarchy.setValue(property(hierarchy, "Edition"), "Cloud", false);
        Assertions.assertEquals("OAuth", hierarchy.getValue(property(hierarchy, "AuthScheme")));
        Assertions.assertEquals(java.util.List.of("OAuth", "Token"), property(hierarchy, "AuthScheme").choices());
        Assertions.assertEquals("built-in", hierarchy.getConnectionProperties().get("ClientId"));
        Assertions.assertEquals("45", hierarchy.getConnectionProperties().get("Timeout"));
        Assertions.assertFalse(hierarchy.getConnectionProperties().containsKey("Password"));
        hierarchy.setValue(property(hierarchy, "AuthScheme"), "Token", false);
        Assertions.assertFalse(hierarchy.getConnectionProperties().containsKey("ClientId"));
        Assertions.assertEquals("", hierarchy.getConnectionProperties().get("Token"));
    }

    @Test
    public void includesOnlyChangedAdvancedValuesAndPreservesUnknownUrlProperties() throws Exception {
        var hierarchy = CDataConnectionHierarchy.parse(DEFINITION);
        hierarchy.loadValues(Map.of("timeout", "30", "UseSSL", "true", "CustomOption", "keep"));
        var properties = hierarchy.getConnectionProperties();
        Assertions.assertFalse(properties.containsKey("Timeout"));
        Assertions.assertEquals("true", properties.get("UseSSL"));
        Assertions.assertEquals("keep", properties.get("CustomOption"));
    }

    @Test
    public void basicDefaultsOverrideFlatAdvancedDefaults() throws Exception {
        var hierarchy = CDataConnectionHierarchy.parse(DEFINITION);
        hierarchy.loadValues(Map.of("Edition", "Cloud"));
        Assertions.assertEquals("OAuth", hierarchy.getConnectionProperties().get("AuthScheme"));
    }

    @Test
    public void supportsExplicitAdvancedOverridesOutsideActiveBranch() throws Exception {
        var hierarchy = CDataConnectionHierarchy.parse(DEFINITION);
        Property client = hierarchy.getAdvancedGroups().getFirst().properties().get(2);
        hierarchy.setValue(client, "custom-client", true);
        Assertions.assertEquals("custom-client", hierarchy.getConnectionProperties().get("ClientId"));
        hierarchy.setValue(property(hierarchy, "Edition"), "Cloud", false);
        Assertions.assertEquals("built-in", hierarchy.getConnectionProperties().get("ClientId"));
    }

    @Test
    public void matchesJdbcNamesCaseInsensitively() throws Exception {
        var hierarchy = CDataConnectionHierarchy.parse(DEFINITION);
        hierarchy.loadValues(Map.of("edition", "Cloud", "server", "localhost"));
        Assertions.assertEquals("OAuth", hierarchy.getConnectionProperties().get("authscheme"));
        Assertions.assertEquals("localhost", hierarchy.getValue(property(hierarchy, "Server")));
        Assertions.assertTrue(hierarchy.getPropertyNames().contains("clientid"));
    }

    @Test
    public void roundTripsUrlDelimitersQuotesUnicodeAndWhitespace() throws Exception {
        Map<String, String> properties = Map.of("Password", " a;\"b'c=д\\ ", "Server", "localhost", "Empty", "");
        String url = CDataConnectionUrl.build("postgresql", properties);
        Assertions.assertEquals(properties, CDataConnectionUrl.parse(url, "postgresql"));
        Assertions.assertFalse(url.contains("config:"));
    }

    @Test
    public void parsesBothPrefixesAndQuotedValues() throws Exception {
        Assertions.assertEquals(Map.of("Password", "a'b;c", "Server", "last"), CDataConnectionUrl.parse(
            "jdbc:cdata:postgresql:Password='a''b;c';Server=first;server=last;", "postgresql"));
        Assertions.assertTrue(CDataConnectionUrl.parse("", "postgresql").isEmpty());
    }

    @Test
    public void rejectsMalformedUrlsWithoutIncludingCredentialsInErrors() {
        for (String url : java.util.List.of(
            "jdbc:mysql:Password=secret", "jdbc:postgresql:Password='secret", "jdbc:postgresql:bad;Password=secret",
            "jdbc:postgresql:Password='secret'junk;", "jdbc:postgresql:=secret;"
        )) {
            DBException error = Assertions.assertThrows(DBException.class, () -> CDataConnectionUrl.parse(url, "postgresql"));
            Assertions.assertFalse(error.getMessage().contains("secret"));
        }
    }

    @Test
    public void editingAuthenticationPreservesManualUrlProperties() throws Exception {
        var hierarchy = CDataConnectionHierarchy.parse(DEFINITION);
        hierarchy.loadValues(Map.of("Edition", "Cloud", "Server", "form-server"));
        Property client = property(hierarchy, "ClientId");
        hierarchy.setValue(client, "new;client", false);
        String url = hierarchy.updateUrl("test",
            "jdbc:test:Server=manual-server;Timeout=90;CustomOption=keep;ClientId=old;",
            CDataConnectionHierarchy.getPropertyNames(client));
        Assertions.assertEquals(Map.of("Server", "manual-server", "Timeout", "90", "CustomOption", "keep", "ClientId", "new;client"),
            CDataConnectionUrl.parse(url, "test"));
        Assertions.assertEquals("form-server", hierarchy.getValue(property(hierarchy, "Server")));
    }

    @Test
    public void changingUrlAuthenticationRemovesInactiveBranchProperties() throws Exception {
        var hierarchy = CDataConnectionHierarchy.parse(DEFINITION);
        hierarchy.loadValues(Map.of("Edition", "Cloud"));
        Property scheme = property(hierarchy, "AuthScheme");
        hierarchy.setValue(scheme, "Token", false);
        String url = hierarchy.updateUrl("test", "jdbc:test:Server=manual-server;AuthScheme=OAuth;ClientId=old;",
            CDataConnectionHierarchy.getPropertyNames(scheme));
        Assertions.assertEquals(Map.of("Server", "manual-server", "AuthScheme", "Token", "Token", ""),
            CDataConnectionUrl.parse(url, "test"));
    }

    @Test
    public void editingUrlPasswordKeepsItInCredentials() throws Exception {
        var hierarchy = CDataConnectionHierarchy.parse(DEFINITION);
        Property password = property(hierarchy, "Password");
        hierarchy.setValue(password, "new-secret", false);
        String url = hierarchy.updateUrl("test", "jdbc:test:Server=manual-server;Password=old-secret;",
            CDataConnectionHierarchy.getPropertyNames(password));
        Assertions.assertEquals(Map.of("Server", "manual-server"), CDataConnectionUrl.parse(url, "test"));
        Assertions.assertEquals("new-secret", hierarchy.getConnectionProperties().get("Password"));
    }

    @Test
    public void resettingAdvancedUrlPropertyRemovesItsOverride() throws Exception {
        var hierarchy = CDataConnectionHierarchy.parse(DEFINITION);
        Property timeout = hierarchy.getAdvancedGroups().getFirst().properties().get(4);
        hierarchy.setValue(timeout, timeout.defaultValue(), true);
        String url = hierarchy.updateUrl("test", "jdbc:test:Server=manual-server;Timeout=90;",
            CDataConnectionHierarchy.getPropertyNames(timeout));
        Assertions.assertEquals(Map.of("Server", "manual-server"), CDataConnectionUrl.parse(url, "test"));
        Assertions.assertThrows(DBException.class, () -> hierarchy.updateUrl("test", "jdbc:test:broken",
            CDataConnectionHierarchy.getPropertyNames(timeout)));
    }

    @Test
    public void loadingEditedUrlReplacesFormValuesAndKeepsCredentials() throws Exception {
        var hierarchy = CDataConnectionHierarchy.parse(DEFINITION);
        hierarchy.loadValues(Map.of("Server", "old-server", "Timeout", "90", "Password", "stored-secret", "User", "stored-user"));

        hierarchy.loadUrl("test", "jdbc:test:Server=edited-server;CustomOption=keep;");

        Assertions.assertEquals("edited-server", hierarchy.getValue(property(hierarchy, "Server")));
        var configuration = new DBPConnectionConfiguration();
        hierarchy.saveConfiguration("test", configuration);
        Assertions.assertEquals("stored-secret", configuration.getUserPassword());
        Assertions.assertEquals("stored-user", configuration.getUserName());
        var properties = CDataConnectionUrl.parse(configuration.getUrl(), "test");
        Assertions.assertEquals("edited-server", properties.get("Server"));
        Assertions.assertEquals("keep", properties.get("CustomOption"));
        Assertions.assertFalse(properties.containsKey("Timeout"));
        Assertions.assertFalse(properties.containsKey("Password"));
    }

    @Test
    public void loadingUrlChangesAuthenticationBranchAndUsesUrlCredentials() throws Exception {
        var hierarchy = CDataConnectionHierarchy.parse(DEFINITION);
        hierarchy.loadValues(Map.of("Password", "old-secret", "User", "old-user"));

        hierarchy.loadUrl("test", "jdbc:test:Edition=Cloud;AuthScheme=Token;Token=url-token;User=url-user;");

        Assertions.assertEquals("Token", hierarchy.getValue(property(hierarchy, "AuthScheme")));
        var configuration = new DBPConnectionConfiguration();
        hierarchy.saveConfiguration("test", configuration);
        Assertions.assertEquals("url-user", configuration.getUserName());
        Assertions.assertEquals("url-token", CDataConnectionUrl.parse(configuration.getUrl(), "test").get("Token"));
        Assertions.assertNull(configuration.getUserPassword());
    }

    @Test
    public void loadingMalformedUrlKeepsCurrentFormValues() throws Exception {
        var hierarchy = CDataConnectionHierarchy.parse(DEFINITION);
        hierarchy.loadValues(Map.of("Server", "keep-server", "Password", "keep-secret"));
        var properties = hierarchy.getConnectionProperties();

        Assertions.assertThrows(DBException.class, () -> hierarchy.loadUrl("test", "jdbc:test:Server='broken"));
        Assertions.assertEquals(properties, hierarchy.getConnectionProperties());
    }

    @Test
    public void rejectsMalformedHierarchy() {
        Assertions.assertThrows(DBException.class, () -> CDataConnectionHierarchy.parse("{}"));
        Assertions.assertThrows(DBException.class, () -> CDataConnectionHierarchy.parse("not json"));
    }

    @Test
    public void keepsPasswordInCredentialsAndPreservesDriverProperties() throws Exception {
        final var hierarchy = CDataConnectionHierarchy.parse(DEFINITION);
        var configuration = new DBPConnectionConfiguration();
        configuration.setUrl("jdbc:test:Password=url-secret;CustomOption=keep;");
        configuration.setUserName("test-user");
        configuration.setUserPassword("stored-secret");
        configuration.getProperties().put("Timeout", "45");
        configuration.getProperties().put("internal.setting", "keep-in-properties");
        hierarchy.loadConfiguration("test", configuration);
        Assertions.assertEquals("url-secret", hierarchy.getValue(property(hierarchy, "Password")));
        hierarchy.saveConfiguration("test", configuration);
        Assertions.assertEquals("test-user", configuration.getUserName());
        Assertions.assertEquals("url-secret", configuration.getUserPassword());
        Assertions.assertFalse(configuration.getUrl().contains("secret"));
        Assertions.assertFalse(configuration.getUrl().contains("internal.setting"));
        Assertions.assertEquals(Map.of("Timeout", "45", "internal.setting", "keep-in-properties"), configuration.getProperties());
        Assertions.assertFalse(CDataConnectionUrl.parse(configuration.getUrl(), "test").containsKey("Timeout"));
        Assertions.assertEquals("keep", CDataConnectionUrl.parse(configuration.getUrl(), "test").get("CustomOption"));
    }

    @Test
    public void savingFormPreservesLaterDriverPropertyEdits() throws Exception {
        var hierarchy = CDataConnectionHierarchy.parse(DEFINITION);
        var configuration = new DBPConnectionConfiguration();
        configuration.setUrl("jdbc:test:Server=remote;");
        configuration.getProperties().put("Timeout", "45");
        hierarchy.loadConfiguration("test", configuration);
        configuration.getProperties().put("Timeout", "90");
        configuration.getProperties().put("UseSSL", "true");
        hierarchy.saveConfiguration("test", configuration);
        Assertions.assertEquals(Map.of("Timeout", "90", "UseSSL", "true"), configuration.getProperties());
        configuration.getProperties().remove("Timeout");
        hierarchy.saveConfiguration("test", configuration);
        Assertions.assertFalse(CDataConnectionUrl.parse(configuration.getUrl(), "test").containsKey("Timeout"));
    }

    @Test
    public void readingConfigurationDoesNotModifyIt() throws Exception {
        final var hierarchy = CDataConnectionHierarchy.parse(DEFINITION);
        var configuration = new DBPConnectionConfiguration();
        String url = "jdbc:test:Password='dummy;secret';";
        configuration.setUrl(url);
        configuration.getProperties().put("Timeout", "40");
        hierarchy.loadConfiguration("test", configuration);
        hierarchy.setValue(property(hierarchy, "Edition"), "Cloud", false);
        Assertions.assertEquals(url, configuration.getUrl());
        Assertions.assertEquals(Map.of("Timeout", "40"), configuration.getProperties());
    }

    @Test
    public void removesStalePasswordWhenChangingAuthentication() throws Exception {
        var hierarchy = CDataConnectionHierarchy.parse(DEFINITION);
        var configuration = new DBPConnectionConfiguration();
        configuration.setUserPassword("old-secret");
        hierarchy.loadConfiguration("test", configuration);
        hierarchy.setValue(property(hierarchy, "Edition"), "Cloud", false);
        hierarchy.saveConfiguration("test", configuration);
        Assertions.assertNull(configuration.getUserPassword());
        Assertions.assertFalse(configuration.getUrl().contains("Password"));
    }

    @Test
    // result navigation is stubbed here; the reader checks next()
    @SuppressWarnings("PMD.CheckResultSet")
    public void readsUsingConfigurationConnectionAndClosesResources() throws Exception {
        Driver driver = Mockito.mock(Driver.class);
        Connection connection = Mockito.mock(Connection.class);
        Statement statement = Mockito.mock(Statement.class);
        ResultSet result = Mockito.mock(ResultSet.class);
        Mockito.when(driver.connect("jdbc:cdata:postgresql:config:", new Properties())).thenReturn(connection);
        Mockito.when(connection.createStatement()).thenReturn(statement);
        Mockito.when(statement.executeQuery("SELECT Definition FROM sys_connection_hierarchy WHERE Context = '_jdbc'"))
            .thenReturn(result);
        Mockito.when(result.next()).thenReturn(true);
        Mockito.when(result.getString(1)).thenReturn(DEFINITION);
        Assertions.assertFalse(CDataConnectionHierarchyReader.read(driver, "postgresql").getBasicProperties().isEmpty());
        Mockito.verify(result).close();
        Mockito.verify(statement).close();
        Mockito.verify(connection).close();
    }

    @Test
    public void reportsUnsupportedDriversAndClosesFailedConnections() throws Exception {
        Driver driver = Mockito.mock(Driver.class);
        Assertions.assertThrows(DBException.class, () -> CDataConnectionHierarchyReader.read(driver, "postgresql"));
        Connection connection = Mockito.mock(Connection.class);
        Mockito.when(driver.connect(Mockito.anyString(), Mockito.any())).thenReturn(connection);
        Mockito.when(connection.createStatement()).thenThrow(new SQLException("unsupported"));
        Assertions.assertThrows(DBException.class, () -> CDataConnectionHierarchyReader.read(driver, "postgresql"));
        Mockito.verify(connection).close();
    }

    @Test
    public void urlOverridesDriverPropertiesAfterEditingAndReloading() throws Exception {
        var hierarchy = CDataConnectionHierarchy.parse(DEFINITION);
        var configuration = new DBPConnectionConfiguration();
        configuration.setUrl("jdbc:test:Server=url-host;");
        configuration.getProperties().put("server", "properties-host");
        hierarchy.loadConfiguration("test", configuration);
        Assertions.assertEquals("url-host", hierarchy.getValue(property(hierarchy, "Server")));
        hierarchy.setValue(property(hierarchy, "Server"), "edited-host", false);
        hierarchy.saveConfiguration("test", configuration);
        hierarchy.loadConfiguration("test", configuration);
        Assertions.assertEquals("edited-host", hierarchy.getValue(property(hierarchy, "Server")));
        CDataConnectionUrl.updateEndpoint("test", configuration, Map.of());
        Assertions.assertEquals("edited-host", configuration.getHostName());
    }

    @NotNull
    private Property property(@NotNull CDataConnectionHierarchy hierarchy, @NotNull String name) {
        return hierarchy.getBasicProperties().stream().filter(property -> property.name().equals(name)).findFirst().orElseThrow();
    }
}

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

import org.jkiss.dbeaver.ext.cdata.model.CDataConnectionHierarchy;
import org.jkiss.dbeaver.ext.cdata.model.CDataConnectionUrl;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.impl.auth.AuthModelDatabaseNativeCredentials;
import org.jkiss.dbeaver.registry.SecureCredentials;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;
import java.util.Properties;

public class CDataConnectionSecretsTest extends DBeaverUnitTest {
    private static final String DEFINITION = """
        {
          "basic": [{"propertyName":"AuthScheme", "default":"OAuth", "hierarchyRules": {
            "OAuth": [{"propertyName":"OAuthClientSecret", "sensitivity":"SENSITIVE", "cdataUserCredential":false}],
            "Password": [{"propertyName":"Password", "sensitivity":"PASSWORD"}]
          }}],
          "advanced": [{"name":"SSH", "properties":[
            {"propertyName":"SSHPassword", "sensitivity":"PASSWORD"}
          ]}]
        }
        """;

    @Test
    public void remembersAdditionalCredentialFieldsWithoutRetainingTheirSecrets() throws Exception {
        var hierarchy = CDataConnectionHierarchy.parse(DEFINITION);
        var configuration = new DBPConnectionConfiguration();
        configuration.getProperties().put("SSHPassword", "ssh-secret");
        hierarchy.loadConfiguration("test", configuration);
        hierarchy.saveConfiguration("test", configuration);
        CDataAuthModel.clearSecrets(configuration);
        Assertions.assertFalse(configuration.getUrl().contains("ssh-secret"));
        Assertions.assertFalse(configuration.getProperties().containsValue("ssh-secret"));
        Assertions.assertFalse(configuration.getProviderProperties().containsValue("ssh-secret"));
        Assertions.assertTrue(configuration.getAuthProperties().isEmpty());
        hierarchy = CDataConnectionHierarchy.parse(DEFINITION);
        hierarchy.loadConfiguration("test", configuration);
        var password = hierarchy.getCredentialProperties().stream().filter(property -> property.name().equals("SSHPassword"))
            .findFirst().orElseThrow();
        Assertions.assertEquals("", hierarchy.getValue(password));
        hierarchy.saveConfiguration("test", configuration);
        hierarchy = CDataConnectionHierarchy.parse(DEFINITION);
        hierarchy.loadConfiguration("test", configuration);
        Assertions.assertTrue(hierarchy.getCredentialProperties().stream().anyMatch(property -> property.name().equals("SSHPassword")));
        hierarchy.setValue(password, "session-secret", false);
        hierarchy.saveCredentialValues("test", configuration);
        Assertions.assertEquals("session-secret", configuration.getAuthProperty(CDataAuthModel.SECRET_PROPERTY_PREFIX + "SSHPassword"));
        Assertions.assertFalse(configuration.getUrl().contains("session-secret"));
    }

    @Test
    public void explicitResetRemovesAdditionalSecretAndItsPromptField() throws Exception {
        for (boolean urlMode : List.of(false, true)) {
            var hierarchy = CDataConnectionHierarchy.parse(DEFINITION);
            var configuration = new DBPConnectionConfiguration();
            configuration.setUrl("jdbc:test:Server=keep;");
            configuration.setAuthProperty(CDataAuthModel.SECRET_PROPERTY_PREFIX + "SSHPassword", "old-secret");
            hierarchy.loadConfiguration("test", configuration);
            hierarchy.saveConfiguration("test", configuration);
            hierarchy.setValue(hierarchy.getAdvancedSecretProperties().getFirst(), "", true);
            configuration.getProperties().put("SSHPassword", "");
            if (urlMode) {
                hierarchy.saveUrlConfiguration("test", configuration);
            } else {
                hierarchy.saveConfiguration("test", configuration);
            }
            Assertions.assertFalse(configuration.getAuthProperties().containsValue("old-secret"));
            Assertions.assertTrue(configuration.getProviderProperties().isEmpty());
            hierarchy = CDataConnectionHierarchy.parse(DEFINITION);
            hierarchy.loadConfiguration("test", configuration);
            Assertions.assertFalse(hierarchy.getCredentialProperties().stream()
                .anyMatch(property -> property.name().equals("SSHPassword")));
        }
    }

    @Test
    public void promptIncludesAllFieldsOfTheSelectedAuthenticationModel() throws Exception {
        var hierarchy = CDataConnectionHierarchy.parse("""
            {
              "basic": [
                {"propertyName":"Server"},
                {"propertyName":"AuthScheme", "default":"OAuth", "hierarchyRules": {
                  "OAuth": [
                    {"propertyName":"User"},
                    {"propertyName":"OAuthClientId"},
                    {"propertyName":"OAuthClientSecret", "sensitivity":"SENSITIVE"},
                    {"propertyName":"Scope"}
                  ],
                  "Password": [{"propertyName":"Password", "sensitivity":"PASSWORD"}]
                }}
              ],
              "advanced": []
            }
            """);
        Assertions.assertEquals(List.of("User", "OAuthClientId", "OAuthClientSecret", "Scope"),
            hierarchy.getCredentialProperties().stream().map(CDataConnectionHierarchy.Property::name).toList());
        var saved = new DBPConnectionConfiguration();
        saved.setUrl("jdbc:test:AuthScheme=OAuth;Server=keep;OAuthClientId=old-client;Scope=old-scope;");
        saved.getProperties().put("oauthclientid", "stale-client");
        saved.getProperties().put("Timeout", "90");
        var temporary = new DBPConnectionConfiguration(saved);
        hierarchy.loadConfiguration("test", temporary);
        Map<String, String> values = Map.of(
            "User", "user", "OAuthClientId", "new-client", "OAuthClientSecret", "secret", "Scope", "");
        for (var property : hierarchy.getCredentialProperties()) {
            hierarchy.setValue(property, values.get(property.name()), false);
        }
        hierarchy.saveCredentialValues("test", temporary);
        Assertions.assertEquals(Map.of("AuthScheme", "OAuth", "Server", "keep", "OAuthClientId", "new-client", "Scope", ""),
            CDataConnectionUrl.parse(temporary.getUrl(), "test"));
        Assertions.assertEquals(Map.of("Timeout", "90"), temporary.getProperties());
        Assertions.assertEquals("user", temporary.getUserName());
        Assertions.assertEquals(Map.of(CDataAuthModel.SECRET_PROPERTY_PREFIX + "OAuthClientSecret", "secret"),
            temporary.getAuthProperties());
        Assertions.assertEquals("old-client", CDataConnectionUrl.parse(saved.getUrl(), "test").get("OAuthClientId"));
        Assertions.assertEquals("old-scope", CDataConnectionUrl.parse(saved.getUrl(), "test").get("Scope"));
        Assertions.assertTrue(saved.getAuthProperties().isEmpty());
    }

    @Test
    public void credentialFieldsFollowTheSelectedScheme() throws Exception {
        var hierarchy = CDataConnectionHierarchy.parse(DEFINITION);
        Assertions.assertEquals(List.of("OAuthClientSecret"), hierarchy.getCredentialProperties().stream()
            .map(CDataConnectionHierarchy.Property::name).toList());
        hierarchy.setValue(hierarchy.getBasicProperties().getFirst(), "Password", false);
        Assertions.assertEquals(List.of("Password"), hierarchy.getCredentialProperties().stream()
            .map(CDataConnectionHierarchy.Property::name).toList());
    }

    @Test
    public void applyingPromptCredentialsPreservesConnectionSettingsAndSavedSnapshot() throws Exception {
        var hierarchy = CDataConnectionHierarchy.parse(DEFINITION);
        var saved = new DBPConnectionConfiguration();
        String url = "jdbc:cdata:test:Server=manual;AuthScheme=OAuth;CustomOption='quoted;value';";
        saved.setUrl(url);
        saved.getProperties().put("Timeout", "90");
        saved.setAuthProperty(CDataAuthModel.SECRET_PROPERTY_PREFIX + "SSHPassword", "ssh-secret");
        var temporary = new DBPConnectionConfiguration(saved);
        hierarchy.loadConfiguration("test", temporary);
        hierarchy.setValue(hierarchy.getCredentialProperties().getFirst(), "prompt-secret", false);
        hierarchy.saveCredentialValues("test", temporary);
        Assertions.assertEquals(url, temporary.getUrl());
        Assertions.assertEquals(Map.of("Timeout", "90"), temporary.getProperties());
        Assertions.assertEquals("ssh-secret", temporary.getAuthProperty(CDataAuthModel.SECRET_PROPERTY_PREFIX + "SSHPassword"));
        Assertions.assertEquals("prompt-secret", temporary.getAuthProperty(CDataAuthModel.SECRET_PROPERTY_PREFIX + "OAuthClientSecret"));
        Assertions.assertNull(saved.getAuthProperty(CDataAuthModel.SECRET_PROPERTY_PREFIX + "OAuthClientSecret"));
    }

    @Test
    public void promptCredentialsReplaceStaleUrlAndDriverPropertyValues() throws Exception {
        var hierarchy = CDataConnectionHierarchy.parse(DEFINITION);
        var configuration = new DBPConnectionConfiguration();
        configuration.setUrl("jdbc:test:Server=manual;OAuthClientSecret=old-url-secret;");
        configuration.getProperties().put("oauthclientsecret", "old-property-secret");
        hierarchy.loadConfiguration("test", configuration);
        hierarchy.setValue(hierarchy.getCredentialProperties().getFirst(), "prompt-secret", false);
        hierarchy.saveCredentialValues("test", configuration);
        Assertions.assertEquals(Map.of("Server", "manual"), CDataConnectionUrl.parse(configuration.getUrl(), "test"));
        Assertions.assertTrue(configuration.getProperties().isEmpty());
        Assertions.assertEquals("prompt-secret", configuration.getAuthProperty(CDataAuthModel.SECRET_PROPERTY_PREFIX + "OAuthClientSecret"));
    }

    @Test
    public void passwordPromptUpdatesNativeCredentials() throws Exception {
        var hierarchy = CDataConnectionHierarchy.parse(DEFINITION);
        var configuration = new DBPConnectionConfiguration();
        configuration.setUrl("jdbc:test:AuthScheme=Password;Server=manual;");
        hierarchy.loadConfiguration("test", configuration);
        hierarchy.setValue(hierarchy.getCredentialProperties().getFirst(), "prompt-password", false);
        hierarchy.saveCredentialValues("test", configuration);
        Assertions.assertEquals("prompt-password", configuration.getUserPassword());
        Assertions.assertTrue(configuration.getAuthProperties().isEmpty());
    }

    @Test
    public void recognizesSensitiveFieldsWithoutCredentialFlag() throws Exception {
        var hierarchy = CDataConnectionHierarchy.parse(DEFINITION);
        Assertions.assertTrue(hierarchy.getAuthenticationProperties().get(1).password());
        Assertions.assertTrue(hierarchy.getAdvancedGroups().getFirst().properties().getFirst().password());
        Assertions.assertFalse(hierarchy.getBasicProperties().getFirst().password());
    }

    @Test
    public void storesAndReloadsSecretsThroughSecureCredentials() throws Exception {
        var hierarchy = CDataConnectionHierarchy.parse(DEFINITION);
        hierarchy.loadValues(Map.of("OAuthClientSecret", "client-secret", "SSHPassword", "ssh-secret"));
        var configuration = new DBPConnectionConfiguration();
        configuration.setAuthProperty("unrelated", "keep");
        hierarchy.saveConfiguration("test", configuration);
        Assertions.assertFalse(configuration.getUrl().contains("secret"));
        Assertions.assertEquals("client-secret",
            configuration.getAuthProperty(CDataAuthModel.SECRET_PROPERTY_PREFIX + "OAuthClientSecret"));
        Assertions.assertEquals("ssh-secret", configuration.getAuthProperty(CDataAuthModel.SECRET_PROPERTY_PREFIX + "SSHPassword"));
        Assertions.assertEquals("keep", configuration.getAuthProperty("unrelated"));
        var dataSource = Mockito.mock(DBPDataSourceContainer.class);
        Mockito.when(dataSource.getConnectionConfiguration()).thenReturn(configuration);
        var credentials = new SecureCredentials(dataSource);
        Assertions.assertEquals(configuration.getAuthProperties(), credentials.getProperties());
        var restored = new DBPConnectionConfiguration();
        restored.setUrl(configuration.getUrl());
        restored.setAuthProperties(credentials.getProperties());
        hierarchy = CDataConnectionHierarchy.parse(DEFINITION);
        hierarchy.loadConfiguration("test", restored);
        Assertions.assertEquals("client-secret", hierarchy.getConnectionProperties().get("OAuthClientSecret"));
        Assertions.assertEquals("ssh-secret", hierarchy.getConnectionProperties().get("SSHPassword"));
    }

    @Test
    public void migratesLegacyUrlAndDriverSecretsWithUrlPriority() throws Exception {
        var hierarchy = CDataConnectionHierarchy.parse(DEFINITION);
        var configuration = new DBPConnectionConfiguration();
        configuration.setUrl("jdbc:test:OAuthClientSecret=url-secret;SSHPassword=url-ssh;CustomOption=keep;");
        configuration.getProperties().put("oauthclientsecret", "properties-secret");
        configuration.getProperties().put("sshpassword", "properties-ssh");
        configuration.getProperties().put("Timeout", "90");
        configuration.setAuthProperty(CDataAuthModel.SECRET_PROPERTY_PREFIX + "OAuthClientSecret", "stored-secret");
        hierarchy.loadConfiguration("test", configuration);
        hierarchy.saveConfiguration("test", configuration);
        Assertions.assertEquals("url-secret", configuration.getAuthProperty(CDataAuthModel.SECRET_PROPERTY_PREFIX + "OAuthClientSecret"));
        Assertions.assertEquals("url-ssh", configuration.getAuthProperty(CDataAuthModel.SECRET_PROPERTY_PREFIX + "SSHPassword"));
        Assertions.assertEquals(Map.of("Timeout", "90"), configuration.getProperties());
        Assertions.assertEquals(Map.of("AuthScheme",
            "OAuth", "CustomOption", "keep"), CDataConnectionUrl.parse(configuration.getUrl(), "test"));
    }

    @Test
    public void preservesNewAdvancedSecretEdits() throws Exception {
        var hierarchy = CDataConnectionHierarchy.parse(DEFINITION);
        var configuration = new DBPConnectionConfiguration();
        configuration.setAuthProperty(CDataAuthModel.SECRET_PROPERTY_PREFIX + "SSHPassword", "old-secret");
        hierarchy.loadConfiguration("test", configuration);
        configuration.getProperties().put("sshpassword", "new-secret");
        hierarchy.saveConfiguration("test", configuration);
        Assertions.assertEquals("new-secret", configuration.getAuthProperty(CDataAuthModel.SECRET_PROPERTY_PREFIX + "SSHPassword"));
        Assertions.assertTrue(configuration.getProperties().isEmpty());
        Assertions.assertFalse(configuration.getUrl().contains("secret"));
    }

    @Test
    public void preservesAdvancedSecretEditsInUrlMode() throws Exception {
        var hierarchy = CDataConnectionHierarchy.parse(DEFINITION);
        var configuration = new DBPConnectionConfiguration();
        configuration.setAuthProperty(CDataAuthModel.SECRET_PROPERTY_PREFIX + "SSHPassword", "old-secret");
        hierarchy.loadConfiguration("test", configuration);
        configuration.getProperties().put("sshpassword", "new-secret");
        hierarchy.saveUrlConfiguration("test", configuration);
        Assertions.assertEquals("new-secret", configuration.getAuthProperty(CDataAuthModel.SECRET_PROPERTY_PREFIX + "SSHPassword"));
        Assertions.assertTrue(configuration.getProperties().isEmpty());
        Assertions.assertFalse(configuration.getUrl().contains("secret"));
    }

    @Test
    public void retainsProtectionForStoredSecretsMissingFromNewMetadata() throws Exception {
        var hierarchy = CDataConnectionHierarchy.parse(DEFINITION);
        var configuration = new DBPConnectionConfiguration();
        configuration.setAuthProperty(CDataAuthModel.SECRET_PROPERTY_PREFIX + "LegacyToken", "legacy-secret");
        hierarchy.loadConfiguration("test", configuration);
        hierarchy.saveConfiguration("test", configuration);
        Assertions.assertEquals("legacy-secret", configuration.getAuthProperty(CDataAuthModel.SECRET_PROPERTY_PREFIX + "LegacyToken"));
        Assertions.assertFalse(configuration.getUrl().contains("legacy-secret"));
    }

    @Test
    public void migratesDriverPropertiesWithoutUrlSecrets() throws Exception {
        var hierarchy = CDataConnectionHierarchy.parse(DEFINITION);
        var configuration = new DBPConnectionConfiguration();
        configuration.getProperties().put("oauthclientsecret", "driver-secret");
        configuration.getProperties().put("SSHPassword", "driver-ssh");
        hierarchy.loadConfiguration("test", configuration);
        hierarchy.saveConfiguration("test", configuration);
        Assertions.assertEquals("driver-secret",
            configuration.getAuthProperty(CDataAuthModel.SECRET_PROPERTY_PREFIX + "OAuthClientSecret"));
        Assertions.assertEquals("driver-ssh", configuration.getAuthProperty(CDataAuthModel.SECRET_PROPERTY_PREFIX + "SSHPassword"));
        Assertions.assertTrue(configuration.getProperties().isEmpty());
    }

    @Test
    public void manualUrlSecretsOverridePreviouslyLoadedCredentialsIncludingEmptyValues() throws Exception {
        for (String secret : java.util.List.of("typed-secret", "")) {
            var hierarchy = CDataConnectionHierarchy.parse(DEFINITION);
            var configuration = new DBPConnectionConfiguration();
            configuration.setAuthProperty(CDataAuthModel.SECRET_PROPERTY_PREFIX + "OAuthClientSecret", "stored-secret");
            hierarchy.loadConfiguration("test", configuration);
            configuration.setUrl("jdbc:test:Server=manual;OAuthClientSecret=" + secret + ";CustomOption=keep;");
            hierarchy.saveUrlConfiguration("test", configuration);
            Assertions.assertEquals(secret, configuration.getAuthProperty(CDataAuthModel.SECRET_PROPERTY_PREFIX + "OAuthClientSecret"));
            Assertions.assertEquals(Map.of("Server",
            "manual", "CustomOption", "keep"), CDataConnectionUrl.parse(configuration.getUrl(), "test"));
        }
    }

    @Test
    public void keepsManualUrlFormattingWhenItContainsNoCredentials() throws Exception {
        var hierarchy = CDataConnectionHierarchy.parse(DEFINITION);
        var configuration = new DBPConnectionConfiguration();
        String url = "jdbc:cdata:test:Server=manual;Port=5432;CustomOption='quoted;value';";
        configuration.setUrl(url);
        hierarchy.loadConfiguration("test", configuration);
        hierarchy.saveUrlConfiguration("test", configuration);
        Assertions.assertEquals(url, configuration.getUrl());
    }

    @Test
    public void editingSecretInUrlModeKeepsItOutOfUrl() throws Exception {
        var hierarchy = CDataConnectionHierarchy.parse(DEFINITION);
        var property = hierarchy.getAuthenticationProperties().get(1);
        hierarchy.setValue(property, "edited-secret", false);
        var configuration = new DBPConnectionConfiguration();
        configuration.setUrl(hierarchy.updateUrl("test", "jdbc:test:Server=manual;OAuthClientSecret=old-secret;",
            CDataConnectionHierarchy.getPropertyNames(property)));
        Assertions.assertFalse(configuration.getUrl().contains("secret"));
        hierarchy.saveUrlConfiguration("test", configuration);
        Assertions.assertEquals("edited-secret",
            configuration.getAuthProperty(CDataAuthModel.SECRET_PROPERTY_PREFIX + "OAuthClientSecret"));
    }

    @Test
    public void removesInactiveSecretsAndHonorsDisablingPasswordStorage() throws Exception {
        var hierarchy = CDataConnectionHierarchy.parse(DEFINITION);
        var configuration = new DBPConnectionConfiguration();
        configuration.setAuthProperty(CDataAuthModel.SECRET_PROPERTY_PREFIX + "OAuthClientSecret", "old-secret");
        configuration.getProperties().put("OAuthClientSecret", "old-secret");
        hierarchy.loadConfiguration("test", configuration);
        hierarchy.setValue(hierarchy.getBasicProperties().getFirst(), "Password", false);
        hierarchy.saveConfiguration("test", configuration);
        Assertions.assertNull(configuration.getAuthProperty(CDataAuthModel.SECRET_PROPERTY_PREFIX + "OAuthClientSecret"));
        Assertions.assertTrue(configuration.getProperties().isEmpty());
        configuration.setAuthProperty("unrelated", "keep");
        configuration.setAuthProperty(CDataAuthModel.SECRET_PROPERTY_PREFIX + "SSHPassword", "secret");
        CDataAuthModel.clearSecrets(configuration);
        Assertions.assertEquals(Map.of("unrelated", "keep"), configuration.getAuthProperties());
    }

    @Test
    public void suppliesSecretsOnlyWhenCollectingSecuredConnectionProperties() {
        var configuration = new DBPConnectionConfiguration();
        configuration.setAuthProperty(CDataAuthModel.SECRET_PROPERTY_PREFIX + "OAuthClientSecret", "secret");
        configuration.setAuthProperty("unrelated", "keep");
        var model = new CDataAuthModel();
        var credentials = new AuthModelDatabaseNativeCredentials();
        var dataSource = Mockito.mock(DBPDataSourceContainer.class);
        credentials.setUserName("user");
        credentials.setUserPassword("password");
        var properties = new Properties();
        model.collectConnectionProperties(dataSource, credentials, configuration, properties, false);
        Assertions.assertFalse(properties.containsValue("secret"));
        Assertions.assertFalse(properties.containsValue("password"));
        properties.setProperty("oauthclientsecret", "stale");
        model.collectConnectionProperties(dataSource, credentials, configuration, properties, true);
        Assertions.assertEquals("secret", properties.getProperty("OAuthClientSecret"));
        Assertions.assertEquals("password", properties.getProperty("password"));
        Assertions.assertFalse(properties.containsKey("oauthclientsecret"));
        Assertions.assertFalse(properties.containsKey("unrelated"));
        Assertions.assertFalse(properties.containsKey(CDataAuthModel.SECRET_PROPERTY_PREFIX + "OAuthClientSecret"));
    }
}

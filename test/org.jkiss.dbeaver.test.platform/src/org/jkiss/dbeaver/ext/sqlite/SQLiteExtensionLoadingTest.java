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
package org.jkiss.dbeaver.ext.sqlite;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.StringTemplate;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

public class SQLiteExtensionLoadingTest {
    private static final String URL = "jdbc:sqlite::memory:";

    @TempDir
    Path directory;

    @Test
    public void approvesLoadingOnDemandAndRemembersItForReconnect() throws Exception {
        DBPDataSourceContainer container = container();
        container.getConnectionConfiguration().setProviderProperty(SQLiteExtensionLoading.PROP_ENABLED, "false");
        Assertions.assertTrue(SQLiteExtensionLoading.requestEnable(container, store(), settings -> {
            Assertions.assertTrue(settings.enabled());
            Assertions.assertEquals("false", container.getConnectionConfiguration().getProviderProperty(
                SQLiteExtensionLoading.PROP_ENABLED));
            return true;
        }));
        Assertions.assertEquals("true", container.getConnectionConfiguration().getProviderProperty(SQLiteExtensionLoading.PROP_ENABLED));
        Mockito.verify(container.getRegistry()).updateDataSource(container);
        Properties properties = new Properties();
        new SQLiteExtensionLoading().validate(SQLiteExtensionSettings.capture(container), properties, store(),
            () -> Assertions.fail("Unexpected repeat confirmation after reconnect"));
        Assertions.assertEquals("true", properties.getProperty(SQLiteExtensionLoading.ENABLE_LOAD_EXTENSION));
    }

    @Test
    public void decliningOnDemandApprovalPreservesDisabledConfiguration() throws Exception {
        DBPDataSourceContainer container = container();
        Assertions.assertFalse(SQLiteExtensionLoading.requestEnable(container, store(), settings -> false));
        Assertions.assertFalse(SQLiteExtensionSettings.capture(container).enabled());
        Assertions.assertFalse(store().isApproved(SQLiteExtensionSettings.captureForLoading(container)));
        Mockito.verify(container.getRegistry(), Mockito.never()).updateDataSource(Mockito.any());
    }

    @Test
    public void changingConfigurationDuringApprovalDoesNotEnableLoading() throws Exception {
        DBPDataSourceContainer container = container();
        Assertions.assertThrows(DBException.class, () -> SQLiteExtensionLoading.requestEnable(container, store(), settings -> {
            container.getConnectionConfiguration().setUrl("jdbc:sqlite:changed.db");
            return true;
        }));
        Assertions.assertFalse(SQLiteExtensionSettings.capture(container).enabled());
        Assertions.assertFalse(store().isApproved(SQLiteExtensionSettings.captureForLoading(container)));
    }

    @Test
    public void temporaryConnectionCannotPersistOnDemandEnablement() throws Exception {
        DBPDataSourceContainer container = container();
        Mockito.when(container.isTemporary()).thenReturn(true);
        Assertions.assertFalse(SQLiteExtensionLoading.requestEnable(container, store(),
            settings -> Assertions.fail("Unexpected confirmation for temporary connection")));
    }

    @Test
    public void defaultConnectionForcesLoadingOffWithoutConfirmation() throws Exception {
        DBPDataSourceContainer container = container();
        Properties properties = new Properties();
        new SQLiteExtensionLoading().validate(SQLiteExtensionSettings.capture(container), properties, store(),
            () -> Assertions.fail("Unexpected confirmation"));
        Assertions.assertEquals("false", properties.getProperty(SQLiteExtensionLoading.ENABLE_LOAD_EXTENSION));
    }

    @Test
    public void incompleteNewConnectionCanRenderExtensionSettings() throws Exception {
        DBPDataSourceContainer container = container();
        container.getConnectionConfiguration().setUrl(null);
        Mockito.when(container.getDriver().getConnectionURL(Mockito.any(DBPConnectionConfiguration.class)))
            .thenThrow(new DBException("Failed to generate database URL",
                new StringTemplate.StringTemplateMissingParametersException("jdbc:sqlite:{file}", "Missing file")));
        SQLiteExtensionSettings settings = SQLiteExtensionSettings.capture(container);
        Assertions.assertEquals("", settings.url());
        Assertions.assertFalse(settings.enabled());
        Mockito.when(container.getDriver().getConnectionURL(Mockito.any(DBPConnectionConfiguration.class)))
            .thenThrow(new DBException("Unexpected driver failure"));
        Assertions.assertThrows(DBException.class, () -> SQLiteExtensionSettings.capture(container));
    }

    @Test
    public void parsesUrlAsTheDriverDoes() {
        for (String parameter : new String[] {
            "enable_load_extension=true", "ENABLE_LOAD_EXTENSION=TrUe", " enable_load_extension = true ",
            "enable_load_extension=true=ignored", "other=value&enable_load_extension=true",
            "enable_load_extension=false&enable_load_extension=true", "enable_load_extension=true&enable_load_extension= &other=1"
        }) {
            Assertions.assertTrue(SQLiteExtensionLoading.isEnabled(URL + "?" + parameter, new Properties()), parameter);
        }
        for (String parameter : new String[] {
            "other=enable_load_extension=true", "enable_load_extension=false", "enable_load_extension=1",
            "enable_load_extension=", "enable_load_extension_other=true", "enable_load_extension=%74rue",
            "enable_load_extension=true&enable_load_extension=false"
        }) {
            Assertions.assertFalse(SQLiteExtensionLoading.isEnabled(URL + "?" + parameter, new Properties()), parameter);
        }
        Assertions.assertFalse(SQLiteExtensionLoading.isEnabled(URL + "?enable_load_extension=true", properties("false")));
        Assertions.assertTrue(SQLiteExtensionLoading.isEnabled(URL + "?enable_load_extension=false", properties("true")));
        Assertions.assertTrue(SQLiteExtensionLoading.isEnabled(null, properties("true")));
    }

    @Test
    public void importedEnablementDoesNotGrantPermission() throws Exception {
        DBPDataSourceContainer container = container();
        container.getConnectionConfiguration().setProviderProperty(SQLiteExtensionLoading.PROP_ENABLED, "true");
        SQLiteExtensionSettings settings = SQLiteExtensionSettings.capture(container);
        SQLiteExtensionPermissionStore permissions = store();
        Assertions.assertTrue(settings.enabled());
        Assertions.assertFalse(permissions.isApproved(settings));
        for (int attempt = 0; attempt < 2; attempt++) {
            Assertions.assertThrows(DBCException.class,
                () -> new SQLiteExtensionLoading().validate(settings, new Properties(), permissions, () -> false));
        }
        Assertions.assertFalse(permissions.isApproved(settings));
    }

    @Test
    public void permissionSurvivesNewValidatorAndStore() throws Exception {
        SQLiteExtensionSettings settings = enabledSettings();
        new SQLiteExtensionLoading().validate(settings, new Properties(), store(), () -> true);
        new SQLiteExtensionLoading().validate(settings, new Properties(), store(),
            () -> Assertions.fail("Unexpected repeat confirmation"));
        String saved = Files.readString(directory.resolve("permissions.json"));
        Assertions.assertFalse(saved.contains(URL));
        Assertions.assertFalse(saved.contains("SELECT"));
    }

    @Test
    public void changedSettingsInvalidatePreviousPermissionWithoutResurrectingIt() throws Exception {
        DBPDataSourceContainer container = container();
        DBPConnectionConfiguration configuration = container.getConnectionConfiguration();
        configuration.setProviderProperty(SQLiteExtensionLoading.PROP_ENABLED, "true");
        final SQLiteExtensionSettings original = SQLiteExtensionSettings.capture(container);
        SQLiteExtensionPermissionStore permissions = store();
        permissions.approve(original);
        configuration.getBootstrap().getInitQueries().add("SELECT load_extension('new-library')");
        SQLiteExtensionSettings changed = SQLiteExtensionSettings.capture(container);
        Assertions.assertFalse(permissions.isApproved(changed));
        Assertions.assertTrue(permissions.isApproved(original));
        Assertions.assertThrows(DBCException.class,
            () -> new SQLiteExtensionLoading().validate(changed, new Properties(), permissions, () -> false));
        Assertions.assertFalse(permissions.isApproved(original));
    }

    @Test
    public void disablingOverridesUrlPropertiesAndPreservesLibraries() throws Exception {
        DBPDataSourceContainer container = container();
        DBPConnectionConfiguration configuration = container.getConnectionConfiguration();
        configuration.setUrl(URL + "?enable_load_extension=true");
        configuration.setProperty(SQLiteExtensionLoading.ENABLE_LOAD_EXTENSION, "true");
        configuration.setProviderProperty(SQLiteExtensionLoading.PROP_EXTENSIONS, "example-library");
        configuration.setProviderProperty(SQLiteExtensionLoading.PROP_ENABLED, "false");
        SQLiteExtensionSettings settings = SQLiteExtensionSettings.capture(container);
        Properties properties = properties("true");
        new SQLiteExtensionLoading().validate(settings, properties, store(), () -> Assertions.fail("Unexpected confirmation"));
        Assertions.assertFalse(settings.enabled());
        Assertions.assertEquals("false", properties.getProperty(SQLiteExtensionLoading.ENABLE_LOAD_EXTENSION));
        Assertions.assertEquals(List.of("example-library"), settings.libraries());
    }

    @Test
    public void legacyConfigurationRequestsApproval() throws Exception {
        DBPDataSourceContainer container = container();
        DBPConnectionConfiguration configuration = container.getConnectionConfiguration();
        configuration.setProviderProperty(SQLiteExtensionLoading.PROP_EXTENSIONS, "example-library");
        Assertions.assertTrue(SQLiteExtensionSettings.capture(container).enabled());
        configuration.removeProviderProperty(SQLiteExtensionLoading.PROP_EXTENSIONS);
        configuration.setUrl(URL + "?enable_load_extension=true");
        Assertions.assertTrue(SQLiteExtensionSettings.capture(container).enabled());
        configuration.setUrl(URL);
        configuration.setProperty(SQLiteExtensionLoading.ENABLE_LOAD_EXTENSION, "true");
        Assertions.assertTrue(SQLiteExtensionSettings.capture(container).enabled());
        configuration.removeProperty(SQLiteExtensionLoading.ENABLE_LOAD_EXTENSION);
        container.getDriver().getConnectionProperties().put(SQLiteExtensionLoading.ENABLE_LOAD_EXTENSION, "true");
        Assertions.assertTrue(SQLiteExtensionSettings.capture(container).enabled());
    }

    @Test
    public void trustIsIsolatedByProjectAndConnection() throws Exception {
        DBPDataSourceContainer first = container();
        first.getConnectionConfiguration().setProviderProperty(SQLiteExtensionLoading.PROP_ENABLED, "true");
        SQLiteExtensionPermissionStore permissions = store();
        permissions.approve(SQLiteExtensionSettings.capture(first));
        Mockito.when(first.getId()).thenReturn("imported-connection");
        Assertions.assertFalse(permissions.isApproved(SQLiteExtensionSettings.capture(first)));
        Mockito.when(first.getId()).thenReturn("connection");
        Mockito.when(first.getProject().getAbsolutePath()).thenReturn(directory.resolve("imported-project"));
        Assertions.assertFalse(permissions.isApproved(SQLiteExtensionSettings.capture(first)));
    }

    @Test
    public void renameAndCredentialsDoNotInvalidatePermission() throws Exception {
        DBPDataSourceContainer container = container();
        container.getConnectionConfiguration().setProviderProperty(SQLiteExtensionLoading.PROP_ENABLED, "true");
        final SQLiteExtensionSettings original = SQLiteExtensionSettings.capture(container);
        container.getConnectionConfiguration().setUserName("new-user");
        container.getConnectionConfiguration().setUserPassword("new-password");
        Mockito.when(container.getName()).thenReturn("new-name");
        Assertions.assertEquals(original.fingerprint(), SQLiteExtensionSettings.capture(container).fingerprint());
    }

    @Test
    public void urlLibrariesAndLoadingPropertiesInvalidatePermission() throws Exception {
        DBPDataSourceContainer container = container();
        DBPConnectionConfiguration configuration = container.getConnectionConfiguration();
        configuration.setProviderProperty(SQLiteExtensionLoading.PROP_ENABLED, "true");
        final SQLiteExtensionSettings original = SQLiteExtensionSettings.capture(container);
        configuration.setUrl(URL + "new");
        Assertions.assertNotEquals(original.fingerprint(), SQLiteExtensionSettings.capture(container).fingerprint());
        configuration.setUrl(URL);
        configuration.setProviderProperty(SQLiteExtensionLoading.PROP_EXTENSIONS, "new-library");
        Assertions.assertNotEquals(original.fingerprint(), SQLiteExtensionSettings.capture(container).fingerprint());
        configuration.removeProviderProperty(SQLiteExtensionLoading.PROP_EXTENSIONS);
        configuration.setProperty(SQLiteExtensionLoading.ENABLE_LOAD_EXTENSION, "false");
        Assertions.assertNotEquals(original.fingerprint(), SQLiteExtensionSettings.capture(container).fingerprint());
    }

    @Test
    public void bootstrapVariableChangesRequireNewApproval() throws Exception {
        DBPDataSourceContainer container = container();
        DBPConnectionConfiguration configuration = container.getConnectionConfiguration();
        configuration.setProviderProperty(SQLiteExtensionLoading.PROP_ENABLED, "true");
        configuration.getBootstrap().getInitQueries().add("SELECT load_extension('${extension_path}')");
        configuration.setProperty("extension_path", "first-library");
        final SQLiteExtensionSettings original = SQLiteExtensionSettings.capture(container);
        Assertions.assertEquals(List.of("SELECT load_extension('first-library')"), original.initQueries());
        configuration.setProperty("extension_path", "other-library");
        Assertions.assertNotEquals(original.fingerprint(), SQLiteExtensionSettings.capture(container).fingerprint());
    }

    @Test
    public void temporaryTestPermissionIsNotSavedForTheRealConnection() throws Exception {
        DBPDataSourceContainer container = container();
        container.getConnectionConfiguration().setProviderProperty(SQLiteExtensionLoading.PROP_ENABLED, "true");
        final SQLiteExtensionSettings real = SQLiteExtensionSettings.capture(container);
        Mockito.when(container.isTemporary()).thenReturn(true);
        SQLiteExtensionSettings temporary = SQLiteExtensionSettings.capture(container);
        SQLiteExtensionPermissionStore permissions = store();
        permissions.approve(temporary);
        Assertions.assertTrue(permissions.isApproved(temporary));
        Assertions.assertFalse(permissions.isApproved(real));
        Assertions.assertFalse(Files.exists(directory.resolve("permissions.json")));
    }

    @Test
    public void trustedConnectionCanBeTestedWithoutRenewingApproval() throws Exception {
        DBPDataSourceContainer original = container();
        original.getConnectionConfiguration().setProviderProperty(SQLiteExtensionLoading.PROP_ENABLED, "true");
        SQLiteExtensionPermissionStore permissions = store();
        SQLiteExtensionSettings trusted = SQLiteExtensionSettings.capture(original);
        permissions.approve(trusted);
        DBPDataSourceContainer temporary = container();
        Mockito.when(temporary.isTemporary()).thenReturn(true);
        Mockito.when(temporary.getId()).thenReturn("temporary-id");
        temporary.getConnectionConfiguration().setProviderProperty(SQLiteExtensionLoading.PROP_ENABLED, "true");
        temporary.getConnectionConfiguration().setRuntimeAttribute(DBPConnectionConfiguration.ATTRIBUTE_TEST_CONNECTION_SOURCE, original);
        SQLiteExtensionSettings test = SQLiteExtensionSettings.capture(temporary);
        Assertions.assertEquals(trusted.connectionKey(), test.connectionKey());
        new SQLiteExtensionLoading().validate(test, new Properties(), permissions,
            () -> Assertions.fail("Unexpected repeat confirmation"));
        temporary.getConnectionConfiguration().getBootstrap().getInitQueries().add("SELECT 1");
        SQLiteExtensionSettings changed = SQLiteExtensionSettings.capture(temporary);
        Assertions.assertFalse(permissions.isApproved(changed));
        Assertions.assertTrue(permissions.isApproved(trusted));
    }

    @Test
    public void temporaryApprovalCannotBeReusedByAnotherTestOrAfterCancellingTheEditor() throws Exception {
        DBPDataSourceContainer original = container();
        DBPDataSourceContainer temporary = container();
        Mockito.when(temporary.isTemporary()).thenReturn(true);
        Mockito.when(temporary.getId()).thenReturn("first-test");
        temporary.getConnectionConfiguration().setProviderProperty(SQLiteExtensionLoading.PROP_ENABLED, "true");
        temporary.getConnectionConfiguration().setRuntimeAttribute(DBPConnectionConfiguration.ATTRIBUTE_TEST_CONNECTION_SOURCE, original);
        SQLiteExtensionPermissionStore permissions = store();
        SQLiteExtensionSettings first = SQLiteExtensionSettings.capture(temporary);
        permissions.approve(first);
        Assertions.assertTrue(permissions.isApproved(first));
        Mockito.when(temporary.getId()).thenReturn("second-test");
        SQLiteExtensionSettings second = SQLiteExtensionSettings.capture(temporary);
        Assertions.assertEquals(first.connectionKey(), second.connectionKey());
        Assertions.assertFalse(permissions.isApproved(second));
    }

    @Test
    public void recognizesDisabledExtensionErrorsWithoutMatchingOtherAuthorizationFailures() {
        Assertions.assertTrue(SQLiteExtensionLoading.isLoadingDenied(
            new DBException("Error executing SELECT load_extension('library')", new java.sql.SQLException("not authorized"))));
        Assertions.assertTrue(SQLiteExtensionLoading.isLoadingDenied(new java.sql.SQLException("no such function: load_extension")));
        Assertions.assertFalse(SQLiteExtensionLoading.isLoadingDenied(new java.sql.SQLException("not authorized")));
        Assertions.assertFalse(SQLiteExtensionLoading.isLoadingDenied(new java.sql.SQLException("load_extension: file not found")));
    }

    @Test
    public void migratesPreviousLocalApprovalsWithoutExportingThem() throws Exception {
        SQLiteExtensionSettings settings = enabledSettings();
        Path legacy = directory.resolve("legacy.properties");
        Properties values = new Properties();
        values.setProperty(settings.connectionKey(), settings.fingerprint());
        try (var writer = Files.newBufferedWriter(legacy)) {
            values.store(writer, null);
        }
        Path file = directory.resolve("permissions.json");
        SQLiteExtensionPermissionStore permissions = new SQLiteExtensionPermissionStore(file, legacy);
        Assertions.assertTrue(permissions.isApproved(settings));
        permissions.revoke(settings);
        Assertions.assertFalse(new SQLiteExtensionPermissionStore(file, legacy).isApproved(settings));
    }

    @Test
    public void revokeRemovesSavedPermission() throws Exception {
        SQLiteExtensionSettings settings = enabledSettings();
        SQLiteExtensionPermissionStore permissions = store();
        permissions.approve(settings);
        permissions.revoke(settings);
        Assertions.assertFalse(store().isApproved(settings));
    }

    @Test
    public void unreadableStoreCannotEnableNativeCode() throws Exception {
        Path path = directory.resolve("permissions.json");
        Files.createDirectory(path);
        Assertions.assertThrows(DBCException.class,
            () -> new SQLiteExtensionLoading().validate(enabledSettings(), new Properties(), store(),
                () -> Assertions.fail("Must not grant permission when storage cannot be read")));
    }

    @NotNull
    private SQLiteExtensionSettings enabledSettings() throws DBException {
        DBPDataSourceContainer container = container();
        container.getConnectionConfiguration().setProviderProperty(SQLiteExtensionLoading.PROP_ENABLED, "true");
        return SQLiteExtensionSettings.capture(container);
    }

    @NotNull
    private SQLiteExtensionPermissionStore store() {
        return new SQLiteExtensionPermissionStore(directory.resolve("permissions.json"));
    }

    @NotNull
    private DBPDataSourceContainer container() {
        DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class, Mockito.RETURNS_DEEP_STUBS);
        DBPConnectionConfiguration configuration = new DBPConnectionConfiguration();
        configuration.setUrl(URL);
        Mockito.when(container.getConnectionConfiguration()).thenReturn(configuration);
        Mockito.when(container.getActualConnectionConfiguration()).thenReturn(configuration);
        Mockito.when(container.getId()).thenReturn("connection");
        Mockito.when(container.getName()).thenReturn("SQLite test");
        Mockito.when(container.getDriver().getDriverClassName()).thenReturn("org.sqlite.JDBC");
        Mockito.when(container.getDriver().getFullId()).thenReturn("sqlite:sqlite_jdbc");
        Mockito.when(container.getDriver().getConnectionProperties()).thenReturn(new HashMap<>());
        Mockito.when(container.getProject().getAbsolutePath()).thenReturn(directory.resolve("project"));
        return container;
    }

    @NotNull
    private static Properties properties(@NotNull String value) {
        Properties properties = new Properties();
        properties.setProperty(SQLiteExtensionLoading.ENABLE_LOAD_EXTENSION, value);
        return properties;
    }
}

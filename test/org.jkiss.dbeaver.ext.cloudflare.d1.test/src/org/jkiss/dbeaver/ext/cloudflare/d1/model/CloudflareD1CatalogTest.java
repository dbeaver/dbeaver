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
package org.jkiss.dbeaver.ext.cloudflare.d1.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.cloudflare.d1.CloudflareD1ConnectionSettings;
import org.jkiss.dbeaver.ext.cloudflare.d1.edit.CloudflareD1CatalogManager;
import org.jkiss.dbeaver.ext.cloudflare.d1.internal.CloudflareD1ModelMessages;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.sqlite.model.SQLiteTable;
import org.jkiss.dbeaver.ext.sqlite.model.SQLiteTableForeignKey;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.qm.QMExecutionHandler;
import org.jkiss.dbeaver.model.qm.QMUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.runtime.properties.DataSourcePropertyFilter;
import org.jkiss.dbeaver.runtime.properties.ObjectAttributeDescriptor;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.osgi.framework.FrameworkUtil;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

class CloudflareD1CatalogTest {
    private static Object previousQueryHandler;

    @Test
    void creationConfiguresTheDraftNameBeforeAddingTheCreateCommand() throws Exception {
        var source = Mockito.mock(CloudflareD1DataSource.class, Mockito.RETURNS_DEEP_STUBS);
        var context = Mockito.mock(DBECommandContext.class);
        Mockito.when(context.getUserParams()).thenReturn(new HashMap<>());
        var manager = new CloudflareD1CatalogManager() {
            @Override
            protected CloudflareD1Catalog configureObject(
                @NotNull DBRProgressMonitor monitor,
                @Nullable DBECommandContext commandContext,
                @Nullable Object parent,
                @NotNull CloudflareD1Catalog object,
                @NotNull Map<String, Object> options
            ) {
                Assertions.assertFalse(object.isPersisted());
                object.setName("new-database");
                object.setJurisdiction("eu");
                return object;
            }
        };

        var catalog = manager.createNewObject(Mockito.mock(DBRProgressMonitor.class),
            context, source, null, new HashMap<>());

        Assertions.assertNotNull(catalog);
        Assertions.assertEquals("new-database", catalog.getName());
        Assertions.assertEquals("eu", catalog.getJurisdiction());
        Assertions.assertTrue(context.getUserParams().containsKey(catalog));
        Mockito.verify(source, Mockito.never()).getDefaultInstance();
    }

    @BeforeAll
    static void initializeQueryManager() throws ReflectiveOperationException {
        var field = QMUtils.class.getDeclaredField("defaultHandler");
        field.setAccessible(true);
        previousQueryHandler = field.get(null);
        field.set(null, Mockito.mock(QMExecutionHandler.class));
    }

    @AfterAll
    static void restoreQueryManager() throws ReflectiveOperationException {
        var field = QMUtils.class.getDeclaredField("defaultHandler");
        field.setAccessible(true);
        field.set(null, previousQueryHandler);
    }

    @Test
    void catalogContextsDoNotReuseAnotherDatabaseUrl() {
        CloudflareD1DataSource dataSource = Mockito.mock(CloudflareD1DataSource.class, Mockito.RETURNS_DEEP_STUBS);
        var monitor = new VoidProgressMonitor();
        var first = new CloudflareD1Instance(dataSource, "first-id");
        var second = new CloudflareD1Instance(dataSource, "second-id");
        var firstContext = (CloudflareD1ExecutionContext) first.getDefaultContext(monitor, true);
        var secondContext = (CloudflareD1ExecutionContext) second.getDefaultContext(monitor, true);

        DBPConnectionConfiguration original = new DBPConnectionConfiguration();
        original.setDatabaseName("configured-id");
        original.setUrl("jdbc:d1://configured-id");
        original.setUserName("account");
        original.setUserPassword("test-token");
        original.setProperty("queryTimeout", "1234");

        var firstConfig = firstContext.prepareConfiguration(original);
        var secondConfig = secondContext.prepareConfiguration(original);
        Assertions.assertEquals("jdbc:d1://first-id", firstConfig.getUrl());
        Assertions.assertEquals("jdbc:d1://second-id", secondConfig.getUrl());
        Assertions.assertEquals("second-id", secondConfig.getDatabaseName());
        Assertions.assertEquals("account", secondConfig.getUserName());
        Assertions.assertEquals("test-token", secondConfig.getUserPassword());
        Assertions.assertEquals("1234", secondConfig.getProperty("queryTimeout"));
        Assertions.assertEquals("jdbc:d1://configured-id", original.getUrl());
        Assertions.assertEquals("configured-id", original.getDatabaseName());
        Assertions.assertSame(firstContext, first.getDefaultContext(monitor, false));
        Assertions.assertNotSame(firstContext, secondContext);
    }

    @Test
    void pinnedContextRejectsSwitchingToAnotherDatabaseBeforeOpeningConnection() {
        CloudflareD1DataSource dataSource = Mockito.mock(CloudflareD1DataSource.class, Mockito.RETURNS_DEEP_STUBS);
        var monitor = new VoidProgressMonitor();
        var instance = new CloudflareD1Instance(dataSource, "first-id");
        var context = (CloudflareD1ExecutionContext) instance.getDefaultContext(monitor, false);
        var other = Mockito.mock(CloudflareD1Catalog.class);
        Mockito.when(other.getDatabaseId()).thenReturn("second-id");
        Assertions.assertFalse(context.supportsCatalogChange());
        Assertions.assertThrows(DBCException.class, () -> context.setDefaultCatalog(monitor, other, null));
        Assertions.assertFalse(context.isConnected());
    }

    @Test
    void defaultContextSupportsActiveDatabaseSelectionWhilePinnedContextsStayIsolated() {
        var source = Mockito.mock(CloudflareD1DataSource.class, Mockito.RETURNS_DEEP_STUBS);
        var instance = new JDBCRemoteInstance(source) {};
        var context = new CloudflareD1ExecutionContext(instance, "Main");
        var pinned = new CloudflareD1ExecutionContext(new CloudflareD1Instance(source, "first-id"), "Main");

        Assertions.assertTrue(context.getContextDefaults().supportsCatalogChange());
        Assertions.assertFalse(pinned.supportsCatalogChange());
        Assertions.assertNotSame(context.getOwnerInstance(), pinned.getOwnerInstance());
        var original = new DBPConnectionConfiguration();
        original.setDatabaseName("configured-id");
        Assertions.assertEquals("first-id", pinned.prepareConfiguration(original).getDatabaseName());
    }

    @Test
    void databaseNamesAreNotAddedToSqlObjectNames() {
        Assertions.assertFalse(new CloudflareD1MetaModel().useCatalogInObjectNames());
    }

    @Test
    void mainSchemaTablesAreNotDiscardedEvenWithLegacyOmitSchemaSetting() throws Exception {
        CloudflareD1DataSource dataSource = Mockito.mock(CloudflareD1DataSource.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(dataSource.getContainer().getDriver().getDriverParameter("omit-schema")).thenReturn(true);
        Mockito.when(dataSource.isOmitSchema()).thenCallRealMethod();
        var owner = Mockito.mock(GenericStructContainer.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(owner.getDataSource()).thenReturn(dataSource);
        var row = Mockito.mock(JDBCResultSet.class);
        Mockito.when(row.getString("TABLE_NAME")).thenReturn("qa strange table");
        Mockito.when(row.getString("TABLE_SCHEM")).thenReturn("main");
        Mockito.when(row.getString("TABLE_TYPE")).thenReturn("TABLE");
        var table = new CloudflareD1MetaModel().createTableImpl(Mockito.mock(JDBCSession.class), owner, null, row);
        Assertions.assertNotNull(table);
        Assertions.assertEquals("qa strange table", table.getName());
    }

    @Test
    void databasesHaveDistinctQueryManagerInstanceIds() {
        CloudflareD1DataSource dataSource = Mockito.mock(CloudflareD1DataSource.class);
        Mockito.when(dataSource.getName()).thenReturn("same connection");
        var first = new CloudflareD1Instance(dataSource, "first-id");
        var second = new CloudflareD1Instance(dataSource, "second-id");
        Assertions.assertEquals("first-id", first.getName());
        Assertions.assertEquals("second-id", second.getName());
    }

    @Test
    void savingScopeRemovesCredentialAliasesAndPreservesNativeCredentials() {
        var configuration = new DBPConnectionConfiguration();
        configuration.setUserName("current-account");
        configuration.setUserPassword("current-test-token");
        configuration.setProperty("accountId", "old-account");
        configuration.setProperty("apiToken", "old-test-token");
        CloudflareD1ConnectionSettings.save(configuration, " initial-id ", true, " second-id,initial-id,second-id ");
        Assertions.assertEquals("initial-id", configuration.getDatabaseName());
        Assertions.assertEquals("initial-id,second-id", configuration.getProperty("databaseIds"));
        Assertions.assertFalse(configuration.getProperties().containsKey("accountId"));
        Assertions.assertFalse(configuration.getProperties().containsKey("apiToken"));
        Assertions.assertEquals("current-account", configuration.getUserName());
        Assertions.assertEquals("current-test-token", configuration.getUserPassword());
        Assertions.assertEquals("", configuration.getUrl());
        Assertions.assertFalse(configuration.getProperties().containsValue("current-test-token"));
    }

    @Test
    void scopeRoundTripIncludesInitialDatabaseOnceAndCanBeDisabled() {
        var configuration = new DBPConnectionConfiguration();
        CloudflareD1ConnectionSettings.save(configuration, "initial-id", true, "second-id,third-id");
        String additional = CloudflareD1ConnectionSettings.additionalIds(
            configuration.getDatabaseName(), configuration.getProperty("databaseIds"));
        Assertions.assertEquals("second-id,third-id", additional);
        CloudflareD1ConnectionSettings.save(configuration, "initial-id", true, additional);
        Assertions.assertEquals("initial-id,second-id,third-id", configuration.getProperty("databaseIds"));
        CloudflareD1ConnectionSettings.save(configuration, "initial-id", false, additional);
        Assertions.assertFalse(configuration.getProperties().containsKey("databaseIds"));
    }

    @Test
    void validatesScopeWithoutRequiringStrictUuidSyntax() {
        Assertions.assertEquals(CloudflareD1ConnectionSettings.Validation.VALID,
            CloudflareD1ConnectionSettings.validate(" test_database-1 ", true, ""));
        Assertions.assertEquals(CloudflareD1ConnectionSettings.Validation.INITIAL_ID_REQUIRED,
            CloudflareD1ConnectionSettings.validate(" ", false, ""));
        Assertions.assertEquals(CloudflareD1ConnectionSettings.Validation.INVALID_INITIAL_ID,
            CloudflareD1ConnectionSettings.validate("db/path", false, ""));
        Assertions.assertEquals(CloudflareD1ConnectionSettings.Validation.EMPTY_SCOPE_ENTRY,
            CloudflareD1ConnectionSettings.validate("initial-id", true, "second-id,"));
        Assertions.assertEquals(CloudflareD1ConnectionSettings.Validation.EMPTY_SCOPE_ENTRY,
            CloudflareD1ConnectionSettings.validate("initial-id", true, "second-id, ,third-id"));
        Assertions.assertEquals(CloudflareD1ConnectionSettings.Validation.INVALID_SCOPE_ID,
            CloudflareD1ConnectionSettings.validate("initial-id", true, "second id"));
        Assertions.assertEquals(CloudflareD1ConnectionSettings.Validation.VALID,
            CloudflareD1ConnectionSettings.validate("initial-id", false, "ignored invalid input,"));
    }

    @Test
    void withoutRowidTablesDoNotRequestInvalidRowidPseudoColumn() throws Exception {
        var owner = Mockito.mock(GenericStructContainer.class, Mockito.RETURNS_DEEP_STUBS);
        var row = Mockito.mock(JDBCResultSet.class);
        Mockito.when(row.getInt("WITHOUT_ROWID")).thenReturn(1);

        var table = Assertions.assertInstanceOf(CloudflareD1Table.class,
            new CloudflareD1MetaModel().createTableOrViewImpl(owner, "qc", "TABLE", row));

        Assertions.assertNull(table.getPseudoAttributes());
        Assertions.assertEquals(0, table.getAllPseudoAttributes(new VoidProgressMonitor()).length);
        Mockito.verify(row).getInt("WITHOUT_ROWID");
    }

    @Test
    void referencesUseTheOwningSchemaInsteadOfDatasourceTables() throws Exception {
        var monitor = new VoidProgressMonitor();
        var dataSource = Mockito.mock(CloudflareD1DataSource.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(dataSource.getInfo().supportsReferentialIntegrity()).thenReturn(true);
        Mockito.when(dataSource.getTables(monitor)).thenReturn(null);
        var owner = Mockito.mock(GenericStructContainer.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(owner.getDataSource()).thenReturn(dataSource);
        var table = (SQLiteTable) new CloudflareD1MetaModel().createTableOrViewImpl(
            owner, "parent", "TABLE", Mockito.mock(JDBCResultSet.class));
        var child = Mockito.mock(GenericTableBase.class);
        var foreignKey = Mockito.mock(SQLiteTableForeignKey.class);
        var unrelatedKey = Mockito.mock(SQLiteTableForeignKey.class);
        Mockito.when(foreignKey.refersToTable(table)).thenReturn(true);
        Mockito.doReturn(List.of(foreignKey, unrelatedKey)).when(child).getAssociations(monitor);
        Mockito.doReturn(List.of(child)).when(owner).getTables(monitor);

        Assertions.assertEquals(List.of(foreignKey), List.copyOf(table.getReferences(monitor)));
        Mockito.verify(dataSource, Mockito.never()).getTables(monitor);
        Mockito.verify(owner).getTables(monitor);
    }

    @Test
    void metamodelCreatesD1CatalogWithAllDatabaseProperties() throws Exception {
        var source = Mockito.mock(CloudflareD1DataSource.class, Mockito.RETURNS_DEEP_STUBS);
        var database = new CloudflareD1Database("database-id", "database", "2026-09-23T12:00:00Z", "eu", "production",
            4096L, "WEUR", CloudflareD1ConnectionAdapter.ReplicationMode.AUTO);
        var snapshots = CloudflareD1DataSource.class.getDeclaredField("databases");
        snapshots.setAccessible(true);
        snapshots.set(source, Map.of("database", database));
        var catalog = Assertions.assertInstanceOf(
            CloudflareD1Catalog.class, new CloudflareD1MetaModel().createCatalogImpl(source, "database"));
        Assertions.assertEquals("database", catalog.getName());
        Assertions.assertEquals("database-id", catalog.getDatabaseId());
        Assertions.assertEquals("2026-09-23T12:00:00Z", catalog.getCreatedAt());
        Assertions.assertEquals("eu", catalog.getJurisdiction());
        Assertions.assertEquals("production", catalog.getVersion());
        Assertions.assertEquals(4096L, catalog.getFileSize());
        Assertions.assertEquals("WEUR", catalog.getRunningInRegion());
        Assertions.assertEquals("AUTO", catalog.getReadReplicationMode());
    }

    @Test
    void initialDatabaseDeletionIsRejectedBeforeOpeningAnyConnection() {
        var source = Mockito.mock(CloudflareD1DataSource.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(source.getContainer().getConnectionConfiguration().getDatabaseName()).thenReturn("initial-id");
        var catalog = new CloudflareD1Catalog(source, new CloudflareD1Database(
            "initial-id", "initial", null, null, null, null, null, null));
        Assertions.assertTrue(catalog.isInitialDatabase());
        Assertions.assertThrows(DBException.class,
            () -> CloudflareD1ConnectionAdapter.delete(new VoidProgressMonitor(), catalog));
        Mockito.verify(source, Mockito.never()).getDefaultInstance();
    }

    @Test
    void jurisdictionExcludesLocationHint() {
        Assertions.assertNull(CloudflareD1ConnectionAdapter.placement("eu", "weur").primaryLocationHint());
        Assertions.assertEquals("weur", CloudflareD1ConnectionAdapter.placement(null, "weur").primaryLocationHint());
        Assertions.assertEquals("eu", CloudflareD1ConnectionAdapter.placement("eu", "weur").jurisdiction());
    }

    @Test
    void errorMessagesAreActionableWithoutEchoingServerContent() {
        var cases = List.of(
            new String[] {"28000", "HTTP 401", CloudflareD1ModelMessages.error_authentication},
            new String[] {"28000", "HTTP 403", CloudflareD1ModelMessages.error_permission},
            new String[] {"08006", "HTTP 404", CloudflareD1ModelMessages.error_not_found},
            new String[] {"08006", "HTTP 429", CloudflareD1ModelMessages.error_rate_limit},
            new String[] {"HYT00", "", CloudflareD1ModelMessages.error_timeout},
            new String[] {"HY008", "", CloudflareD1ModelMessages.error_cancelled}
        );
        for (String[] sample : cases) {
            String message = CloudflareD1ConnectionAdapter.errorMessage(
                new SQLException("unexpected-private-token " + sample[1], sample[0]));
            Assertions.assertEquals(sample[2], message);
            Assertions.assertFalse(message.contains("unexpected-private-token"));
            Assertions.assertFalse(message.isBlank());
        }
    }

    @Test
    void warnsAboutHttpEndpointsExceptLoopbackTests() {
        Assertions.assertTrue(CloudflareD1ConnectionSettings.isInsecureEndpoint("http://example.com/client/v4"));
        Assertions.assertFalse(CloudflareD1ConnectionSettings.isInsecureEndpoint("https://api.cloudflare.com/client/v4"));
        Assertions.assertFalse(CloudflareD1ConnectionSettings.isInsecureEndpoint("http://localhost:8080/client/v4"));
        Assertions.assertFalse(CloudflareD1ConnectionSettings.isInsecureEndpoint("http://127.0.0.1:8080/client/v4"));
        Assertions.assertFalse(CloudflareD1ConnectionSettings.isInsecureEndpoint("http://[::1]:8080/client/v4"));
    }

    @Test
    void cancellationStopsAdministrationBeforeOpeningConnection() {
        var monitor = Mockito.mock(DBRProgressMonitor.class);
        Mockito.when(monitor.isCanceled()).thenReturn(true);
        var source = Mockito.mock(CloudflareD1DataSource.class);
        var error = Assertions.assertThrows(DBException.class,
            () -> CloudflareD1ConnectionAdapter.list(monitor, source));
        Assertions.assertEquals(CloudflareD1ModelMessages.error_cancelled, error.getMessage());
        Mockito.verifyNoInteractions(source);
    }

    @Test
    void catalogPropertyLabelsLoadThroughOsgiBundleLocalization() {
        var bundle = FrameworkUtil.getBundle(CloudflareD1Catalog.class);
        Assertions.assertNotNull(bundle);
        var resources = RuntimeUtils.getBundleLocalization(bundle, null);
        for (String property : List.of("name", "databaseId", "createdAt", "jurisdiction", "version",
            "fileSize", "runningInRegion", "readReplicationMode")) {
            Assertions.assertFalse(resources.getString("meta." + CloudflareD1Catalog.class.getName() + "." + property + ".name").isBlank());
        }
    }

    @Test
    void databaseChoicesMatchNavigatorCatalogsWithoutOpeningAnotherConnection() throws Exception {
        var source = Mockito.mock(CloudflareD1DataSource.class);
        var products = Mockito.mock(CloudflareD1Catalog.class);
        var customers = Mockito.mock(CloudflareD1Catalog.class);
        var initial = Mockito.mock(CloudflareD1Catalog.class);
        Mockito.when(products.requireDatabaseId()).thenReturn("products-id");
        Mockito.when(products.getName()).thenReturn("products");
        Mockito.when(customers.requireDatabaseId()).thenReturn("customers-id");
        Mockito.when(customers.getName()).thenReturn("customers");
        Mockito.when(initial.requireDatabaseId()).thenReturn("initial-id");
        Mockito.when(initial.getName()).thenReturn("test_db");
        Mockito.when(source.getCatalogs()).thenReturn(List.of(products, customers, initial));

        var monitor = Mockito.mock(DBRProgressMonitor.class);
        var choices = CloudflareD1ConnectionAdapter.availableDatabases(monitor, source);
        Assertions.assertEquals(List.of("products", "customers", "test_db"), choices.stream()
            .map(CloudflareD1ConnectionAdapter.DatabaseChoice::name).toList());
        Assertions.assertEquals(List.of("products-id", "customers-id", "initial-id"), choices.stream()
            .map(CloudflareD1ConnectionAdapter.DatabaseChoice::id).toList());
        Mockito.verify(source).getCatalogs();
        Mockito.verifyNoMoreInteractions(source);
    }

    @Test
    void catalogDiagramUsesTablesFromMainSchema() throws Exception {
        var monitor = Mockito.mock(DBRProgressMonitor.class);
        var catalog = Mockito.mock(CloudflareD1Catalog.class);
        var schema = Mockito.mock(GenericSchema.class);
        var table = Mockito.mock(CloudflareD1Table.class);
        Mockito.when(catalog.getSchema(monitor, "main")).thenReturn(schema);
        Mockito.doReturn(List.of(table)).when(schema).getTables(monitor);
        Mockito.when(catalog.getChildren(monitor)).thenCallRealMethod();
        Mockito.when(catalog.getPrimaryChildType(null)).thenCallRealMethod();
        Assertions.assertTrue(DBSTable.class.isAssignableFrom(catalog.getPrimaryChildType(null)));
        Assertions.assertEquals(List.of(table), List.copyOf(catalog.getChildren(monitor)));
        Mockito.doCallRealMethod().when(catalog).cacheStructure(monitor, 7);
        catalog.cacheStructure(monitor, 7);
        Mockito.verify(schema).cacheStructure(monitor, 7);
    }

    @Test
    void propertiesLoadDetailedDatabaseOnceAndStageReplicationLocally() throws Exception {
        var source = Mockito.mock(CloudflareD1DataSource.class, Mockito.RETURNS_DEEP_STUBS);
        var summary = new CloudflareD1Database("id", "database", null, null, null, null, null, null);
        var details = new CloudflareD1Database("id", "database", "2026-09-23T12:00:00Z", "eu", "production",
            4096L, "WEUR", CloudflareD1ConnectionAdapter.ReplicationMode.AUTO);
        var reads = new AtomicInteger();
        var catalog = new CloudflareD1Catalog(source, summary) {
            @NotNull
            @Override
            protected CloudflareD1Database readDetails(@NotNull DBRProgressMonitor monitor) {
                reads.incrementAndGet();
                return details;
            }
        };
        var monitor = Mockito.mock(DBRProgressMonitor.class);
        Assertions.assertEquals("eu", catalog.getJurisdiction(monitor));
        Assertions.assertEquals("WEUR", catalog.getRunningInRegion(monitor));
        Assertions.assertEquals(4096L, catalog.getFileSize(monitor));
        Assertions.assertEquals(CloudflareD1ConnectionAdapter.ReplicationMode.AUTO, catalog.getReadReplicationMode(monitor));
        Assertions.assertEquals(1, reads.get());
        catalog.setReadReplicationMode(CloudflareD1ConnectionAdapter.ReplicationMode.DISABLED);
        Assertions.assertEquals(CloudflareD1ConnectionAdapter.ReplicationMode.DISABLED, catalog.getReadReplicationMode(monitor));
        Assertions.assertEquals(1, reads.get());
        var property = CloudflareD1Catalog.class.getMethod("getReadReplicationMode",
            DBRProgressMonitor.class).getAnnotation(Property.class);
        Assertions.assertFalse(property.expensive());
        Assertions.assertTrue(property.updatable());
        var descriptors = ObjectAttributeDescriptor.extractAnnotations(
            null, CloudflareD1Catalog.class, new DataSourcePropertyFilter(source), null);
        Assertions.assertEquals(8, descriptors.size());
        for (String id : List.of("jurisdiction", "runningInRegion", "readReplicationMode")) {
            var descriptor = descriptors.stream().filter(item -> id.equals(item.getId())).findFirst().orElseThrow();
            Assertions.assertTrue(descriptor.isLazy());
            if (id.equals("readReplicationMode")) {
                Assertions.assertEquals(CloudflareD1ConnectionAdapter.ReplicationMode.class, descriptor.getGetter().getReturnType());
            }
        }
    }

    @Test
    void missingRegionIsDisplayedAsNoneInProperties() throws Exception {
        var source = Mockito.mock(CloudflareD1DataSource.class, Mockito.RETURNS_DEEP_STUBS);
        var catalog = new CloudflareD1Catalog(source, "database");
        var monitor = Mockito.mock(DBRProgressMonitor.class);
        for (String region : new String[] {null, "", " "}) {
            catalog.setDatabaseDetails(new CloudflareD1Database("id", "database", null, null, null, null, region, null));
            Assertions.assertEquals("none", catalog.getRunningInRegion(monitor));
        }
        catalog.setDatabaseDetails(new CloudflareD1Database("id", "database", null, null, null, null, "WEUR", null));
        Assertions.assertEquals("WEUR", catalog.getRunningInRegion(monitor));
    }

}

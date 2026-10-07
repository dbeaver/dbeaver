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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.edit.PostgrePublicationManager;
import org.jkiss.dbeaver.ext.postgresql.edit.PostgreSubscriptionManager;
import org.jkiss.dbeaver.ext.postgresql.model.impls.PostgreServerPostgreSQL;
import org.jkiss.dbeaver.ext.postgresql.model.impls.redshift.PostgreServerRedshift;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.connection.DBPDriverConfigurationType;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCStatementType;
import org.jkiss.dbeaver.model.exec.DBCTransactionManager;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.auth.AuthModelDatabaseNative;
import org.jkiss.dbeaver.model.impl.edit.DBECommandAbstract;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistAction;
import org.jkiss.dbeaver.model.impl.edit.SQLDatabasePersistActionAtomic;
import org.jkiss.dbeaver.model.impl.edit.TestCommandContext;
import org.jkiss.dbeaver.model.impl.net.SSLConfigurationMethod;
import org.jkiss.dbeaver.model.impl.net.SSLHandlerTrustStoreImpl;
import org.jkiss.dbeaver.model.impl.sql.edit.SQLObjectEditor;
import org.jkiss.dbeaver.model.net.DBWHandlerConfiguration;
import org.jkiss.dbeaver.model.net.DBWHandlerDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.DBSQLException;
import org.jkiss.dbeaver.runtime.properties.ObjectAttributeDescriptor;
import org.jkiss.dbeaver.runtime.properties.ObjectPropertyDescriptor;
import org.jkiss.dbeaver.runtime.properties.PropertySourceEditable;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class PostgreReplicationTest extends DBeaverUnitTest {
    private PostgreDataSource dataSource;
    private PostgreDatabase database;
    private PostgreExecutionContext executionContext;

    @BeforeEach
    public void setUp() throws Exception {
        dataSource = new PostgreDataSource(configureTestContainer("postgresql"), "PG Test", "postgres") {
            @Override
            public boolean isServerVersionAtLeast(int major, int minor) {
                return major <= 14;
            }
        };
        database = new PostgreDatabase(dataSource, "testdb");
        executionContext = new PostgreExecutionContext(database, "Test");
    }

    @Test
    public void publicationSupportsEmptyAndAllTableDefinitions() throws Exception {
        PostgrePublication publication = new PostgrePublication(database, "publication");
        assertFalse(publication.isPersisted());
        assertEquals("CREATE PUBLICATION publication\nWITH (publish = 'insert, update, delete, truncate', " +
            "publish_via_partition_root = false);", publication.getObjectDefinitionText(monitor, Map.of()));
        publication.setAllTables(true);
        publication.setPublishViaPartitionRoot(true);
        assertEquals("CREATE PUBLICATION publication FOR ALL TABLES\nWITH (publish = 'insert, update, delete, truncate', " +
            "publish_via_partition_root = true);", publication.getObjectDefinitionText(monitor, Map.of()));
    }

    @Test
    public void publicationQuotesNamesAndSelectedTables() throws Exception {
        PostgrePublication publication = new PostgrePublication(database, "My\"Publication");
        PostgreSchema schema = new PostgreSchema(database, "My Schema", (PostgreRole) null);
        PostgreTableRegular table = new PostgreTableRegular(schema);
        table.setName("My\"Table");
        publication.setTables(List.of(table));
        publication.setPublishUpdate(false);
        publication.setPublishDelete(false);
        publication.setPublishTruncate(false);
        assertEquals("CREATE PUBLICATION \"My\"\"Publication\" FOR TABLE \"My Schema\".\"My\"\"Table\"\n" +
            "WITH (publish = 'insert', publish_via_partition_root = false);",
            publication.getObjectDefinitionText(monitor, Map.of()));
        publication.setAllTables(true);
        assertThrows(DBException.class, () -> publication.getObjectDefinitionText(monitor, Map.of()));
        publication.setAllTables(false);
        publication.setPersisted(true);
        assertTrue(publication.getObjectDefinitionText(monitor, Map.of()).contains("FOR TABLE \"My Schema\".\"My\"\"Table\""));
    }

    @Test
    public void publicationLoadsMetadataFlags() throws Exception {
        ResultSet result = mock(ResultSet.class);
        when(result.getString("pubname")).thenReturn("publication");
        when(result.getLong("oid")).thenReturn(123L);
        when(result.getBoolean("puballtables")).thenReturn(true);
        when(result.getBoolean("pubinsert")).thenReturn(true);
        when(result.getBoolean("pubviaroot")).thenReturn(true);
        PostgrePublication publication = new PostgrePublication(database, result);
        assertTrue(publication.isPersisted());
        assertEquals(123L, publication.getObjectId());
        assertTrue(publication.isAllTables());
        assertTrue(publication.isPublishViaPartitionRoot());
        assertFalse(publication.isPublishUpdate());
    }

    @Test
    public void publicationDefinitionPreservesExplicitInheritanceMembershipOn14() throws Exception {
        assertPublicationMembershipDefinition(false,
            List.of("ONLY public.parent", "ONLY public.explicit_child"),
            "TABLE ONLY public.parent, ONLY public.explicit_child");
    }

    @Test
    public void publicationDefinitionPreservesExplicitInheritanceMembershipWithFiltersAndSchemas() throws Exception {
        assertPublicationMembershipDefinition(true,
            List.of("TABLE ONLY public.parent (id) WHERE (id > 0)", "TABLES IN SCHEMA published"),
            "TABLE ONLY public.parent (id) WHERE (id > 0), TABLES IN SCHEMA published");
    }

    private void assertPublicationMembershipDefinition(
        boolean supportsFilters, @NotNull List<String> definitions, @NotNull String expectedMembership
    ) throws Exception {
        PostgreDataSource publisher = mock(PostgreDataSource.class);
        when(publisher.getSQLDialect()).thenReturn(dataSource.getSQLDialect());
        when(publisher.isServerVersionAtLeast(15, 0)).thenReturn(supportsFilters);
        PostgreDatabase owner = mock(PostgreDatabase.class);
        when(owner.getDataSource()).thenReturn(publisher);
        when(owner.isInstanceConnected()).thenReturn(true);
        PostgreExecutionContext context = mock(PostgreExecutionContext.class);
        when(owner.getDefaultContext(any(DBRProgressMonitor.class), eq(true))).thenReturn(context);
        JDBCSession session = mock(JDBCSession.class);
        when(context.openSession(eq(monitor), eq(DBCExecutionPurpose.META), anyString())).thenReturn(session);
        JDBCPreparedStatement statement = mock(JDBCPreparedStatement.class);
        when(session.prepareStatement(anyString())).thenReturn(statement);
        JDBCResultSet result = mock(JDBCResultSet.class);
        when(statement.executeQuery()).thenReturn(result);
        when(result.next()).thenReturn(true, true, false);
        when(result.getString(1)).thenReturn(definitions.get(0), definitions.get(1));
        ResultSet metadata = mock(ResultSet.class);
        when(metadata.getString("pubname")).thenReturn("publication");
        when(metadata.getLong("oid")).thenReturn(123L);
        PostgrePublication publication = new PostgrePublication(owner, metadata);

        assertTrue(publication.getObjectDefinitionText(monitor, Map.of())
            .startsWith("CREATE PUBLICATION publication FOR " + expectedMembership + "\n"));
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(session).prepareStatement(sql.capture());
        assertTrue(sql.getValue().startsWith(supportsFilters ? "SELECT 'TABLE ONLY ' || " : "SELECT 'ONLY ' || "));
        assertTrue(sql.getValue().contains("FROM pg_catalog.pg_publication_rel pr"));
        assertFalse(sql.getValue().contains("pg_publication_tables"));
        assertEquals(supportsFilters, sql.getValue().contains("pr.prattrs"));
        assertEquals(supportsFilters, sql.getValue().contains("pr.prqual"));
        assertEquals(supportsFilters, sql.getValue().contains("TABLES IN SCHEMA"));
        verify(statement).setLong(1, 123L);
        if (supportsFilters) {
            verify(statement).setLong(2, 123L);
        } else {
            verify(statement, never()).setLong(eq(2), anyLong());
        }
        verify(result).close();
        verify(statement).close();
        verify(session).close();
    }

    @Test
    public void subscriptionQuotesPublicationNamesAndConnectionString() throws Exception {
        PostgreSubscription subscription = newSubscription();
        subscription.setName("My\"Subscription");
        subscription.setPublications("publication\nMy Publication\ncomma,name\n\n");
        subscription.setConnectionInfo("host=publisher application_name=O'Brien");
        String ddl = subscription.getCreateStatement();
        assertTrue(ddl.startsWith("CREATE SUBSCRIPTION \"My\"\"Subscription\""));
        assertTrue(ddl.contains("CONNECTION 'host=publisher application_name=O''Brien'"));
        assertTrue(ddl.contains("PUBLICATION publication, \"My Publication\", \"comma,name\""));
        assertTrue(ddl.contains("slot_name = 'My\"Subscription'"));
    }

    @Test
    public void subscriptionDefinitionDoesNotExposeCredentialsOrCreateRemoteSlots() throws Exception {
        PostgreSubscription subscription = newSubscription();
        subscription.setConnectionInfo("host=publisher password=do-not-export");
        String ddl = subscription.getObjectDefinitionText(monitor, Map.of());
        assertFalse(ddl.contains("do-not-export"));
        assertTrue(ddl.contains("CONNECTION '<publisher connection string>'"));
        assertTrue(ddl.contains("connect = false, create_slot = false, copy_data = false, enabled = false"));
        subscription.setPersisted(true);
        assertEquals("", subscription.getConnectionInfo());
        assertEquals("subscription", subscription.getSlotName());
    }

    @Test
    public void disconnectedSubscriptionRequiresConsistentOptions() throws Exception {
        PostgreSubscription subscription = newSubscription();
        subscription.setConnect(false);
        assertThrows(DBException.class, subscription::getCreateStatement);
        subscription.setEnabled(false);
        subscription.setCreateSlot(false);
        subscription.setCopyData(false);
        assertFalse(subscription.isEnabled());
        assertFalse(subscription.isCreateSlot());
        assertFalse(subscription.isCopyData());
        assertTrue(subscription.getCreateStatement().contains("connect = false"));
        subscription.setEnabled(true);
        assertThrows(DBException.class, subscription::getCreateStatement);
    }

    @Test
    public void subscriptionRejectsMissingNamesConnectionAndPublications() throws Exception {
        PostgreSubscription subscription = new PostgreSubscription(database, "subscription");
        assertThrows(DBException.class, subscription::getCreateStatement);
        subscription.setConnectionInfo("host=publisher");
        assertThrows(DBException.class, subscription::getCreateStatement);
        subscription.setPublications("publication");
        subscription.setName(" ");
        assertThrows(DBException.class, subscription::getCreateStatement);
    }

    @Test
    public void subscriptionLoadsMetadataWithoutReadingConnectionInformation() throws Exception {
        ResultSet result = mock(ResultSet.class);
        when(result.getString("subname")).thenReturn("subscription");
        when(result.getString("substream")).thenReturn("t");
        when(result.getBoolean("subenabled")).thenReturn(true);
        Array publications = mock(Array.class);
        when(publications.getArray()).thenReturn(new String[]{"publication", "Other Publication"});
        when(result.getArray("subpublications")).thenReturn(publications);
        PostgreSubscription subscription = new PostgreSubscription(database, result);
        assertTrue(subscription.isPersisted());
        assertTrue(subscription.isEnabled());
        assertTrue(subscription.isStreaming());
        assertEquals("publication\nOther Publication", subscription.getPublications());
        assertEquals("", subscription.getConnectionInfo());
        verify(result, never()).getString("subconninfo");
        assertTrue(subscription.getObjectDefinitionText(monitor, Map.of()).contains("slot_name = NONE"));
    }

    @Test
    public void subscriptionCacheFiltersDatabaseAndDoesNotSelectCredentialsOrNewerColumnsOn14() throws Exception {
        JDBCSession session = mock(JDBCSession.class);
        JDBCPreparedStatement statement = mock(JDBCPreparedStatement.class);
        when(session.prepareStatement(anyString())).thenReturn(statement);
        PostgreDatabase owner = mock(PostgreDatabase.class);
        when(owner.getDataSource()).thenReturn(dataSource);
        when(owner.getObjectId()).thenReturn(42L);
        new PostgreDatabase.SubscriptionCache().prepareLookupStatement(session, owner, null, "subscription");
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(session).prepareStatement(sql.capture());
        assertFalse(sql.getValue().contains("subconninfo"));
        assertFalse(sql.getValue().contains("s.*"));
        assertFalse(sql.getValue().contains("s.subtwophasestate"));
        assertTrue(sql.getValue().contains("WHERE s.subdbid=? AND s.subname=?"));
        verify(statement).setLong(1, 42L);
        verify(statement).setString(2, "subscription");
    }

    @Test
    public void logicalReplicationIsVersionAndServerSpecific() {
        assertTrue(new PostgreServerPostgreSQL(dataSource).supportsLogicalReplication());
        PostgreDataSource older = mock(PostgreDataSource.class);
        assertFalse(new PostgreServerPostgreSQL(older).supportsLogicalReplication());
        assertFalse(new PostgreServerRedshift(dataSource).supportsLogicalReplication());
    }

    @Test
    public void subscriptionCreateAndDropActionsRequireAutocommit() throws Exception {
        PostgreSubscriptionManager manager = new PostgreSubscriptionManager();
        TestCommandContext context = new TestCommandContext(executionContext, false);
        Map<String, Object> options = new HashMap<>();
        options.put(SQLObjectEditor.OPTION_SKIP_CONFIGURATION, true);
        PostgreSubscription subscription = manager.createNewObject(monitor, context, database, null, options);
        assertNotNull(subscription);
        subscription.setConnectionInfo("host=publisher");
        subscription.setPublications("publication");
        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(monitor, context, executionContext, Map.of(), null);
        assertEquals(1, actions.size());
        assertInstanceOf(SQLDatabasePersistActionAtomic.class, actions.getFirst());
        assertTrue(actions.getFirst().getScript().startsWith("CREATE SUBSCRIPTION"));

        subscription.setPersisted(true);
        TestCommandContext deleteContext = new TestCommandContext(executionContext, false);
        manager.deleteObject(deleteContext, subscription, Map.of());
        actions = DBExecUtils.getActionsListFromCommandContext(monitor, deleteContext, executionContext, Map.of(), null);
        assertEquals(1, actions.size());
        assertInstanceOf(SQLDatabasePersistActionAtomic.class, actions.getFirst());
        assertEquals("DROP SUBSCRIPTION new_subscription", actions.getFirst().getScript());
    }

    @Test
    public void subscriptionDropIsolatesTransactionSwitchingAndCommit() throws Exception {
        assertSubscriptionDropIsolation(false);
    }

    @Test
    public void failedSubscriptionDropDoesNotRollbackOrCommitUserWork() throws Exception {
        assertSubscriptionDropIsolation(true);
    }

    private void assertSubscriptionDropIsolation(boolean fail) throws Exception {
        PostgreDataSource subscriber = mock(PostgreDataSource.class);
        when(subscriber.getSQLDialect()).thenReturn(dataSource.getSQLDialect());
        DBPDataSourceInfo info = mock(DBPDataSourceInfo.class);
        when(info.supportsTransactionsForDDL()).thenReturn(true);
        when(subscriber.getInfo()).thenReturn(info);
        PostgreDatabase owner = mock(PostgreDatabase.class);
        when(owner.getDataSource()).thenReturn(subscriber);
        when(owner.getSubscriptionCache()).thenReturn(database.getSubscriptionCache());
        PostgreExecutionContext shared = mock(PostgreExecutionContext.class);
        when(shared.isConnected()).thenReturn(true);
        when(shared.getOwnerInstance()).thenReturn(owner);
        when(shared.getAdapter(DBCTransactionManager.class)).thenReturn(shared);
        // Model a shared connection with pending work in manual-commit mode.
        when(shared.isAutoCommit()).thenReturn(false);
        PostgreExecutionContext isolated = mock(PostgreExecutionContext.class);
        when(owner.openIsolatedContext(eq(monitor), anyString(), eq(shared))).thenReturn(isolated);
        when(isolated.isConnected()).thenReturn(true);
        when(isolated.getDataSource()).thenReturn(subscriber);
        when(isolated.getAdapter(DBCTransactionManager.class)).thenReturn(isolated);
        when(isolated.isSupportsTransactions()).thenReturn(true);
        AtomicBoolean autoCommit = new AtomicBoolean(true);
        when(isolated.isAutoCommit()).thenAnswer(invocation -> autoCommit.get());
        doAnswer(invocation -> {
            autoCommit.set(invocation.getArgument(1));
            return null;
        }).when(isolated).setAutoCommit(eq(monitor), anyBoolean());
        JDBCSession session = mock(JDBCSession.class);
        when(isolated.openSession(eq(monitor), any(), anyString())).thenReturn(session);
        when(session.getExecutionContext()).thenReturn(isolated);
        when(session.getDataSource()).thenReturn(subscriber);
        when(session.getProgressMonitor()).thenReturn(monitor);
        JDBCStatement statement = mock(JDBCStatement.class);
        when(session.prepareStatement(eq(DBCStatementType.SCRIPT), anyString(), eq(false), eq(false), eq(false)))
            .thenReturn(statement);
        DBCException failure = new DBCException("Cannot drop subscription", null, isolated);
        if (fail) {
            when(statement.executeStatement()).thenThrow(failure);
        }
        PostgreSubscription subscription = new PostgreSubscription(owner, "subscription");
        subscription.setPersisted(true);
        TestCommandContext context = new TestCommandContext(shared, true);
        new PostgreSubscriptionManager().deleteObject(context, subscription, Map.of());
        Map<String, Object> validation = new HashMap<>();
        context.getFinalCommands().iterator().next().validateCommand(monitor, validation);
        assertEquals(true, validation.get(DBECommandContext.OPTION_ISOLATED_EXECUTION));
        if (fail) {
            assertSame(failure, assertThrows(DBException.class, () -> context.saveChanges(monitor, Map.of())));
        } else {
            context.saveChanges(monitor, Map.of());
        }
        verify(session).prepareStatement(DBCStatementType.SCRIPT, "DROP SUBSCRIPTION subscription", false, false, false);
        verify(statement).executeStatement();
        verify(isolated, atLeastOnce()).setAutoCommit(monitor, true);
        assertTrue(autoCommit.get());
        verify(isolated).close();
        verify(session, atLeastOnce()).close();
        verify(shared, never()).getAdapter(DBCTransactionManager.class);
        verify(shared, never()).isAutoCommit();
        verify(shared, never()).setAutoCommit(any(), anyBoolean());
        verify(shared, never()).commit(any());
        verify(shared, never()).rollback(any(), any());
        verify(shared, never()).openSession(any(), any(), anyString());
    }

    @Test
    public void subscriptionCreateFailuresDoNotExposePasswordTokensOrSql() throws Exception {
        PostgreSubscriptionManager manager = new PostgreSubscriptionManager();
        var command = manager.makeCreateCommand(newSubscription(), Map.of());
        DBEPersistAction action = command.getPersistActions(monitor, executionContext, Map.of())[0];
        assertCredentialSafePersistFailure(manager, command, action);
    }

    @Test
    @SuppressWarnings("unchecked")
    public void subscriptionConnectionReplacementFailuresDoNotExposePasswordTokensOrSql() throws Exception {
        PostgreSubscription subscription = newSubscription();
        subscription.setPersisted(true);
        TestCommandContext context = new TestCommandContext(executionContext, false);
        PropertySourceEditable source = new PropertySourceEditable(context, subscription, subscription);
        source.collectProperties();
        source.setPropertyValue(monitor, "connectionInfo", "postgresql://user:review%ZZsecret@publisher/database");
        DBECommand<PostgreSubscription> command = (DBECommand<PostgreSubscription>) context.getFinalCommands().iterator().next();
        DBEPersistAction action = command.getPersistActions(monitor, executionContext, Map.of())[0];
        assertCredentialSafePersistFailure(new PostgreSubscriptionManager(), command, action);
    }

    private void assertCredentialSafePersistFailure(
        @NotNull PostgreSubscriptionManager manager, @NotNull DBECommand<PostgreSubscription> command, @NotNull DBEPersistAction action
    ) throws Exception {
        assertTrue(command.isDisableSessionLogging());
        JDBCSession session = mock(JDBCSession.class);
        when(session.getDataSource()).thenReturn(dataSource);
        JDBCStatement statement = mock(JDBCStatement.class);
        when(session.prepareStatement(any(), anyString(), anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(statement);
        when(statement.executeStatement()).thenThrow(new DBSQLException(action.getScript(),
            new SQLException("invalid percent-encoded token: review%ZZsecret", "42601"), executionContext));
        DBException error = assertThrows(DBException.class, () -> manager.executePersistAction(session, command, action));
        assertFalse(error.getMessage().contains("review%ZZsecret"));
        assertFalse(error.getMessage().contains(action.getScript()));
        assertTrue(error.getMessage().contains("42601"));
        assertNull(error.getCause());
        assertEquals(0, error.getSuppressed().length);
        verify(statement).close();
    }

    @Test
    public void subscriptionNonCredentialErrorsKeepTheirDiagnostics() throws Exception {
        JDBCSession session = mock(JDBCSession.class);
        when(session.getDataSource()).thenReturn(dataSource);
        JDBCStatement statement = mock(JDBCStatement.class);
        when(session.prepareStatement(any(), anyString(), anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(statement);
        DBCException failure = new DBCException("Permission denied", null, executionContext);
        when(statement.executeStatement()).thenThrow(failure);
        assertSame(failure, assertThrows(DBException.class, () -> new PostgreSubscriptionManager().executePersistAction(
            session, new DBECommandAbstract<>(newSubscription(), "Alter subscription"),
            new SQLDatabasePersistAction("ALTER SUBSCRIPTION subscription DISABLE"))));
    }

    @Test
    public void subscriptionCreationDisablesQueryLoggingForPublisherCredentials() throws Exception {
        PostgreSubscription configured = newSubscription();
        configured.setConnectionInfo("host=publisher password=do-not-log");
        TestCommandContext context = new TestCommandContext(executionContext, false);
        new PostgreSubscriptionManager().createNewObject(monitor, context, database, configured,
            new HashMap<>(Map.of(SQLObjectEditor.OPTION_SKIP_CONFIGURATION, true)));
        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(monitor, context, executionContext, Map.of(), null);
        assertTrue(actions.getFirst().getScript().contains("password=do-not-log"));
        assertFalse(context.getFinalCommands().isEmpty());
        for (var command : context.getFinalCommands()) {
            assertTrue(command.isDisableSessionLogging());
        }
    }

    @Test
    public void modelessSubscriptionConfigurationUsesNormalCreateCommandsWithoutChangingTheFormObject() throws Exception {
        PostgreSubscription configured = newSubscription();
        configured.setName("configured_subscription");
        configured.setSlotName("existing_slot");
        configured.setSynchronousCommit("local");
        configured.setConnect(false);
        configured.setEnabled(false);
        configured.setCreateSlot(false);
        configured.setCopyData(false);
        configured.setBinary(true);
        configured.setStreaming(true);

        TestCommandContext context = new TestCommandContext(executionContext, true);
        Map<String, Object> options = new HashMap<>();
        options.put(SQLObjectEditor.OPTION_SKIP_CONFIGURATION, true);
        PostgreSubscription created = new PostgreSubscriptionManager().createNewObject(monitor, context, database, configured, options);
        assertNotNull(created);
        assertNotSame(configured, created);
        assertSame(database, created.getDatabase());
        assertEquals(configured.getCreateStatement(), created.getCreateStatement());
        assertFalse(created.isPersisted());
        assertFalse(configured.isPersisted());

        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(monitor, context, executionContext, Map.of(), null);
        assertEquals(1, actions.size());
        assertInstanceOf(SQLDatabasePersistActionAtomic.class, actions.getFirst());
        assertEquals(created.getCreateStatement(), actions.getFirst().getScript());
        created.setConnectionInfo("host=different_publisher");
        assertEquals("host=publisher", configured.getConnectionInfo());
    }

    @Test
    public void configuredSubscriptionIsCreatedInTheRequestedDatabase() throws Exception {
        PostgreSubscription configured = newSubscription();
        PostgreDatabase target = new PostgreDatabase(dataSource, "targetdb");
        TestCommandContext context = new TestCommandContext(executionContext, true);
        Map<String, Object> options = new HashMap<>();
        options.put(SQLObjectEditor.OPTION_SKIP_CONFIGURATION, true);
        PostgreSubscription created = new PostgreSubscriptionManager().createNewObject(monitor, context, target, configured, options);
        assertNotNull(created);
        assertSame(target, created.getDatabase());
        assertSame(database, configured.getDatabase());
    }

    @Test
    public void persistedSubscriptionCanBeDisabledAndEnabledWithoutPublisherCredentials() throws Exception {
        PostgreSubscription subscription = newSubscription();
        subscription.setPersisted(true);
        assertTrue(new PostgreSubscriptionManager().canEditObject(subscription));
        assertEquals("", subscription.getConnectionInfo());
        assertEquals(List.of("ALTER SUBSCRIPTION subscription DISABLE"),
            alterSubscription(subscription, Map.of("enabled", false)).stream().map(DBEPersistAction::getScript).toList());
        assertEquals(List.of("ALTER SUBSCRIPTION subscription ENABLE"),
            alterSubscription(subscription, Map.of("enabled", true)).stream().map(DBEPersistAction::getScript).toList());
    }

    @Test
    public void persistedSubscriptionEditsOnlyChangedOptionsAndDisablesBeforeRemovingTheSlot() throws Exception {
        PostgreSubscription subscription = newSubscription();
        subscription.setPersisted(true);
        Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("binary", true);
        changes.put("streaming", true);
        changes.put("synchronousCommit", "local");
        changes.put("slotName", null);
        changes.put("enabled", false);
        List<DBEPersistAction> actions = alterSubscription(subscription, changes);
        assertEquals(List.of(
            "ALTER SUBSCRIPTION subscription DISABLE",
            "ALTER SUBSCRIPTION subscription SET (slot_name = NONE, binary = true, streaming = true, synchronous_commit = 'local')"
        ), actions.stream().map(DBEPersistAction::getScript).toList());
        assertTrue(actions.stream().noneMatch(SQLDatabasePersistActionAtomic.class::isInstance));
        assertTrue(actions.stream().noneMatch(action -> action.getScript().contains("CONNECTION")));
    }

    @Test
    public void savingSubscriptionConnectionReplacementExecutesAlterConnection() throws Exception {
        PostgreSubscription subscription = new PostgreSubscription(database, "subscription");
        subscription.setPersisted(true);
        assertEquals("", subscription.getConnectionInfo());
        PostgreExecutionContext context = mock(PostgreExecutionContext.class);
        when(context.isConnected()).thenReturn(true);
        PostgreDataSource subscriber = mock(PostgreDataSource.class);
        DBPDataSourceInfo info = mock(DBPDataSourceInfo.class);
        when(info.supportsTransactionsForDDL()).thenReturn(true);
        when(subscriber.getInfo()).thenReturn(info);
        when(context.getDataSource()).thenReturn(subscriber);
        when(context.getAdapter(DBCTransactionManager.class)).thenReturn(context);
        when(context.isSupportsTransactions()).thenReturn(true);
        when(context.isAutoCommit()).thenReturn(false);
        JDBCSession session = mock(JDBCSession.class);
        when(context.openSession(eq(monitor), eq(DBCExecutionPurpose.META_DDL), anyString())).thenReturn(session);
        when(session.getDataSource()).thenReturn(dataSource);
        when(session.getExecutionContext()).thenReturn(context);
        when(session.isLoggingEnabled()).thenReturn(true);
        JDBCStatement statement = mock(JDBCStatement.class);
        when(session.prepareStatement(any(), anyString(), anyBoolean(), anyBoolean(), anyBoolean())).thenReturn(statement);
        TestCommandContext commands = new TestCommandContext(context, false);
        PropertySourceEditable source = new PropertySourceEditable(commands, subscription, subscription);
        source.collectProperties();
        assertFalse(commands.isDirty());
        source.setPropertyValue(monitor, "connectionInfo", "host=publisher dbname=replication user=replicator password=O'Brien");
        assertTrue(commands.isDirty());
        commands.saveChanges(monitor, Map.of());
        verify(session).prepareStatement(DBCStatementType.SCRIPT,
            "ALTER SUBSCRIPTION subscription CONNECTION 'host=publisher dbname=replication user=replicator password=O''Brien'",
            false, false, false);
        InOrder execution = inOrder(session, statement, context);
        execution.verify(session).enableLogging(false);
        execution.verify(statement).executeStatement();
        execution.verify(session).enableLogging(true);
        execution.verify(context).commit(session);
        assertFalse(commands.isDirty());
    }

    @Test
    public void persistedSubscriptionAppliesNewSettingsBeforeEnablingReplication() throws Exception {
        PostgreSubscription subscription = newSubscription();
        subscription.setPersisted(true);
        subscription.setEnabled(false);
        Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("enabled", true);
        changes.put("slotName", "existing_slot");
        changes.put("publications", "New Publication\nOther\"Publication");
        changes.put("connectionInfo", "host=publisher password=O'Brien");
        assertEquals(List.of(
            "ALTER SUBSCRIPTION subscription CONNECTION 'host=publisher password=O''Brien'",
            "ALTER SUBSCRIPTION subscription SET (slot_name = 'existing_slot')",
            "ALTER SUBSCRIPTION subscription SET PUBLICATION \"New Publication\", \"Other\"\"Publication\" WITH (refresh = false)",
            "ALTER SUBSCRIPTION subscription ENABLE"
        ), alterSubscription(subscription, changes).stream().map(DBEPersistAction::getScript).toList());
        assertFalse(subscription.getObjectDefinitionText(monitor, Map.of()).contains("O'Brien"));
    }

    @Test
    public void persistedSubscriptionRejectsUnsafeOrEmptyChanges() {
        PostgreSubscription subscription = newSubscription();
        subscription.setPersisted(true);
        Map<String, Object> slotChange = new HashMap<>();
        slotChange.put("slotName", null);
        assertThrows(DBException.class, () -> alterSubscription(subscription, slotChange));
        assertThrows(DBException.class, () -> alterSubscription(subscription, Map.of("publications", " ")));
        assertThrows(DBException.class, () -> alterSubscription(subscription, Map.of("connectionInfo", " ")));
        assertThrows(DBException.class, () -> alterSubscription(subscription, Map.of("synchronousCommit", "invalid")));
    }

    @Test
    public void subscriptionMetadataExposesAlterableOptionsButHidesCreationOnlyOptions() {
        PostgreSubscription subscription = newSubscription();
        subscription.setPersisted(true);
        PropertySourceEditable source = new PropertySourceEditable(subscription, subscription);
        source.collectProperties();
        for (String property : List.of("enabled", "connectionInfo", "publications", "slotName", "binary", "streaming", "synchronousCommit")) {
            assertTrue(Arrays.stream(source.getProperties()).anyMatch(descriptor -> property.equals(descriptor.getId()) && descriptor.isEditable(subscription)), property);
        }
        for (String property : List.of("connect", "createSlot", "copyData")) {
            assertTrue(Arrays.stream(source.getProperties()).noneMatch(descriptor -> property.equals(descriptor.getId())), property);
        }
        PostgreSubscription.CreationPropertyValidator visibility = new PostgreSubscription.CreationPropertyValidator();
        assertFalse(visibility.isValidValue(subscription, null));
        assertTrue(visibility.isValidValue(newSubscription(), null));
        PostgreSubscription.SynchronousCommitListProvider modes = new PostgreSubscription.SynchronousCommitListProvider();
        assertFalse(modes.allowCustomValue());
        assertArrayEquals(new String[]{"off", "local", "remote_write", "on", "remote_apply"}, modes.getPossibleValues(subscription));
    }

    @NotNull
    private List<DBEPersistAction> alterSubscription(@NotNull PostgreSubscription subscription, @NotNull Map<String, Object> changes)
        throws DBException {
        TestCommandContext context = new TestCommandContext(executionContext, false);
        PropertySourceEditable source = new PropertySourceEditable(context, subscription, subscription);
        source.collectProperties();
        for (Map.Entry<String, Object> change : changes.entrySet()) {
            source.setPropertyValue(monitor, change.getKey(), change.getValue());
        }
        for (var command : context.getFinalCommands()) {
            command.validateCommand(monitor, Map.of());
        }
        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(monitor, context, executionContext, Map.of(), null);
        for (var command : context.getFinalCommands()) {
            assertEquals(changes.containsKey("connectionInfo"), command.isDisableSessionLogging());
        }
        return actions;
    }

    @Test
    public void publicationManagerGeneratesCreateAndDropActions() throws Exception {
        PostgrePublicationManager manager = new PostgrePublicationManager();
        TestCommandContext context = new TestCommandContext(executionContext, false);
        Map<String, Object> options = new HashMap<>();
        options.put(SQLObjectEditor.OPTION_SKIP_CONFIGURATION, true);
        PostgrePublication publication = manager.createNewObject(monitor, context, database, null, options);
        assertNotNull(publication);
        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(monitor, context, executionContext, Map.of(), null);
        assertTrue(actions.getFirst().getScript().startsWith("CREATE PUBLICATION"));
        assertFalse(actions.getFirst() instanceof SQLDatabasePersistActionAtomic);
        publication.setPersisted(true);
        TestCommandContext deleteContext = new TestCommandContext(executionContext, false);
        manager.deleteObject(deleteContext, publication, Map.of());
        actions = DBExecUtils.getActionsListFromCommandContext(monitor, deleteContext, executionContext, Map.of(), null);
        assertEquals("DROP PUBLICATION new_publication", actions.getFirst().getScript());
    }

    @Test
    public void publicationCreationOpensMetadataEditorAndDoesNotSaveImmediately() {
        long options = new PostgrePublicationManager().getMakerOptions(dataSource);
        assertEquals(DBEObjectMaker.FEATURE_EDITOR_ON_CREATE, options);
        assertEquals(0, options & DBEObjectMaker.FEATURE_SAVE_IMMEDIATELY);
    }

    @Test
    public void filteredPublicationTableSelectionPreservesHiddenChecks() {
        PostgreSchema schema = new PostgreSchema(database, "public", (PostgreRole) null);
        PostgreTableRegular hidden = new PostgreTableRegular(schema);
        hidden.setName("customers");
        PostgreTableRegular visible = new PostgreTableRegular(schema);
        visible.setName("orders");
        PostgreTableRegular added = new PostgreTableRegular(schema);
        added.setName("order_items");
        List<PostgreTable> original = List.of(hidden, visible);
        List<PostgreTable> selected = PostgrePublication.mergeTableSelection(original, added, true);
        assertEquals(List.of(hidden, visible, added), selected);
        assertEquals(List.of(hidden, added), PostgrePublication.mergeTableSelection(selected, visible, false));
        assertEquals(List.of(hidden, visible), original);
        assertEquals(selected, PostgrePublication.mergeTableSelection(selected, added, true));
        assertEquals(original, PostgrePublication.mergeTableSelection(original, added, false));
    }

    @Test
    public void publicationTableSelectorParticipatesInMetadataEditorUndoAndCreateScript() throws Exception {
        TestCommandContext context = new TestCommandContext(executionContext, false);
        PostgrePublication publication = new PostgrePublicationManager().createNewObject(
            monitor, context, database, null, new HashMap<>(Map.of(SQLObjectEditor.OPTION_SKIP_CONFIGURATION, true)));
        assertNotNull(publication);
        PostgreSchema schema = new PostgreSchema(database, "public", (PostgreRole) null);
        PostgreTableRegular table = new PostgreTableRegular(schema);
        table.setName("authors");
        PropertySourceEditable source = new PropertySourceEditable(context, publication, publication);
        source.collectProperties();
        assertTrue(Arrays.stream(source.getProperties()).noneMatch(property -> PostgrePublication.PROP_ID_CREATION_TABLES.equals(property.getId())));
        ObjectPropertyDescriptor descriptor = ObjectAttributeDescriptor.extractAnnotations(source, PostgrePublication.class, null, null)
            .stream().filter(property -> PostgrePublication.PROP_ID_CREATION_TABLES.equals(property.getId())).findFirst().orElseThrow();
        source.setPropertyValue(monitor, publication, descriptor, List.of(table));
        assertEquals(List.of(table), publication.getCreationTables());
        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(monitor, context, executionContext, Map.of(), null);
        assertEquals(1, actions.size());
        assertTrue(actions.getFirst().getScript().contains("FOR TABLE public.authors"));
        assertFalse(publication.isPersisted());
        context.undoCommand();
        assertTrue(publication.getCreationTables().isEmpty());
        context.redoCommand();
        assertEquals(List.of(table), publication.getCreationTables());
        PostgreTableRegular anotherTable = new PostgreTableRegular(schema);
        anotherTable.setName("orders");
        source.setPropertyValue(monitor, publication, descriptor,
            PostgrePublication.mergeTableSelection(publication.getCreationTables(), anotherTable, true));
        assertEquals(List.of(table, anotherTable), publication.getCreationTables());
        // The property editor coalesces consecutive changes to the same property into one undo step.
        context.undoCommand();
        assertTrue(publication.getCreationTables().isEmpty());
        context.redoCommand();
        assertEquals(List.of(table, anotherTable), publication.getCreationTables());
        actions = DBExecUtils.getActionsListFromCommandContext(monitor, context, executionContext, Map.of(), null);
        assertEquals(1, actions.size());
        assertTrue(actions.getFirst().getScript().contains("FOR TABLE public.authors, public.orders"));
    }

    private PostgreSubscription newSubscription() {
        PostgreSubscription subscription = new PostgreSubscription(database, "subscription");
        subscription.setConnectionInfo("host=publisher");
        subscription.setPublications("publication");
        return subscription;
    }

    @Test
    public void publisherConnectionParametersUseLibpqEscaping() {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("host", "publisher");
        parameters.put("dbname", "database with spaces");
        parameters.put("password", "O'Brien\\secret");
        assertEquals("host='publisher' dbname='database with spaces' password='O\\'Brien\\\\secret'",
            PostgreSubscription.buildConnectionInfo(parameters));
    }

    @Test
    public void publisherConnectionOmitsEmptyOptionalParametersAndQuotesSqlSeparately() throws Exception {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("host", "publisher");
        parameters.put("password", "");
        assertEquals("host='publisher'", PostgreSubscription.buildConnectionInfo(parameters));
        PostgreSubscription subscription = newSubscription();
        subscription.setConnectionInfo(PostgreSubscription.buildConnectionInfo(parameters));
        assertTrue(subscription.getCreateStatement().contains("CONNECTION 'host=''publisher'''"));
    }

    @Test
    public void publisherConfigurationUsesNativeCredentialsAndDefaultPort() throws Exception {
        DBPConnectionConfiguration configuration = publisherConfiguration();
        configuration.setUserName("replicator");
        configuration.setUserPassword("O'Brien\\secret");
        String connection = PostgreSubscription.buildConnectionInfo(configuration);
        assertTrue(connection.startsWith("host='publisher' port='5432' dbname='published database' user='replicator'"));
        assertTrue(connection.contains("password='O\\'Brien\\\\secret'"));
        assertFalse(connection.contains("sslmode="));
        assertEquals("O'Brien\\secret", configuration.getUserPassword());
    }

    @Test
    public void publisherConfigurationRejectsInvalidEndpointAndDesktopAuthentication() {
        DBPConnectionConfiguration configuration = publisherConfiguration();
        configuration.setHostPort("70000");
        assertThrows(DBException.class, () -> PostgreSubscription.buildConnectionInfo(configuration));
        configuration.setHostPort("not a port");
        assertThrows(DBException.class, () -> PostgreSubscription.buildConnectionInfo(configuration));
        configuration.setHostPort("5432");
        configuration.setAuthModelId("desktop-authentication");
        assertThrows(DBException.class, () -> PostgreSubscription.buildConnectionInfo(configuration));
        configuration.setAuthModelId(AuthModelDatabaseNative.ID);
        configuration.setHostName(" ");
        assertThrows(DBException.class, () -> PostgreSubscription.buildConnectionInfo(configuration));
    }

    @Test
    public void publisherConfigurationDoesNotSilentlyIgnoreJdbcUrlsOrTunnels() {
        DBPConnectionConfiguration configuration = publisherConfiguration();
        configuration.setConfigurationType(DBPDriverConfigurationType.URL);
        configuration.setUrl("jdbc:postgresql://different-publisher/database");
        assertThrows(DBException.class, () -> PostgreSubscription.buildConnectionInfo(configuration));
        configuration.setConfigurationType(DBPDriverConfigurationType.MANUAL);
        DBWHandlerConfiguration tunnel = publisherHandler("desktop-tunnel");
        configuration.updateHandler(tunnel);
        assertThrows(DBException.class, () -> PostgreSubscription.buildConnectionInfo(configuration));
        tunnel.setEnabled(false);
        assertDoesNotThrow(() -> PostgreSubscription.buildConnectionInfo(configuration));
    }

    @Test
    public void publisherConfigurationMapsSslModeAndServerCertificatePaths() throws Exception {
        DBPConnectionConfiguration configuration = publisherConfiguration();
        DBWHandlerConfiguration ssl = publisherHandler(PostgreConstants.HANDLER_SSL);
        ssl.setProperty(PostgreConstants.PROP_SSL_MODE, "verify-full");
        ssl.setProperty(SSLHandlerTrustStoreImpl.PROP_SSL_CA_CERT, "/server/ca.pem");
        ssl.setProperty(SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_CERT, "/server/client.pem");
        ssl.setProperty(SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_KEY, "/server/client.key");
        configuration.updateHandler(ssl);
        String connection = PostgreSubscription.buildConnectionInfo(configuration);
        assertTrue(connection.contains("sslmode='verify-full'"));
        assertTrue(connection.contains("sslrootcert='/server/ca.pem'"));
        assertTrue(connection.contains("sslcert='/server/client.pem'"));
        assertTrue(connection.contains("sslkey='/server/client.key'"));
    }

    @Test
    public void publisherConfigurationPreservesJdbcSslVerificationDefaults() throws Exception {
        DBPConnectionConfiguration configuration = publisherConfiguration();
        configuration.setProperty(PostgreConstants.PROP_SSL, "true");
        assertTrue(PostgreSubscription.buildConnectionInfo(configuration).contains("sslmode='verify-full'"));
        configuration.setProperty("sslmode", "require");
        assertTrue(PostgreSubscription.buildConnectionInfo(configuration).contains("sslmode='require'"));
        configuration.setProperty("sslfactory", "desktop.SSLFactory");
        assertThrows(DBException.class, () -> PostgreSubscription.buildConnectionInfo(configuration));
    }

    @Test
    public void publisherConfigurationRejectsJavaKeystoresAndEmbeddedCertificates() {
        DBPConnectionConfiguration configuration = publisherConfiguration();
        DBWHandlerConfiguration ssl = publisherHandler(PostgreConstants.HANDLER_SSL);
        configuration.updateHandler(ssl);
        ssl.setProperty(SSLHandlerTrustStoreImpl.PROP_SSL_METHOD, SSLConfigurationMethod.KEYSTORE.name());
        assertThrows(DBException.class, () -> PostgreSubscription.buildConnectionInfo(configuration));
        ssl.setProperty(SSLHandlerTrustStoreImpl.PROP_SSL_METHOD, SSLConfigurationMethod.CERTIFICATES.name());
        ssl.setSecureProperty(SSLHandlerTrustStoreImpl.PROP_SSL_CA_CERT_VALUE, "embedded certificate");
        assertThrows(DBException.class, () -> PostgreSubscription.buildConnectionInfo(configuration));
    }

    @Test
    public void existingPublisherConnectionsRequirePostgresqlAndNativeAuthentication() throws Exception {
        DBPConnectionConfiguration configuration = publisherConfiguration();
        DBPDataSourceContainer connection = publisherConnection(configuration);
        assertTrue(PostgreSubscription.supportsPublisherConnection(connection));
        configuration.setAuthModelId(AuthModelDatabaseNative.ID);
        assertTrue(PostgreSubscription.supportsPublisherConnection(connection));
        configuration.setAuthModelId("desktop-authentication");
        assertFalse(PostgreSubscription.supportsPublisherConnection(connection));
        assertThrows(DBException.class, () -> PostgreSubscription.copyPublisherConnectionConfiguration(monitor, connection));
        configuration.setAuthModelId(AuthModelDatabaseNative.ID);
        DBPConnectionConfiguration actual = new DBPConnectionConfiguration(configuration);
        actual.setAuthModelId("desktop-authentication");
        when(connection.getActualConnectionConfiguration()).thenReturn(actual);
        assertFalse(PostgreSubscription.supportsPublisherConnection(connection));
        when(connection.getActualConnectionConfiguration()).thenReturn(configuration);
        when(connection.getDriver()).thenReturn(mock(DBPDriver.class));
        assertFalse(PostgreSubscription.supportsPublisherConnection(connection));
    }

    @Test
    public void existingPublisherConnectionCopiesSettingsWithoutChangingSource() throws Exception {
        DBPConnectionConfiguration original = publisherConfiguration();
        original.setUserName("replicator");
        original.setUserPassword("  O'Brien\\secret  ");
        DBWHandlerConfiguration ssl = publisherHandler(PostgreConstants.HANDLER_SSL);
        ssl.setProperty(PostgreConstants.PROP_SSL_MODE, "require");
        original.updateHandler(ssl);
        DBPDataSourceContainer source = publisherConnection(original);
        DBPConnectionConfiguration copy = PostgreSubscription.copyPublisherConnectionConfiguration(monitor, source);
        verify(source).isCredentialsSaved();
        assertNotSame(original, copy);
        assertEquals("  O'Brien\\secret  ", copy.getUserPassword());
        copy.setHostName("server-side-publisher");
        copy.setUserPassword("different password");
        copy.getHandler(PostgreConstants.HANDLER_SSL).setProperty(PostgreConstants.PROP_SSL_MODE, "disable");
        assertEquals("publisher", original.getHostName());
        assertEquals("  O'Brien\\secret  ", original.getUserPassword());
        assertEquals("require", ssl.getStringProperty(PostgreConstants.PROP_SSL_MODE));
        verify(source, never()).persistConfiguration();
    }

    @Test
    public void existingPublisherConnectionUsesRemoteEndpointNotResolvedTunnelAddress() throws Exception {
        DBPConnectionConfiguration original = publisherConfiguration();
        original.setHostPort("5432");
        original.updateHandler(publisherHandler("desktop-tunnel"));
        DBPConnectionConfiguration actual = new DBPConnectionConfiguration(original);
        actual.setHostName("localhost");
        actual.setHostPort("16432");
        actual.setUserName("runtime-user");
        actual.setUserPassword("runtime-password");
        DBPDataSourceContainer source = publisherConnection(original);
        when(source.getActualConnectionConfiguration()).thenReturn(actual);
        DBPConnectionConfiguration copy = PostgreSubscription.copyPublisherConnectionConfiguration(monitor, source);
        assertEquals("publisher", copy.getHostName());
        assertEquals("5432", copy.getHostPort());
        assertEquals("runtime-user", copy.getUserName());
        assertEquals("runtime-password", copy.getUserPassword());
        assertFalse(copy.hasHandler("desktop-tunnel"));
        assertTrue(original.hasHandler("desktop-tunnel"));
    }

    @Test
    public void existingPublisherConnectionRejectsJdbcUrlsRatherThanUsingStaleHostFields() {
        DBPConnectionConfiguration configuration = publisherConfiguration();
        configuration.setConfigurationType(DBPDriverConfigurationType.URL);
        DBPDataSourceContainer connection = publisherConnection(configuration);
        assertTrue(PostgreSubscription.supportsPublisherConnection(connection));
        assertThrows(DBException.class, () -> PostgreSubscription.copyPublisherConnectionConfiguration(monitor, connection));
    }

    @Test
    public void publicationChecklistPreservesManualNamesAndReplacesOnlyAvailableSelections() {
        assertEquals("manual publication\ncomma,name\nsecond",
            PostgreSubscription.mergePublicationSelection("first\nmanual publication\ncomma,name\nmanual publication\n\n",
                List.of("first", "second"), List.of("second")));
        assertEquals("manual", PostgreSubscription.mergePublicationSelection("first\nmanual", List.of("first"), List.of()));
        assertEquals("", PostgreSubscription.mergePublicationSelection("first\n\n", List.of("first"), List.of()));
    }

    @Test
    public void publicationChecklistKeepsSpecialNamesUnquotedAndProducesQuotedCreationSql() throws Exception {
        PostgreSubscription subscription = newSubscription();
        subscription.setPublications(PostgreSubscription.mergePublicationSelection("", List.of("My\"Publication", "comma,name"),
            List.of("My\"Publication", "comma,name")));
        assertEquals("My\"Publication\ncomma,name", subscription.getPublications());
        assertTrue(subscription.getCreateStatement().contains("PUBLICATION \"My\"\"Publication\", \"comma,name\""));
    }

    @Test
    public void publicationFieldFormatsAndParsesMultipleNamesWithoutLosingSpecialCharacters() throws Exception {
        List<String> names = List.of("first", "My Publication", "comma,name", "My\"Publication", " spaced ");
        String field = PostgreSubscription.formatPublicationNames(names);
        assertEquals("first, My Publication, \"comma,name\", \"My\"\"Publication\", \" spaced \"", field);
        assertEquals(names, PostgreSubscription.parsePublicationNames(field));
        assertEquals(List.of("first", "second"), PostgreSubscription.parsePublicationNames(" first , second "));
        assertEquals(List.of("comma,name", "second"), PostgreSubscription.parsePublicationNames("  \"comma,name\"  , second"));
    }

    @Test
    public void publicationFieldAllowsEmptyInputButRejectsMalformedLists() throws Exception {
        assertEquals(List.of(), PostgreSubscription.parsePublicationNames(" "));
        assertEquals("", PostgreSubscription.formatPublicationNames(List.of()));
        for (String value : List.of("first,", ",first", "first,,second", "\"unfinished", "\"\"", "\"name\"suffix", "unquoted\"quote")) {
            assertThrows(DBException.class, () -> PostgreSubscription.parsePublicationNames(value), value);
        }
    }

    @Test
    public void publicationSelectionPreservesTypedNamesAndProducesValidCreationSql() throws Exception {
        List<String> typed = PostgreSubscription.parsePublicationNames("manual, \"comma,name\", first");
        String merged = PostgreSubscription.mergePublicationSelection(String.join("\n", typed), List.of("first", "second"), List.of("second"));
        String field = PostgreSubscription.formatPublicationNames(merged.lines().toList());
        assertEquals("manual, \"comma,name\", second", field);
        PostgreSubscription subscription = newSubscription();
        subscription.setPublications(String.join("\n", PostgreSubscription.parsePublicationNames(field)));
        String ddl = subscription.getCreateStatement();
        assertTrue(ddl.contains("PUBLICATION manual, \"comma,name\", \"second\""), ddl);
    }

    @Test
    public void filteredPublicationSelectionPreservesHiddenSelectionsAndManualNames() throws Exception {
        String current = "manual\nhidden_publication\nvisible_publication";
        String added = PostgreSubscription.mergePublicationSelection(current, List.of("another_publication"), List.of("another_publication"));
        assertEquals("manual\nhidden_publication\nvisible_publication\nanother_publication", added);
        String removed = PostgreSubscription.mergePublicationSelection(added, List.of("visible_publication"), List.of());
        assertEquals("manual\nhidden_publication\nanother_publication", removed);
        assertEquals(removed.lines().toList(), PostgreSubscription.parsePublicationNames(
            PostgreSubscription.formatPublicationNames(removed.lines().toList())));
    }

    @Test
    public void publicationLookupReadsFreshNamesInIsolatedPublisherContext() throws Exception {
        PublicationLookup lookup = publicationLookup();
        when(lookup.result().next()).thenReturn(true, true, false);
        when(lookup.result().getString(1)).thenReturn("My Publication", "comma,name");
        assertEquals(List.of("My Publication", "comma,name"),
            PostgreSubscription.readPublisherPublicationNames(monitor, lookup.connection(), "testdb"));
        verify(lookup.session()).prepareStatement("SELECT pubname FROM pg_catalog.pg_publication ORDER BY pubname");
        verify(lookup.statement()).setQueryTimeout(anyInt());
        verify(lookup.result()).close();
        verify(lookup.statement()).close();
        verify(lookup.session()).close();
        verify(lookup.context()).close();
        verify(lookup.connection(), never()).connect(any(), anyBoolean(), anyBoolean());
        verify(lookup.connection(), never()).persistConfiguration();
        verify(lookup.session(), never()).commit();
        verify(lookup.session(), never()).createStatement();
        // Each request re-queries the catalog instead of reusing the publisher's metadata cache.
        assertTrue(PostgreSubscription.readPublisherPublicationNames(monitor, lookup.connection(), "testdb").isEmpty());
        verify(lookup.statement(), times(2)).executeQuery();
    }

    @Test
    public void publicationLookupConnectsTemporaryPublisherWithoutNavigatorReflection() throws Exception {
        PublicationLookup lookup = publicationLookup();
        when(lookup.connection().isConnected()).thenReturn(false);
        when(lookup.connection().isTemporary()).thenReturn(true);
        when(lookup.connection().connect(monitor, true, false)).thenReturn(true);
        assertTrue(PostgreSubscription.readPublisherPublicationNames(monitor, lookup.connection(), "testdb").isEmpty());
        verify(lookup.connection()).connect(monitor, true, false);
    }

    @Test
    public void publicationDiscoveryUsesSavedDesktopDatabaseAndPreservesConnectionSettings() throws Exception {
        PublicationLookup lookup = publicationLookup();
        DBPConnectionConfiguration saved = lookup.connection().getConnectionConfiguration();
        saved.setDatabaseName("testdb");
        saved.setHostName("localhost");
        saved.setHostPort("16432");
        saved.updateHandler(publisherHandler("desktop-tunnel"));
        DBPConnectionConfiguration replication = PostgreSubscription.copyPublisherConnectionConfiguration(monitor, lookup.connection());
        replication.setHostName("pg8117-publisher");
        replication.setHostPort("5432");
        replication.setDatabaseName("subscriber-side-database");
        assertTrue(PostgreSubscription.readPublisherPublicationNames(monitor, lookup.connection()).isEmpty());
        verify((PostgreDataSource) lookup.connection().getDataSource()).getDefaultInstance();
        verify((PostgreDataSource) lookup.connection().getDataSource(), never()).getDatabase("subscriber-side-database");
        assertEquals("localhost", saved.getHostName());
        assertEquals("16432", saved.getHostPort());
        assertTrue(saved.hasHandler("desktop-tunnel"));
        assertFalse(replication.hasHandler("desktop-tunnel"));
        verify(lookup.connection(), never()).disconnect(any());
        verify(lookup.connection(), never()).persistConfiguration();
    }

    @Test
    public void publicationLookupClosesResourcesWhenQueryFails() throws Exception {
        PublicationLookup lookup = publicationLookup();
        when(lookup.statement().executeQuery()).thenThrow(new SQLException("Query failed"));
        assertThrows(DBException.class, () -> PostgreSubscription.readPublisherPublicationNames(monitor, lookup.connection(), "testdb"));
        verify(lookup.statement()).close();
        verify(lookup.session()).close();
        verify(lookup.context()).close();
    }

    @Test
    public void publicationLookupRejectsMissingDatabaseAndFailedConnection() throws Exception {
        PublicationLookup lookup = publicationLookup();
        assertThrows(DBException.class, () -> PostgreSubscription.readPublisherPublicationNames(monitor, lookup.connection(), "missing"));
        verifyNoInteractions(lookup.context());
        when(lookup.connection().isConnected()).thenReturn(false);
        assertThrows(DBException.class, () -> PostgreSubscription.readPublisherPublicationNames(monitor, lookup.connection(), "testdb"));
        verifyNoInteractions(lookup.session());
    }

    @Test
    public void publicationLookupHonorsCancellationBeforeConnecting() throws Exception {
        PublicationLookup lookup = publicationLookup();
        DBRProgressMonitor canceled = mock(DBRProgressMonitor.class);
        when(canceled.isCanceled()).thenReturn(true);
        assertThrows(DBException.class, () -> PostgreSubscription.readPublisherPublicationNames(canceled, lookup.connection(), "testdb"));
        verify(lookup.connection(), never()).connect(any(), anyBoolean(), anyBoolean());
        verifyNoInteractions(lookup.context());
    }

    @Test
    public void publicationLookupAllowsDesktopAuthenticationWithoutReusingItForReplication() throws Exception {
        PublicationLookup lookup = publicationLookup();
        lookup.connection().getConnectionConfiguration().setAuthModelId("desktop-authentication");
        assertTrue(PostgreSubscription.supportsPublicationDiscoveryConnection(lookup.connection()));
        assertFalse(PostgreSubscription.supportsPublisherConnection(lookup.connection()));
        assertTrue(PostgreSubscription.readPublisherPublicationNames(monitor, lookup.connection(), "testdb").isEmpty());
        verify(lookup.statement()).executeQuery();
        assertThrows(DBException.class, () -> PostgreSubscription.copyPublisherConnectionConfiguration(monitor, lookup.connection()));
    }

    @Test
    public void publicationLookupRejectsNonPostgresqlDatasourcesAndUnsupportedServers() throws Exception {
        PublicationLookup lookup = publicationLookup();
        when(lookup.connection().getDriver()).thenReturn(mock(DBPDriver.class));
        assertFalse(PostgreSubscription.supportsPublicationDiscoveryConnection(lookup.connection()));
        assertThrows(DBException.class, () -> PostgreSubscription.readPublisherPublicationNames(monitor, lookup.connection(), "testdb"));
        verifyNoInteractions(lookup.context());
        when(lookup.connection().getDriver()).thenReturn(dataSource.getContainer().getDriver());
        when(((PostgreDataSource) lookup.connection().getDataSource()).getServerType()).thenReturn(new PostgreServerRedshift(dataSource));
        assertThrows(DBException.class, () -> PostgreSubscription.readPublisherPublicationNames(monitor, lookup.connection(), "testdb"));
        verifyNoInteractions(lookup.session());
    }

    @Test
    public void publicationLookupUsesActiveDatabaseForUrlBasedDatasources() throws Exception {
        PublicationLookup lookup = publicationLookup();
        DBPConnectionConfiguration configuration = lookup.connection().getConnectionConfiguration();
        configuration.setConfigurationType(DBPDriverConfigurationType.URL);
        configuration.setUrl("jdbc:postgresql://publisher/testdb");
        configuration.setDatabaseName("stale-field");
        assertTrue(PostgreSubscription.readPublisherPublicationNames(monitor, lookup.connection()).isEmpty());
        PostgreDataSource publisher = (PostgreDataSource) lookup.connection().getDataSource();
        verify(publisher).getDefaultInstance();
        verify(publisher, never()).getDatabase("stale-field");
        assertEquals("stale-field", configuration.getDatabaseName());
        verify(lookup.connection(), never()).persistConfiguration();
    }

    @NotNull
    private PublicationLookup publicationLookup() throws Exception {
        DBPDataSourceContainer connection = publisherConnection(publisherConfiguration());
        when(connection.isConnected()).thenReturn(true);
        PostgreDataSource publisher = mock(PostgreDataSource.class);
        when(connection.getDataSource()).thenReturn(publisher);
        when(publisher.getServerType()).thenReturn(new PostgreServerPostgreSQL(dataSource));
        PostgreDatabase owner = mock(PostgreDatabase.class);
        when(publisher.getDatabase("testdb")).thenReturn(owner);
        when(publisher.getDefaultInstance()).thenReturn(owner);
        PostgreExecutionContext context = mock(PostgreExecutionContext.class);
        when(owner.openIsolatedContext(eq(monitor), anyString(), isNull())).thenReturn(context);
        JDBCSession session = mock(JDBCSession.class);
        when(context.openSession(eq(monitor), eq(DBCExecutionPurpose.META), anyString())).thenReturn(session);
        JDBCPreparedStatement statement = mock(JDBCPreparedStatement.class);
        when(session.prepareStatement(anyString())).thenReturn(statement);
        JDBCResultSet result = mock(JDBCResultSet.class);
        when(statement.executeQuery()).thenReturn(result);
        return new PublicationLookup(connection, context, session, statement, result);
    }

    private record PublicationLookup(
        @NotNull DBPDataSourceContainer connection,
        @NotNull PostgreExecutionContext context,
        @NotNull JDBCSession session,
        @NotNull JDBCPreparedStatement statement,
        @NotNull JDBCResultSet result
    ) {
    }

    @Test
    public void publisherConnectionTestUsesIsolatedTransactionAndAlwaysRollsBack() throws Exception {
        ConnectionProbe probe = connectionProbe();
        PostgreSubscription.testPublisherConnection(monitor, probe.subscriber(), "host=publisher password=O'Brien");
        InOrder order = inOrder(probe.session(), probe.statement());
        order.verify(probe.session()).enableLogging(false);
        order.verify(probe.session()).setAutoCommit(false);
        order.verify(probe.statement()).setQueryTimeout(anyInt());
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        order.verify(probe.statement()).execute(sql.capture());
        order.verify(probe.statement()).close();
        order.verify(probe.session()).rollback();
        assertTrue(sql.getValue().startsWith("CREATE SUBSCRIPTION "));
        assertTrue(sql.getValue().contains("CONNECTION 'host=publisher password=O''Brien'"));
        assertTrue(sql.getValue().contains("connect = true, create_slot = false, enabled = false, copy_data = false, slot_name = NONE"));
        verify(probe.session(), never()).commit();
        verify(probe.session()).close();
        verify(probe.context()).close();
    }

    @Test
    public void publisherConnectionTestRollsBackFailuresWithoutExposingCredentials() throws Exception {
        ConnectionProbe probe = connectionProbe();
        when(probe.statement().execute(anyString())).thenThrow(new SQLException("Failed password=private-test-password", "08006"));
        DBException error = assertThrows(DBException.class,
            () -> PostgreSubscription.testPublisherConnection(monitor, probe.subscriber(), "host=publisher password=private-test-password"));
        assertFalse(error.getMessage().contains("private-test-password"));
        assertTrue(error.getMessage().contains("08006"));
        assertNull(error.getCause());
        verify(probe.session()).rollback();
        verify(probe.session(), never()).commit();
        verify(probe.context()).close();
    }

    @Test
    public void publisherConnectionTestClosesItsConnectionWhenRollbackFails() throws Exception {
        ConnectionProbe probe = connectionProbe();
        doThrow(new SQLException("Rollback failed", "08006")).when(probe.session()).rollback();
        assertThrows(DBException.class, () -> PostgreSubscription.testPublisherConnection(monitor, probe.subscriber(), "host=publisher"));
        verify(probe.context()).close();
        verify(probe.session(), never()).commit();
    }

    @Test
    public void publisherConnectionTestRejectsMissingInputWithoutOpeningConnection() throws Exception {
        ConnectionProbe probe = connectionProbe();
        assertThrows(DBException.class, () -> PostgreSubscription.testPublisherConnection(monitor, probe.subscriber(), " "));
        verify(probe.subscriber(), never()).openIsolatedContext(any(), anyString(), any());
        verifyNoInteractions(probe.session());
    }

    @Test
    public void publisherConnectionTestReportsPermissionAndTimeoutFailuresSafely() throws Exception {
        for (String state : new String[]{PostgreConstants.EC_PERMISSION_DENIED, PostgreConstants.EC_QUERY_CANCELED}) {
            ConnectionProbe probe = connectionProbe();
            when(probe.statement().execute(anyString())).thenThrow(new SQLException("Credential-bearing driver error", state));
            DBException error = assertThrows(DBException.class,
                () -> PostgreSubscription.testPublisherConnection(monitor, probe.subscriber(), "host=publisher"));
            assertFalse(error.getMessage().contains("Credential-bearing"));
            assertTrue(error.getMessage().contains(PostgreConstants.EC_PERMISSION_DENIED.equals(state) ? "permission" : "timed out"));
            verify(probe.session()).rollback();
        }
    }

    @Test
    public void publisherConnectionTestHonorsCancellationBeforeOpeningConnection() throws Exception {
        ConnectionProbe probe = connectionProbe();
        DBRProgressMonitor canceled = mock(DBRProgressMonitor.class);
        when(canceled.isCanceled()).thenReturn(true);
        assertThrows(DBException.class, () -> PostgreSubscription.testPublisherConnection(canceled, probe.subscriber(), "host=publisher"));
        verify(probe.subscriber(), never()).openIsolatedContext(any(), anyString(), any());
    }

    @NotNull
    private ConnectionProbe connectionProbe() throws Exception {
        PostgreDatabase subscriber = mock(PostgreDatabase.class);
        when(subscriber.getDataSource()).thenReturn(dataSource);
        PostgreExecutionContext context = mock(PostgreExecutionContext.class);
        when(subscriber.openIsolatedContext(eq(monitor), anyString(), isNull())).thenReturn(context);
        JDBCSession session = mock(JDBCSession.class);
        when(context.openSession(eq(monitor), eq(DBCExecutionPurpose.UTIL), anyString())).thenReturn(session);
        JDBCStatement statement = mock(JDBCStatement.class);
        when(session.createStatement()).thenReturn(statement);
        return new ConnectionProbe(subscriber, context, session, statement);
    }

    private record ConnectionProbe(
        @NotNull PostgreDatabase subscriber,
        @NotNull PostgreExecutionContext context,
        @NotNull JDBCSession session,
        @NotNull JDBCStatement statement
    ) {
    }

    @NotNull
    private DBPDataSourceContainer publisherConnection(@NotNull DBPConnectionConfiguration configuration) {
        DBPDataSourceContainer connection = mock(DBPDataSourceContainer.class);
        when(connection.getDriver()).thenReturn(dataSource.getContainer().getDriver());
        when(connection.getConnectionConfiguration()).thenReturn(configuration);
        when(connection.getActualConnectionConfiguration()).thenReturn(configuration);
        return connection;
    }

    @NotNull
    private DBPConnectionConfiguration publisherConfiguration() {
        DBPConnectionConfiguration configuration = new DBPConnectionConfiguration();
        configuration.setHostName("publisher");
        configuration.setDatabaseName("published database");
        return configuration;
    }

    @NotNull
    private DBWHandlerConfiguration publisherHandler(@NotNull String id) {
        DBWHandlerDescriptor descriptor = mock(DBWHandlerDescriptor.class);
        when(descriptor.getId()).thenReturn(id);
        DBWHandlerConfiguration handler = new DBWHandlerConfiguration(descriptor, null);
        handler.setEnabled(true);
        return handler;
    }
}

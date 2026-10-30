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
import org.jkiss.dbeaver.ext.postgresql.PostgreDataSourceProvider;
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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatchers;
import org.mockito.InOrder;
import org.mockito.Mockito;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

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
        database.getPublicationCache().setCache(new ArrayList<>());
        executionContext = new PostgreExecutionContext(database, "Test");
    }

    @Test
    public void publicationSupportsEmptyAndAllTableDefinitions() throws Exception {
        PostgrePublication publication = new PostgrePublication(database, "publication");
        Assertions.assertFalse(publication.isPersisted());
        Assertions.assertEquals("CREATE PUBLICATION publication\nWITH (publish = 'insert, update, delete, truncate', " +
            "publish_via_partition_root = false);", publication.getObjectDefinitionText(monitor, Map.of()));
        publication.setAllTables(true);
        publication.setPublishViaPartitionRoot(true);
        Assertions.assertEquals("CREATE PUBLICATION publication FOR ALL TABLES\nWITH (publish = 'insert, update, delete, truncate', " +
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
        Assertions.assertEquals("CREATE PUBLICATION \"My\"\"Publication\" FOR TABLE \"My Schema\".\"My\"\"Table\"\n" +
            "WITH (publish = 'insert', publish_via_partition_root = false);",
            publication.getObjectDefinitionText(monitor, Map.of()));
        publication.setAllTables(true);
        Assertions.assertThrows(DBException.class, () -> publication.getObjectDefinitionText(monitor, Map.of()));
        publication.setAllTables(false);
        publication.setPersisted(true);
        Assertions.assertTrue(publication.getObjectDefinitionText(monitor, Map.of()).contains("FOR TABLE \"My Schema\".\"My\"\"Table\""));
    }

    @Test
    public void publicationLoadsMetadataFlags() throws Exception {
        ResultSet result = Mockito.mock(ResultSet.class);
        Mockito.when(result.getString("pubname")).thenReturn("publication");
        Mockito.when(result.getLong("oid")).thenReturn(123L);
        Mockito.when(result.getBoolean("puballtables")).thenReturn(true);
        Mockito.when(result.getBoolean("pubinsert")).thenReturn(true);
        Mockito.when(result.getBoolean("pubviaroot")).thenReturn(true);
        PostgrePublication publication = new PostgrePublication(database, result);
        Assertions.assertTrue(publication.isPersisted());
        Assertions.assertEquals(123L, publication.getObjectId());
        Assertions.assertTrue(publication.isAllTables());
        Assertions.assertTrue(publication.isPublishViaPartitionRoot());
        Assertions.assertFalse(publication.isPublishUpdate());
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
        PostgreDataSource publisher = Mockito.mock(PostgreDataSource.class);
        Mockito.when(publisher.getSQLDialect()).thenReturn(dataSource.getSQLDialect());
        Mockito.when(publisher.isServerVersionAtLeast(15, 0)).thenReturn(supportsFilters);
        PostgreDatabase owner = Mockito.mock(PostgreDatabase.class);
        Mockito.when(owner.getDataSource()).thenReturn(publisher);
        Mockito.when(owner.isInstanceConnected()).thenReturn(true);
        PostgreExecutionContext context = Mockito.mock(PostgreExecutionContext.class);
        Mockito.when(owner.getDefaultContext(ArgumentMatchers.any(DBRProgressMonitor.class),
            ArgumentMatchers.eq(true))).thenReturn(context);
        JDBCSession session = Mockito.mock(JDBCSession.class);
        Mockito.when(context.openSession(ArgumentMatchers.eq(monitor), ArgumentMatchers.eq(DBCExecutionPurpose.META),
            ArgumentMatchers.anyString())).thenReturn(session);
        JDBCPreparedStatement statement = Mockito.mock(JDBCPreparedStatement.class);
        Mockito.when(session.prepareStatement(ArgumentMatchers.anyString())).thenReturn(statement);
        JDBCResultSet result = Mockito.mock(JDBCResultSet.class);
        Mockito.when(statement.executeQuery()).thenReturn(result);
        Mockito.when(result.next()).thenReturn(true, true, false);
        Mockito.when(result.getString(1)).thenReturn(definitions.get(0), definitions.get(1));
        ResultSet metadata = Mockito.mock(ResultSet.class);
        Mockito.when(metadata.getString("pubname")).thenReturn("publication");
        Mockito.when(metadata.getLong("oid")).thenReturn(123L);
        PostgrePublication publication = new PostgrePublication(owner, metadata);

        Assertions.assertTrue(publication.getObjectDefinitionText(monitor, Map.of())
            .startsWith("CREATE PUBLICATION publication FOR " + expectedMembership + "\n"));
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        Mockito.verify(session).prepareStatement(sql.capture());
        Assertions.assertTrue(sql.getValue().startsWith(supportsFilters ? "SELECT 'TABLE ONLY ' || " : "SELECT 'ONLY ' || "));
        Assertions.assertTrue(sql.getValue().contains("FROM pg_catalog.pg_publication_rel pr"));
        Assertions.assertFalse(sql.getValue().contains("pg_publication_tables"));
        Assertions.assertEquals(supportsFilters, sql.getValue().contains("pr.prattrs"));
        Assertions.assertEquals(supportsFilters, sql.getValue().contains("pr.prqual"));
        Assertions.assertEquals(supportsFilters, sql.getValue().contains("TABLES IN SCHEMA"));
        Mockito.verify(statement).setLong(1, 123L);
        if (supportsFilters) {
            Mockito.verify(statement).setLong(2, 123L);
        } else {
            Mockito.verify(statement, Mockito.never()).setLong(ArgumentMatchers.eq(2), ArgumentMatchers.anyLong());
        }
        Mockito.verify(result).close();
        Mockito.verify(statement).close();
        Mockito.verify(session).close();
    }

    @Test
    public void subscriptionQuotesPublicationNamesAndConnectionString() throws Exception {
        PostgreSubscription subscription = newSubscription();
        subscription.setName("My\"Subscription");
        subscription.setSlotName("quoted_subscription");
        subscription.setPublications("publication\nMy Publication\ncomma,name\n\n");
        subscription.setConnectionInfo("host=publisher application_name=O'Brien");
        String ddl = subscription.getCreateStatement();
        Assertions.assertTrue(ddl.startsWith("CREATE SUBSCRIPTION \"My\"\"Subscription\""));
        Assertions.assertTrue(ddl.contains("CONNECTION 'host=publisher application_name=O''Brien'"));
        Assertions.assertTrue(ddl.contains("PUBLICATION publication, \"My Publication\", \"comma,name\""));
        Assertions.assertTrue(ddl.contains("slot_name = 'quoted_subscription'"));
    }

    @Test
    public void subscriptionDefinitionDoesNotExposeCredentialsOrCreateRemoteSlots() throws Exception {
        PostgreSubscription subscription = newSubscription();
        subscription.setConnectionInfo("host=publisher password=do-not-export");
        String ddl = subscription.getObjectDefinitionText(monitor, Map.of());
        Assertions.assertFalse(ddl.contains("do-not-export"));
        Assertions.assertTrue(ddl.contains("CONNECTION '<publisher connection string>'"));
        Assertions.assertTrue(ddl.contains("connect = false, create_slot = false, copy_data = false, enabled = false"));
        subscription.setPersisted(true);
        Assertions.assertEquals("", subscription.getConnectionInfo());
        Assertions.assertEquals("subscription", subscription.getSlotName());
    }

    @Test
    public void disconnectedSubscriptionRequiresConsistentOptions() throws Exception {
        PostgreSubscription subscription = newSubscription();
        subscription.setConnect(false);
        Assertions.assertThrows(DBException.class, subscription::getCreateStatement);
        subscription.setEnabled(false);
        subscription.setCreateSlot(false);
        subscription.setCopyData(false);
        Assertions.assertFalse(subscription.isEnabled());
        Assertions.assertFalse(subscription.isCreateSlot());
        Assertions.assertFalse(subscription.isCopyData());
        Assertions.assertTrue(subscription.getCreateStatement().contains("connect = false"));
        subscription.setEnabled(true);
        Assertions.assertThrows(DBException.class, subscription::getCreateStatement);
    }

    @Test
    public void subscriptionRejectsMissingNamesConnectionAndPublications() throws Exception {
        PostgreSubscription subscription = new PostgreSubscription(database, "subscription");
        Assertions.assertThrows(DBException.class, subscription::getCreateStatement);
        subscription.setConnectionInfo("host=publisher");
        Assertions.assertThrows(DBException.class, subscription::getCreateStatement);
        subscription.setPublications("publication");
        subscription.setName(" ");
        Assertions.assertThrows(DBException.class, subscription::getCreateStatement);
    }

    @Test
    public void subscriptionLoadsMetadataWithoutReadingConnectionInformation() throws Exception {
        ResultSet result = Mockito.mock(ResultSet.class);
        Mockito.when(result.getString("subname")).thenReturn("subscription");
        Mockito.when(result.getString("substream")).thenReturn("t");
        Mockito.when(result.getBoolean("subenabled")).thenReturn(true);
        Array publications = Mockito.mock(Array.class);
        Mockito.when(publications.getArray()).thenReturn(new String[]{"publication", "Other Publication"});
        Mockito.when(result.getArray("subpublications")).thenReturn(publications);
        PostgreSubscription subscription = new PostgreSubscription(database, result);
        Assertions.assertTrue(subscription.isPersisted());
        Assertions.assertTrue(subscription.isEnabled());
        Assertions.assertTrue(subscription.isStreaming());
        Assertions.assertEquals("publication\nOther Publication", subscription.getPublications());
        Assertions.assertEquals("", subscription.getConnectionInfo());
        Mockito.verify(result, Mockito.never()).getString("subconninfo");
        Assertions.assertTrue(subscription.getObjectDefinitionText(monitor, Map.of()).contains("slot_name = NONE"));
    }

    @Test
    public void subscriptionDefinitionPreservesTwoPhaseStatesWithoutAddingUnsupportedOptions() throws Exception {
        for (String state : new String[]{"e", "p", "d", null}) {
            ResultSet result = Mockito.mock(ResultSet.class);
            Mockito.when(result.getString("subname")).thenReturn("subscription");
            Mockito.when(result.getString("subsynccommit")).thenReturn("off");
            Mockito.when(result.getString("subtwophasestate")).thenReturn(state);
            Array publications = Mockito.mock(Array.class);
            Mockito.when(publications.getArray()).thenReturn(new String[]{"publication"});
            Mockito.when(result.getArray("subpublications")).thenReturn(publications);
            PostgreSubscription subscription = new PostgreSubscription(database, result);
            String ddl = subscription.getObjectDefinitionText(monitor, Map.of());
            Assertions.assertEquals(state, subscription.getTwoPhaseState());
            if (state == null) {
                Assertions.assertFalse(ddl.contains("two_phase"));
            } else {
                Assertions.assertTrue(ddl.contains("two_phase = " + !"d".equals(state)));
            }
        }
    }

    @Test
    public void subscriptionCacheFiltersDatabaseAndDoesNotSelectCredentialsOrNewerColumnsOn14() throws Exception {
        JDBCSession session = Mockito.mock(JDBCSession.class);
        JDBCPreparedStatement statement = Mockito.mock(JDBCPreparedStatement.class);
        Mockito.when(session.prepareStatement(ArgumentMatchers.anyString())).thenReturn(statement);
        PostgreDatabase owner = Mockito.mock(PostgreDatabase.class);
        Mockito.when(owner.getDataSource()).thenReturn(dataSource);
        Mockito.when(owner.getObjectId()).thenReturn(42L);
        new PostgreDatabase.SubscriptionCache().prepareLookupStatement(session, owner, null, "subscription");
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        Mockito.verify(session).prepareStatement(sql.capture());
        Assertions.assertFalse(sql.getValue().contains("subconninfo"));
        Assertions.assertFalse(sql.getValue().contains("s.*"));
        Assertions.assertFalse(sql.getValue().contains("s.subtwophasestate"));
        Assertions.assertTrue(sql.getValue().contains("WHERE s.subdbid=? AND s.subname=?"));
        Mockito.verify(statement).setLong(1, 42L);
        Mockito.verify(statement).setString(2, "subscription");
    }

    @Test
    public void logicalReplicationIsVersionAndServerSpecific() {
        Assertions.assertTrue(new PostgreServerPostgreSQL(dataSource).supportsLogicalReplication());
        PostgreDataSource older = Mockito.mock(PostgreDataSource.class);
        Assertions.assertFalse(new PostgreServerPostgreSQL(older).supportsLogicalReplication());
        Assertions.assertFalse(new PostgreServerRedshift(dataSource).supportsLogicalReplication());
    }

    @Test
    public void subscriptionDefaultNameSkipsExistingAndPendingSubscriptions() throws Exception {
        List<PostgreSubscription> existing = List.of(
            new PostgreSubscription(database, "new_subscription_1"),
            new PostgreSubscription(database, "new_subscription_2"));
        database.getSubscriptionCache().setCache(new ArrayList<>(existing));
        TestCommandContext context = new TestCommandContext(executionContext, false);
        Map<String, Object> options = new HashMap<>(Map.of(SQLObjectEditor.OPTION_SKIP_CONFIGURATION, true));
        PostgreSubscriptionManager manager = new PostgreSubscriptionManager();
        PostgreSubscription first = manager.createNewObject(monitor, context, database, null, options);
        database.getSubscriptionCache().setCache(new ArrayList<>(existing));
        PostgreSubscription second = manager.createNewObject(monitor, context, database, null, options);
        Assertions.assertNotNull(first);
        Assertions.assertNotNull(second);
        Assertions.assertEquals("new_subscription_3", first.getName());
        Assertions.assertEquals("new_subscription_4", second.getName());
    }

    @Test
    public void subscriptionCreateAndDropActionsRequireAutocommit() throws Exception {
        database.getSubscriptionCache().setCache(new ArrayList<>());
        PostgreSubscriptionManager manager = new PostgreSubscriptionManager();
        TestCommandContext context = new TestCommandContext(executionContext, false);
        Map<String, Object> options = new HashMap<>();
        options.put(SQLObjectEditor.OPTION_SKIP_CONFIGURATION, true);
        PostgreSubscription subscription = manager.createNewObject(monitor, context, database, null, options);
        Assertions.assertNotNull(subscription);
        Assertions.assertEquals("new_subscription_1", subscription.getName());
        subscription.setConnectionInfo("host=publisher");
        subscription.setPublications("publication");
        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(monitor, context, executionContext, Map.of(), null);
        Assertions.assertEquals(1, actions.size());
        Assertions.assertInstanceOf(SQLDatabasePersistActionAtomic.class, actions.getFirst());
        Assertions.assertTrue(actions.getFirst().getScript().startsWith("CREATE SUBSCRIPTION"));

        subscription.setPersisted(true);
        TestCommandContext deleteContext = new TestCommandContext(executionContext, false);
        manager.deleteObject(deleteContext, subscription, Map.of());
        actions = DBExecUtils.getActionsListFromCommandContext(monitor, deleteContext, executionContext, Map.of(), null);
        Assertions.assertEquals(1, actions.size());
        Assertions.assertInstanceOf(SQLDatabasePersistActionAtomic.class, actions.getFirst());
        Assertions.assertEquals("DROP SUBSCRIPTION new_subscription_1", actions.getFirst().getScript());
    }

    @Test
    public void subscriptionDropIsolatesTransactionSwitchingAndCommit() throws Exception {
        assertSubscriptionDropIsolation(false, false);
    }

    @Test
    public void failedSubscriptionDropDoesNotRollbackOrCommitUserWork() throws Exception {
        assertSubscriptionDropIsolation(true, false);
    }

    @Test
    public void missingPublisherSlotIsDetachedBeforeRetryingSubscriptionDrop() throws Exception {
        assertSubscriptionDropIsolation(false, true);
    }

    @Test
    public void failedMissingSlotRecoveryStopsWithoutTouchingUserWork() throws Exception {
        assertSubscriptionDropIsolation(true, true);
    }

    @Test
    public void missingPublisherSlotDetectionRequiresExactRemoteDiagnosticAndSlotName() {
        String message = "ERROR: could not drop replication slot \"slot\" on publisher: "
            + "ERROR: replication slot \"slot\" does not exist";
        SQLException missing = new SQLException(message + "\n", "08006");
        Assertions.assertTrue(PostgreSubscription.isMissingPublisherSlot(new DBException("Drop failed", missing), "slot"));
        Assertions.assertTrue(PostgreSubscription.isMissingPublisherSlot(
            new SQLException(message.replace("publisher: ERROR: ", "publisher: ERROR:  "), "08006"), "slot"));
        Assertions.assertFalse(PostgreSubscription.isMissingPublisherSlot(missing, "another_slot"));
        Assertions.assertFalse(PostgreSubscription.isMissingPublisherSlot(missing, null));
        Assertions.assertFalse(PostgreSubscription.isMissingPublisherSlot(new SQLException(message, "42501"), "slot"));
        Assertions.assertFalse(PostgreSubscription.isMissingPublisherSlot(new SQLException("Connection refused", "08006"), "slot"));
        Assertions.assertFalse(PostgreSubscription.isMissingPublisherSlot(new SQLException(
            "ERROR: replication slot \"slot\" does not exist", "42704"), "slot"));
    }

    private void assertSubscriptionDropIsolation(boolean fail, boolean missingSlot) throws Exception {
        PostgreDataSource subscriber = Mockito.mock(PostgreDataSource.class);
        Mockito.when(subscriber.getSQLDialect()).thenReturn(dataSource.getSQLDialect());
        DBPDataSourceInfo info = Mockito.mock(DBPDataSourceInfo.class);
        Mockito.when(info.supportsTransactionsForDDL()).thenReturn(true);
        Mockito.when(subscriber.getInfo()).thenReturn(info);
        PostgreDatabase owner = Mockito.mock(PostgreDatabase.class);
        Mockito.when(owner.getDataSource()).thenReturn(subscriber);
        Mockito.when(owner.getSubscriptionCache()).thenReturn(database.getSubscriptionCache());
        PostgreExecutionContext shared = Mockito.mock(PostgreExecutionContext.class);
        Mockito.when(shared.isConnected()).thenReturn(true);
        Mockito.when(shared.getOwnerInstance()).thenReturn(owner);
        Mockito.when(shared.getAdapter(DBCTransactionManager.class)).thenReturn(shared);
        // Model a shared connection with pending work in manual-commit mode.
        Mockito.when(shared.isAutoCommit()).thenReturn(false);
        PostgreExecutionContext isolated = Mockito.mock(PostgreExecutionContext.class);
        Mockito.when(owner.openIsolatedContext(ArgumentMatchers.eq(monitor), ArgumentMatchers.anyString(),
            ArgumentMatchers.eq(shared))).thenReturn(isolated);
        Mockito.when(isolated.isConnected()).thenReturn(true);
        Mockito.when(isolated.getDataSource()).thenReturn(subscriber);
        Mockito.when(isolated.getAdapter(DBCTransactionManager.class)).thenReturn(isolated);
        Mockito.when(isolated.isSupportsTransactions()).thenReturn(true);
        AtomicBoolean autoCommit = new AtomicBoolean(true);
        Mockito.when(isolated.isAutoCommit()).thenAnswer(invocation -> autoCommit.get());
        Mockito.doAnswer(invocation -> {
            autoCommit.set(invocation.getArgument(1));
            return null;
        }).when(isolated).setAutoCommit(ArgumentMatchers.eq(monitor), ArgumentMatchers.anyBoolean());
        JDBCSession session = Mockito.mock(JDBCSession.class);
        Mockito.when(isolated.openSession(ArgumentMatchers.eq(monitor), ArgumentMatchers.any(),
            ArgumentMatchers.anyString())).thenReturn(session);
        Mockito.when(session.getExecutionContext()).thenReturn(isolated);
        Mockito.when(session.getDataSource()).thenReturn(subscriber);
        Mockito.when(session.getProgressMonitor()).thenReturn(monitor);
        JDBCStatement statement = Mockito.mock(JDBCStatement.class);
        Mockito.when(session.prepareStatement(ArgumentMatchers.eq(DBCStatementType.SCRIPT),
            ArgumentMatchers.anyString(), ArgumentMatchers.eq(false), ArgumentMatchers.eq(false), ArgumentMatchers.eq(false)))
            .thenReturn(statement);
        DBCException failure = new DBCException("Cannot drop subscription", null, isolated);
        if (missingSlot) {
            DBCException missing = new DBCException("Drop failed", new SQLException(
                "ERROR: could not drop replication slot \"subscription_slot\" on publisher: "
                    + "ERROR: replication slot \"subscription_slot\" does not exist", "08006"), isolated);
            if (fail) {
                Mockito.when(statement.executeStatement()).thenThrow(missing).thenThrow(failure);
            } else {
                Mockito.when(statement.executeStatement()).thenThrow(missing).thenReturn(true);
            }
        } else if (fail) {
            Mockito.when(statement.executeStatement()).thenThrow(failure);
        }
        PostgreSubscription subscription = new PostgreSubscription(owner, "subscription");
        subscription.setSlotName("subscription_slot");
        subscription.setPersisted(true);
        TestCommandContext context = new TestCommandContext(shared, true);
        new PostgreSubscriptionManager().deleteObject(context, subscription, Map.of());
        Map<String, Object> validation = new HashMap<>();
        context.getFinalCommands().iterator().next().validateCommand(monitor, validation);
        Assertions.assertEquals(true, validation.get(DBECommandContext.OPTION_ISOLATED_EXECUTION));
        if (fail) {
            Assertions.assertSame(failure, Assertions.assertThrows(DBException.class, () -> context.saveChanges(monitor, Map.of())));
        } else {
            context.saveChanges(monitor, Map.of());
        }
        ArgumentCaptor<String> scripts = ArgumentCaptor.forClass(String.class);
        List<String> expectedScripts = new ArrayList<>(List.of("DROP SUBSCRIPTION subscription"));
        if (missingSlot) {
            expectedScripts.add("ALTER SUBSCRIPTION subscription DISABLE");
            if (!fail) {
                expectedScripts.add("ALTER SUBSCRIPTION subscription SET (slot_name = NONE)");
                expectedScripts.add("DROP SUBSCRIPTION subscription");
            }
        }
        Mockito.verify(session, Mockito.times(expectedScripts.size())).prepareStatement(
            ArgumentMatchers.eq(DBCStatementType.SCRIPT), scripts.capture(), ArgumentMatchers.eq(false),
                ArgumentMatchers.eq(false), ArgumentMatchers.eq(false));
        Assertions.assertEquals(expectedScripts, scripts.getAllValues());
        Mockito.verify(statement, Mockito.times(expectedScripts.size())).executeStatement();
        Mockito.verify(isolated, Mockito.atLeastOnce()).setAutoCommit(monitor, true);
        Assertions.assertTrue(autoCommit.get());
        Mockito.verify(isolated).close();
        Mockito.verify(session, Mockito.atLeastOnce()).close();
        Mockito.verify(shared, Mockito.never()).getAdapter(DBCTransactionManager.class);
        Mockito.verify(shared, Mockito.never()).isAutoCommit();
        Mockito.verify(shared, Mockito.never()).setAutoCommit(ArgumentMatchers.any(), ArgumentMatchers.anyBoolean());
        Mockito.verify(shared, Mockito.never()).commit(ArgumentMatchers.any());
        Mockito.verify(shared, Mockito.never()).rollback(ArgumentMatchers.any(), ArgumentMatchers.any());
        Mockito.verify(shared, Mockito.never()).openSession(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.anyString());
    }

    @Test
    public void subscriptionCreateFailuresDoNotExposePasswordTokensOrSql() throws Exception {
        PostgreSubscriptionManager manager = new PostgreSubscriptionManager();
        var command = manager.makeCreateCommand(newSubscription(), Map.of());
        DBEPersistAction action = command.getPersistActions(monitor, executionContext, Map.of())[0];
        assertCredentialSafePersistFailure(manager, command, action);
    }

    @Test
    public void subscriptionReportsPublisherWalLevelWithoutExposingCredentials() throws Exception {
        PostgreSubscriptionManager manager = new PostgreSubscriptionManager();
        var command = manager.makeCreateCommand(newSubscription(), Map.of());
        DBEPersistAction action = command.getPersistActions(monitor, executionContext, Map.of())[0];
        JDBCSession session = Mockito.mock(JDBCSession.class);
        Mockito.when(session.getDataSource()).thenReturn(dataSource);
        JDBCStatement statement = Mockito.mock(JDBCStatement.class);
        Mockito.when(session.prepareStatement(ArgumentMatchers.any(), ArgumentMatchers.anyString(),
            ArgumentMatchers.anyBoolean(), ArgumentMatchers.anyBoolean(), ArgumentMatchers.anyBoolean())).thenReturn(statement);
        Mockito.when(statement.executeStatement()).thenThrow(new DBSQLException(action.getScript(),
            new SQLException("could not create replication slot: ERROR: logical decoding requires wal_level >= logical; "
                + "password=private-test-password", "08P01"), executionContext));
        DBException error = Assertions.assertThrows(DBException.class, () -> manager.executePersistAction(session, command, action));
        Assertions.assertTrue(error.getMessage().contains("wal_level = logical"));
        Assertions.assertTrue(error.getMessage().contains("restart PostgreSQL"));
        Assertions.assertTrue(error.getMessage().contains("08P01"));
        Assertions.assertFalse(error.getMessage().contains("private-test-password"));
        Assertions.assertFalse(error.getMessage().contains(action.getScript()));
        Assertions.assertNull(error.getCause());
        Mockito.verify(statement).close();
    }

    @Test
    public void replicationFailureReasonsRecognizeQuotedServerSettingsAndCapacityErrors() {
        Assertions.assertTrue(PostgreSubscription.getReplicationFailureReason(
            new SQLException("logical decoding requires \"wal_level\" >= \"logical\"", "55000")).contains("wal_level = logical"));
        Assertions.assertTrue(PostgreSubscription.getReplicationFailureReason(
            new SQLException("all replication slots are in use", "53400")).contains("max_replication_slots"));
        Assertions.assertTrue(PostgreSubscription.getReplicationFailureReason(
            new SQLException("must be superuser or replication role to start walsender", "42501")).contains("REPLICATION"));
        Assertions.assertNull(PostgreSubscription.getReplicationFailureReason(new SQLException(
            "invalid password token: private-test-password")));
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
        Assertions.assertTrue(command.isDisableSessionLogging());
        JDBCSession session = Mockito.mock(JDBCSession.class);
        Mockito.when(session.getDataSource()).thenReturn(dataSource);
        JDBCStatement statement = Mockito.mock(JDBCStatement.class);
        Mockito.when(session.prepareStatement(ArgumentMatchers.any(), ArgumentMatchers.anyString(),
            ArgumentMatchers.anyBoolean(), ArgumentMatchers.anyBoolean(), ArgumentMatchers.anyBoolean())).thenReturn(statement);
        Mockito.when(statement.executeStatement()).thenThrow(new DBSQLException(action.getScript(),
            new SQLException("invalid percent-encoded token: review%ZZsecret", "42601"), executionContext));
        DBException error = Assertions.assertThrows(DBException.class, () -> manager.executePersistAction(session, command, action));
        Assertions.assertFalse(error.getMessage().contains("review%ZZsecret"));
        Assertions.assertFalse(error.getMessage().contains(action.getScript()));
        Assertions.assertTrue(error.getMessage().contains("42601"));
        Assertions.assertNull(error.getCause());
        Assertions.assertEquals(0, error.getSuppressed().length);
        Mockito.verify(statement).close();
    }

    @Test
    public void subscriptionNonCredentialErrorsKeepTheirDiagnostics() throws Exception {
        JDBCSession session = Mockito.mock(JDBCSession.class);
        Mockito.when(session.getDataSource()).thenReturn(dataSource);
        JDBCStatement statement = Mockito.mock(JDBCStatement.class);
        Mockito.when(session.prepareStatement(ArgumentMatchers.any(), ArgumentMatchers.anyString(),
            ArgumentMatchers.anyBoolean(), ArgumentMatchers.anyBoolean(), ArgumentMatchers.anyBoolean())).thenReturn(statement);
        DBCException failure = new DBCException("Permission denied", null, executionContext);
        Mockito.when(statement.executeStatement()).thenThrow(failure);
        Assertions.assertSame(failure, Assertions.assertThrows(DBException.class,
            () -> new PostgreSubscriptionManager().executePersistAction(
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
        Assertions.assertTrue(actions.getFirst().getScript().contains("password=do-not-log"));
        Assertions.assertFalse(context.getFinalCommands().isEmpty());
        for (var command : context.getFinalCommands()) {
            Assertions.assertTrue(command.isDisableSessionLogging());
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
        Assertions.assertNotNull(created);
        Assertions.assertNotSame(configured, created);
        Assertions.assertSame(database, created.getDatabase());
        Assertions.assertEquals(configured.getCreateStatement(), created.getCreateStatement());
        Assertions.assertFalse(created.isPersisted());
        Assertions.assertFalse(configured.isPersisted());

        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(monitor, context, executionContext, Map.of(), null);
        Assertions.assertEquals(1, actions.size());
        Assertions.assertInstanceOf(SQLDatabasePersistActionAtomic.class, actions.getFirst());
        Assertions.assertEquals(created.getCreateStatement(), actions.getFirst().getScript());
        created.setConnectionInfo("host=different_publisher");
        Assertions.assertEquals("host=publisher", configured.getConnectionInfo());
    }

    @Test
    public void configuredSubscriptionIsCreatedInTheRequestedDatabase() throws Exception {
        PostgreSubscription configured = newSubscription();
        PostgreDatabase target = new PostgreDatabase(dataSource, "targetdb");
        TestCommandContext context = new TestCommandContext(executionContext, true);
        Map<String, Object> options = new HashMap<>();
        options.put(SQLObjectEditor.OPTION_SKIP_CONFIGURATION, true);
        PostgreSubscription created = new PostgreSubscriptionManager().createNewObject(monitor, context, target, configured, options);
        Assertions.assertNotNull(created);
        Assertions.assertSame(target, created.getDatabase());
        Assertions.assertSame(database, configured.getDatabase());
    }

    @Test
    public void persistedSubscriptionCanBeDisabledAndEnabledWithoutPublisherCredentials() throws Exception {
        PostgreSubscription subscription = newSubscription();
        subscription.setPersisted(true);
        Assertions.assertTrue(new PostgreSubscriptionManager().canEditObject(subscription));
        Assertions.assertEquals("", subscription.getConnectionInfo());
        Assertions.assertEquals(List.of("ALTER SUBSCRIPTION subscription DISABLE"),
            alterSubscription(subscription, Map.of("enabled", false)).stream().map(DBEPersistAction::getScript).toList());
        Assertions.assertEquals(List.of("ALTER SUBSCRIPTION subscription ENABLE"),
            alterSubscription(subscription, Map.of("enabled", true)).stream().map(DBEPersistAction::getScript).toList());
    }

    @Test
    public void subscriptionNameIsEditableOnlyBeforeSaving() {
        PostgreSubscription subscription = newSubscription();
        PropertySourceEditable source = new PropertySourceEditable(
            new TestCommandContext(executionContext, false), subscription, subscription);
        ObjectPropertyDescriptor name = ObjectAttributeDescriptor.extractAnnotations(
            source, PostgreSubscription.class, null, null).stream()
            .filter(property -> "name".equals(property.getId())).findFirst().orElseThrow();
        Assertions.assertTrue(name.isEditable(subscription));
        subscription.setPersisted(true);
        Assertions.assertFalse(name.isEditable(subscription));
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
        Assertions.assertEquals(List.of(
            "ALTER SUBSCRIPTION subscription DISABLE",
            "ALTER SUBSCRIPTION subscription SET (slot_name = NONE, binary = true, streaming = true, synchronous_commit = 'local')"
        ), actions.stream().map(DBEPersistAction::getScript).toList());
        Assertions.assertTrue(actions.stream().noneMatch(SQLDatabasePersistActionAtomic.class::isInstance));
        Assertions.assertTrue(actions.stream().noneMatch(action -> action.getScript().contains("CONNECTION")));
    }

    @Test
    public void savingSubscriptionConnectionReplacementExecutesAlterConnection() throws Exception {
        PostgreSubscription subscription = new PostgreSubscription(database, "subscription");
        subscription.setPersisted(true);
        Assertions.assertEquals("", subscription.getConnectionInfo());
        PostgreExecutionContext context = Mockito.mock(PostgreExecutionContext.class);
        Mockito.when(context.isConnected()).thenReturn(true);
        PostgreDataSource subscriber = Mockito.mock(PostgreDataSource.class);
        DBPDataSourceInfo info = Mockito.mock(DBPDataSourceInfo.class);
        Mockito.when(info.supportsTransactionsForDDL()).thenReturn(true);
        Mockito.when(subscriber.getInfo()).thenReturn(info);
        Mockito.when(context.getDataSource()).thenReturn(subscriber);
        Mockito.when(context.getAdapter(DBCTransactionManager.class)).thenReturn(context);
        Mockito.when(context.isSupportsTransactions()).thenReturn(true);
        Mockito.when(context.isAutoCommit()).thenReturn(false);
        JDBCSession session = Mockito.mock(JDBCSession.class);
        Mockito.when(context.openSession(ArgumentMatchers.eq(monitor),
            ArgumentMatchers.eq(DBCExecutionPurpose.META_DDL), ArgumentMatchers.anyString())).thenReturn(session);
        Mockito.when(session.getDataSource()).thenReturn(dataSource);
        Mockito.when(session.getExecutionContext()).thenReturn(context);
        Mockito.when(session.isLoggingEnabled()).thenReturn(true);
        JDBCStatement statement = Mockito.mock(JDBCStatement.class);
        Mockito.when(session.prepareStatement(ArgumentMatchers.any(), ArgumentMatchers.anyString(),
            ArgumentMatchers.anyBoolean(), ArgumentMatchers.anyBoolean(), ArgumentMatchers.anyBoolean())).thenReturn(statement);
        TestCommandContext commands = new TestCommandContext(context, false);
        PropertySourceEditable source = new PropertySourceEditable(commands, subscription, subscription);
        source.collectProperties();
        Assertions.assertFalse(commands.isDirty());
        source.setPropertyValue(monitor, "connectionInfo", "host=publisher dbname=replication user=replicator password=O'Brien");
        Assertions.assertTrue(commands.isDirty());
        commands.saveChanges(monitor, Map.of());
        Mockito.verify(session).prepareStatement(DBCStatementType.SCRIPT,
            "ALTER SUBSCRIPTION subscription CONNECTION 'host=publisher dbname=replication user=replicator password=O''Brien'",
            false, false, false);
        InOrder execution = Mockito.inOrder(session, statement, context);
        execution.verify(session).enableLogging(false);
        execution.verify(statement).executeStatement();
        execution.verify(session).enableLogging(true);
        execution.verify(context).commit(session);
        Assertions.assertFalse(commands.isDirty());
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
        Assertions.assertEquals(List.of(
            "ALTER SUBSCRIPTION subscription CONNECTION 'host=publisher password=O''Brien'",
            "ALTER SUBSCRIPTION subscription SET (slot_name = 'existing_slot')",
            "ALTER SUBSCRIPTION subscription SET PUBLICATION \"New Publication\", \"Other\"\"Publication\" WITH (refresh = false)",
            "ALTER SUBSCRIPTION subscription ENABLE"
        ), alterSubscription(subscription, changes).stream().map(DBEPersistAction::getScript).toList());
        Assertions.assertFalse(subscription.getObjectDefinitionText(monitor, Map.of()).contains("O'Brien"));
    }

    @Test
    public void persistedSubscriptionRejectsUnsafeOrEmptyChanges() {
        PostgreSubscription subscription = newSubscription();
        subscription.setPersisted(true);
        Map<String, Object> slotChange = new HashMap<>();
        slotChange.put("slotName", null);
        Assertions.assertThrows(DBException.class, () -> alterSubscription(subscription, slotChange));
        Assertions.assertThrows(DBException.class, () -> alterSubscription(subscription, Map.of("publications", " ")));
        Assertions.assertThrows(DBException.class, () -> alterSubscription(subscription, Map.of("connectionInfo", " ")));
        Assertions.assertThrows(DBException.class, () -> alterSubscription(subscription, Map.of("synchronousCommit", "invalid")));
    }

    @Test
    public void subscriptionMetadataExposesAlterableOptionsButHidesCreationOnlyOptions() {
        PostgreSubscription subscription = newSubscription();
        subscription.setPersisted(true);
        PropertySourceEditable source = new PropertySourceEditable(subscription, subscription);
        source.collectProperties();
        for (String property : List.of("enabled", "connectionInfo", "publications", "slotName", "binary", "streaming",
            "synchronousCommit")) {
            Assertions.assertTrue(Arrays.stream(source.getProperties()).anyMatch(descriptor -> property.equals(
                descriptor.getId()) && descriptor.isEditable(subscription)), property);
        }
        for (String property : List.of("connect", "createSlot", "copyData")) {
            Assertions.assertTrue(Arrays.stream(source.getProperties()).noneMatch(descriptor -> property.equals(
                descriptor.getId())), property);
        }
        PostgreSubscription.CreationPropertyValidator visibility = new PostgreSubscription.CreationPropertyValidator();
        Assertions.assertFalse(visibility.isValidValue(subscription, null));
        Assertions.assertTrue(visibility.isValidValue(newSubscription(), null));
        PostgreSubscription.SynchronousCommitListProvider modes = new PostgreSubscription.SynchronousCommitListProvider();
        Assertions.assertFalse(modes.allowCustomValue());
        Assertions.assertArrayEquals(new String[]{"off", "local", "remote_write", "on", "remote_apply"},
            modes.getPossibleValues(subscription));
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
            Assertions.assertEquals(changes.containsKey("connectionInfo"), command.isDisableSessionLogging());
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
        Assertions.assertNotNull(publication);
        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(monitor, context, executionContext, Map.of(), null);
        Assertions.assertTrue(actions.getFirst().getScript().startsWith("CREATE PUBLICATION"));
        Assertions.assertFalse(actions.getFirst() instanceof SQLDatabasePersistActionAtomic);
        publication.setPersisted(true);
        TestCommandContext deleteContext = new TestCommandContext(executionContext, false);
        manager.deleteObject(deleteContext, publication, Map.of());
        actions = DBExecUtils.getActionsListFromCommandContext(monitor, deleteContext, executionContext, Map.of(), null);
        Assertions.assertEquals("DROP PUBLICATION new_publication_1", actions.getFirst().getScript());
    }

    @Test
    public void publicationDefaultNameSkipsExistingAndPendingPublications() throws Exception {
        PostgrePublication existing = new PostgrePublication(database, "new_publication_1");
        existing.setPersisted(true);
        database.getPublicationCache().setCache(new ArrayList<>(List.of(existing)));
        TestCommandContext context = new TestCommandContext(executionContext, false);
        Map<String, Object> options = new HashMap<>(Map.of(SQLObjectEditor.OPTION_SKIP_CONFIGURATION, true));
        PostgrePublicationManager manager = new PostgrePublicationManager();
        PostgrePublication first = manager.createNewObject(monitor, context, database, null, options);
        database.getPublicationCache().setCache(new ArrayList<>(List.of(existing)));
        PostgrePublication second = manager.createNewObject(monitor, context, database, null, options);
        Assertions.assertNotNull(first);
        Assertions.assertNotNull(second);
        Assertions.assertEquals("new_publication_2", first.getName());
        Assertions.assertEquals("new_publication_3", second.getName());
        for (var command : context.getFinalCommands()) {
            command.validateCommand(monitor, new HashMap<>());
        }
    }

    @Test
    public void publicationCreationOpensMetadataEditorAndDoesNotSaveImmediately() {
        long options = new PostgrePublicationManager().getMakerOptions(dataSource);
        Assertions.assertEquals(DBEObjectMaker.FEATURE_EDITOR_ON_CREATE, options);
        Assertions.assertEquals(0, options & DBEObjectMaker.FEATURE_SAVE_IMMEDIATELY);
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
        Assertions.assertEquals(List.of(hidden, visible, added), selected);
        Assertions.assertEquals(List.of(hidden, added), PostgrePublication.mergeTableSelection(selected, visible, false));
        Assertions.assertEquals(List.of(hidden, visible), original);
        Assertions.assertEquals(selected, PostgrePublication.mergeTableSelection(selected, added, true));
        Assertions.assertEquals(original, PostgrePublication.mergeTableSelection(original, added, false));
    }

    @Test
    public void publicationTableSelectorParticipatesInMetadataEditorUndoAndCreateScript() throws Exception {
        TestCommandContext context = new TestCommandContext(executionContext, false);
        PostgrePublication publication = new PostgrePublicationManager().createNewObject(
            monitor, context, database, null, new HashMap<>(Map.of(SQLObjectEditor.OPTION_SKIP_CONFIGURATION, true)));
        Assertions.assertNotNull(publication);
        PostgreSchema schema = new PostgreSchema(database, "public", (PostgreRole) null);
        PostgreTableRegular table = new PostgreTableRegular(schema);
        table.setName("authors");
        PropertySourceEditable source = new PropertySourceEditable(context, publication, publication);
        source.collectProperties();
        Assertions.assertTrue(Arrays.stream(source.getProperties()).noneMatch(
            property -> PostgrePublication.PROP_ID_CREATION_TABLES.equals(property.getId())));
        ObjectPropertyDescriptor descriptor = ObjectAttributeDescriptor.extractAnnotations(source, PostgrePublication.class, null, null)
            .stream().filter(property -> PostgrePublication.PROP_ID_CREATION_TABLES.equals(property.getId())).findFirst().orElseThrow();
        source.setPropertyValue(monitor, publication, descriptor, List.of(table));
        Assertions.assertEquals(List.of(table), publication.getCreationTables());
        List<DBEPersistAction> actions = DBExecUtils.getActionsListFromCommandContext(monitor, context, executionContext, Map.of(), null);
        Assertions.assertEquals(1, actions.size());
        Assertions.assertTrue(actions.getFirst().getScript().contains("FOR TABLE public.authors"));
        Assertions.assertFalse(publication.isPersisted());
        context.undoCommand();
        Assertions.assertTrue(publication.getCreationTables().isEmpty());
        context.redoCommand();
        Assertions.assertEquals(List.of(table), publication.getCreationTables());
        PostgreTableRegular anotherTable = new PostgreTableRegular(schema);
        anotherTable.setName("orders");
        source.setPropertyValue(monitor, publication, descriptor,
            PostgrePublication.mergeTableSelection(publication.getCreationTables(), anotherTable, true));
        Assertions.assertEquals(List.of(table, anotherTable), publication.getCreationTables());
        // The property editor coalesces consecutive changes to the same property into one undo step.
        context.undoCommand();
        Assertions.assertTrue(publication.getCreationTables().isEmpty());
        context.redoCommand();
        Assertions.assertEquals(List.of(table, anotherTable), publication.getCreationTables());
        actions = DBExecUtils.getActionsListFromCommandContext(monitor, context, executionContext, Map.of(), null);
        Assertions.assertEquals(1, actions.size());
        Assertions.assertTrue(actions.getFirst().getScript().contains("FOR TABLE public.authors, public.orders"));
    }

    @NotNull
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
        Assertions.assertEquals("host='publisher' dbname='database with spaces' password='O\\'Brien\\\\secret'",
            PostgreSubscription.buildConnectionInfo(parameters));
    }

    @Test
    public void publisherConnectionOmitsEmptyOptionalParametersAndQuotesSqlSeparately() throws Exception {
        Map<String, String> parameters = new LinkedHashMap<>();
        parameters.put("host", "publisher");
        parameters.put("password", "");
        Assertions.assertEquals("host='publisher'", PostgreSubscription.buildConnectionInfo(parameters));
        PostgreSubscription subscription = newSubscription();
        subscription.setConnectionInfo(PostgreSubscription.buildConnectionInfo(parameters));
        Assertions.assertTrue(subscription.getCreateStatement().contains("CONNECTION 'host=''publisher'''"));
    }

    @Test
    public void publisherConfigurationUsesNativeCredentialsAndDefaultPort() throws Exception {
        DBPConnectionConfiguration configuration = publisherConfiguration();
        configuration.setUserName("replicator");
        configuration.setUserPassword("O'Brien\\secret");
        String connection = PostgreSubscription.buildConnectionInfo(configuration);
        Assertions.assertTrue(connection.startsWith("host='publisher' port='5432' dbname='published database' user='replicator'"));
        Assertions.assertTrue(connection.contains("password='O\\'Brien\\\\secret'"));
        Assertions.assertFalse(connection.contains("sslmode="));
        Assertions.assertEquals("O'Brien\\secret", configuration.getUserPassword());
    }

    @Test
    public void publisherUriUsesExplicitCredentialsAndSslWithoutChangingTheEndpoint() throws Exception {
        DBPConnectionConfiguration configuration = publisherConfiguration();
        configuration.setUserName("replicator +&");
        configuration.setUserPassword(" secret?&=+#é ");
        configuration.setProperty("sslmode", "verify-full");
        configuration.setProperty("sslrootcert", "/server/ca cert.pem");
        String url = "jdbc:postgresql://publisher.internal:5433/sales?application_name=subscription";
        Assertions.assertEquals("postgresql://publisher.internal:5433/sales?application_name=subscription"
            + "&user=replicator%20%2B%26&password=%20secret%3F%26%3D%2B%23%C3%A9%20"
            + "&sslmode=verify-full&sslrootcert=%2Fserver%2Fca%20cert.pem",
            PostgreSubscription.buildConnectionUri(url, configuration));
        Assertions.assertEquals("publisher", configuration.getHostName());
        Assertions.assertEquals(" secret?&=+#é ", configuration.getUserPassword());
    }

    @Test
    public void publisherUriKeepsEncodedAndMultipleHostAddresses() throws Exception {
        DBPConnectionConfiguration configuration = new DBPConnectionConfiguration();
        for (String url : List.of("postgresql://first:5432,second:5433/sales?target_session_attrs=read-write",
            "postgresql://[2001:db8::1]:5432/sales", "postgresql:///sales?host=%2Fvar%2Flib%2Fpostgresql",
            "postgres://replicator:p%40ss@publisher/sales")) {
            Assertions.assertEquals(url, PostgreSubscription.buildConnectionUri(url, configuration));
        }
        configuration.setUserName("replicator");
        Assertions.assertEquals("postgresql://publisher/sales?user=replicator",
            PostgreSubscription.buildConnectionUri("postgresql://publisher/sales", configuration));
    }

    @Test
    public void publisherUriPreservesExplicitCredentialsAndSslOverFormDefaults() throws Exception {
        DBPConnectionConfiguration configuration = publisherConfiguration();
        configuration.setUserName("form-user");
        configuration.setUserPassword("form-password");
        configuration.setProperty("sslmode", "prefer");
        String url = "postgresql://uri-user:uri-password@publisher/sales?sslmode=verify-full";
        Assertions.assertEquals(url, PostgreSubscription.buildConnectionUri(url, configuration));
        url = "postgresql://uri-user:uri-password@first:5432,second:5433/sales?sslmode=verify-full";
        Assertions.assertEquals(url, PostgreSubscription.buildConnectionUri(url, configuration));
        url = "postgresql://publisher/sales?user=uri-user&password=uri-password&ssl=true";
        Assertions.assertEquals(url, PostgreSubscription.buildConnectionUri(url, configuration));
    }

    @Test
    public void publisherUriRejectsInvalidUrisAndDesktopOnlySettings() {
        DBPConnectionConfiguration configuration = new DBPConnectionConfiguration();
        for (String url : List.of("", "jdbc:mysql://publisher/sales", "host=publisher", "postgresql:publisher",
            "postgresql://publisher/sales#fragment", "postgresql://publisher/sales with spaces")) {
            DBException error = Assertions.assertThrows(DBException.class,
                () -> PostgreSubscription.buildConnectionUri(url, configuration));
            Assertions.assertEquals("Enter a PostgreSQL connection URL (postgresql://host:port/database)", error.getMessage());
        }
        configuration.setAuthModelId("desktop-authentication");
        Assertions.assertThrows(DBException.class,
            () -> PostgreSubscription.buildConnectionUri("postgresql://publisher/sales", configuration));
        configuration.setAuthModelId(AuthModelDatabaseNative.ID);
        configuration.updateHandler(publisherHandler("desktop-tunnel"));
        Assertions.assertThrows(DBException.class,
            () -> PostgreSubscription.buildConnectionUri("postgresql://publisher/sales", configuration));
    }

    @Test
    public void publisherConfigurationRejectsInvalidEndpointAndDesktopAuthentication() {
        DBPConnectionConfiguration configuration = publisherConfiguration();
        configuration.setHostPort("70000");
        Assertions.assertThrows(DBException.class, () -> PostgreSubscription.buildConnectionInfo(configuration));
        configuration.setHostPort("not a port");
        Assertions.assertThrows(DBException.class, () -> PostgreSubscription.buildConnectionInfo(configuration));
        configuration.setHostPort("5432");
        configuration.setAuthModelId("desktop-authentication");
        Assertions.assertThrows(DBException.class, () -> PostgreSubscription.buildConnectionInfo(configuration));
        configuration.setAuthModelId(AuthModelDatabaseNative.ID);
        configuration.setHostName(" ");
        Assertions.assertThrows(DBException.class, () -> PostgreSubscription.buildConnectionInfo(configuration));
    }

    @Test
    public void publisherConfigurationDoesNotSilentlyIgnoreJdbcUrlsOrTunnels() {
        DBPConnectionConfiguration configuration = publisherConfiguration();
        configuration.setConfigurationType(DBPDriverConfigurationType.URL);
        configuration.setUrl("jdbc:postgresql://different-publisher/database");
        Assertions.assertThrows(DBException.class, () -> PostgreSubscription.buildConnectionInfo(configuration));
        configuration.setConfigurationType(DBPDriverConfigurationType.MANUAL);
        DBWHandlerConfiguration tunnel = publisherHandler("desktop-tunnel");
        configuration.updateHandler(tunnel);
        Assertions.assertThrows(DBException.class, () -> PostgreSubscription.buildConnectionInfo(configuration));
        tunnel.setEnabled(false);
        Assertions.assertDoesNotThrow(() -> PostgreSubscription.buildConnectionInfo(configuration));
    }

    @Test
    public void publisherConfigurationMapsSslModeAndServerCertificatePaths() throws Exception {
        DBWHandlerConfiguration ssl = publisherHandler(PostgreConstants.HANDLER_SSL);
        ssl.setProperty(PostgreConstants.PROP_SSL_MODE, "verify-full");
        ssl.setProperty(SSLHandlerTrustStoreImpl.PROP_SSL_CA_CERT, "/server/ca.pem");
        ssl.setProperty(SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_CERT, "/server/client.pem");
        ssl.setProperty(SSLHandlerTrustStoreImpl.PROP_SSL_CLIENT_KEY, "/server/client.key");
        DBPConnectionConfiguration configuration = publisherConfiguration();
        configuration.updateHandler(ssl);
        String connection = PostgreSubscription.buildConnectionInfo(configuration);
        Assertions.assertTrue(connection.contains("sslmode='verify-full'"));
        Assertions.assertTrue(connection.contains("sslrootcert='/server/ca.pem'"));
        Assertions.assertTrue(connection.contains("sslcert='/server/client.pem'"));
        Assertions.assertTrue(connection.contains("sslkey='/server/client.key'"));
    }

    @Test
    public void publisherConfigurationPreservesJdbcSslVerificationDefaults() throws Exception {
        DBPConnectionConfiguration configuration = publisherConfiguration();
        configuration.setProperty(PostgreConstants.PROP_SSL, "true");
        Assertions.assertTrue(PostgreSubscription.buildConnectionInfo(configuration).contains("sslmode='verify-full'"));
        configuration.setProperty("sslmode", "require");
        Assertions.assertTrue(PostgreSubscription.buildConnectionInfo(configuration).contains("sslmode='require'"));
        configuration.setProperty("sslfactory", "desktop.SSLFactory");
        Assertions.assertThrows(DBException.class, () -> PostgreSubscription.buildConnectionInfo(configuration));
    }

    @Test
    public void publisherConfigurationRejectsJavaKeystoresAndEmbeddedCertificates() {
        DBPConnectionConfiguration configuration = publisherConfiguration();
        DBWHandlerConfiguration ssl = publisherHandler(PostgreConstants.HANDLER_SSL);
        configuration.updateHandler(ssl);
        ssl.setProperty(SSLHandlerTrustStoreImpl.PROP_SSL_METHOD, SSLConfigurationMethod.KEYSTORE.name());
        Assertions.assertThrows(DBException.class, () -> PostgreSubscription.buildConnectionInfo(configuration));
        ssl.setProperty(SSLHandlerTrustStoreImpl.PROP_SSL_METHOD, SSLConfigurationMethod.CERTIFICATES.name());
        ssl.setSecureProperty(SSLHandlerTrustStoreImpl.PROP_SSL_CA_CERT_VALUE, "embedded certificate");
        Assertions.assertThrows(DBException.class, () -> PostgreSubscription.buildConnectionInfo(configuration));
    }

    @Test
    public void existingPublisherConnectionsRequirePostgresqlAndNativeAuthentication() throws Exception {
        DBPConnectionConfiguration configuration = publisherConfiguration();
        DBPDataSourceContainer connection = publisherConnection(configuration);
        Assertions.assertTrue(PostgreSubscription.supportsPublisherConnection(connection));
        configuration.setAuthModelId(AuthModelDatabaseNative.ID);
        Assertions.assertTrue(PostgreSubscription.supportsPublisherConnection(connection));
        configuration.setAuthModelId("desktop-authentication");
        Assertions.assertFalse(PostgreSubscription.supportsPublisherConnection(connection));
        Assertions.assertThrows(DBException.class, () -> PostgreSubscription.copyPublisherConnectionConfiguration(monitor, connection));
        configuration.setAuthModelId(AuthModelDatabaseNative.ID);
        DBPConnectionConfiguration actual = new DBPConnectionConfiguration(configuration);
        actual.setAuthModelId("desktop-authentication");
        Mockito.when(connection.getActualConnectionConfiguration()).thenReturn(actual);
        Assertions.assertFalse(PostgreSubscription.supportsPublisherConnection(connection));
        Mockito.when(connection.getActualConnectionConfiguration()).thenReturn(configuration);
        Mockito.when(connection.getDriver()).thenReturn(Mockito.mock(DBPDriver.class));
        Assertions.assertFalse(PostgreSubscription.supportsPublisherConnection(connection));
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
        Mockito.verify(source).isCredentialsSaved();
        Assertions.assertNotSame(original, copy);
        Assertions.assertEquals("  O'Brien\\secret  ", copy.getUserPassword());
        copy.setHostName("server-side-publisher");
        copy.setUserPassword("different password");
        copy.getHandler(PostgreConstants.HANDLER_SSL).setProperty(PostgreConstants.PROP_SSL_MODE, "disable");
        Assertions.assertEquals("publisher", original.getHostName());
        Assertions.assertEquals("  O'Brien\\secret  ", original.getUserPassword());
        Assertions.assertEquals("require", ssl.getStringProperty(PostgreConstants.PROP_SSL_MODE));
        Mockito.verify(source, Mockito.never()).persistConfiguration();
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
        Mockito.when(source.getActualConnectionConfiguration()).thenReturn(actual);
        DBPConnectionConfiguration copy = PostgreSubscription.copyPublisherConnectionConfiguration(monitor, source);
        Assertions.assertEquals("publisher", copy.getHostName());
        Assertions.assertEquals("5432", copy.getHostPort());
        Assertions.assertEquals("runtime-user", copy.getUserName());
        Assertions.assertEquals("runtime-password", copy.getUserPassword());
        Assertions.assertFalse(copy.hasHandler("desktop-tunnel"));
        Assertions.assertTrue(original.hasHandler("desktop-tunnel"));
    }

    @Test
    public void existingPublisherConnectionRejectsJdbcUrlsRatherThanUsingStaleHostFields() {
        DBPConnectionConfiguration configuration = publisherConfiguration();
        configuration.setConfigurationType(DBPDriverConfigurationType.URL);
        DBPDataSourceContainer connection = publisherConnection(configuration);
        Assertions.assertTrue(PostgreSubscription.supportsPublisherConnection(connection));
        Assertions.assertThrows(DBException.class, () -> PostgreSubscription.copyPublisherConnectionConfiguration(monitor, connection));
    }

    @Test
    public void publicationChecklistKeepsSpecialNamesUnquotedAndProducesQuotedCreationSql() throws Exception {
        PostgreSubscription subscription = newSubscription();
        subscription.setPublications("My\"Publication\ncomma,name");
        Assertions.assertEquals("My\"Publication\ncomma,name", subscription.getPublications());
        Assertions.assertTrue(subscription.getCreateStatement().contains("PUBLICATION \"My\"\"Publication\", \"comma,name\""));
    }

    @Test
    public void publicationLookupReadsFreshNamesInIsolatedPublisherContext() throws Exception {
        PublicationLookup lookup = publicationLookup();
        Mockito.when(lookup.result().next()).thenReturn(true, true, false);
        Mockito.when(lookup.result().getString(1)).thenReturn("My Publication", "comma,name");
        Assertions.assertEquals(List.of("My Publication", "comma,name"),
            PostgreSubscription.readPublisherPublicationNames(monitor, lookup.connection()));
        Mockito.verify(lookup.session()).prepareStatement("SELECT pubname FROM pg_catalog.pg_publication ORDER BY pubname");
        Mockito.verify(lookup.statement()).setQueryTimeout(ArgumentMatchers.anyInt());
        Mockito.verify(lookup.result()).close();
        Mockito.verify(lookup.statement()).close();
        Mockito.verify(lookup.session()).close();
        Mockito.verify(lookup.context()).close();
        Mockito.verify(lookup.connection(), Mockito.never()).connect(ArgumentMatchers.any(),
            ArgumentMatchers.anyBoolean(), ArgumentMatchers.anyBoolean());
        Mockito.verify(lookup.connection(), Mockito.never()).persistConfiguration();
        Mockito.verify(lookup.session(), Mockito.never()).commit();
        Mockito.verify(lookup.session(), Mockito.never()).createStatement();
        // Each request re-queries the catalog instead of reusing the publisher's metadata cache.
        Assertions.assertTrue(PostgreSubscription.readPublisherPublicationNames(monitor, lookup.connection()).isEmpty());
        Mockito.verify(lookup.statement(), Mockito.times(2)).executeQuery();
    }

    @Test
    public void publicationLookupConnectsTemporaryPublisherWithoutNavigatorReflection() throws Exception {
        PublicationLookup lookup = publicationLookup();
        Mockito.when(lookup.connection().isConnected()).thenReturn(false);
        Mockito.when(lookup.connection().isTemporary()).thenReturn(true);
        Mockito.when(lookup.connection().connect(monitor, true, false)).thenReturn(true);
        Assertions.assertTrue(PostgreSubscription.readPublisherPublicationNames(monitor, lookup.connection()).isEmpty());
        Mockito.verify(lookup.connection()).connect(monitor, true, false);
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
        Assertions.assertTrue(PostgreSubscription.readPublisherPublicationNames(monitor, lookup.connection()).isEmpty());
        Mockito.verify((PostgreDataSource) lookup.connection().getDataSource()).getDefaultInstance();
        Mockito.verify((PostgreDataSource) lookup.connection().getDataSource(), Mockito.never()).getDatabase("subscriber-side-database");
        Assertions.assertEquals("localhost", saved.getHostName());
        Assertions.assertEquals("16432", saved.getHostPort());
        Assertions.assertTrue(saved.hasHandler("desktop-tunnel"));
        Assertions.assertFalse(replication.hasHandler("desktop-tunnel"));
        Mockito.verify(lookup.connection(), Mockito.never()).disconnect(ArgumentMatchers.any());
        Mockito.verify(lookup.connection(), Mockito.never()).persistConfiguration();
    }

    @Test
    public void publicationLookupClosesResourcesWhenQueryFails() throws Exception {
        PublicationLookup lookup = publicationLookup();
        Mockito.when(lookup.statement().executeQuery()).thenThrow(new SQLException("Query failed"));
        Assertions.assertThrows(DBException.class, () -> PostgreSubscription.readPublisherPublicationNames(monitor, lookup.connection()));
        Mockito.verify(lookup.statement()).close();
        Mockito.verify(lookup.session()).close();
        Mockito.verify(lookup.context()).close();
    }

    @Test
    public void publisherReadinessChecksLogicalWalAndClosesItsConnection() throws Exception {
        for (String level : new String[]{"logical", "replica", "minimal"}) {
            PublicationLookup lookup = publicationLookup();
            Mockito.when(lookup.result().next()).thenReturn(true);
            Mockito.when(lookup.result().getString(1)).thenReturn(level);
            Assertions.assertEquals("logical".equals(level),
                PostgreSubscription.isPublisherLogicalReplicationEnabled(monitor, lookup.connection()));
            Mockito.verify(lookup.session()).prepareStatement("SHOW wal_level");
            Mockito.verify(lookup.result()).close();
            Mockito.verify(lookup.statement()).close();
            Mockito.verify(lookup.session()).close();
            Mockito.verify(lookup.context()).close();
        }
    }

    @Test
    public void publicationLookupRejectsFailedConnection() throws Exception {
        PublicationLookup lookup = publicationLookup();
        Mockito.when(lookup.connection().isConnected()).thenReturn(false);
        Assertions.assertThrows(DBException.class, () -> PostgreSubscription.readPublisherPublicationNames(monitor, lookup.connection()));
        Mockito.verifyNoInteractions(lookup.session());
    }

    @Test
    public void publicationLookupHonorsCancellationBeforeConnecting() throws Exception {
        PublicationLookup lookup = publicationLookup();
        DBRProgressMonitor canceled = Mockito.mock(DBRProgressMonitor.class);
        Mockito.when(canceled.isCanceled()).thenReturn(true);
        Assertions.assertThrows(DBException.class, () -> PostgreSubscription.readPublisherPublicationNames(canceled, lookup.connection()));
        Mockito.verify(lookup.connection(), Mockito.never()).connect(ArgumentMatchers.any(),
            ArgumentMatchers.anyBoolean(), ArgumentMatchers.anyBoolean());
        Mockito.verifyNoInteractions(lookup.context());
    }

    @Test
    public void publicationLookupAllowsDesktopAuthenticationWithoutReusingItForReplication() throws Exception {
        PublicationLookup lookup = publicationLookup();
        lookup.connection().getConnectionConfiguration().setAuthModelId("desktop-authentication");
        Assertions.assertTrue(PostgreSubscription.supportsPublicationDiscoveryConnection(lookup.connection()));
        Assertions.assertFalse(PostgreSubscription.supportsPublisherConnection(lookup.connection()));
        Assertions.assertTrue(PostgreSubscription.readPublisherPublicationNames(monitor, lookup.connection()).isEmpty());
        Mockito.verify(lookup.statement()).executeQuery();
        Assertions.assertThrows(DBException.class,
            () -> PostgreSubscription.copyPublisherConnectionConfiguration(monitor, lookup.connection()));
    }

    @Test
    public void publicationLookupRejectsNonPostgresqlDatasourcesAndUnsupportedServers() throws Exception {
        PublicationLookup lookup = publicationLookup();
        Mockito.when(lookup.connection().getDriver()).thenReturn(Mockito.mock(DBPDriver.class));
        Assertions.assertFalse(PostgreSubscription.supportsPublicationDiscoveryConnection(lookup.connection()));
        Assertions.assertThrows(DBException.class, () -> PostgreSubscription.readPublisherPublicationNames(monitor, lookup.connection()));
        Mockito.verifyNoInteractions(lookup.context());
        Mockito.when(lookup.connection().getDriver()).thenReturn(dataSource.getContainer().getDriver());
        Mockito.when(((PostgreDataSource) lookup.connection().getDataSource()).getServerType()).thenReturn(
            new PostgreServerRedshift(dataSource));
        Assertions.assertThrows(DBException.class, () -> PostgreSubscription.readPublisherPublicationNames(monitor, lookup.connection()));
        Mockito.verifyNoInteractions(lookup.session());
    }

    @Test
    public void publicationLookupUsesActiveDatabaseForUrlBasedDatasources() throws Exception {
        PublicationLookup lookup = publicationLookup();
        DBPConnectionConfiguration configuration = lookup.connection().getConnectionConfiguration();
        configuration.setConfigurationType(DBPDriverConfigurationType.URL);
        configuration.setUrl("jdbc:postgresql://publisher/testdb");
        configuration.setDatabaseName("stale-field");
        Assertions.assertTrue(PostgreSubscription.readPublisherPublicationNames(monitor, lookup.connection()).isEmpty());
        PostgreDataSource publisher = (PostgreDataSource) lookup.connection().getDataSource();
        Mockito.verify(publisher).getDefaultInstance();
        Mockito.verify(publisher, Mockito.never()).getDatabase("stale-field");
        Assertions.assertEquals("stale-field", configuration.getDatabaseName());
        Mockito.verify(lookup.connection(), Mockito.never()).persistConfiguration();
    }

    @NotNull
    private PublicationLookup publicationLookup() throws Exception {
        DBPDataSourceContainer connection = publisherConnection(publisherConfiguration());
        Mockito.when(connection.isConnected()).thenReturn(true);
        PostgreDataSource publisher = Mockito.mock(PostgreDataSource.class);
        Mockito.when(connection.getDataSource()).thenReturn(publisher);
        Mockito.when(publisher.getServerType()).thenReturn(new PostgreServerPostgreSQL(dataSource));
        PostgreDatabase owner = Mockito.mock(PostgreDatabase.class);
        Mockito.when(publisher.getDefaultInstance()).thenReturn(owner);
        PostgreExecutionContext context = Mockito.mock(PostgreExecutionContext.class);
        Mockito.when(owner.openIsolatedContext(ArgumentMatchers.eq(monitor), ArgumentMatchers.anyString(),
            ArgumentMatchers.isNull())).thenReturn(context);
        JDBCSession session = Mockito.mock(JDBCSession.class);
        Mockito.when(context.openSession(ArgumentMatchers.eq(monitor), ArgumentMatchers.eq(DBCExecutionPurpose.META),
            ArgumentMatchers.anyString())).thenReturn(session);
        JDBCPreparedStatement statement = Mockito.mock(JDBCPreparedStatement.class);
        Mockito.when(session.prepareStatement(ArgumentMatchers.anyString())).thenReturn(statement);
        JDBCResultSet result = Mockito.mock(JDBCResultSet.class);
        Mockito.when(statement.executeQuery()).thenReturn(result);
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
        InOrder order = Mockito.inOrder(probe.session(), probe.statement());
        order.verify(probe.session()).enableLogging(false);
        order.verify(probe.session()).setAutoCommit(false);
        order.verify(probe.statement()).setQueryTimeout(ArgumentMatchers.anyInt());
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        order.verify(probe.statement()).execute(sql.capture());
        order.verify(probe.statement()).close();
        order.verify(probe.session()).rollback();
        Assertions.assertTrue(sql.getValue().startsWith("CREATE SUBSCRIPTION "));
        Assertions.assertTrue(sql.getValue().contains("CONNECTION 'host=publisher password=O''Brien'"));
        Assertions.assertTrue(sql.getValue().contains(
            "connect = true, create_slot = false, enabled = false, copy_data = false, slot_name = NONE"));
        Mockito.verify(probe.session(), Mockito.never()).commit();
        Mockito.verify(probe.session()).close();
        Mockito.verify(probe.context()).close();
    }

    @Test
    public void publisherConnectionTestRollsBackFailuresWithoutExposingCredentials() throws Exception {
        ConnectionProbe probe = connectionProbe();
        Mockito.when(probe.statement().execute(ArgumentMatchers.anyString())).thenThrow(new SQLException(
            "Failed password=private-test-password", "08006"));
        DBException error = Assertions.assertThrows(DBException.class,
            () -> PostgreSubscription.testPublisherConnection(monitor, probe.subscriber(),
                "host=publisher password=private-test-password"));
        Assertions.assertFalse(error.getMessage().contains("private-test-password"));
        Assertions.assertTrue(error.getMessage().contains("08006"));
        Assertions.assertNull(error.getCause());
        Mockito.verify(probe.session()).rollback();
        Mockito.verify(probe.session(), Mockito.never()).commit();
        Mockito.verify(probe.context()).close();
    }

    @Test
    public void publisherConnectionTestClosesItsConnectionWhenRollbackFails() throws Exception {
        ConnectionProbe probe = connectionProbe();
        Mockito.doThrow(new SQLException("Rollback failed", "08006")).when(probe.session()).rollback();
        Assertions.assertThrows(DBException.class, () -> PostgreSubscription.testPublisherConnection(monitor,
            probe.subscriber(), "host=publisher"));
        Mockito.verify(probe.context()).close();
        Mockito.verify(probe.session(), Mockito.never()).commit();
    }

    @Test
    public void publisherConnectionTestRejectsMissingInputWithoutOpeningConnection() throws Exception {
        ConnectionProbe probe = connectionProbe();
        Assertions.assertThrows(DBException.class, () -> PostgreSubscription.testPublisherConnection(monitor, probe.subscriber(), " "));
        Mockito.verify(probe.subscriber(), Mockito.never()).openIsolatedContext(ArgumentMatchers.any(),
            ArgumentMatchers.anyString(), ArgumentMatchers.any());
        Mockito.verifyNoInteractions(probe.session());
    }

    @Test
    public void publisherConnectionTestReportsPermissionAndTimeoutFailuresSafely() throws Exception {
        for (String state : new String[]{PostgreConstants.EC_PERMISSION_DENIED, PostgreConstants.EC_QUERY_CANCELED}) {
            ConnectionProbe probe = connectionProbe();
            Mockito.when(probe.statement().execute(ArgumentMatchers.anyString())).thenThrow(new SQLException(
                "Credential-bearing driver error", state));
            DBException error = Assertions.assertThrows(DBException.class,
                () -> PostgreSubscription.testPublisherConnection(monitor, probe.subscriber(), "host=publisher"));
            Assertions.assertFalse(error.getMessage().contains("Credential-bearing"));
            Assertions.assertTrue(error.getMessage().contains(PostgreConstants.EC_PERMISSION_DENIED.equals(
                state) ? "permission" : "timed out"));
            Mockito.verify(probe.session()).rollback();
        }
    }

    @Test
    public void publisherConnectionTestHonorsCancellationBeforeOpeningConnection() throws Exception {
        ConnectionProbe probe = connectionProbe();
        DBRProgressMonitor canceled = Mockito.mock(DBRProgressMonitor.class);
        Mockito.when(canceled.isCanceled()).thenReturn(true);
        Assertions.assertThrows(DBException.class, () -> PostgreSubscription.testPublisherConnection(canceled,
            probe.subscriber(), "host=publisher"));
        Mockito.verify(probe.subscriber(), Mockito.never()).openIsolatedContext(ArgumentMatchers.any(),
            ArgumentMatchers.anyString(), ArgumentMatchers.any());
    }

    @Test
    public void publicationDiscoveryConvertsLibpqAliasesAndAuthorityCredentialsToJdbc() throws Exception {
        DBPConnectionConfiguration original = publisherConfiguration();
        original.setUserName("form-user");
        original.setUserPassword("form-password");
        DBPConnectionConfiguration copy = PostgreSubscription.createPublicationConnectionConfiguration(
            "postgres://uri%20user:p%40ss+word@publisher:5433/sales%20db?sslmode=require", original);
        Assertions.assertEquals("jdbc:postgresql://publisher:5433/sales%20db?sslmode=require", copy.getUrl());
        Assertions.assertEquals("uri user", copy.getUserName());
        Assertions.assertEquals("p@ss+word", copy.getUserPassword());
        Assertions.assertEquals("sales db", copy.getDatabaseName());
        Assertions.assertEquals(DBPDriverConfigurationType.URL, copy.getConfigurationType());
        Assertions.assertEquals("form-user", original.getUserName());
        Assertions.assertEquals("form-password", original.getUserPassword());
        Assertions.assertEquals("published database", original.getDatabaseName());
    }

    @Test
    public void publicationDiscoveryUsesQueryCredentialsBeforeAuthorityCredentials() throws Exception {
        DBPConnectionConfiguration copy = PostgreSubscription.createPublicationConnectionConfiguration(
            "postgresql://authority:secret@publisher/sales?user=query&password=a+b%26c&sslmode=disable", publisherConfiguration());
        Assertions.assertEquals("query", copy.getUserName());
        Assertions.assertEquals("a+b&c", copy.getUserPassword());
        Assertions.assertEquals("jdbc:postgresql://publisher/sales?sslmode=disable", copy.getUrl());
    }

    @Test
    public void publicationDiscoveryPreservesLibpqSslAliasAndOptionOrderOverFormDefaults() throws Exception {
        DBPConnectionConfiguration original = publisherConfiguration();
        DBWHandlerConfiguration ssl = publisherHandler(PostgreConstants.HANDLER_SSL);
        ssl.setProperty(PostgreConstants.PROP_SSL_MODE, "prefer");
        original.updateHandler(ssl);
        Map<String, String> options = Map.of(
            "ssl=true", "require",
            "sslmode=disable&ssl=true", "require",
            "ssl=true&sslmode=verify-full", "verify-full");
        for (var option : options.entrySet()) {
            String uri = PostgreSubscription.buildConnectionUri("postgresql://publisher/sales?" + option.getKey(), original);
            DBPConnectionConfiguration copy = PostgreSubscription.createPublicationConnectionConfiguration(uri, original);
            Assertions.assertEquals("jdbc:postgresql://publisher/sales?sslmode=" + option.getValue(), copy.getUrl());
        }
        Assertions.assertEquals("prefer",
            original.getHandler(PostgreConstants.HANDLER_SSL).getStringProperty(PostgreConstants.PROP_SSL_MODE));
    }

    @Test
    public void publicationDiscoveryRejectsSslAliasesThatLibpqCannotUse() {
        for (String value : List.of("false", "1", "")) {
            Assertions.assertThrows(DBException.class, () -> PostgreSubscription.createPublicationConnectionConfiguration(
                "postgresql://publisher/sales?ssl=" + value, publisherConfiguration()));
        }
    }

    @Test
    public void publicationDiscoveryKeepsIpv6MultipleHostsAndExplicitEndpointOptions() throws Exception {
        for (String uri : List.of("postgresql://[2001:db8::1]:5433/sales", "postgresql://first:5432,second:5433/sales")) {
            Assertions.assertEquals("jdbc:" + uri,
                PostgreSubscription.createPublicationConnectionConfiguration(uri, publisherConfiguration()).getUrl());
        }
        Assertions.assertEquals("jdbc:postgresql://first:5433,second:5434/other%20db",
            PostgreSubscription.createPublicationConnectionConfiguration(
                "postgresql://old/sales?host=first,second&port=5433,5434&dbname=other%20db", publisherConfiguration()).getUrl());
        Assertions.assertEquals("jdbc:postgresql://[::1]:5434/sales",
            PostgreSubscription.createPublicationConnectionConfiguration(
                "postgresql://old/sales?host=%3A%3A1&port=5434", publisherConfiguration()).getUrl());
    }

    @Test
    public void publicationDiscoveryTranslatesSessionRequirementsWithoutChangingSubscriberUri() throws Exception {
        Map<String, String> modes = Map.of("any", "any", "read-write", "primary", "read-only", "secondary", "", "any");
        for (var mode : modes.entrySet()) {
            DBPConnectionConfiguration original = publisherConfiguration();
            String uri = PostgreSubscription.buildConnectionUri(
                "postgresql://first:5432,second:5433/sales?target_session_attrs=" + mode.getKey(), original);
            DBPConnectionConfiguration discovery = PostgreSubscription.createPublicationConnectionConfiguration(uri, original);
            Assertions.assertEquals("jdbc:postgresql://first:5432,second:5433/sales?targetServerType=" + mode.getValue(
                ), discovery.getUrl());
            Assertions.assertTrue(uri.contains("target_session_attrs=" + mode.getKey()));
            Assertions.assertFalse(uri.contains("targetServerType"));
        }
        Assertions.assertEquals("jdbc:postgresql://publisher/sales?targetServerType=secondary",
            PostgreSubscription.createPublicationConnectionConfiguration(
                "postgresql://publisher/sales?target_session_attrs=read-write&target_session_attrs=read-only",
                publisherConfiguration()).getUrl());
    }

    @Test
    public void publicationDiscoveryPreservesAuthorityPortsWhenOnlyHostsAreOverridden() throws Exception {
        Map<String, String> addresses = Map.of(
            "postgresql://old:65431/sales?host=publisher", "jdbc:postgresql://publisher:65431/sales",
            "postgresql://old:65431/sales?host=first,second", "jdbc:postgresql://first:65431,second:65431/sales",
            "postgresql://old:65431,other:65432/sales?host=first,second", "jdbc:postgresql://first:65431,second:65432/sales",
            "postgresql://old:65431/sales?host=%3A%3A1", "jdbc:postgresql://[::1]:65431/sales",
            "postgresql://old:%36%35%34%33%31/sales?host=publisher", "jdbc:postgresql://publisher:65431/sales",
            "postgresql://%70ublisher:5433/sales", "jdbc:postgresql://publisher:5433/sales",
            "postgresql:///sales?host=publisher&port=5433", "jdbc:postgresql://publisher:5433/sales");
        for (var address : addresses.entrySet()) {
            Assertions.assertEquals(address.getValue(), PostgreSubscription.createPublicationConnectionConfiguration(
                address.getKey(), publisherConfiguration()).getUrl());
        }
    }

    @Test
    public void hostBasedDiscoveryAppliesThePortToEveryHostWithoutChangingDesktopSettings() throws Exception {
        DBPConnectionConfiguration configuration = publisherConfiguration();
        configuration.setHostName("first,second");
        configuration.setHostPort("6543");
        configuration.setDatabaseName("sales");
        configuration.setProperty("targetServerType", "secondary");
        configuration.updateHandler(publisherHandler("desktop-tunnel"));
        PostgreDataSourceProvider provider = new PostgreDataSourceProvider();
        DBPDriver driver = dataSource.getContainer().getDriver();
        Assertions.assertEquals("jdbc:postgresql://first:6543,second:6543/sales", provider.getConnectionURL(driver, configuration));
        Assertions.assertEquals("first,second", configuration.getHostName());
        Assertions.assertEquals("secondary", configuration.getProperty("targetServerType"));
        Assertions.assertTrue(configuration.hasHandler("desktop-tunnel"));
        configuration.setHostName("::1,second");
        configuration.setHostPort("");
        Assertions.assertEquals("jdbc:postgresql://[::1],second/sales", provider.getConnectionURL(driver, configuration));
    }

    @Test
    public void hostBasedDiscoveryBracketsSingleIpv6HostsWithoutChangingDesktopSettings() throws Exception {
        PostgreDataSourceProvider provider = new PostgreDataSourceProvider();
        DBPDriver driver = dataSource.getContainer().getDriver();
        Map<String, String> hosts = Map.of(
            "::1", "[::1]",
            "2001:db8::1", "[2001:db8::1]",
            "[::1]", "[::1]",
            "[2001:db8::1]", "[2001:db8::1]"
        );
        for (var host : hosts.entrySet()) {
            for (String port : List.of("", "5432", "6543")) {
                DBPConnectionConfiguration configuration = publisherConfiguration();
                configuration.setHostName(host.getKey());
                configuration.setHostPort(port);
                configuration.setDatabaseName("sales");
                configuration.setProperty("targetServerType", "secondary");
                configuration.updateHandler(publisherHandler("desktop-tunnel"));
                String expected = "jdbc:postgresql://" + host.getValue() + (port.isEmpty() ? "" : ":" + port) + "/sales";
                Assertions.assertEquals(expected, provider.getConnectionURL(driver, configuration));
                Assertions.assertEquals(host.getKey(), configuration.getHostName());
                Assertions.assertEquals(port, configuration.getHostPort());
                Assertions.assertEquals("secondary", configuration.getProperty("targetServerType"));
                Assertions.assertTrue(configuration.hasHandler("desktop-tunnel"));
            }
        }
    }

    @Test
    public void publicationDiscoveryRejectsMismatchedHostAndPortCounts() {
        for (String uri : List.of(
            "postgresql://old:65431,other:65432/sales?host=publisher",
            "postgresql://first,second,third/sales?port=5433,5434")) {
            Assertions.assertThrows(DBException.class,
                () -> PostgreSubscription.createPublicationConnectionConfiguration(uri, publisherConfiguration()));
        }
    }

    @Test
    public void publicationDiscoveryRejectsImplicitUnixSocketHosts() throws Exception {
        for (String address : List.of("postgresql:///sales", "postgresql:///sales?port=65431",
            "postgresql://:65431/sales", "postgresql://first,,second/sales", "postgresql://first:5432,/sales",
            "postgresql://publisher/sales?host=", "postgresql://%40socket/sales", "postgresql://first%2Csecond/sales")) {
            String uri = PostgreSubscription.buildConnectionUri(address, publisherConfiguration());
            Assertions.assertTrue(uri.startsWith(address));
            DBException error = Assertions.assertThrows(DBException.class,
                () -> PostgreSubscription.createPublicationConnectionConfiguration(uri, publisherConfiguration()));
            Assertions.assertTrue(error.getMessage().contains("publication names manually"));
            Assertions.assertNull(error.getCause());
        }
    }

    @Test
    public void publicationDiscoveryRejectsSessionRequirementsWithoutExactJdbcEquivalent() {
        for (String mode : List.of("primary", "standby", "prefer-standby", "invalid")) {
            DBException error = Assertions.assertThrows(DBException.class,
                () -> PostgreSubscription.createPublicationConnectionConfiguration(
                "postgresql://user:secret@publisher/sales?target_session_attrs=" + mode, publisherConfiguration()));
            Assertions.assertTrue(error.getMessage().contains("publication names manually"));
            Assertions.assertFalse(error.getMessage().contains("secret"));
            Assertions.assertNull(error.getCause());
        }
    }

    @Test
    public void publicationDiscoveryTranslatesLibpqOptionsAndResetsCopiedHostSelection() throws Exception {
        DBPConnectionConfiguration original = publisherConfiguration();
        original.setProperty("targetServerType", "secondary");
        original.setProperty("loadBalanceHosts", "true");
        DBPConnectionConfiguration discovery = PostgreSubscription.createPublicationConnectionConfiguration(
            "postgresql://publisher/sales?application_name=publication%20lookup&connect_timeout=5"
                + "&channel_binding=require&gssencmode=disable&sslnegotiation=postgres&options=-c%20search_path%3Dpublic",
            original);
        Assertions.assertEquals("jdbc:postgresql://publisher/sales?ApplicationName=publication%20lookup&connectTimeout=5"
            + "&channelBinding=require&gssEncMode=disable&sslNegotiation=postgres&options=-c%20search_path%3Dpublic", discovery.getUrl());
        Assertions.assertEquals("any", discovery.getProperty("targetServerType"));
        Assertions.assertEquals("false", discovery.getProperty("loadBalanceHosts"));
        Assertions.assertEquals("secondary", original.getProperty("targetServerType"));
        Assertions.assertEquals("true", original.getProperty("loadBalanceHosts"));
    }

    @Test
    public void publicationDiscoveryRejectsUnmappedOptionsInsteadOfIgnoringTheirMeaning() throws Exception {
        for (String option : List.of("hostaddr=192.0.2.1", "service=production", "load_balance_hosts=random",
            "require_auth=scram-sha-256", "unknown=secret")) {
            String uri = PostgreSubscription.buildConnectionUri("postgresql://publisher/sales?" + option, publisherConfiguration());
            DBException error = Assertions.assertThrows(DBException.class,
                () -> PostgreSubscription.createPublicationConnectionConfiguration(uri, publisherConfiguration()));
            Assertions.assertTrue(error.getMessage().contains("publication names manually"));
            Assertions.assertFalse(error.getMessage().contains("secret"));
            Assertions.assertNull(error.getCause());
            Assertions.assertTrue(uri.contains(option));
        }
    }

    @Test
    public void publicationDiscoveryRejectsInvalidOrDesktopInaccessibleUrisWithoutExposingCredentials() {
        for (String uri : List.of("postgresql:opaque", "postgresql://publisher/sales#fragment", "mysql://publisher/sales",
            "postgresql://dummy:secret@publisher/sales?password=%GG", "postgresql:///sales?host=%2Fserver%2Fsocket")) {
            DBException error = Assertions.assertThrows(DBException.class,
                () -> PostgreSubscription.createPublicationConnectionConfiguration(uri, publisherConfiguration()));
            Assertions.assertFalse(error.getMessage().contains("secret"));
            Assertions.assertNull(error.getCause());
        }
    }

    @Test
    public void subscriptionValidatesDefaultAndExplicitSlotNamesBeforeCreation() throws Exception {
        PostgreSubscription subscription = newSubscription();
        for (String name : List.of("SalesSub", "sales-sub", "sales sub", "a".repeat(64))) {
            subscription.setName(name);
            Assertions.assertThrows(DBException.class, subscription::getCreateStatement);
        }
        subscription.setSlotName("sales_sub");
        Assertions.assertTrue(subscription.getCreateStatement().contains("slot_name = 'sales_sub'"));
        for (String name : List.of("SalesSub", "sales-sub", "a".repeat(64))) {
            subscription.setSlotName(name);
            Assertions.assertThrows(DBException.class, subscription::getCreateStatement);
        }
        Assertions.assertTrue(PostgreSubscription.isValidSlotName("a".repeat(63)));
        Assertions.assertFalse(PostgreSubscription.isValidSlotName(""));
        Assertions.assertFalse(PostgreSubscription.isValidSlotName("ä"));
        subscription.setName("subscription");
        subscription.setSlotName(null);
        subscription.setPersisted(true);
        Assertions.assertThrows(DBException.class, () -> alterSubscription(subscription, Map.of("slotName", "INVALID")));
    }

    @Test
    public void subscriptionRefreshRunsInAutocommitOnAnIsolatedConnection() throws Exception {
        ConnectionProbe probe = connectionProbe();
        PostgreSubscription subscription = new PostgreSubscription(probe.subscriber(), "refresh_subscription");
        subscription.setPersisted(true);
        subscription.refreshPublications(monitor, false);
        InOrder order = Mockito.inOrder(probe.session(), probe.statement());
        order.verify(probe.session()).setAutoCommit(true);
        order.verify(probe.statement()).execute(
            "ALTER SUBSCRIPTION refresh_subscription REFRESH PUBLICATION WITH (copy_data = false)");
        order.verify(probe.statement()).close();
        order.verify(probe.session()).close();
        Mockito.verify(probe.context()).close();
        Assertions.assertTrue(subscription.getRefreshPublicationsStatement(true).endsWith("copy_data = true)"));
        Mockito.verify(probe.session(), Mockito.never()).commit();
    }

    @Test
    public void subscriptionRefreshRequiresSavedEnabledSubscriptionAndClosesResourcesOnFailure() throws Exception {
        ConnectionProbe probe = connectionProbe();
        PostgreSubscription subscription = new PostgreSubscription(probe.subscriber(), "refresh_subscription");
        Assertions.assertThrows(DBException.class, () -> subscription.refreshPublications(monitor, true));
        subscription.setPersisted(true);
        subscription.setEnabled(false);
        Assertions.assertThrows(DBException.class, () -> subscription.refreshPublications(monitor, true));
        Mockito.verify(probe.subscriber(), Mockito.never()).openIsolatedContext(ArgumentMatchers.any(),
            ArgumentMatchers.anyString(), ArgumentMatchers.any());
        subscription.setEnabled(true);
        Mockito.when(probe.statement().execute(ArgumentMatchers.anyString())).thenThrow(new SQLException("Publisher is unavailable"));
        Assertions.assertThrows(DBException.class, () -> subscription.refreshPublications(monitor, true));
        Mockito.verify(probe.statement()).close();
        Mockito.verify(probe.session()).close();
        Mockito.verify(probe.context()).close();
    }

    @NotNull
    private ConnectionProbe connectionProbe() throws Exception {
        PostgreDatabase subscriber = Mockito.mock(PostgreDatabase.class);
        Mockito.when(subscriber.getDataSource()).thenReturn(dataSource);
        PostgreExecutionContext context = Mockito.mock(PostgreExecutionContext.class);
        Mockito.when(subscriber.openIsolatedContext(ArgumentMatchers.eq(monitor), ArgumentMatchers.anyString(),
            ArgumentMatchers.isNull())).thenReturn(context);
        JDBCSession session = Mockito.mock(JDBCSession.class);
        Mockito.when(context.openSession(ArgumentMatchers.eq(monitor), ArgumentMatchers.eq(DBCExecutionPurpose.UTIL),
            ArgumentMatchers.anyString())).thenReturn(session);
        JDBCStatement statement = Mockito.mock(JDBCStatement.class);
        Mockito.when(session.createStatement()).thenReturn(statement);
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
        DBPDataSourceContainer connection = Mockito.mock(DBPDataSourceContainer.class);
        Mockito.when(connection.getDriver()).thenReturn(dataSource.getContainer().getDriver());
        Mockito.when(connection.getConnectionConfiguration()).thenReturn(configuration);
        Mockito.when(connection.getActualConnectionConfiguration()).thenReturn(configuration);
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
        DBWHandlerDescriptor descriptor = Mockito.mock(DBWHandlerDescriptor.class);
        Mockito.when(descriptor.getId()).thenReturn(id);
        DBWHandlerConfiguration handler = new DBWHandlerConfiguration(descriptor, null);
        handler.setEnabled(true);
        return handler;
    }
}

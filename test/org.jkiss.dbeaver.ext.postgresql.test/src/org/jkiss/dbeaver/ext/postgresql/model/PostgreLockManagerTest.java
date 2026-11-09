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
import org.jkiss.dbeaver.ext.postgresql.model.impls.PostgreServerPostgreSQL;
import org.jkiss.dbeaver.ext.postgresql.model.impls.redshift.PostgreServerRedshift;
import org.jkiss.dbeaver.ext.postgresql.model.impls.redshift.RedshiftQueries;
import org.jkiss.dbeaver.ext.postgresql.model.lock.PostgreLock;
import org.jkiss.dbeaver.ext.postgresql.model.lock.PostgreLockItem;
import org.jkiss.dbeaver.ext.postgresql.model.lock.PostgreLockManager;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.admin.locks.LockGraph;
import org.jkiss.dbeaver.model.impl.admin.locks.LockGraphManager;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;

public class PostgreLockManagerTest extends DBeaverUnitTest {
    @Mock
    private PostgreDataSource dataSource;
    @Mock
    private JDBCSession session;
    @Mock
    private JDBCPreparedStatement statement;
    @Mock
    private JDBCResultSet resultSet;
    @Mock
    private JDBCPreparedStatement metadataStatement;
    @Mock
    private JDBCResultSet metadataResults;

    private PostgreLockManager manager;

    @BeforeEach
    public void setUp() throws Exception {
        Mockito.when(dataSource.getServerType()).thenReturn(new PostgreServerRedshift(dataSource));
        Mockito.when(session.prepareStatement(ArgumentMatchers.anyString())).thenReturn(statement);
        Mockito.when(session.prepareStatement(ArgumentMatchers.contains("pg_catalog.pg_class"))).thenReturn(metadataStatement);
        Mockito.when(metadataStatement.executeQuery()).thenReturn(metadataResults);
        Mockito.when(metadataResults.next()).thenReturn(true);
        Mockito.when(metadataResults.getString(1)).thenReturn("1");
        Mockito.when(statement.executeQuery()).thenReturn(resultSet);
        manager = new PostgreLockManager(dataSource);
    }

    @Test
    public void readRedshiftLocksBuildsBlockingChain() throws Exception {
        Mockito.when(resultSet.next()).thenReturn(true, true, true, false);
        Mockito.when(resultSet.getInt("blocked_pid")).thenReturn(11, 22, 33);
        Mockito.when(resultSet.getInt("blocking_pid")).thenReturn(0, 11, 22);
        Mockito.when(resultSet.getString("blocked_user")).thenReturn("owner");

        Map<Object, PostgreLock> locks = manager.getLocks(session, Map.of());

        Mockito.verify(session).prepareStatement(RedshiftQueries.LOCK_QUERY_WITH_RECENTS);
        Assertions.assertEquals(3, locks.size());
        Assertions.assertNull(locks.get(11).getHoldBy());
        Assertions.assertSame(locks.get(11), locks.get(22).getHoldBy());
        Assertions.assertSame(locks.get(22), locks.get(33).getHoldBy());
        Assertions.assertEquals(List.of(locks.get(22)), locks.get(11).waitThis());
        Assertions.assertNotNull(manager.getGraph(locks.get(33)));
    }

    @Test
    public void readRedshiftWaitingLockDetails() throws Exception {
        Mockito.when(resultSet.next()).thenReturn(true, false);
        Mockito.when(resultSet.getString("datname")).thenReturn("dev");
        Mockito.when(resultSet.getString("locktype")).thenReturn("relation");
        Mockito.when(resultSet.getString("relation")).thenReturn("100068");
        Mockito.when(resultSet.getString("mode")).thenReturn("AccessExclusiveLock");
        Mockito.when(resultSet.getString("tid")).thenReturn("438505");
        Mockito.when(resultSet.getInt("pid")).thenReturn(22);

        Collection<PostgreLockItem> items = manager.getLockItems(session, Map.of(
            LockGraphManager.keyType, LockGraphManager.typeWait, PostgreLockManager.pidWait, 22));

        Mockito.verify(session).prepareStatement(RedshiftQueries.LOCK_ITEM_QUERY);
        Mockito.verify(statement).setInt(1, 22);
        Assertions.assertEquals(1, items.size());
        PostgreLockItem item = items.iterator().next();
        Assertions.assertEquals("dev", item.getDatname());
        Assertions.assertEquals("relation", item.getLocktype());
        Assertions.assertEquals("100068", item.getRelation());
        Assertions.assertEquals("AccessExclusiveLock", item.getMode());
        Assertions.assertEquals("438505", item.getTid());
        Assertions.assertFalse(item.isGranted());
    }

    @Test
    public void readRedshiftHoldingLockDetails() throws Exception {
        manager.getLockItems(session, Map.of(
            LockGraphManager.keyType, LockGraphManager.typeHold, PostgreLockManager.pidHold, 11));

        Mockito.verify(session).prepareStatement(RedshiftQueries.LOCK_ITEM_QUERY);
        Mockito.verify(statement).setInt(1, 11);
    }

    @Test
    public void postgresqlKeepsItsLockQueries() throws Exception {
        Mockito.when(dataSource.getServerType()).thenReturn(new PostgreServerPostgreSQL(dataSource));

        manager.getLocks(session, Map.of());
        manager.getLockItems(session, Map.of(
            LockGraphManager.keyType, LockGraphManager.typeWait, PostgreLockManager.pidWait, 22));

        Mockito.verify(session).prepareStatement(PostgreLockManager.LOCK_QUERY);
        Mockito.verify(session).prepareStatement(PostgreLockManager.LOCK_ITEM_QUERY);
    }

    @Test
    public void redshiftWithoutRecentsStillReadsLocks() throws Exception {
        Mockito.when(metadataResults.next()).thenReturn(false);

        manager.getLocks(session, Map.of());

        Mockito.verify(session).prepareStatement(RedshiftQueries.LOCK_QUERY);
    }

    @Test
    public void redshiftCachesRecentsAvailability() throws Exception {
        manager.getLocks(session, Map.of());
        manager.getLocks(session, Map.of());

        Mockito.verify(metadataStatement).executeQuery();
        Mockito.verify(session, Mockito.times(2)).prepareStatement(RedshiftQueries.LOCK_QUERY_WITH_RECENTS);
    }

    @Test
    public void redshiftSupportsLockManager() {
        Assertions.assertTrue(dataSource.getServerType().supportsLocks());
    }

    @Test
    public void graphRefreshPublishesOnlyCompleteSnapshot() throws Exception {
        PostgreLock previousLock = mockLock(11);
        manager.buildGraphs(Map.of(11, previousLock));
        LockGraph previousGraph = manager.getGraph(previousLock);
        PostgreLock nextLock = mockLock(22);
        CountDownLatch building = new CountDownLatch(1);
        CountDownLatch resume = new CountDownLatch(1);
        Mockito.when(nextLock.getHoldID()).thenAnswer(invocation -> {
            building.countDown();
            if (!resume.await(5, TimeUnit.SECONDS)) {
                throw new AssertionError("Graph refresh timed out");
            }
            return 0;
        });
        FutureTask<Void> refresh = new FutureTask<>(() -> {
            manager.buildGraphs(Map.of(22, nextLock));
            return null;
        });
        Thread.ofPlatform().start(refresh);
        try {
            Assertions.assertTrue(building.await(5, TimeUnit.SECONDS));
            Assertions.assertSame(previousGraph, manager.getGraph(previousLock));
            Assertions.assertNull(manager.getGraph(nextLock));
        } finally {
            resume.countDown();
            refresh.get(5, TimeUnit.SECONDS);
        }
        Assertions.assertNull(manager.getGraph(previousLock));
        Assertions.assertSame(nextLock, manager.getGraph(nextLock).getLockRoot());
    }

    @Test
    public void failedGraphRefreshKeepsPreviousSnapshot() {
        PostgreLock previousLock = mockLock(11);
        manager.buildGraphs(Map.of(11, previousLock));
        LockGraph previousGraph = manager.getGraph(previousLock);
        PostgreLock nextLock = mockLock(22);
        Mockito.when(nextLock.getHoldID()).thenThrow(new IllegalStateException("Invalid lock snapshot"));

        Assertions.assertThrows(IllegalStateException.class, () -> manager.buildGraphs(Map.of(22, nextLock)));

        Assertions.assertSame(previousGraph, manager.getGraph(previousLock));
        Assertions.assertNull(manager.getGraph(nextLock));
    }

    @NotNull
    private PostgreLock mockLock(int pid) {
        PostgreLock lock = Mockito.mock(PostgreLock.class);
        Mockito.when(lock.getId()).thenReturn(pid);
        Mockito.when(lock.getHoldID()).thenReturn(0);
        Mockito.when(lock.waitThis()).thenReturn(new ArrayList<>());
        return lock;
    }
}

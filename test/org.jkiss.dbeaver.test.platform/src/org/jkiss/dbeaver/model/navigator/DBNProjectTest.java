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
package org.jkiss.dbeaver.model.navigator;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPEventListener;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DBNProjectTest extends DBeaverUnitTest {
    private DBPProject project;
    private DBPDataSourceRegistry registry;

    @Override
    public boolean verifyLaunched() {
        return super.verifyLaunched()
            && DBWorkbench.getPlatform().getNavigatorModel() != null
            && DBWorkbench.getPlatform().getNavigatorModel().getRoot() != null;
    }

    @BeforeEach
    public void setUpProject() {
        project = mock(DBPProject.class);
        registry = mock(DBPDataSourceRegistry.class);
        when(project.isOpen()).thenReturn(true);
        when(project.getDataSourceRegistry()).thenReturn(registry);
    }

    @Test
    public void registryLoadingDoesNotBlockNavigatorQueries() throws Exception {
        CountDownLatch loading = new CountDownLatch(1);
        CountDownLatch finishLoading = new CountDownLatch(1);
        AtomicBoolean firstLoad = new AtomicBoolean(true);
        when(project.getDataSourceRegistry()).thenAnswer(invocation -> {
            // Like BaseProjectImpl, publish the registry before prompting for its password.
            if (firstLoad.getAndSet(false)) {
                loading.countDown();
                awaitLoading(finishLoading);
            }
            return registry;
        });
        DBNProject node = new TestProject();
        var executor = Executors.newFixedThreadPool(2);
        try {
            var initialization = executor.submit(() -> node.getChildren(new VoidProgressMonitor()));
            assertTrue(loading.await(1, TimeUnit.SECONDS));
            var uiQuery = executor.submit(() -> {
                assertTrue(node.needsInitialization());
                assertNull(node.getCachedChildren());
                return node.getChildren(new VoidProgressMonitor());
            });
            DBNNode[] children = uiQuery.get(1, TimeUnit.SECONDS);
            finishLoading.countDown();
            assertArrayEquals(children, initialization.get(1, TimeUnit.SECONDS));
            verify(registry).addDataSourceListener(any(DBNProjectDatabases.class));
        } finally {
            finishLoading.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
            DBNUtils.disposeNode(node, false);
        }
        verifyListenerRemoved();
    }

    @Test
    public void disposalDuringRegistryLoadingPreventsChildPublication() throws Exception {
        CountDownLatch loading = new CountDownLatch(1);
        CountDownLatch finishLoading = new CountDownLatch(1);
        when(project.getDataSourceRegistry()).thenAnswer(invocation -> {
            loading.countDown();
            awaitLoading(finishLoading);
            return registry;
        });
        DBNProject node = new TestProject();
        var executor = Executors.newFixedThreadPool(2);
        try {
            var initialization = executor.submit(() ->
                assertThrows(DBException.class, () -> node.getChildren(new VoidProgressMonitor())));
            assertTrue(loading.await(1, TimeUnit.SECONDS));
            executor.submit(() -> DBNUtils.disposeNode(node, false)).get(1, TimeUnit.SECONDS);
            finishLoading.countDown();
            initialization.get(1, TimeUnit.SECONDS);
            assertTrue(node.isDisposed());
            assertNull(node.getCachedChildren());
            verifyNoInteractions(registry);
        } finally {
            finishLoading.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
            DBNUtils.disposeNode(node, false);
        }
    }

    @Test
    public void concurrentInitializationRegistersOneListener() throws Exception {
        CountDownLatch loading = new CountDownLatch(1);
        CountDownLatch finishLoading = new CountDownLatch(1);
        AtomicBoolean firstLoad = new AtomicBoolean(true);
        DBNProject node = new TestProject() {
            @Override
            protected void addProjectNodes(@NotNull DBRProgressMonitor monitor, @NotNull List<DBNNode> children)
                throws DBException {
                if (firstLoad.getAndSet(false)) {
                    loading.countDown();
                    awaitLoading(finishLoading);
                }
            }
        };
        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> node.getChildren(new VoidProgressMonitor()));
            assertTrue(loading.await(1, TimeUnit.SECONDS));
            CountDownLatch secondStarted = new CountDownLatch(1);
            var second = executor.submit(() -> {
                secondStarted.countDown();
                return node.getChildren(new VoidProgressMonitor());
            });
            assertTrue(secondStarted.await(1, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> second.get(100, TimeUnit.MILLISECONDS));
            finishLoading.countDown();
            assertSame(first.get(1, TimeUnit.SECONDS)[0], second.get(1, TimeUnit.SECONDS)[0]);
            verify(registry).addDataSourceListener(any(DBNProjectDatabases.class));
        } finally {
            finishLoading.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
            DBNUtils.disposeNode(node, false);
        }
        verifyListenerRemoved();
    }

    @Test
    public void disposalWaitsForInitializationAndRemovesListener() throws Exception {
        CountDownLatch loading = new CountDownLatch(1);
        CountDownLatch finishLoading = new CountDownLatch(1);
        DBNProject node = new TestProject() {
            @Override
            protected void addProjectNodes(@NotNull DBRProgressMonitor monitor, @NotNull List<DBNNode> children)
                throws DBException {
                loading.countDown();
                awaitLoading(finishLoading);
            }
        };
        var executor = Executors.newFixedThreadPool(2);
        try {
            var initialization = executor.submit(() -> node.getChildren(new VoidProgressMonitor()));
            assertTrue(loading.await(1, TimeUnit.SECONDS));
            CountDownLatch disposalStarted = new CountDownLatch(1);
            var disposal = executor.submit(() -> {
                disposalStarted.countDown();
                DBNUtils.disposeNode(node, false);
            });
            assertTrue(disposalStarted.await(1, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> disposal.get(100, TimeUnit.MILLISECONDS));
            finishLoading.countDown();
            initialization.get(1, TimeUnit.SECONDS);
            disposal.get(1, TimeUnit.SECONDS);
            assertTrue(node.isDisposed());
            assertNull(node.getCachedChildren());
            assertTrue(node.needsInitialization());
            assertThrows(DBException.class, () -> node.getChildren(new VoidProgressMonitor()));
        } finally {
            finishLoading.countDown();
            executor.shutdownNow();
            assertTrue(executor.awaitTermination(1, TimeUnit.SECONDS));
            DBNUtils.disposeNode(node, false);
        }
        verifyListenerRemoved();
    }

    @Test
    public void failedChildLoadingRemovesListenerBeforeProjectDisposal() {
        DBException failure = new DBException("Cannot load project resources");
        DBNProject node = new TestProject() {
            @Override
            protected void addProjectNodes(@NotNull DBRProgressMonitor monitor, @NotNull List<DBNNode> children)
                throws DBException {
                throw failure;
            }
        };
        try {
            assertSame(failure, assertThrows(DBException.class, () -> node.getChildren(new VoidProgressMonitor())));
            assertNull(node.getCachedChildren());
            var order = inOrder(registry, project);
            ArgumentCaptor<DBPEventListener> listener = ArgumentCaptor.forClass(DBPEventListener.class);
            order.verify(registry).addDataSourceListener(listener.capture());
            order.verify(registry).removeDataSourceListener(listener.getValue());
            order.verify(project).dispose();
        } finally {
            DBNUtils.disposeNode(node, false);
        }
    }

    @Test
    public void failedDatabaseNodeConstructionRemovesListener() {
        when(registry.getDataSources()).thenThrow(new IllegalStateException("Cannot read data sources"));
        DBNProject node = new TestProject();
        try {
            assertThrows(DBException.class, () -> node.getChildren(new VoidProgressMonitor()));
            verifyListenerRemoved();
            verify(project).dispose();
        } finally {
            DBNUtils.disposeNode(node, false);
        }
    }

    @Test
    public void disposedNodeCannotInitializeChildren() {
        DBNProject node = new TestProject();
        DBNUtils.disposeNode(node, false);
        assertTrue(node.isDisposed());
        assertTrue(node.needsInitialization());
        assertNull(node.getCachedChildren());
        assertThrows(DBException.class, () -> node.getChildren(new VoidProgressMonitor()));
        verifyNoInteractions(registry);
    }

    @Test
    public void reentrantDisposalDoesNotPublishChildren() {
        DBNProject node = new TestProject() {
            @Override
            protected void addProjectNodes(@NotNull DBRProgressMonitor monitor, @NotNull List<DBNNode> children) {
                DBNUtils.disposeNode(this, false);
            }
        };
        assertThrows(DBException.class, () -> node.getChildren(new VoidProgressMonitor()));
        assertTrue(node.isDisposed());
        assertNull(node.getCachedChildren());
        verifyListenerRemoved();
    }

    private void verifyListenerRemoved() {
        ArgumentCaptor<DBPEventListener> listener = ArgumentCaptor.forClass(DBPEventListener.class);
        verify(registry).addDataSourceListener(listener.capture());
        verify(registry).removeDataSourceListener(listener.getValue());
    }

    private static void awaitLoading(@NotNull CountDownLatch latch) throws DBException {
        try {
            if (!latch.await(2, TimeUnit.SECONDS)) {
                throw new DBException("Timed out waiting for test initialization");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DBException("Interrupted test initialization", e);
        }
    }

    private class TestProject extends DBNProject {
        TestProject() {
            super(DBWorkbench.getPlatform().getNavigatorModel().getRoot(), project);
        }
    }
}

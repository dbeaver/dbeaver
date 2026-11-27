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
package org.jkiss.dbeaver.runtime.jobs;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPExclusiveResource;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.impl.SimpleExclusiveLock;
import org.jkiss.dbeaver.model.net.DBWNetworkHandler;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class InvalidateJobTest extends DBeaverUnitTest {
    private static final String TASK_INVALIDATE = "dsInvalidate";

    @Test
    public void acquiresSharedLocksInTheSameOrderFromEitherDataSource() {
        TestDataSource first = createDataSource("project", "a");
        TestDataSource second = createDataSource("project", "b");

        assertLockOrderFromEitherDataSource(first, second);
    }

    @Test
    public void consistentlyOrdersCopiedDataSourceIdsAcrossProjects() {
        TestDataSource first = createDataSource("a", "same-id");
        TestDataSource second = createDataSource("b", "same-id");

        assertLockOrderFromEitherDataSource(first, second);
    }

    @Test
    public void consistentlyOrdersDistinctContainersWithIdenticalIds() {
        TestDataSource first = createDataSource("same-project", "same-id");
        TestDataSource second = createDataSource("same-project", "same-id");

        assertLockOrderFromEitherDataSource(first, second);
    }

    @Test
    public void releasesPreviouslyAcquiredLocksWhenAcquisitionFails() {
        TestDataSource first = createDataSource("project", "a");
        TestDataSource second = createDataSource("project", "b");
        shareNetworkHandler(first, second);
        IllegalStateException failure = new IllegalStateException("Lock acquisition failed");
        doThrow(failure).when(second.lock()).acquireTaskLock(TASK_INVALIDATE, true);

        assertSame(failure, assertThrows(IllegalStateException.class, () ->
            InvalidateJob.invalidateDataSource(monitor, first.dataSource(), false, false, null)));

        verify(first.lock()).releaseTaskLock(TASK_INVALIDATE, Thread.currentThread());
        verify(second.lock(), never()).releaseTaskLock(anyString(), any());
        verify(monitor).done();
    }

    @Test
    public void doesNotInvalidateOrReleaseAlreadyProcessedDataSource() {
        TestDataSource first = createDataSource("project", "a");
        TestDataSource second = createDataSource("project", "b");
        shareNetworkHandler(first, second);
        doReturn(DBPExclusiveResource.TASK_PROCESED).when(first.lock()).acquireTaskLock(TASK_INVALIDATE, true);

        invalidateAndCheckRecoveryOrder(second, List.of(second.container()));

        verify(first.lock(), never()).releaseTaskLock(anyString(), any());
        verify(second.lock()).releaseTaskLock(TASK_INVALIDATE, Thread.currentThread());
    }

    private void assertLockOrderFromEitherDataSource(@NotNull TestDataSource first, @NotNull TestDataSource second) {
        shareNetworkHandler(first, second);
        List<DBPDataSourceContainer> acquisitions = new ArrayList<>();
        for (TestDataSource source : List.of(first, second)) {
            doAnswer(invocation -> {
                acquisitions.add(source.container());
                return invocation.callRealMethod();
            }).when(source.lock()).acquireTaskLock(TASK_INVALIDATE, true);
        }

        // Check the acquisition sequence directly so a regression fails deterministically instead of hanging two threads.
        invalidateAndCheckRecoveryOrder(second, List.of(second.container(), first.container()));
        List<DBPDataSourceContainer> firstOrder = List.copyOf(acquisitions);
        assertEquals(2, firstOrder.size());
        assertEquals(Set.of(first.container(), second.container()), Set.copyOf(firstOrder));
        acquisitions.clear();
        invalidateAndCheckRecoveryOrder(first, List.of(first.container(), second.container()));

        assertEquals(firstOrder, acquisitions);
        verify(first.lock(), Mockito.times(2)).releaseTaskLock(TASK_INVALIDATE, Thread.currentThread());
        verify(second.lock(), Mockito.times(2)).releaseTaskLock(TASK_INVALIDATE, Thread.currentThread());
    }

    private void invalidateAndCheckRecoveryOrder(
        @NotNull TestDataSource source,
        @NotNull List<DBPDataSourceContainer> expectedContainers
    ) {
        InvalidateJob.InvalidationFeedbackHandler feedback = mock(InvalidateJob.InvalidationFeedbackHandler.class);
        when(feedback.confirmInvalidate(any())).thenAnswer(invocation -> {
            Set<DBPDataSourceContainer> containers = invocation.getArgument(0);
            assertEquals(expectedContainers, new ArrayList<>(containers));
            // Stop before network/database I/O, after all recovery locks have been acquired.
            return false;
        });

        assertEquals(List.of(), InvalidateJob.invalidateDataSource(monitor, source.dataSource(), false, false, feedback));
        verify(feedback).confirmInvalidate(new LinkedHashSet<>(expectedContainers));
    }

    private void shareNetworkHandler(@NotNull TestDataSource first, @NotNull TestDataSource second) {
        DBWNetworkHandler handler = mock(DBWNetworkHandler.class);
        when(handler.getDependentDataSources()).thenReturn(new DBPDataSourceContainer[] {first.container(), second.container()});
        when(first.container().getActiveNetworkHandlers()).thenReturn(new DBWNetworkHandler[] {handler});
        when(second.container().getActiveNetworkHandlers()).thenReturn(new DBWNetworkHandler[] {handler});
    }

    @NotNull
    private TestDataSource createDataSource(@NotNull String projectId, @NotNull String id) {
        DBPProject project = mock(DBPProject.class);
        when(project.getId()).thenReturn(projectId);
        DBPDataSourceContainer container = mock(DBPDataSourceContainer.class);
        when(container.getId()).thenReturn(id);
        when(container.getName()).thenReturn(id);
        when(container.getProject()).thenReturn(project);
        DBPDataSource dataSource = mock(DBPDataSource.class);
        when(dataSource.getContainer()).thenReturn(container);
        SimpleExclusiveLock lock = spy(new SimpleExclusiveLock());
        when(container.getExclusiveLock()).thenReturn(lock);
        return new TestDataSource(container, dataSource, lock);
    }

    private record TestDataSource(
        @NotNull DBPDataSourceContainer container,
        @NotNull DBPDataSource dataSource,
        @NotNull SimpleExclusiveLock lock
    ) {
    }
}

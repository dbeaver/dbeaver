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
package org.jkiss.dbeaver.runtime.properties;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.runtime.load.ILoadService;
import org.jkiss.dbeaver.model.runtime.load.ILoadVisualizer;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.DBPPlatformUI;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class PropertySourceLazyLoadTest {
    private final DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class);
    private final DBPDataSource dataSource = Mockito.mock(DBPDataSource.class);
    private final DBSObject object = Mockito.mock(DBSObject.class);
    private final DBPPlatformUI platformUI = Mockito.mock(DBPPlatformUI.class);
    private final List<Load> loads = new ArrayList<>();
    private final PropertySourceEditable source = new PropertySourceEditable(object, object);
    private MockedStatic<DBWorkbench> workbench;

    @BeforeEach
    public void setUp() {
        Mockito.when(object.getDataSource()).thenReturn(dataSource);
        Mockito.when(dataSource.getContainer()).thenReturn(container);
        Mockito.when(container.getDataSource()).thenReturn(dataSource);
        Mockito.when(container.isConnected()).thenReturn(true);
        workbench = Mockito.mockStatic(DBWorkbench.class, Mockito.withSettings().mockMaker("mock-maker-inline"));
        workbench.when(DBWorkbench::getPlatformUI).thenReturn(platformUI);
        workbench.when(DBWorkbench::getPlatform).thenReturn(Mockito.mock(DBPPlatform.class));
        Mockito.when(platformUI.createLoadingService(Mockito.any(), Mockito.any())).thenAnswer(invocation -> {
            Load load = new Load(invocation.getArgument(0), invocation.getArgument(1));
            loads.add(load);
            return load.job;
        });
    }

    @AfterEach
    public void tearDown() {
        workbench.close();
    }

    @Test
    public void doesNotScheduleForDisconnectedConnectingOrReplacedDataSource() {
        addProperty("value");
        Mockito.when(container.isConnected()).thenReturn(false);
        assertNull(source.getPropertyValue(null, "value"));
        Mockito.when(container.isConnected()).thenReturn(true);
        Mockito.when(container.isConnecting()).thenReturn(true);
        assertNull(source.getPropertyValue(null, "value"));
        Mockito.when(container.isConnecting()).thenReturn(false);
        Mockito.when(container.getDataSource()).thenReturn(Mockito.mock(DBPDataSource.class));
        assertNull(source.getPropertyValue(null, "value"));
        assertTrue(loads.isEmpty());
    }

    @Test
    public void delayedReadIsSkippedAndCanBeRetried() throws Exception {
        ObjectPropertyDescriptor property = addProperty("value");
        source.getPropertyValue(null, "value");
        Load load = loads.getFirst();
        assertSame(dataSource, load.service.getFamily());

        Mockito.when(container.isConnected()).thenReturn(false);
        load.complete(load.service.evaluate(new VoidProgressMonitor()));
        Mockito.verify(property, Mockito.never()).readValue(Mockito.any(), Mockito.any(), Mockito.anyBoolean());

        Mockito.when(container.isConnected()).thenReturn(true);
        source.getPropertyValue(null, "value");
        assertEquals(2, loads.size());
    }

    @Test
    public void stopsBetweenPropertiesWhenDisconnectBegins() throws Exception {
        ObjectPropertyDescriptor first = addProperty("first");
        ObjectPropertyDescriptor second = addProperty("second");
        Mockito.when(first.readValue(Mockito.same(object), Mockito.any(), Mockito.eq(true))).thenAnswer(invocation -> {
            Mockito.when(container.isConnected()).thenReturn(false);
            return "first value";
        });
        source.getPropertyValue(null, "first");
        source.getPropertyValue(null, "second");
        Load load = loads.getFirst();
        load.complete(load.service.evaluate(new VoidProgressMonitor()));
        Mockito.verify(second, Mockito.never()).readValue(Mockito.any(), Mockito.any(), Mockito.anyBoolean());
    }

    @Test
    public void staleCompletionDoesNotCacheValuesFromPreviousConnection() throws Exception {
        ObjectPropertyDescriptor property = addProperty("value");
        Mockito.when(property.readValue(Mockito.same(object), Mockito.any(), Mockito.eq(true))).thenReturn("old value");
        source.getPropertyValue(null, "value");
        Load load = loads.getFirst();
        Map<ObjectPropertyDescriptor, Object> result = load.service.evaluate(new VoidProgressMonitor());
        Mockito.when(container.getDataSource()).thenReturn(Mockito.mock(DBPDataSource.class));
        load.complete(result);
        assertTrue(load.visualizer.isCompleted());
        assertNull(source.getPropertyValue(null, "value"));
    }

    @Test
    public void cancellationDuringReadDoesNotReportConnectionFailureOrCacheNull() throws Exception {
        ObjectPropertyDescriptor property = addProperty("value");
        Mockito.when(property.readValue(Mockito.same(object), Mockito.any(), Mockito.eq(true))).thenAnswer(invocation -> {
            Mockito.when(container.isConnected()).thenReturn(false);
            throw new InvocationTargetException(new IllegalStateException("Connection closed"));
        });
        source.getPropertyValue(null, "value");
        Load load = loads.getFirst();
        load.complete(assertDoesNotThrow(() -> load.service.evaluate(new VoidProgressMonitor())));
        Mockito.when(container.isConnected()).thenReturn(true);
        source.getPropertyValue(null, "value");
        assertEquals(2, loads.size());
    }

    @Test
    public void cancelledQueuedJobDoesNotRescheduleAndAllowsRetry() {
        addProperty("value");
        source.getPropertyValue(null, "value");
        Load load = loads.getFirst();
        load.done(Status.CANCEL_STATUS);
        Mockito.verify(load.job).schedule(100);
        source.getPropertyValue(null, "value");
        assertEquals(2, loads.size());
    }

    @Test
    public void successfulNullValueIsCachedAndDuplicateRequestsAreCoalesced() throws Exception {
        addProperty("value");
        source.getPropertyValue(null, "value");
        source.getPropertyValue(null, "value");
        assertEquals(1, loads.size());
        Load load = loads.getFirst();
        Map<ObjectPropertyDescriptor, Object> result = load.service.evaluate(new VoidProgressMonitor());
        assertEquals(1, result.size());
        load.complete(result);
        assertNull(source.getPropertyValue(null, "value"));
        assertEquals(1, loads.size());
    }

    @Test
    public void failedBatchReleasesPendingProperties() throws Exception {
        ObjectPropertyDescriptor first = addProperty("first");
        addProperty("second");
        Mockito.when(first.readValue(Mockito.same(object), Mockito.any(), Mockito.eq(true)))
            .thenThrow(new InvocationTargetException(new IllegalStateException("Read failed")));
        source.getPropertyValue(null, "first");
        source.getPropertyValue(null, "second");
        Load load = loads.getFirst();
        assertThrows(InvocationTargetException.class, () -> load.service.evaluate(new VoidProgressMonitor()));
        load.done(Status.OK_STATUS);
        load.visualizer.completeLoading(null);

        source.getPropertyValue(null, "first");
        source.getPropertyValue(null, "second");
        assertEquals(2, loads.size());
        Mockito.when(first.readValue(Mockito.same(object), Mockito.any(), Mockito.eq(true))).thenReturn("value");
        Load retry = loads.getLast();
        Map<ObjectPropertyDescriptor, Object> result = retry.service.evaluate(new VoidProgressMonitor());
        assertEquals(2, result.size());
        retry.complete(result);
    }

    @Test
    public void nonDatabasePropertiesStillLoad() throws Exception {
        Object editable = new Object();
        PropertySourceEditable nonDatabaseSource = new PropertySourceEditable(editable, editable);
        ObjectPropertyDescriptor property = Mockito.mock(ObjectPropertyDescriptor.class);
        Mockito.when(property.getId()).thenReturn("value");
        Mockito.when(property.isLazy(editable, true)).thenReturn(true);
        Mockito.when(property.readValue(Mockito.same(editable), Mockito.any(), Mockito.eq(true))).thenReturn("value");
        nonDatabaseSource.addProperty(property);
        nonDatabaseSource.getPropertyValue(null, "value");
        Load load = loads.getFirst();
        assertSame(editable, load.service.getFamily());
        load.complete(load.service.evaluate(new VoidProgressMonitor()));
        assertEquals("value", nonDatabaseSource.getPropertyValue(null, "value"));
    }

    @NotNull
    private ObjectPropertyDescriptor addProperty(@NotNull String id) {
        ObjectPropertyDescriptor property = Mockito.mock(ObjectPropertyDescriptor.class);
        Mockito.when(property.getId()).thenReturn(id);
        Mockito.when(property.isLazy(object, true)).thenReturn(true);
        source.addProperty(property);
        return property;
    }

    private static class Load {
        private final Job job = Mockito.mock(Job.class, Mockito.withSettings().mockMaker("mock-maker-inline"));
        private final ILoadService<Map<ObjectPropertyDescriptor, Object>> service;
        private final ILoadVisualizer<Map<ObjectPropertyDescriptor, Object>> visualizer;
        private JobChangeAdapter listener;

        private Load(
            @NotNull ILoadService<Map<ObjectPropertyDescriptor, Object>> service,
            @NotNull ILoadVisualizer<Map<ObjectPropertyDescriptor, Object>> visualizer
        ) {
            this.service = service;
            this.visualizer = visualizer;
            Mockito.doAnswer(invocation -> {
                listener = invocation.getArgument(0);
                return null;
            }).when(job).addJobChangeListener(Mockito.any());
        }

        private void done(@NotNull IStatus status) {
            IJobChangeEvent event = Mockito.mock(IJobChangeEvent.class);
            Mockito.when(event.getResult()).thenReturn(status);
            listener.done(event);
        }

        private void complete(@NotNull Map<ObjectPropertyDescriptor, Object> result) {
            done(Status.OK_STATUS);
            visualizer.completeLoading(result);
        }
    }
}

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
package org.jkiss.dbeaver.registry;

import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPDataSourceConfigurationStorage;
import org.jkiss.dbeaver.model.app.DBPApplication;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class DataSourceDisconnectJobsTest {
    @Test
    public void cancelsFamilyJobsBeforeShutdownWithoutCancellingCurrentJob() throws Exception {
        DBPPlatform platform = Mockito.mock(DBPPlatform.class);
        DBPApplication application = Mockito.mock(DBPApplication.class);
        DBPPreferenceStore preferences = Mockito.mock(DBPPreferenceStore.class);
        Mockito.when(platform.getApplication()).thenReturn(application);
        Mockito.when(platform.getPreferenceStore()).thenReturn(preferences);
        Mockito.when(application.isDetachedProcess()).thenReturn(true);

        DBPDataSourceRegistry registry = Mockito.mock(DBPDataSourceRegistry.class);
        Mockito.when(registry.getPreferenceStore()).thenReturn(preferences);
        DBPDriver driver = Mockito.mock(DBPDriver.class);
        Mockito.when(driver.getDefaultConnectionProperties()).thenReturn(Map.of());
        Mockito.when(driver.getFullId()).thenReturn("test-driver");
        DBPDataSource dataSource = Mockito.mock(DBPDataSource.class);
        Mockito.when(dataSource.getAvailableInstances()).thenReturn(List.of());

        Job queued = Mockito.mock(Job.class, Mockito.withSettings().mockMaker("mock-maker-inline"));
        Job running = Mockito.mock(Job.class, Mockito.withSettings().mockMaker("mock-maker-inline"));
        Job current = Mockito.mock(Job.class, Mockito.withSettings().mockMaker("mock-maker-inline"));
        Mockito.when(running.getState()).thenReturn(Job.RUNNING, Job.NONE);
        IJobManager manager = Mockito.mock(IJobManager.class);
        Mockito.when(manager.find(dataSource)).thenReturn(new Job[]{queued, running, current});
        Mockito.when(manager.currentJob()).thenReturn(current);

        try (
            var workbench = Mockito.mockStatic(DBWorkbench.class, Mockito.withSettings().mockMaker("mock-maker-inline"));
            var jobs = Mockito.mockStatic(Job.class, Mockito.withSettings().mockMaker("mock-maker-inline"));
            var providers = Mockito.mockStatic(DataSourceProviderRegistry.class, Mockito.withSettings().mockMaker("mock-maker-inline"))
        ) {
            workbench.when(DBWorkbench::getPlatform).thenReturn(platform);
            jobs.when(Job::getJobManager).thenReturn(manager);
            providers.when(DataSourceProviderRegistry::getInstance).thenReturn(Mockito.mock(DataSourceProviderRegistry.class));
            DataSourceDescriptor container = new DataSourceDescriptor(
                registry, Mockito.mock(DBPDataSourceConfigurationStorage.class), DataSourceOriginLocal.INSTANCE,
                "test-datasource", driver, new DBPConnectionConfiguration()
            ) {
                @NotNull
                @Override
                protected DBPDataSource openConnectionDetached(@NotNull DBRProgressMonitor monitor, boolean detachedProcess) {
                    return dataSource;
                }
            };
            VoidProgressMonitor monitor = new VoidProgressMonitor();
            assertTrue(container.connect(monitor, false, false));
            assertTrue(container.getTasks().isEmpty());
            assertTrue(container.disconnect(monitor));

            var order = Mockito.inOrder(queued, running, dataSource);
            order.verify(queued).cancel();
            order.verify(running).cancel();
            order.verify(running, Mockito.times(2)).getState();
            order.verify(dataSource).shutdown(monitor);
            Mockito.verify(current, Mockito.never()).cancel();
            assertFalse(container.isConnected());
        }
    }
}

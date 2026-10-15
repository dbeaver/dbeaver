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
package org.jkiss.dbeaver.model.impl.app;

import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class BaseProjectImplTest {
    @Test
    public void projectPropertiesAccessibleDuringRegistryInitialization() throws Exception {
        DBPWorkspace workspace = Mockito.mock(DBPWorkspace.class);
        BaseProjectImpl project = Mockito.mock(BaseProjectImpl.class,
            Mockito.withSettings().useConstructor(workspace, null).defaultAnswer(Mockito.CALLS_REAL_METHODS));
        project.setInMemory(true);
        project.setProjectProperty("password-management", true);

        DBPDataSourceRegistry registry = Mockito.mock(DBPDataSourceRegistry.class);
        Mockito.doReturn(registry).when(project).createDataSourceRegistry();

        ExecutorService uiThread = Executors.newSingleThreadExecutor();
        try {
            Mockito.doAnswer(invocation -> {
                Assertions.assertEquals(true,
                    uiThread.submit(() -> project.getProjectProperty("password-management")).get(2, TimeUnit.SECONDS));
                return null;
            }).when(registry).initializeDataSources();

            Assertions.assertSame(registry, project.getDataSourceRegistry());
        } finally {
            uiThread.shutdownNow();
        }
    }
}

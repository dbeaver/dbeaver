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
package org.jkiss.dbeaver.ext.h2;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.h2.model.H2DataSource;
import org.jkiss.dbeaver.ext.h2.util.H2InitConfirmation;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPApplicationWorkbench;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ServiceRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.Properties;

@Isolated
public class H2ConnectionValidationTest {
    private final DBPApplicationWorkbench workbench = Mockito.mock(DBPApplicationWorkbench.class, Mockito.RETURNS_DEEP_STUBS);
    private final ServiceRegistry services = Mockito.mock(ServiceRegistry.class);
    private final H2InitConfirmation confirmation = Mockito.mock(H2InitConfirmation.class);
    private final DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class);
    private final TestDataSource dataSource = Mockito.mock(TestDataSource.class, Mockito.CALLS_REAL_METHODS);
    private final VoidProgressMonitor monitor = new VoidProgressMonitor();
    private Field workbenchField;
    private Field registryField;
    private Object originalWorkbench;
    private Object originalRegistry;

    @BeforeEach
    public void setUp() throws Exception {
        workbenchField = DBWorkbench.class.getDeclaredField("applicationWorkbench");
        workbenchField.setAccessible(true);
        registryField = ServiceRegistry.class.getDeclaredField("instance");
        registryField.setAccessible(true);
        originalWorkbench = workbenchField.get(null);
        originalRegistry = registryField.get(null);
        workbenchField.set(null, workbench);
        registryField.set(null, services);
        Mockito.doReturn(container).when(dataSource).getContainer();
        Mockito.when(services.getService(H2InitConfirmation.class)).thenReturn(confirmation);
    }

    @AfterEach
    public void tearDown() throws Exception {
        registryField.set(null, originalRegistry);
        workbenchField.set(null, originalWorkbench);
    }

    @Test
    public void connectsWithoutInitWithoutRequestingApproval() throws DBCException {
        dataSource.validateConnectionParameters(monitor, "jdbc:h2:mem:test;MODE=PostgreSQL", new Properties());

        Mockito.verifyNoInteractions(services, confirmation);
    }

    @Test
    public void rejectsInitWhenServiceIsUnavailable() {
        Mockito.when(services.getService(H2InitConfirmation.class)).thenReturn(null);

        Assertions.assertThrows(DBCException.class, () ->
            dataSource.validateConnectionParameters(monitor, "jdbc:h2:mem:test;INIT=CALL 1", new Properties()));
    }

    @Test
    public void rejectsInitWhenUserDeclines() {
        Assertions.assertThrows(DBCException.class, () ->
            dataSource.validateConnectionParameters(monitor, "jdbc:h2:mem:test;INIT=CALL 1", new Properties()));

        Mockito.verify(confirmation).confirmInitScript(container, "CALL 1");
    }

    @Test
    public void rejectsInitInMultiuserApplicationEvenWithApprovingService() {
        Mockito.when(workbench.getPlatform().getApplication().isMultiuser()).thenReturn(true);
        Mockito.when(confirmation.confirmInitScript(Mockito.any(), Mockito.anyString())).thenReturn(true);

        Assertions.assertThrows(DBCException.class, () ->
            dataSource.validateConnectionParameters(monitor, "jdbc:h2:mem:test;INIT=CALL 1", new Properties()));

        Mockito.verifyNoInteractions(services, confirmation);
    }

    @Test
    public void acceptsInitFromHeadlessServiceAndAsksAgainWhenScriptChanges() throws DBCException {
        Mockito.when(workbench.getPlatform().getApplication().isHeadlessMode()).thenReturn(true);
        Mockito.when(confirmation.confirmInitScript(Mockito.any(), Mockito.anyString())).thenReturn(true);
        Properties properties = new Properties();
        properties.setProperty("INIT", "CALL 1");

        dataSource.validateConnectionParameters(monitor, "jdbc:h2:mem:test", properties);
        dataSource.validateConnectionParameters(monitor, "jdbc:h2:mem:test", properties);
        properties.setProperty("INIT", "CALL 2");
        dataSource.validateConnectionParameters(monitor, "jdbc:h2:mem:test", properties);

        Mockito.verify(confirmation).confirmInitScript(container, "CALL 1");
        Mockito.verify(confirmation).confirmInitScript(container, "CALL 2");
        Mockito.verifyNoMoreInteractions(confirmation);
    }

    public abstract static class TestDataSource extends H2DataSource {
        public TestDataSource(@NotNull DBRProgressMonitor monitor, @NotNull DBPDataSourceContainer container) throws DBException {
            super(monitor, container);
        }

        @Override
        public void validateConnectionParameters(
            @NotNull DBRProgressMonitor monitor,
            @Nullable String url,
            @NotNull Properties connectProps
        ) throws DBCException {
            super.validateConnectionParameters(monitor, url, connectProps);
        }
    }
}

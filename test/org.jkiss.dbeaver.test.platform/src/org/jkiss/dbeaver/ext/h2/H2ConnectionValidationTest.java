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
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ServiceRegistry;
import org.jkiss.dbeaver.runtime.ui.UIServiceConnectionInit;
import org.jkiss.junit.StaticFieldOverride;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.Mockito;

import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

@Isolated
public class H2ConnectionValidationTest {
    private final DBPPlatform platform = Mockito.mock(DBPPlatform.class, Mockito.RETURNS_DEEP_STUBS);
    private final ServiceRegistry services = Mockito.mock(ServiceRegistry.class);
    private final UIServiceConnectionInit confirmation = Mockito.mock(UIServiceConnectionInit.class);
    private final DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class);
    private final TestDataSource dataSource = Mockito.mock(TestDataSource.class, Mockito.CALLS_REAL_METHODS);
    private final VoidProgressMonitor monitor = new VoidProgressMonitor();
    private final DBPConnectionConfiguration configuration = new DBPConnectionConfiguration();
    private StaticFieldOverride workbench;
    private StaticFieldOverride registry;

    @BeforeEach
    public void setUp() throws ReflectiveOperationException {
        workbench = new StaticFieldOverride(DBWorkbench.class, "platform", platform);
        registry = new StaticFieldOverride(ServiceRegistry.class, "instance", services);
        Mockito.doReturn(container).when(dataSource).getContainer();
        Mockito.when(container.getActualConnectionConfiguration()).thenReturn(configuration);
        Mockito.when(services.getService(UIServiceConnectionInit.class)).thenReturn(confirmation);
    }

    @AfterEach
    public void tearDown() throws IllegalAccessException {
        registry.close();
        workbench.close();
    }

    @Test
    public void connectsWithoutInitWithoutRequestingApproval() throws DBCException {
        dataSource.validateConnectionParameters(monitor, "jdbc:h2:mem:test;MODE=PostgreSQL", new Properties());

        Mockito.verifyNoInteractions(services, confirmation);
    }

    @Test
    public void rejectsInitWhenServiceIsUnavailable() {
        Mockito.when(services.getService(UIServiceConnectionInit.class)).thenReturn(null);

        Assertions.assertThrows(DBCException.class, () ->
            dataSource.validateConnectionParameters(monitor, "jdbc:h2:mem:test;INIT=CALL 1", new Properties()));
    }

    @Test
    public void rejectsInitWhenUserDeclines() {
        Assertions.assertThrows(DBCException.class, () ->
            dataSource.validateConnectionParameters(monitor, "jdbc:h2:mem:test;INIT=CALL 1", new Properties()));

        Mockito.verify(confirmation).confirmInitScript(container, "jdbc:h2:mem:test;INIT=CALL 1", "CALL 1");
    }

    @Test
    public void rejectsInitInMultiuserApplicationEvenWithApprovingService() {
        Mockito.when(platform.getApplication().isMultiuser()).thenReturn(true);
        Mockito.when(confirmation.confirmInitScript(Mockito.any(), Mockito.anyString(), Mockito.anyString())).thenReturn(true);

        Assertions.assertThrows(DBCException.class, () ->
            dataSource.validateConnectionParameters(monitor, "jdbc:h2:mem:test;INIT=CALL 1", new Properties()));

        Mockito.verifyNoInteractions(services, confirmation);
    }

    @Test
    public void acceptsInitFromHeadlessServiceAndAsksAgainWhenScriptChanges() throws DBCException {
        Mockito.when(platform.getApplication().isHeadlessMode()).thenReturn(true);
        Mockito.when(confirmation.confirmInitScript(Mockito.any(), Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        Properties properties = new Properties();
        properties.setProperty("INIT", "CALL 1");

        dataSource.validateConnectionParameters(monitor, "jdbc:h2:mem:test", properties);
        dataSource.validateConnectionParameters(monitor, "jdbc:h2:mem:test", properties);
        properties.setProperty("INIT", "CALL 2");
        dataSource.validateConnectionParameters(monitor, "jdbc:h2:mem:test", properties);

        Mockito.verify(confirmation).confirmInitScript(container, "jdbc:h2:mem:test", "CALL 1");
        Mockito.verify(confirmation).confirmInitScript(container, "jdbc:h2:mem:test", "CALL 2");
        Mockito.verifyNoMoreInteractions(confirmation);
    }

    @Test
    public void rejectsBootstrapFileWriteWithoutInit() {
        String script = "CALL FILE_WRITE(STRINGTOUTF8('marker'), '${project.path}/marker.txt')";
        configuration.getBootstrap().getInitQueries().add(script);
        Mockito.when(container.getVariablesResolver(true)).thenReturn(name -> "/tmp/h2-bootstrap-test");

        Assertions.assertThrows(DBCException.class, () ->
            dataSource.validateConnectionParameters(monitor, "jdbc:h2:mem:test", new Properties()));

        Mockito.verify(confirmation).confirmInitScripts(container, "jdbc:h2:mem:test",
            List.of("CALL FILE_WRITE(STRINGTOUTF8('marker'), '/tmp/h2-bootstrap-test/marker.txt')"));
    }

    @Test
    public void rejectsBootstrapWithoutServiceAndInMultiuserMode() {
        configuration.getBootstrap().getInitQueries().add("CALL 1");
        Mockito.when(services.getService(UIServiceConnectionInit.class)).thenReturn(null);
        Assertions.assertThrows(DBCException.class, () ->
            dataSource.validateConnectionParameters(monitor, "jdbc:h2:mem:test", new Properties()));

        Mockito.when(services.getService(UIServiceConnectionInit.class)).thenReturn(confirmation);
        Mockito.when(confirmation.confirmInitScript(Mockito.any(), Mockito.anyString(), Mockito.anyString())).thenReturn(true);
        Mockito.when(platform.getApplication().isMultiuser()).thenReturn(true);
        Assertions.assertThrows(DBCException.class, () ->
            dataSource.validateConnectionParameters(monitor, "jdbc:h2:mem:test", new Properties()));
        Mockito.verifyNoInteractions(confirmation);
    }

    @Test
    public void confirmsAllBootstrapQueriesAndRechecksChangedVariables() throws DBCException {
        AtomicReference<String> value = new AtomicReference<>("1");
        configuration.getBootstrap().getInitQueries().add("CALL ${value}");
        configuration.getBootstrap().getInitQueries().add("CALL 2");
        Mockito.when(container.getVariablesResolver(true)).thenReturn(name -> value.get());
        Mockito.when(confirmation.confirmInitScripts(Mockito.any(), Mockito.anyString(), Mockito.anyList())).thenReturn(true);

        dataSource.validateConnectionParameters(monitor, "jdbc:h2:mem:test", new Properties());
        dataSource.validateConnectionParameters(monitor, "jdbc:h2:mem:test", new Properties());
        value.set("3");
        dataSource.validateConnectionParameters(monitor, "jdbc:h2:mem:test", new Properties());
        configuration.getBootstrap().getInitQueries().add("CALL 4");
        dataSource.validateConnectionParameters(monitor, "jdbc:h2:mem:test", new Properties());

        Mockito.verify(confirmation).confirmInitScripts(container, "jdbc:h2:mem:test", List.of("CALL 1", "CALL 2"));
        Mockito.verify(confirmation).confirmInitScripts(container, "jdbc:h2:mem:test", List.of("CALL 3", "CALL 2"));
        Mockito.verify(confirmation).confirmInitScripts(container, "jdbc:h2:mem:test", List.of("CALL 3", "CALL 2", "CALL 4"));
        Mockito.verifyNoMoreInteractions(confirmation);
    }

    @Test
    public void checksInitAndBootstrapBeforeAcceptingConnection() {
        configuration.getBootstrap().getInitQueries().add("CALL 2");
        Mockito.when(confirmation.confirmInitScript(container, "jdbc:h2:mem:test;INIT=CALL 1", "CALL 1")).thenReturn(true);

        Assertions.assertThrows(DBCException.class, () ->
            dataSource.validateConnectionParameters(monitor, "jdbc:h2:mem:test;INIT=CALL 1", new Properties()));

        Mockito.verify(confirmation).confirmInitScript(container, "jdbc:h2:mem:test;INIT=CALL 1", "CALL 1");
        Mockito.verify(confirmation).confirmInitScripts(container, "jdbc:h2:mem:test;INIT=CALL 1", List.of("CALL 2"));
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

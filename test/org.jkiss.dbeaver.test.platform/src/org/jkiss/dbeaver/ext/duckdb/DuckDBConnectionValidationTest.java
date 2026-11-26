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
package org.jkiss.dbeaver.ext.duckdb;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.duckdb.model.DuckDBDataSource;
import org.jkiss.dbeaver.ext.duckdb.model.DuckDBExecutionContext;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
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

@Isolated
public class DuckDBConnectionValidationTest {
    private static final String URL = "jdbc:duckdb:";
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
        configuration.setUrl(URL);
    }

    @AfterEach
    public void tearDown() throws IllegalAccessException {
        registry.close();
        workbench.close();
    }

    @Test
    public void connectsWithoutExtensionsOrBootstrapWithoutApproval() throws DBCException {
        dataSource.validateConnectionParameters(monitor, URL, new Properties());
        Mockito.verifyNoInteractions(services, confirmation);
    }

    @Test
    public void rejectsNativeExtensionsBeforeBootstrapWhenUserDeclines() {
        configuration.getBootstrap().getInitQueries().add("LOAD 'test.duckdb_extension'");
        configuration.getBootstrap().setIgnoreErrors(true);
        Properties properties = new Properties();
        properties.setProperty("allow_unsigned_extensions", "true");
        properties.setProperty("allow_extensions_metadata_mismatch", "true");

        Assertions.assertThrows(DBCException.class, () -> dataSource.validateConnectionParameters(monitor, URL, properties));

        Mockito.verify(confirmation).confirmNativeExtensions(container, URL,
            List.of("allow_extensions_metadata_mismatch=true", "allow_unsigned_extensions=true"));
        Mockito.verifyNoMoreInteractions(confirmation);
    }

    @Test
    public void requiresNativeApprovalWithoutBootstrapAndWhenServiceIsMissing() {
        String url = URL + ";allow_unsigned_extensions=true";
        Mockito.when(services.getService(UIServiceConnectionInit.class)).thenReturn(null);
        Assertions.assertThrows(DBCException.class, () -> dataSource.validateConnectionParameters(monitor, url, new Properties()));
    }

    @Test
    public void rejectsNativeExtensionsInHeadlessAndMultiuserModesEvenWithApprovingService() {
        Mockito.when(confirmation.confirmNativeExtensions(Mockito.any(), Mockito.anyString(), Mockito.anyList())).thenReturn(true);
        String url = URL + ";allow_unsigned_extensions=1";
        Mockito.when(platform.getApplication().isHeadlessMode()).thenReturn(true);
        Assertions.assertThrows(DBCException.class, () -> dataSource.validateConnectionParameters(monitor, url, new Properties()));
        Mockito.when(platform.getApplication().isHeadlessMode()).thenReturn(false);
        Mockito.when(platform.getApplication().isMultiuser()).thenReturn(true);
        Assertions.assertThrows(DBCException.class, () -> dataSource.validateConnectionParameters(monitor, url, new Properties()));
        Mockito.verifyNoInteractions(services, confirmation);
    }

    @Test
    public void nativeApprovalIsSeparateFromAutomaticSqlApproval() {
        UIServiceConnectionInit automaticSqlApproval = (source, url, script) -> true;
        Assertions.assertTrue(automaticSqlApproval.confirmInitScripts(container, URL, List.of("SELECT 1")));
        Assertions.assertFalse(automaticSqlApproval.confirmNativeExtensions(container, URL, List.of("allow_unsigned_extensions=true")));
    }

    @Test
    public void acceptsBootstrapWithNativeApprovalWithoutRequestingSqlApproval() throws DBCException {
        Mockito.when(confirmation.confirmNativeExtensions(Mockito.any(), Mockito.anyString(), Mockito.anyList())).thenReturn(true);
        configuration.getBootstrap().getInitQueries().add("LOAD 'test.duckdb_extension'");
        String url = URL + ";allow_unsigned_extensions=true";

        dataSource.validateConnectionParameters(monitor, url, new Properties());

        Mockito.verify(confirmation).confirmNativeExtensions(container, url, List.of("allow_unsigned_extensions=true"));
        Mockito.verifyNoMoreInteractions(confirmation);
    }

    @Test
    public void ordinaryBootstrapDoesNotRequestApprovalBeforeConnectionOrExecution() throws DBException {
        configuration.getBootstrap().getInitQueries().add("SELECT 1");
        dataSource.validateConnectionParameters(monitor, URL, new Properties());

        TestExecutionContext context = Mockito.mock(TestExecutionContext.class, Mockito.CALLS_REAL_METHODS);
        context.validateBootstrapQueries(List.of("SELECT 1"));

        Mockito.verifyNoInteractions(services, confirmation);
    }

    @Test
    public void ordinaryBootstrapDoesNotRequireServiceInHeadlessOrMultiuserModes() throws DBCException {
        configuration.getBootstrap().getInitQueries().add("SELECT 1");
        Mockito.when(services.getService(UIServiceConnectionInit.class)).thenReturn(null);
        Mockito.when(platform.getApplication().isHeadlessMode()).thenReturn(true);
        dataSource.validateConnectionParameters(monitor, URL, new Properties());
        Mockito.when(platform.getApplication().isHeadlessMode()).thenReturn(false);
        Mockito.when(platform.getApplication().isMultiuser()).thenReturn(true);
        dataSource.validateConnectionParameters(monitor, URL, new Properties());

        Mockito.verifyNoInteractions(services, confirmation);
    }

    public abstract static class TestDataSource extends DuckDBDataSource {
        public TestDataSource(@NotNull DBRProgressMonitor monitor, @NotNull DBPDataSourceContainer container) throws DBException {
            super(monitor, container, Mockito.mock(GenericMetaModel.class));
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

    public static class TestExecutionContext extends DuckDBExecutionContext {
        public TestExecutionContext(@NotNull JDBCRemoteInstance instance, @NotNull String purpose) {
            super(instance, purpose);
        }

        @Override
        public void validateBootstrapQueries(@NotNull List<String> queries) throws DBException {
            super.validateBootstrapQueries(queries);
        }
    }
}

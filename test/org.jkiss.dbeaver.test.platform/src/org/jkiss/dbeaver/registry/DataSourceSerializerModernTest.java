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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.app.DBPApplication;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.connection.DBPConnectionType;
import org.jkiss.dbeaver.model.connection.DBPDataSourceProviderRegistry;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.nio.charset.StandardCharsets;

public class DataSourceSerializerModernTest {
    private static final String CONNECTION_TYPE_ID = "imported-type";
    private static final String PROJECT_CONFIGURATION = """
        {
          "connection-types": {
            "imported-type": {
              "name": "Imported type",
              "description": "Imported from project",
              "color": "10,20,30",
              "colorDark": "40,50,60",
              "auto-commit": true,
              "confirm-execute": true,
              "confirm-data-change": true,
              "smart-commit": true,
              "smart-commit-recover": true,
              "auto-close-transactions": true,
              "close-transactions-period": 10,
              "auto-close-connections": true,
              "close-connections-period": 20
            }
          },
          "connections": {}
        }
        """;

    @Test
    public void testStandaloneProjectImportsUnknownConnectionType() throws Exception {
        DBPDataSourceProviderRegistry providerRegistry = parseProjectConfiguration(false, false);

        ArgumentCaptor<DBPConnectionType> connectionTypeCaptor = ArgumentCaptor.forClass(DBPConnectionType.class);
        Mockito.verify(providerRegistry).getConnectionType(CONNECTION_TYPE_ID, null);
        Mockito.verify(providerRegistry).addConnectionType(connectionTypeCaptor.capture());

        DBPConnectionType connectionType = connectionTypeCaptor.getValue();
        Assertions.assertEquals(CONNECTION_TYPE_ID, connectionType.getId());
        Assertions.assertEquals("Imported type", connectionType.getName());
        Assertions.assertEquals("Imported from project", connectionType.getDescription());
        Assertions.assertEquals("10,20,30", connectionType.getColorLight());
        Assertions.assertEquals("40,50,60", connectionType.getColorDark());
        Assertions.assertTrue(connectionType.isAutocommit());
        Assertions.assertTrue(connectionType.isConfirmExecute());
        Assertions.assertTrue(connectionType.isConfirmDataChange());
        Assertions.assertTrue(connectionType.isSmartCommit());
        Assertions.assertTrue(connectionType.isSmartCommitRecover());
        Assertions.assertTrue(connectionType.isAutoCloseTransactions());
        Assertions.assertEquals(10, connectionType.getCloseIdleTransactionPeriod());
        Assertions.assertTrue(connectionType.isAutoCloseConnections());
        Assertions.assertEquals(20, connectionType.getCloseIdleConnectionPeriod());
    }

    @ParameterizedTest
    @CsvSource({"true, false", "false, true"})
    public void testProjectConnectionTypesAreIgnoredOutsideStandaloneMode(boolean multiuser, boolean distributed)
        throws Exception {
        DBPDataSourceProviderRegistry providerRegistry = parseProjectConfiguration(multiuser, distributed);

        Mockito.verify(providerRegistry, Mockito.never()).getConnectionType(CONNECTION_TYPE_ID, null);
        Mockito.verify(providerRegistry, Mockito.never()).addConnectionType(Mockito.any());
    }

    @SuppressWarnings("unchecked")
    private static DBPDataSourceProviderRegistry parseProjectConfiguration(boolean multiuser, boolean distributed)
        throws Exception {
        DBPDataSourceProviderRegistry providerRegistry = Mockito.mock(DBPDataSourceProviderRegistry.class);
        DBPApplication application = Mockito.mock(DBPApplication.class);
        Mockito.when(application.isMultiuser()).thenReturn(multiuser);
        Mockito.when(application.isDistributed()).thenReturn(distributed);
        DBPProject project = Mockito.mock(DBPProject.class);
        DataSourceRegistry<DataSourceDescriptor> registry = Mockito.mock(DataSourceRegistry.class);
        Mockito.when(registry.getProject()).thenReturn(project);
        new TestSerializer(registry, application, providerRegistry).parseDataSources(
            new DataSourceMemoryStorage(PROJECT_CONFIGURATION.getBytes(StandardCharsets.UTF_8)),
            new DataSourceConfigurationManagerBuffer(),
            new DataSourceParseResults(),
            null
        );
        return providerRegistry;
    }

    private static final class TestSerializer extends DataSourceSerializerModern<DataSourceDescriptor> {
        private final DBPApplication application;
        private final DBPDataSourceProviderRegistry providerRegistry;

        private TestSerializer(
            DataSourceRegistry<DataSourceDescriptor> registry,
            DBPApplication application,
            DBPDataSourceProviderRegistry providerRegistry
        ) {
            super(registry);
            this.application = application;
            this.providerRegistry = providerRegistry;
        }

        @NotNull
        @Override
        protected DBPApplication getApplication() {
            return application;
        }

        @NotNull
        @Override
        protected DBPDataSourceProviderRegistry getDataSourceProviderRegistry() {
            return providerRegistry;
        }
    }
}

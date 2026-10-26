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

import org.jkiss.dbeaver.model.DBPDataSourceConfigurationStorage;
import org.jkiss.dbeaver.model.DBPDataSourceOrigin;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriver;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

class DataSourceDescriptorFilterTest {
    private DataSourceDescriptor dataSource;

    @BeforeEach
    void setUp() {
        DBPDataSourceRegistry registry = Mockito.mock(DBPDataSourceRegistry.class);
        DBPDataSourceConfigurationStorage storage = Mockito.mock(DBPDataSourceConfigurationStorage.class);
        DBPDriver driver = Mockito.mock(DBPDriver.class);
        Mockito.when(registry.getPreferenceStore()).thenReturn(Mockito.mock(DBPPreferenceStore.class));
        Mockito.when(storage.isDefault()).thenReturn(true);

        dataSource = new DataSourceDescriptor(
            registry,
            storage,
            Mockito.mock(DBPDataSourceOrigin.class),
            "test",
            driver,
            new DBPConnectionConfiguration()
        );
    }

    @Test
    void connectionAndUserFiltersAreStoredSeparately() {
        DBSObjectFilter connectionFilter = new DBSObjectFilter();
        connectionFilter.setInclude(List.of("prod_%"));
        DBSObjectFilter userFilter = new DBSObjectFilter();
        userFilter.setInclude(List.of("%_eu"));

        dataSource.setObjectFilter(String.class, null, connectionFilter);
        dataSource.setUserObjectFilter(String.class, null, userFilter);

        Assertions.assertSame(connectionFilter, dataSource.getConnectionObjectFilter(String.class, null, true));
        Assertions.assertSame(userFilter, dataSource.getUserObjectFilter(String.class, null, true));
        Assertions.assertSame(connectionFilter, dataSource.getObjectFilter(String.class, null, false));

        dataSource.setUserObjectFilter(String.class, null, null);

        Assertions.assertSame(connectionFilter, dataSource.getObjectFilter(String.class, null, false));
        Assertions.assertFalse(dataSource.hasUserObjectFilters());
    }

    @Test
    void firstMatchPreservesDisabledFilterForEditing() {
        DBSObjectFilter connectionFilter = new DBSObjectFilter();
        connectionFilter.setInclude(List.of("prod_%"));
        connectionFilter.setEnabled(false);
        dataSource.setObjectFilter(String.class, null, connectionFilter);

        Assertions.assertSame(connectionFilter, dataSource.getObjectFilter(String.class, null, true));
        Assertions.assertNull(dataSource.getObjectFilter(String.class, null, false));
    }

    @Test
    void userSerializerDoesNotIncludeConnectionFilters() throws Exception {
        DBSObjectFilter connectionFilter = new DBSObjectFilter();
        connectionFilter.setInclude(List.of("connection_%"));
        DBSObjectFilter userFilter = new DBSObjectFilter();
        userFilter.setInclude(List.of("user_%"));
        dataSource.setObjectFilter(String.class, null, connectionFilter);
        dataSource.setUserObjectFilter(String.class, null, userFilter);

        String serialized = new FilterSerializer<DataSourceDescriptor>().serializeCustomUserFilters(dataSource);

        Assertions.assertTrue(serialized.contains("user_%"));
        Assertions.assertFalse(serialized.contains("connection_%"));

        dataSource.setUserObjectFilter(String.class, null, null);
        Assertions.assertEquals("[]", new FilterSerializer<DataSourceDescriptor>().serializeCustomUserFilters(dataSource));
    }

    @Test
    void copyPreservesConnectionAndUserFilters() {
        DBSObjectFilter connectionFilter = new DBSObjectFilter();
        connectionFilter.setInclude(List.of("prod_%"));
        DBSObjectFilter userFilter = new DBSObjectFilter();
        userFilter.setInclude(List.of("%_eu"));
        dataSource.setObjectFilter(String.class, null, connectionFilter);
        dataSource.setUserObjectFilter(String.class, null, userFilter);

        DataSourceDescriptor copy = new DataSourceDescriptor(dataSource, dataSource.getRegistry(), false);

        Assertions.assertEquals(connectionFilter, copy.getConnectionObjectFilter(String.class, null, true));
        Assertions.assertEquals(userFilter, copy.getUserObjectFilter(String.class, null, true));
        Assertions.assertNotSame(connectionFilter, copy.getConnectionObjectFilter(String.class, null, true));
        Assertions.assertNotSame(userFilter, copy.getUserObjectFilter(String.class, null, true));
        Assertions.assertEquals(connectionFilter, copy.getObjectFilter(String.class, null, false));
    }

    @Test
    void copyFromPreservesUserFiltersOnlyForTheSameConnection() {
        DBSObjectFilter userFilter = new DBSObjectFilter();
        userFilter.setInclude(List.of("%_eu"));
        dataSource.setUserObjectFilter(String.class, null, userFilter);

        DataSourceDescriptor sameConnection = new DataSourceDescriptor(dataSource, dataSource.getRegistry(), false);
        sameConnection.clearUserObjectFilters();
        sameConnection.copyFrom(dataSource);
        Assertions.assertEquals(userFilter, sameConnection.getUserObjectFilter(String.class, null, true));

        DataSourceDescriptor differentConnection = new DataSourceDescriptor(dataSource, dataSource.getRegistry(), false);
        differentConnection.setId("copy");
        differentConnection.copyFrom(dataSource);
        Assertions.assertNull(differentConnection.getUserObjectFilter(String.class, null, true));
    }
}

/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.connection.DBPConnectionConfiguration;
import org.jkiss.dbeaver.model.connection.DBPDriverSubstitution;
import org.jkiss.dbeaver.model.connection.DBPDriverSubstitutionDescriptor;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.sql.Driver;
import java.sql.SQLException;

import static org.junit.Assert.assertThrows;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class PostgreDataSourceTest extends DBeaverUnitTest {

    public final String serverVersion = "PG Test";

    private final String activeDatabaseName = "active-db";

    @Mock
    private DBRProgressMonitor monitor;

    @Mock
    private JDBCExecutionContext executionContext;

    @Mock
    private Driver driverInstance;

    @Mock
    private JDBCRemoteInstance instance;

    private PostgreDataSource dataSource;

    @Before
    public void setUp() throws Exception {
        //driver config
        var mockDriverSubstitution = mock(DBPDriverSubstitution.class);
        var mockDriverSubstitutionDescriptor = mock(DBPDriverSubstitutionDescriptor.class);
        when(mockDriverSubstitutionDescriptor.getInstance()).thenReturn(mockDriverSubstitution);
        when(mockDriverSubstitution.getSubstitutingDriverInstance(any())).thenReturn(driverInstance);

        //connection configuration config
        var connectionConfiguration = new DBPConnectionConfiguration();
        connectionConfiguration.setDatabaseName(activeDatabaseName);

        //container config
        var mockDataSourceContainer = mock(DBPDataSourceContainer.class, RETURNS_DEEP_STUBS);
        when(mockDataSourceContainer.getDriver()).thenReturn(
            DBWorkbench.getPlatform().getDataSourceProviderRegistry().findDriver("postgresql"));
        when(mockDataSourceContainer.getDriverSubstitution()).thenReturn(mockDriverSubstitutionDescriptor);
        when(mockDataSourceContainer.getActualConnectionConfiguration()).thenReturn(connectionConfiguration);
        when(mockDataSourceContainer.getConnectionConfiguration()).thenReturn(connectionConfiguration);
        when(mockDataSourceContainer.getDataSource()).thenReturn(dataSource);

        //exec context
        when(executionContext.getOwnerInstance()).thenReturn(instance);

        //test subject creation
        dataSource = new PostgreDataSource(mockDataSourceContainer, serverVersion, activeDatabaseName);
    }

    @Test
    public void shouldUseInstanceDbInUrlWhenActiveDbEqualsInstanceDb() throws Exception {
        //given
        when(instance.getName()).thenReturn(activeDatabaseName);
        var expectedUrl = "jdbc:postgresql://{host}/" + activeDatabaseName;
        //when
        assertThrows(DBCException.class, () -> dataSource.openConnection(monitor, executionContext, activeDatabaseName));
        //then
        verify(driverInstance).connect(eq(expectedUrl), any());
    }

    @Test
    public void shouldUseInstanceDbInUrlWhenActiveDbDiffersFromActiveDb() throws SQLException {
        //given
        String otherDbName = "other-db";
        when(instance.getName()).thenReturn(otherDbName);
        var expectedUrl = "jdbc:postgresql://{host}/" + otherDbName;
        //when
        assertThrows(DBCException.class, () -> dataSource.openConnection(monitor, executionContext, activeDatabaseName));
        //then
        verify(driverInstance).connect(eq(expectedUrl), any());
    }

}

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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.dbeaver.ext.postgresql.model.impls.PostgreServerPostgreSQL;
import org.jkiss.dbeaver.ext.postgresql.model.impls.redshift.PostgreServerRedshift;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.List;

public class PostgreExecutionContextTest extends DBeaverUnitTest {
    @Mock
    private PostgreDataSource dataSource;
    @Mock
    private PostgreDatabase database;
    @Mock
    private PostgreSchema schema;
    @Mock
    private JDBCSession session;
    @Mock
    private JDBCPreparedStatement pathStatement;
    @Mock
    private JDBCPreparedStatement defaultsStatement;
    @Mock
    private JDBCResultSet pathResults;
    @Mock
    private JDBCResultSet defaultsResults;

    private PostgreExecutionContext context;

    @BeforeEach
    public void setUp() throws Exception {
        Mockito.when(database.getDataSource()).thenReturn(dataSource);
        DBPDataSourceContainer container = configureTestContainer("postgresql");
        Mockito.when(dataSource.getContainer()).thenReturn(container);
        Mockito.when(database.getSchemas(monitor)).thenReturn(List.of(schema));
        Mockito.when(database.getSchema(monitor, "public")).thenReturn(schema);
        Mockito.when(database.getSchema(42L)).thenReturn(schema);
        Mockito.when(schema.getName()).thenReturn("public");
        Mockito.when(schema.getObjectId()).thenReturn(42L);
        Mockito.when(session.prepareStatement(ArgumentMatchers.anyString())).thenReturn(pathStatement);
        Mockito.when(session.prepareStatement("SELECT current_schema(),session_user")).thenReturn(defaultsStatement);
        Mockito.when(pathStatement.executeQuery()).thenReturn(pathResults);
        Mockito.when(pathResults.next()).thenReturn(true, false);
        Mockito.when(pathResults.getString(1)).thenReturn("\"$user\", public");
        Mockito.when(defaultsStatement.executeQuery()).thenReturn(defaultsResults);
        Mockito.when(defaultsResults.next()).thenReturn(true, false);
        Mockito.when(defaultsResults.getString(1)).thenReturn("public");
        Mockito.when(dataSource.getSQLDialect()).thenReturn(new PostgreDialect());
        context = Mockito.spy(new PostgreExecutionContext(database, "Context defaults test"));
        Mockito.doReturn(session).when(context).openSession(
            ArgumentMatchers.eq(monitor), ArgumentMatchers.eq(DBCExecutionPurpose.META), ArgumentMatchers.anyString());
    }

    @Test
    public void redshiftReadsDefaultsWithoutResetVal() throws Exception {
        Mockito.when(dataSource.getServerType()).thenReturn(new PostgreServerRedshift(dataSource));

        Assertions.assertTrue(context.refreshDefaults(monitor, false));

        Mockito.verify(session).prepareStatement("SHOW search_path");
        Mockito.verify(session, Mockito.never()).prepareStatement(ArgumentMatchers.contains("reset_val"));
        Assertions.assertEquals(List.of("public", "$user"), context.computeSearchPath());
        Assertions.assertSame(schema, context.getDefaultSchema());
    }

    @Test
    public void postgresqlReadsConfiguredDefaultSearchPath() throws Exception {
        Mockito.when(dataSource.getServerType()).thenReturn(new PostgreServerPostgreSQL(dataSource));

        context.refreshDefaults(monitor, false);

        Mockito.verify(session).prepareStatement("SELECT reset_val FROM pg_settings WHERE name = 'search_path'");
        Assertions.assertEquals(List.of("public", "$user"), context.computeSearchPath());
    }
}

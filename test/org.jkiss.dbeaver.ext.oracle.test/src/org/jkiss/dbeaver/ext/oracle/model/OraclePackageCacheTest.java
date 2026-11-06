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
package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.sql.ResultSet;
import java.sql.Timestamp;

public class OraclePackageCacheTest extends DBeaverUnitTest {

    @Mock
    private DBRProgressMonitor monitor;

    private OracleDataSource dataSource;
    private OracleSchema schema;

    @BeforeEach
    public void setUp() throws DBException {
        DBPDataSourceContainer container = configureTestContainer("oracle");
        dataSource = new OracleDataSource(container);
        schema = new OracleSchema(dataSource, -1, "TEST_SCHEMA");
    }

    @Test
    public void packageListQueryDoesNotLoadPackageBodies() throws Exception {
        JDBCSession session = Mockito.mock(JDBCSession.class);
        JDBCPreparedStatement statement = Mockito.mock(JDBCPreparedStatement.class);
        Mockito.when(session.getProgressMonitor()).thenReturn(monitor);
        Mockito.when(session.prepareStatement(Mockito.anyString())).thenReturn(statement);

        schema.getPackageCache().prepareLookupStatement(session, schema, null, null);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        Mockito.verify(session).prepareStatement(sql.capture());
        Assertions.assertFalse(sql.getValue().contains("LEFT JOIN"));
        Assertions.assertFalse(sql.getValue().contains("BODY_LAST_DDL_TIME"));
    }

    @Test
    public void packageBodyDdlTimeIsLoadedOnDemandAndCached() throws Exception {
        ResultSet packageRow = Mockito.mock(ResultSet.class);
        Mockito.when(packageRow.getString("OBJECT_NAME")).thenReturn("TEST_PACKAGE");
        Mockito.when(packageRow.getString("STATUS")).thenReturn("VALID");

        OraclePackage pack = new OraclePackage(schema, packageRow);
        Mockito.verify(packageRow, Mockito.never()).getTimestamp("BODY_LAST_DDL_TIME");

        JDBCSession session = Mockito.mock(JDBCSession.class);
        JDBCPreparedStatement statement = Mockito.mock(JDBCPreparedStatement.class);
        JDBCResultSet bodyResult = Mockito.mock(JDBCResultSet.class);
        Timestamp bodyLastDDLTime = Timestamp.valueOf("2026-11-05 12:00:00");
        Mockito.when(session.prepareStatement(Mockito.anyString())).thenReturn(statement);
        Mockito.when(statement.executeQuery()).thenReturn(bodyResult);
        Mockito.when(bodyResult.next()).thenReturn(true);
        Mockito.when(bodyResult.getTimestamp("BODY_LAST_DDL_TIME")).thenReturn(bodyLastDDLTime);

        try (MockedStatic<DBUtils> dbUtils = Mockito.mockStatic(
            DBUtils.class, Mockito.withSettings().mockMaker("mock-maker-inline"))
        ) {
            dbUtils.when(() -> DBUtils.openMetaSession(monitor, pack, "Load package body DDL time"))
                .thenReturn(session);

            Assertions.assertEquals(bodyLastDDLTime, pack.getBodyLastDDLTime(monitor));
            Assertions.assertEquals(bodyLastDDLTime, pack.getBodyLastDDLTime(monitor));
        }

        Mockito.verify(session, Mockito.times(1)).prepareStatement(Mockito.anyString());
        Mockito.verify(bodyResult, Mockito.times(1)).next();
    }
}

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
package org.jkiss.dbeaver.ext.postgresql;

import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCColumnMetaData;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSTypedObject;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.sql.Types;

public class PostgreUtilsTest extends DBeaverUnitTest {
    @Mock
    private DBCSession session;
    @Mock
    private DBRProgressMonitor monitor;
    @Mock
    private DBCExecutionContext context;
    @Mock
    private PostgreDataSource dataSource;
    @Mock
    private PostgreDatabase database;
    @Mock
    private JDBCColumnMetaData column;
    @Mock
    private DBSTypedObject otherType;

    @Test
    public void unresolvedResultColumnRowTypeDoesNotLogOrRetrySameDatabase() throws Exception {
        Mockito.when(session.getProgressMonitor()).thenReturn(monitor);
        Mockito.when(session.getExecutionContext()).thenReturn(context);
        Mockito.when(context.getOwnerInstance()).thenReturn(database);
        Mockito.when(column.getCatalogName()).thenReturn("postgres");
        Mockito.when(column.getTypeName()).thenReturn("view6");
        Mockito.when(column.getTypeID()).thenReturn(Types.STRUCT);
        Mockito.when(dataSource.getDatabase("postgres")).thenReturn(database);

        Assertions.assertNull(PostgreUtils.findDataType(session, dataSource, column));
        Mockito.verify(database).getDataType(monitor, "view6", false);
        Mockito.verify(database, Mockito.never()).getDataType(monitor, "view6", true);
    }

    @Test
    public void nonResultColumnTypeIsVerified() throws Exception {
        Mockito.when(session.getProgressMonitor()).thenReturn(monitor);
        Mockito.when(session.getExecutionContext()).thenReturn(context);
        Mockito.when(context.getOwnerInstance()).thenReturn(database);
        Mockito.when(otherType.getTypeName()).thenReturn("test_type");

        Assertions.assertNull(PostgreUtils.findDataType(session, dataSource, otherType));
        Mockito.verify(database).getDataType(monitor, "test_type", true);
    }
}

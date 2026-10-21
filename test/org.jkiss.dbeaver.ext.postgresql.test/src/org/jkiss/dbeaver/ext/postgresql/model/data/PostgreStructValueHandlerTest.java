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
package org.jkiss.dbeaver.ext.postgresql.model.data;

import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataSource;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDataType;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.exec.JDBCColumnMetaData;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.sql.Types;

public class PostgreStructValueHandlerTest extends DBeaverUnitTest {
    @Mock
    private DBCSession session;
    @Mock
    private PostgreDataSource dataSource;
    @Mock
    private PostgreDataType resolvedType;
    @Mock
    private PostgreDatabase database;
    @Mock
    private DBCExecutionContext executionContext;
    @Mock
    private JDBCColumnMetaData column;

    @Test
    public void unresolvedTemporaryRowTypeDoesNotParseFallbackVarchar() throws Exception {
        Mockito.when(session.getDataSource()).thenReturn(dataSource);
        Mockito.when(resolvedType.getTypeID()).thenReturn(Types.VARCHAR);

        String rowValue = "(1,example)";
        Assertions.assertSame(rowValue, PostgreStructValueHandler.INSTANCE.getValueFromObject(
            session, resolvedType, rowValue, false, false));
    }

    @Test
    public void unresolvedTemporaryRowTypeIsLookedUpOnlyOncePerResultColumn() throws Exception {
        Mockito.when(session.getDataSource()).thenReturn(dataSource);
        Mockito.when(session.getExecutionContext()).thenReturn(executionContext);
        Mockito.when(column.getCatalogName()).thenReturn("postgres");
        Mockito.when(column.getTypeName()).thenReturn("view6");
        Mockito.when(column.getTypeID()).thenReturn(Types.STRUCT);
        Mockito.when(dataSource.getDatabase("postgres")).thenReturn(database);

        String rowValue = "(1,AC/DC)";
        Assertions.assertSame(rowValue, PostgreStructValueHandler.INSTANCE.getValueFromObject(
            session, column, rowValue, false, false));
        Assertions.assertSame(rowValue, PostgreStructValueHandler.INSTANCE.getValueFromObject(
            session, column, rowValue, false, false));
        Mockito.verify(database).getDataType(Mockito.any(), Mockito.eq("view6"), Mockito.eq(false));
    }
}

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
package org.jkiss.dbeaver.ext.firebird.model;

import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class FireBirdExceptionTest extends DBeaverUnitTest {

    private FireBirdDataSource dataSource;

    @BeforeEach
    public void setUp() {
        SQLDialect dialect = Mockito.mock(SQLDialect.class);
        Mockito.when(dialect.getQuotedIdentifier(Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyBoolean()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(dialect.getQuotedString(Mockito.anyString()))
            .thenAnswer(invocation -> "'" + invocation.<String>getArgument(0).replace("'", "''") + "'");
        dataSource = Mockito.mock(FireBirdDataSource.class);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(dialect);
    }

    @Test
    public void generatesCreateExceptionWithComment() {
        FireBirdException exception = new FireBirdException(
            dataSource, "E_NOT_FOUND", 1, "Record 'x' not found", "SYSDBA", "Raised by lookups", false);

        Assertions.assertEquals(
            "CREATE EXCEPTION E_NOT_FOUND 'Record ''x'' not found';\n\n"
                + "COMMENT ON EXCEPTION E_NOT_FOUND IS 'Raised by lookups';",
            exception.getObjectDefinitionText(Mockito.mock(DBRProgressMonitor.class), DBPScriptObject.EMPTY_OPTIONS));
    }

    @Test
    public void generatesCreateExceptionWithoutComment() {
        FireBirdException exception = new FireBirdException(dataSource, "E_EMPTY", 2, null, null, null, false);

        Assertions.assertEquals(
            "CREATE EXCEPTION E_EMPTY '';",
            exception.getObjectDefinitionText(Mockito.mock(DBRProgressMonitor.class), DBPScriptObject.EMPTY_OPTIONS));
    }

    @Test
    public void readsExceptionFromResultSet() throws Exception {
        JDBCResultSet resultSet = Mockito.mock(JDBCResultSet.class);
        Mockito.when(resultSet.getString("RDB$EXCEPTION_NAME")).thenReturn("E_NOT_FOUND   ");
        Mockito.when(resultSet.getInt("RDB$EXCEPTION_NUMBER")).thenReturn(7);
        Mockito.when(resultSet.getString("RDB$MESSAGE")).thenReturn("Not found");
        Mockito.when(resultSet.getString("RDB$OWNER_NAME")).thenReturn("SYSDBA   ");
        Mockito.when(resultSet.getInt("RDB$SYSTEM_FLAG")).thenReturn(0);

        FireBirdException exception = new FireBirdExceptionCache()
            .fetchObject(Mockito.mock(JDBCSession.class), dataSource, resultSet);

        Assertions.assertNotNull(exception);
        Assertions.assertEquals("E_NOT_FOUND", exception.getName());
        Assertions.assertEquals(7, exception.getNumber());
        Assertions.assertEquals("Not found", exception.getMessage());
        Assertions.assertEquals("SYSDBA", exception.getOwnerName());
        Assertions.assertFalse(exception.isSystem());
    }
}

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
package org.jkiss.dbeaver.ext.db2;

import org.jkiss.dbeaver.model.exec.jdbc.JDBCCallableStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.sql.SQLException;
import java.sql.Types;

public class DB2UtilsTest extends DBeaverUnitTest {
    private static final String PROCEDURE = "CALL SYSPROC.DB2LK_GENERATE_DDL(?,?)";
    private static final String COMMAND = "-noview -e -x -xd -td ; -t SCHEMA.TABLE";
    private static final String FALLBACK_COMMAND = "-noview -e -td ; -t SCHEMA.TABLE";

    @Mock
    private JDBCSession session;
    @Mock
    private JDBCCallableStatement firstCall;
    @Mock
    private JDBCCallableStatement fallbackCall;

    @BeforeEach
    public void setUp() throws SQLException {
        Mockito.when(session.prepareCall(PROCEDURE)).thenReturn(firstCall, fallbackCall);
    }

    @Test
    public void buildsBothCommandsWithoutChangingOtherOptions() {
        Assertions.assertEquals(COMMAND, DB2Utils.buildDDLCommand(false, true, ";", "SCHEMA.TABLE"));
        Assertions.assertEquals(FALLBACK_COMMAND, DB2Utils.buildDDLCommand(false, false, ";", "SCHEMA.TABLE"));
        Assertions.assertEquals("-e -td \r\n -t SCHEMA.TABLE",
            DB2Utils.buildDDLCommand(true, false, "\r\n", "SCHEMA.TABLE"));
    }

    @Test
    public void successfulFirstCallKeepsAuthorizationOptionsAndHasNoWarning() throws SQLException {
        Mockito.when(firstCall.getInt(2)).thenReturn(12);

        DB2Utils.DDLGenerationResult result = DB2Utils.generateDDL(session, COMMAND, FALLBACK_COMMAND);

        Assertions.assertEquals(12, result.token());
        Assertions.assertFalse(result.fallbackUsed());
        Assertions.assertEquals("CREATE TABLE T (ID INT);", DB2Utils.formatDDLResult("CREATE TABLE T (ID INT);", result.fallbackUsed()));
        Mockito.verify(firstCall).setString(1, COMMAND);
        Mockito.verify(firstCall).registerOutParameter(2, Types.INTEGER);
        Mockito.verify(session).prepareCall(PROCEDURE);
    }

    @Test
    public void invalidInputRetriesWithoutAuthorizationAndMarksDDL() throws SQLException {
        Mockito.when(firstCall.executeUpdate()).thenThrow(new SQLException("Invalid input", "38553", -443));
        Mockito.when(fallbackCall.getInt(2)).thenReturn(42);

        DB2Utils.DDLGenerationResult result = DB2Utils.generateDDL(session, COMMAND, FALLBACK_COMMAND);

        Assertions.assertEquals(42, result.token());
        Assertions.assertTrue(result.fallbackUsed());
        Assertions.assertEquals(
            DB2Messages.ddl_authorization_statements_missing + "\n\nCREATE TABLE T (ID INT);",
            DB2Utils.formatDDLResult("CREATE TABLE T (ID INT);", result.fallbackUsed()));
        Mockito.verify(firstCall).close();
        Mockito.verify(fallbackCall).setString(1, FALLBACK_COMMAND);
        Mockito.verify(session, Mockito.times(2)).prepareCall(PROCEDURE);
    }

    @Test
    public void otherErrorsAreNotRetried() throws SQLException {
        SQLException original = new SQLException("Access denied", "42501", -551);
        Mockito.when(firstCall.executeUpdate()).thenThrow(original);

        Assertions.assertSame(original, Assertions.assertThrows(SQLException.class,
            () -> DB2Utils.generateDDL(session, COMMAND, FALLBACK_COMMAND)));
        Mockito.verify(session).prepareCall(PROCEDURE);
    }

    @Test
    public void otherRoutineErrorsWithSameSqlStateAreNotRetried() throws SQLException {
        SQLException original = new SQLException("Invalid input", "38553", -999);
        Mockito.when(firstCall.executeUpdate()).thenThrow(original);

        Assertions.assertSame(original, Assertions.assertThrows(SQLException.class,
            () -> DB2Utils.generateDDL(session, COMMAND, FALLBACK_COMMAND)));
        Mockito.verify(session).prepareCall(PROCEDURE);
    }

    @Test
    public void failedFallbackPreservesBothErrors() throws SQLException {
        SQLException original = new SQLException("Invalid input", "38553", -443);
        SQLException fallback = new SQLException("Other failure", "58000", -1);
        Mockito.when(firstCall.executeUpdate()).thenThrow(original);
        Mockito.when(fallbackCall.executeUpdate()).thenThrow(fallback);

        SQLException thrown = Assertions.assertThrows(SQLException.class,
            () -> DB2Utils.generateDDL(session, COMMAND, FALLBACK_COMMAND));

        Assertions.assertSame(original, thrown);
        Assertions.assertArrayEquals(new Throwable[] { fallback }, thrown.getSuppressed());
        Mockito.verify(session, Mockito.times(2)).prepareCall(PROCEDURE);
    }
}

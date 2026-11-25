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

public class FireBirdPackageTest extends DBeaverUnitTest {

    private static final String HEADER = "BEGIN\n  FUNCTION ADD_ONE(X INTEGER) RETURNS INTEGER;\nEND";
    private static final String BODY = "BEGIN\n  FUNCTION ADD_ONE(X INTEGER) RETURNS INTEGER AS BEGIN RETURN X + 1; END\nEND";

    private FireBirdDataSource dataSource;
    private DBRProgressMonitor monitor;

    @BeforeEach
    public void setUp() {
        SQLDialect dialect = Mockito.mock(SQLDialect.class);
        Mockito.when(dialect.getQuotedIdentifier(Mockito.anyString(), Mockito.anyBoolean(), Mockito.anyBoolean()))
            .thenAnswer(invocation -> invocation.getArgument(0));
        Mockito.when(dialect.getQuotedString(Mockito.anyString()))
            .thenAnswer(invocation -> "'" + invocation.<String>getArgument(0).replace("'", "''") + "'");
        dataSource = Mockito.mock(FireBirdDataSource.class);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(dialect);
        monitor = Mockito.mock(DBRProgressMonitor.class);
    }

    @Test
    public void generatesHeaderBodyAndComment() {
        FireBirdPackage pkg = new FireBirdPackage(
            dataSource, "PKG_MATH", "SYSDBA", HEADER, BODY, true, true, "Math helpers", false);

        Assertions.assertEquals(
            "SET TERM ^ ;\n\n"
                + "CREATE OR ALTER PACKAGE PKG_MATH\n"
                + "SQL SECURITY DEFINER\n"
                + "AS\n" + HEADER + "^\n\n"
                + "RECREATE PACKAGE BODY PKG_MATH\n"
                + "AS\n" + BODY + "^\n\n"
                + "SET TERM ; ^\n\n"
                + "COMMENT ON PACKAGE PKG_MATH IS 'Math helpers';",
            pkg.getObjectDefinitionText(monitor, DBPScriptObject.EMPTY_OPTIONS));
    }

    @Test
    public void generatesHeaderOnlyWithoutBody() {
        FireBirdPackage pkg = new FireBirdPackage(dataSource, "PKG_MATH", "SYSDBA", HEADER, null, false, null, null, false);

        Assertions.assertEquals(
            "SET TERM ^ ;\n\n"
                + "CREATE OR ALTER PACKAGE PKG_MATH\n"
                + "AS\n" + HEADER + "^\n\n"
                + "SET TERM ; ^",
            pkg.getObjectDefinitionText(monitor, DBPScriptObject.EMPTY_OPTIONS));
        Assertions.assertNull(pkg.getSqlSecurity());
    }

    @Test
    public void readsPackageFromResultSet() throws Exception {
        JDBCResultSet resultSet = Mockito.mock(JDBCResultSet.class);
        Mockito.when(resultSet.getString("RDB$PACKAGE_NAME")).thenReturn("PKG_MATH   ");
        Mockito.when(resultSet.getString("RDB$OWNER_NAME")).thenReturn("SYSDBA   ");
        Mockito.when(resultSet.getString("RDB$PACKAGE_HEADER_SOURCE")).thenReturn(HEADER);
        Mockito.when(resultSet.getString("RDB$PACKAGE_BODY_SOURCE")).thenReturn(BODY);
        Mockito.when(resultSet.getInt("RDB$VALID_BODY_FLAG")).thenReturn(1);
        Mockito.when(resultSet.getObject("RDB$SQL_SECURITY")).thenReturn(Boolean.FALSE);
        Mockito.when(resultSet.getBoolean("RDB$SQL_SECURITY")).thenReturn(false);
        Mockito.when(resultSet.getInt("RDB$SYSTEM_FLAG")).thenReturn(0);

        FireBirdPackage pkg = new FireBirdPackageCache().fetchObject(Mockito.mock(JDBCSession.class), dataSource, resultSet);

        Assertions.assertNotNull(pkg);
        Assertions.assertEquals("PKG_MATH", pkg.getName());
        Assertions.assertEquals("SYSDBA", pkg.getOwnerName());
        Assertions.assertTrue(pkg.isValidBody());
        Assertions.assertEquals("INVOKER", pkg.getSqlSecurity());
        Assertions.assertFalse(pkg.isSystem());
    }

    @Test
    public void readsRoutineFromResultSet() throws Exception {
        FireBirdPackage pkg = new FireBirdPackage(dataSource, "PKG_MATH", null, HEADER, BODY, true, null, null, false);
        JDBCResultSet resultSet = Mockito.mock(JDBCResultSet.class);
        Mockito.when(resultSet.getString("ROUTINE_NAME")).thenReturn("HELPER   ");
        Mockito.when(resultSet.getInt("IS_FUNCTION")).thenReturn(1);
        Mockito.when(resultSet.getInt("RDB$PRIVATE_FLAG")).thenReturn(1);

        FireBirdPackageRoutine routine = new FireBirdPackage.RoutineCache()
            .fetchObject(Mockito.mock(JDBCSession.class), pkg, resultSet);

        Assertions.assertNotNull(routine);
        Assertions.assertEquals("HELPER", routine.getName());
        Assertions.assertTrue(routine.isFunction());
        Assertions.assertTrue(routine.isPrivate());
    }
}

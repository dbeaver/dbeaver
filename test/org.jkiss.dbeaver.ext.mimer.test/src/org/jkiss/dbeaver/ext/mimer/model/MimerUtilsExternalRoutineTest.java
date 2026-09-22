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
package org.jkiss.dbeaver.ext.mimer.model;

import org.jkiss.dbeaver.ext.generic.model.GenericProcedure;
import org.jkiss.dbeaver.ext.generic.model.GenericProcedureParameter;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureParameterKind;
import org.jkiss.dbeaver.model.struct.rdb.DBSProcedureType;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link MimerUtils#readExternalRoutineInfo}/{@link MimerUtils#buildExternalRoutineSource} - the
 * Mimer SQL 11.1+ {@code LANGUAGE CLR EXTERNAL NAME '...' IN "library"} reconstruction, covering
 * the two worked examples from the original feature request ({@code AddOne}/{@code ISJSON}).
 * <p>
 * {@code readExternalRoutineInfo} opens its own {@code JDBCSession} internally (there's no
 * pre-existing session to inject), so exercising it means mocking the whole {@code DBUtils.
 * openMetaSession} chain - {@code dataSource.getDefaultInstance().getDefaultContext(...).
 * openSession(...)} - the same shape {@code GreenplumTableTest} already established as this
 * repo's working pattern for this.
 *
 * @author Mimer Information Technology
 */
public class MimerUtilsExternalRoutineTest extends DBeaverUnitTest {

    private static final String SCHEMA = "mimer_store";
    private static final String SPECIFIC_NAME = "SYS_SPECIFIC_1";

    @Mock
    private GenericProcedure procedure;

    @Mock
    private MimerDataSource dataSource;

    @Mock
    private JDBCRemoteInstance instance;

    @Mock
    private JDBCExecutionContext executionContext;

    @Mock
    private JDBCSession session;

    @Mock
    private JDBCPreparedStatement statement;

    @Mock
    private JDBCResultSet resultSet;

    @BeforeEach
    public void setUp() {
        when(procedure.getDataSource()).thenReturn(dataSource);
    }

    /**
     * The short-circuit in {@link MimerUtils#readExternalRoutineInfo} for a pre-11.1 server -
     * must never touch the database at all (the {@code ROUTINES.EXTERNAL_*} columns don't exist
     * there, so even preparing the statement would be wrong).
     */
    @Test
    public void notExternalWhenServerDoesNotSupportExternalLibraries() throws Exception {
        when(dataSource.supportsExternalLibraries()).thenReturn(false);

        MimerExternalRoutineInfo info = MimerUtils.readExternalRoutineInfo(monitor, procedure, SCHEMA, SPECIFIC_NAME);

        Assertions.assertSame(MimerExternalRoutineInfo.NOT_EXTERNAL, info);
        Assertions.assertNull(MimerUtils.buildExternalRoutineSource(monitor, procedure, SCHEMA, "ISJSON", SPECIFIC_NAME));
        verify(dataSource, never()).getDefaultInstance();
    }

    @Test
    public void notExternalWhenNoMatchingRoutineRow() throws Exception {
        mockDatabaseChain();
        when(resultSet.next()).thenReturn(false);

        MimerExternalRoutineInfo info = MimerUtils.readExternalRoutineInfo(monitor, procedure, SCHEMA, SPECIFIC_NAME);

        Assertions.assertSame(MimerExternalRoutineInfo.NOT_EXTERNAL, info);
    }

    @Test
    public void notExternalForAPlainSqlBodiedRoutine() throws Exception {
        mockDatabaseChain();
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("ROUTINE_BODY")).thenReturn("SQL");

        MimerExternalRoutineInfo info = MimerUtils.readExternalRoutineInfo(monitor, procedure, SCHEMA, SPECIFIC_NAME);

        Assertions.assertFalse(info.external());
        Assertions.assertNull(MimerUtils.buildExternalRoutineSource(monitor, procedure, SCHEMA, "SomeProc", SPECIFIC_NAME));
    }

    /**
     * {@code CREATE PROCEDURE AddOne(IN parameter1 INT, OUT parameter2 INT) LANGUAGE CLR
     * EXTERNAL NAME 'UserLib.UserClass1.AddOne' IN NETLIB} - the procedure example from the
     * original feature request.
     */
    @Test
    public void buildsCreateProcedureForAnExternalClrRoutine() throws Exception {
        mockDatabaseChain();
        mockExternalRoutineRow("CLR", "UserLib.UserClass1.AddOne", "NETLIB");
        when(procedure.getProcedureType()).thenReturn(DBSProcedureType.PROCEDURE);
        // Built as a separate statement, not inline inside thenReturn(...) - mockParameter's own
        // when(...).thenReturn(...) calls must fully complete before this method's own
        // when(procedure.getParameters(monitor)) stub is closed off; Mockito's stubbing state
        // isn't reentrant, so nesting them inside one expression throws UnfinishedStubbingException.
        List<GenericProcedureParameter> params = List.of(
            mockParameter(DBSProcedureParameterKind.IN, "parameter1", "INTEGER"),
            mockParameter(DBSProcedureParameterKind.OUT, "parameter2", "INTEGER"));
        when(procedure.getParameters(monitor)).thenReturn(params);

        String ddl = MimerUtils.buildExternalRoutineSource(monitor, procedure, SCHEMA, "AddOne", SPECIFIC_NAME);

        Assertions.assertEquals(
            "CREATE PROCEDURE \"mimer_store\".\"AddOne\"(IN \"parameter1\" INTEGER, OUT \"parameter2\" INTEGER)\n" +
            "LANGUAGE CLR\n" +
            "EXTERNAL NAME 'UserLib.UserClass1.AddOne' IN \"NETLIB\"",
            ddl);
    }

    /**
     * {@code CREATE FUNCTION ISJSON(IN EXPRESSION NVARCHAR(5000)) RETURNS BOOLEAN LANGUAGE CLR
     * EXTERNAL NAME 'Mimer.Extensions.JSONLib.ISJSON' IN MIMER_DOTNET_EXTENSIONS} - the function
     * example from the original feature request. A function parameter never carries an IN/OUT
     * prefix in the rebuilt DDL (a function parameter's mode is always implicitly IN).
     */
    @Test
    public void buildsCreateFunctionWithReturnTypeForAnExternalClrRoutine() throws Exception {
        mockDatabaseChain();
        mockExternalRoutineRow("CLR", "Mimer.Extensions.JSONLib.ISJSON", "MIMER_DOTNET_EXTENSIONS");
        when(procedure.getProcedureType()).thenReturn(DBSProcedureType.FUNCTION);
        // See the comment in buildsCreateProcedureForAnExternalClrRoutine - built as a separate
        // statement so mockParameter's own stubbing isn't nested inside this method's thenReturn(...).
        List<GenericProcedureParameter> params = List.of(
            mockParameter(DBSProcedureParameterKind.IN, "EXPRESSION", "NVARCHAR(5000)"),
            mockParameter(DBSProcedureParameterKind.RETURN, "RETURN", "BOOLEAN"));
        when(procedure.getParameters(monitor)).thenReturn(params);

        String ddl = MimerUtils.buildExternalRoutineSource(monitor, procedure, SCHEMA, "ISJSON", SPECIFIC_NAME);

        Assertions.assertEquals(
            "CREATE FUNCTION \"mimer_store\".\"ISJSON\"(\"EXPRESSION\" NVARCHAR(5000))\n" +
            "RETURNS BOOLEAN\n" +
            "LANGUAGE CLR\n" +
            "EXTERNAL NAME 'Mimer.Extensions.JSONLib.ISJSON' IN \"MIMER_DOTNET_EXTENSIONS\"",
            ddl);
    }

    @Test
    public void omitsTheInClauseWhenNoLibraryIsReported() throws Exception {
        mockDatabaseChain();
        mockExternalRoutineRow("CLR", "UserLib.UserClass1.AddOne", null);
        when(procedure.getProcedureType()).thenReturn(DBSProcedureType.PROCEDURE);
        when(procedure.getParameters(monitor)).thenReturn(List.of());

        String ddl = MimerUtils.buildExternalRoutineSource(monitor, procedure, SCHEMA, "AddOne", SPECIFIC_NAME);

        Assertions.assertEquals(
            "CREATE PROCEDURE \"mimer_store\".\"AddOne\"()\n" +
            "LANGUAGE CLR\n" +
            "EXTERNAL NAME 'UserLib.UserClass1.AddOne'",
            ddl);
    }

    @Test
    public void escapesASingleQuoteInTheExternalName() throws Exception {
        mockDatabaseChain();
        mockExternalRoutineRow("CLR", "UserLib.O'Brien.Method", "NETLIB");
        when(procedure.getProcedureType()).thenReturn(DBSProcedureType.PROCEDURE);
        when(procedure.getParameters(monitor)).thenReturn(List.of());

        String ddl = MimerUtils.buildExternalRoutineSource(monitor, procedure, SCHEMA, "Weird", SPECIFIC_NAME);

        Assertions.assertTrue(ddl.contains("EXTERNAL NAME 'UserLib.O''Brien.Method' IN \"NETLIB\""),
            () -> "Unexpected DDL: " + ddl);
    }

    /**
     * Wires {@code dataSource.getDefaultInstance().getDefaultContext(...).openSession(...)}
     * (what {@code DBUtils.openMetaSession} walks internally) through to {@link #session},
     * and {@code session.prepareStatement(...).executeQuery()} through to {@link #resultSet} -
     * see the class Javadoc.
     */
    private void mockDatabaseChain() throws Exception {
        when(dataSource.supportsExternalLibraries()).thenReturn(true);
        when(dataSource.getDefaultInstance()).thenReturn(instance);
        when(instance.getDefaultContext(any(), anyBoolean())).thenReturn(executionContext);
        when(executionContext.openSession(eq(monitor), eq(DBCExecutionPurpose.META), anyString())).thenReturn(session);
        when(session.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
    }

    private void mockExternalRoutineRow(String language, String externalName, String library) throws Exception {
        when(resultSet.next()).thenReturn(true);
        when(resultSet.getString("ROUTINE_BODY")).thenReturn("EXTERNAL");
        when(resultSet.getString("EXTERNAL_LANGUAGE")).thenReturn(language);
        when(resultSet.getString("EXTERNAL_NAME")).thenReturn(externalName);
        when(resultSet.getString("EXTERNAL_LIBRARY")).thenReturn(library);
    }

    private static GenericProcedureParameter mockParameter(DBSProcedureParameterKind kind, String name, String fullTypeName) {
        GenericProcedureParameter param = mock(GenericProcedureParameter.class);
        when(param.getParameterKind()).thenReturn(kind);
        when(param.getName()).thenReturn(name);
        when(param.getFullTypeName()).thenReturn(fullTypeName);
        return param;
    }
}

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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.sql.*;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.StringWriter;
import java.sql.Types;
import java.util.List;

public class FireBirdStatementProducerTest extends DBeaverUnitTest {

    @Test
    public void usesScriptStatementWithoutNativeParameters() throws Exception {
        FireBirdDataSource dataSource = mockDataSource();
        DBCSession session = Mockito.mock(DBCSession.class);
        DBCStatement statement = Mockito.mock(DBCStatement.class);
        DBCExecutionSource source = Mockito.mock(DBCExecutionSource.class);
        Mockito.when(session.getDataSource()).thenReturn(dataSource);
        Mockito.when(session.prepareStatement(DBCStatementType.SCRIPT, "EXECUTE BLOCK AS BEGIN END", false, false, false))
            .thenReturn(statement);

        DBCStatement actual = DBUtils.makeStatement(
            source, session, DBCStatementType.SCRIPT,
            new SQLQuery(dataSource, "EXECUTE BLOCK AS BEGIN END"), 0, 0
        );

        Assertions.assertSame(statement, actual);
    }

    @Test
    public void passesInlineParametersInSqlToScriptStatement() throws Exception {
        FireBirdDataSource dataSource = mockDataSource();
        DBCSession session = Mockito.mock(DBCSession.class);
        DBCStatement statement = Mockito.mock(DBCStatement.class);
        Mockito.when(session.getDataSource()).thenReturn(dataSource);
        Mockito.when(session.prepareStatement(DBCStatementType.SCRIPT, "UPDATE t SET v = 3", false, false, false))
            .thenReturn(statement);
        String sql = "UPDATE t SET v = ${value}";
        SQLQuery query = new SQLQuery(dataSource, sql);
        query.setParameters(List.of(new SQLQueryParameter(
            new SQLSyntaxManager(), 0, "value", "value",
            sql.indexOf("${value}"), "${value}".length()
        )));
        SQLScriptContext context = scriptContext();
        context.setParameterDefaultValue("value", "3");

        Assertions.assertTrue(context.fillQueryParameters(query, () -> null, false));
        Assertions.assertEquals("UPDATE t SET v = 3", query.getText());
        Assertions.assertSame(statement, DBUtils.makeStatement(null, session, DBCStatementType.SCRIPT, query, 0, 0));
    }

    @Test
    public void bindsNativeParametersWithoutReplacingMarkersOrBindingInlineParameters() throws Exception {
        FireBirdDataSource dataSource = mockDataSource();
        DBCSession session = Mockito.mock(DBCSession.class);
        JDBCPreparedStatement statement = Mockito.mock(JDBCPreparedStatement.class);
        Mockito.when(session.getDataSource()).thenReturn(dataSource);
        String inlineMark = "${increment}";
        String sql = "EXECUTE BLOCK (x INT = ?) RETURNS (result INT) AS BEGIN result = x + " + inlineMark + "; SUSPEND; END";
        String executedSql = sql.replace(inlineMark, "3");
        Mockito.when(session.prepareStatement(DBCStatementType.QUERY, executedSql, false, false, false)).thenReturn(statement);
        SQLQuery query = new SQLQuery(dataSource, sql);
        SQLSyntaxManager syntax = new SQLSyntaxManager();
        SQLQueryParameter nativeParameter = new SQLQueryParameter(syntax, 0, "?", "?", sql.indexOf('?'), 1);
        nativeParameter.setNativeBinding(true);
        nativeParameter.setValue("7");
        SQLQueryParameter inlineParameter = new SQLQueryParameter(
            syntax, 1, "increment", "increment",
            sql.indexOf(inlineMark), inlineMark.length()
        );
        query.setParameters(List.of(nativeParameter, inlineParameter));
        SQLScriptContext context = scriptContext();
        context.setParameterDefaultValue("increment", "3");

        Assertions.assertTrue(context.fillQueryParameters(query, () -> null, false));
        Assertions.assertEquals(executedSql, query.getText());
        DBCExecutionSource source = Mockito.mock(DBCExecutionSource.class);
        DBCStatement actual = DBUtils.makeStatement(source, session, DBCStatementType.SCRIPT, query, 0, 0);

        Assertions.assertSame(statement, actual);
        Mockito.verify(statement).setString(1, "7");
        Mockito.verify(statement, Mockito.never()).setString(2, "3");
    }

    @Test
    public void bindsQuotedStringsAsValuesAfterCheckingForNull() throws Exception {
        FireBirdDataSource dataSource = mockDataSource();
        DBCSession session = Mockito.mock(DBCSession.class);
        String sql = "EXECUTE BLOCK (x VARCHAR(20) = ?) AS BEGIN END";
        Mockito.when(session.getDataSource()).thenReturn(dataSource);

        for (String[] inputAndValue : new String[][] {
            {"'Alice'", "Alice"}, {"''", ""}, {"'O''Brien'", "O'Brien"}, {"'NULL'", "NULL"}, {"Alice", "Alice"}
        }) {
            JDBCPreparedStatement statement = Mockito.mock(JDBCPreparedStatement.class);
            Mockito.when(session.prepareStatement(DBCStatementType.QUERY, sql, false, false, false)).thenReturn(statement);
            SQLQuery query = nativeQuery(dataSource, sql);
            query.getParameters().getFirst().setValue(inputAndValue[0]);

            Assertions.assertSame(statement, DBUtils.makeStatement(null, session, DBCStatementType.SCRIPT, query, 0, 0));
            Mockito.verify(statement).setString(1, inputAndValue[1]);
        }
    }

    @Test
    public void bindsBlankAndUnquotedNullAsSqlNull() throws Exception {
        FireBirdDataSource dataSource = mockDataSource();
        DBCSession session = Mockito.mock(DBCSession.class);
        String sql = "EXECUTE BLOCK (x VARCHAR(20) = ?) AS BEGIN END";
        Mockito.when(session.getDataSource()).thenReturn(dataSource);

        for (String value : new String[] {null, "", "NULL", "null"}) {
            JDBCPreparedStatement statement = Mockito.mock(JDBCPreparedStatement.class);
            Mockito.when(session.prepareStatement(DBCStatementType.QUERY, sql, false, false, false)).thenReturn(statement);
            SQLQuery query = nativeQuery(dataSource, sql);
            query.getParameters().getFirst().setValue(value);

            Assertions.assertSame(statement, DBUtils.makeStatement(null, session, DBCStatementType.SCRIPT, query, 0, 0));
            Mockito.verify(statement).setNull(1, Types.NULL);
        }
    }

    @Test
    public void ignoreExecutesCurrentAndSubsequentQueriesWithoutBinding() throws Exception {
        FireBirdDataSource dataSource = mockDataSource();
        DBCSession session = Mockito.mock(DBCSession.class);
        DBCStatement statement = Mockito.mock(DBCStatement.class);
        String sql = "EXECUTE BLOCK (x INT = ?) AS BEGIN END";
        Mockito.when(session.getDataSource()).thenReturn(dataSource);
        Mockito.when(session.prepareStatement(DBCStatementType.SCRIPT, sql, false, false, false)).thenReturn(statement);
        SQLParametersProvider ignore = (scriptContext, query, parameters, receiver, useDefaults) -> null;
        SQLScriptContext context = new SQLScriptContext(null, () -> null, null, new StringWriter(), ignore);

        for (int i = 0; i < 2; i++) {
            SQLQuery query = nativeQuery(dataSource, sql);
            Assertions.assertTrue(context.fillQueryParameters(query, () -> null, false));
            Assertions.assertEquals(sql, query.getText());
            Assertions.assertEquals(List.of(), query.getParameters());
            Assertions.assertSame(statement, DBUtils.makeStatement(null, session, DBCStatementType.SCRIPT, query, 0, 0));
        }
    }

    @Test
    public void cancelDoesNotDiscardParameters() {
        FireBirdDataSource dataSource = mockDataSource();
        SQLParametersProvider cancel = (scriptContext, query, parameters, receiver, useDefaults) -> false;
        SQLScriptContext context = new SQLScriptContext(null, () -> null, null, new StringWriter(), cancel);
        SQLQuery query = nativeQuery(dataSource, "EXECUTE BLOCK (x INT = ?) AS BEGIN END");

        Assertions.assertFalse(context.fillQueryParameters(query, () -> null, false));
        Assertions.assertFalse(context.isIgnoreParameters());
        Assertions.assertEquals(1, query.getParameters().size());
    }

    @NotNull
    private static SQLQuery nativeQuery(@NotNull FireBirdDataSource dataSource, @NotNull String sql) {
        SQLQuery query = new SQLQuery(dataSource, sql);
        SQLQueryParameter parameter = new SQLQueryParameter(new SQLSyntaxManager(), 0, "?", "?");
        parameter.setNativeBinding(true);
        query.setParameters(List.of(parameter));
        return query;
    }

    @NotNull
    private static SQLScriptContext scriptContext() {
        return new SQLScriptContext(null, () -> null, null, new StringWriter(), null);
    }

    @NotNull
    private static FireBirdDataSource mockDataSource() {
        FireBirdDataSource dataSource = Mockito.mock(FireBirdDataSource.class);
        Mockito.when(dataSource.getSQLDialect()).thenReturn(new FireBirdSQLDialect());
        Mockito.when(dataSource.getInfo()).thenReturn(Mockito.mock(DBPDataSourceInfo.class));
        Mockito.doCallRealMethod().when(dataSource).getAdapter(DBCStatementProducer.class);
        return dataSource;
    }
}

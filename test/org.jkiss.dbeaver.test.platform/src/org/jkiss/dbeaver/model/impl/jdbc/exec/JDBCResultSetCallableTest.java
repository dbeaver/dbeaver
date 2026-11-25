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
package org.jkiss.dbeaver.model.impl.jdbc.exec;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.CallableStatement;
import java.sql.ParameterMetaData;
import java.sql.Types;

class JDBCResultSetCallableTest {

    @Test
    void outParametersAreReadWithZeroBasedAttributeIndex() throws Exception {
        DBCResultSet results = executeCallWithOutParameters("first", "second");
        Assertions.assertEquals(2, results.getMeta().getAttributes().size());
        Assertions.assertTrue(results.nextRow());
        Assertions.assertEquals("first", results.getAttributeValue(0));
        Assertions.assertEquals("second", results.getAttributeValue(1));
    }

    @NotNull
    private static DBCResultSet executeCallWithOutParameters(@NotNull Object... outValues) throws Exception {
        JDBCSession session = Mockito.mock(JDBCSession.class, Mockito.RETURNS_DEEP_STUBS);
        JDBCDataSource dataSource = Mockito.mock(JDBCDataSource.class, Mockito.RETURNS_DEEP_STUBS);
        Mockito.when(session.getDataSource()).thenReturn(dataSource);

        CallableStatement jdbcStatement = Mockito.mock(CallableStatement.class);
        ParameterMetaData parameterMeta = Mockito.mock(ParameterMetaData.class);
        Mockito.when(jdbcStatement.getParameterMetaData()).thenReturn(parameterMeta);
        Mockito.when(parameterMeta.getParameterCount()).thenReturn(outValues.length);
        Mockito.when(parameterMeta.getParameterMode(Mockito.anyInt())).thenReturn(ParameterMetaData.parameterModeOut);
        Mockito.when(parameterMeta.getParameterTypeName(Mockito.anyInt())).thenReturn("VARCHAR");
        Mockito.when(parameterMeta.getParameterType(Mockito.anyInt())).thenReturn(Types.VARCHAR);
        for (int i = 0; i < outValues.length; i++) {
            // JDBC parameter indexes are 1-based
            Mockito.when(jdbcStatement.getObject(i + 1)).thenReturn(outValues[i]);
        }

        JDBCCallableStatementImpl statement = new JDBCCallableStatementImpl(session, () -> jdbcStatement, "CALL proc(?, ?)", true);
        return statement.getResultSet();
    }
}

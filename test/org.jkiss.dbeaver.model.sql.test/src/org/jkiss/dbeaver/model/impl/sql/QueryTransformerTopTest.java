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
package org.jkiss.dbeaver.model.impl.sql;

import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class QueryTransformerTopTest extends DBeaverUnitTest {

    @ParameterizedTest
    @ValueSource(longs = {100, 200, 1300})
    public void subsequentPagesShouldApplyJdbcOffset(long offset) throws DBCException {
        var transformer = new QueryTransformerTop();
        transformer.setParameters(offset, 100);
        var query = new SQLQuery(null, "SELECT * FROM test_table");
        var statement = mock(DBCStatement.class);

        assertEquals("SELECT TOP " + (offset + 100) + " * FROM test_table", transformer.transformQueryString(query));
        transformer.transformStatement(statement, 0);

        verify(statement).setLimit(offset, 100);
    }

    @Test
    public void firstPageShouldUseSqlLimitOnly() throws DBCException {
        var transformer = new QueryTransformerTop();
        transformer.setParameters(0, 100);
        var query = new SQLQuery(null, "SELECT * FROM test_table");
        var statement = mock(DBCStatement.class);

        assertEquals("SELECT TOP 100 * FROM test_table", transformer.transformQueryString(query));
        transformer.transformStatement(statement, 0);

        verifyNoInteractions(statement);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, 100})
    public void existingTopShouldUseJdbcLimit(long offset) throws DBCException {
        var transformer = new QueryTransformerTop();
        transformer.setParameters(offset, 100);
        var query = new SQLQuery(null, "SELECT TOP 250 * FROM test_table");
        var statement = mock(DBCStatement.class);

        assertEquals(query.getText(), transformer.transformQueryString(query));
        transformer.transformStatement(statement, 0);

        verify(statement).setLimit(offset, 100);
    }
}

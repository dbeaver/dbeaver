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

import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * {@link MimerStatement}'s {@code CREATE [SCROLL|NO SCROLL] STATEMENT} header - translated
 * verbatim from {@code mimer.xml}'s own {@code CASE} branching (see the class Javadoc): {@code
 * NO SCROLL} when not scrollable at all, {@code SCROLL} when scrollable and not forward-only, and
 * omitted entirely when both cursor modes are allowed. Exercised through the real {@link
 * MimerStatement#getObjectDefinitionText} - the header-building method itself is private, so this
 * is the only way to reach it - which means mocking the JDBC session {@link
 * MimerUtils#readStatementDefinition} opens internally, same shape as {@code
 * GreenplumTableTest}/{@code MimerUtilsExternalRoutineTest}.
 *
 * @author Mimer Information Technology
 */
public class MimerStatementTest extends DBeaverUnitTest {

    @Mock
    private MimerSchema schema;

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
    public void setUp() throws Exception {
        when(schema.getName()).thenReturn("mimer_store");
        when(schema.getDataSource()).thenReturn(dataSource);
        when(dataSource.getDefaultInstance()).thenReturn(instance);
        when(instance.getDefaultContext(any(), anyBoolean())).thenReturn(executionContext);
        when(executionContext.openSession(eq(monitor), eq(DBCExecutionPurpose.META), anyString())).thenReturn(session);
        when(session.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getString(1)).thenReturn("SELECT * FROM customers");
    }

    @Test
    public void headerIsNoScrollWhenNotScrollableAtAll() throws Exception {
        MimerStatement stmt = statement(false, false);
        String source = stmt.getObjectDefinitionText(monitor, Map.of());
        Assertions.assertTrue(source.startsWith("CREATE NO SCROLL STATEMENT \"mimer_store\".\"zzz\"\n"), source);
    }

    @Test
    public void headerIsScrollWhenScrollableAndNotForwardOnly() throws Exception {
        MimerStatement stmt = statement(true, false);
        String source = stmt.getObjectDefinitionText(monitor, Map.of());
        Assertions.assertTrue(source.startsWith("CREATE SCROLL STATEMENT \"mimer_store\".\"zzz\"\n"), source);
    }

    @Test
    public void headerOmitsTheClauseWhenScrollableAndForwardOnly() throws Exception {
        MimerStatement stmt = statement(true, true);
        String source = stmt.getObjectDefinitionText(monitor, Map.of());
        Assertions.assertTrue(source.startsWith("CREATE STATEMENT \"mimer_store\".\"zzz\"\n"), source);
    }

    @Test
    public void objectDefinitionTextAppendsTheReadBodyAfterTheHeader() throws Exception {
        MimerStatement stmt = statement(false, false);
        Assertions.assertEquals(
            "CREATE NO SCROLL STATEMENT \"mimer_store\".\"zzz\"\nSELECT * FROM customers",
            stmt.getObjectDefinitionText(monitor, Map.of()));
    }

    private MimerStatement statement(boolean scrollable, boolean forwardOnly) throws Exception {
        JDBCResultSet loadRow = org.mockito.Mockito.mock(JDBCResultSet.class);
        when(loadRow.getString("STATEMENT_NAME")).thenReturn("zzz");
        when(loadRow.getString("IS_SCROLLABLE")).thenReturn(scrollable ? "YES" : "NO");
        when(loadRow.getString("IS_FORWARD_ONLY")).thenReturn(forwardOnly ? "YES" : "NO");
        return new MimerStatement(schema, loadRow);
    }
}

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
package org.jkiss.dbeaver.ext.generic.test;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBDatabaseException;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.ext.generic.model.GenericView;
import org.jkiss.dbeaver.ext.generic.model.meta.hardcharger.HardchargerMetaModel;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformType;
import org.jkiss.dbeaver.model.exec.DBCQueryTransformer;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.junit.jupiter.api.Test;

import java.sql.SQLException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HardchargerMetaModelTest {

    @Test
    void schemaRefreshUsesAllTablesAndTableRefreshUsesTheLiteralName() throws SQLException {
        JDBCSession session = mock(JDBCSession.class);
        JDBCDatabaseMetaData metadata = mock(JDBCDatabaseMetaData.class);
        JDBCResultSet result = mock(JDBCResultSet.class);
        JDBCStatement statement = mock(JDBCStatement.class);
        GenericCatalog catalog = mock(GenericCatalog.class);
        GenericSchema schema = mock(GenericSchema.class);
        GenericTableBase table = mock(GenericTableBase.class);
        when(session.getMetaData()).thenReturn(metadata);
        when(schema.getCatalog()).thenReturn(catalog);
        when(schema.getSchema()).thenReturn(schema);
        when(catalog.getName()).thenReturn("catalog");
        when(schema.getName()).thenReturn("schema");
        when(table.getName()).thenReturn("table_%");
        when(metadata.getIndexInfo("catalog", "schema", null, false, true)).thenReturn(result);
        when(metadata.getIndexInfo("catalog", "schema", "table_%", false, true)).thenReturn(result);
        when(result.getSourceStatement()).thenReturn(statement);

        HardchargerMetaModel model = new HardchargerMetaModel();
        assertSame(statement, model.prepareIndexesLoadStatement(session, schema, null));
        assertSame(statement, model.prepareIndexesLoadStatement(session, schema, table));
        verify(metadata).getIndexInfo("catalog", "schema", null, false, true);
        verify(metadata).getIndexInfo("catalog", "schema", "table_%", false, true);
    }

    @Test
    void viewDefinitionBindsIdentifiersAndClosesMetadataResources() throws Exception {
        DBRProgressMonitor monitor = mock(DBRProgressMonitor.class);
        GenericView view = mock(GenericView.class);
        GenericCatalog catalog = mock(GenericCatalog.class);
        GenericSchema schema = mock(GenericSchema.class);
        JDBCSession session = mock(JDBCSession.class);
        JDBCPreparedStatement statement = mock(JDBCPreparedStatement.class);
        JDBCResultSet result = mock(JDBCResultSet.class);
        when(view.getCatalog()).thenReturn(catalog);
        when(view.getContainer()).thenReturn(schema);
        when(catalog.getName()).thenReturn("catalog'quoted");
        when(schema.getName()).thenReturn("schema'quoted");
        when(view.getName()).thenReturn("view'quoted");
        when(session.prepareStatement(anyString())).thenReturn(statement);
        when(statement.executeQuery()).thenReturn(result);
        when(result.next()).thenReturn(true);
        String ddl = "CREATE VIEW example AS SELECT 42;";
        when(result.getString(1)).thenReturn(ddl);

        configureMetadataSession(monitor, view, session);
        assertEquals(ddl, new HardchargerMetaModel().getViewDDL(monitor, view, Map.of()));
        verify(statement).setObject(1, "catalog'quoted");
        verify(statement).setObject(2, "schema'quoted");
        verify(statement).setObject(3, "view'quoted");
        verify(result).close();
        verify(statement).close();
        verify(session).close();
    }

    @Test
    void viewDefinitionErrorsAreReportedInsteadOfHidden() throws Exception {
        DBRProgressMonitor monitor = mock(DBRProgressMonitor.class);
        GenericView view = mock(GenericView.class);
        GenericSchema schema = mock(GenericSchema.class);
        JDBCSession session = mock(JDBCSession.class);
        when(view.getContainer()).thenReturn(schema);
        SQLException failure = new SQLException("Permission denied", "42501");
        when(session.prepareStatement(anyString())).thenThrow(failure);

        configureMetadataSession(monitor, view, session);
        DBDatabaseException error = assertThrows(
            DBDatabaseException.class, () -> new HardchargerMetaModel().getViewDDL(monitor, view, Map.of())
        );
        assertSame(failure, error.getCause());
        verify(session).close();
    }

    private void configureMetadataSession(
        @NotNull DBRProgressMonitor monitor,
        @NotNull GenericView view,
        @NotNull JDBCSession session
    ) {
        GenericDataSource dataSource = mock(GenericDataSource.class);
        JDBCRemoteInstance instance = mock(JDBCRemoteInstance.class);
        JDBCExecutionContext context = mock(JDBCExecutionContext.class);
        when(view.getDataSource()).thenReturn(dataSource);
        when(dataSource.hasDefaultInstance()).thenReturn(true);
        when(dataSource.getDefaultInstance()).thenReturn(instance);
        when(instance.getDefaultContext(any(), eq(true))).thenReturn(context);
        when(context.openSession(eq(monitor), eq(DBCExecutionPurpose.META), anyString())).thenReturn(session);
    }

    @Test
    void pagingAddsLimitAndOffsetAndPreservesExistingLimitsAndDml() throws Exception {
        DBCQueryTransformer transformer = new HardchargerMetaModel().createQueryTransformer(DBCQueryTransformType.RESULT_SET_LIMIT);
        transformer.setParameters(200, 100);
        assertEquals("SELECT * FROM sample ORDER BY id\nLIMIT 100 OFFSET 200",
            transformer.transformQueryString(new SQLQuery(null, "SELECT * FROM sample ORDER BY id")));
        assertEquals("SELECT * FROM sample LIMIT 5",
            transformer.transformQueryString(new SQLQuery(null, "SELECT * FROM sample LIMIT 5")));
        assertEquals("UPDATE sample SET id = 1",
            transformer.transformQueryString(new SQLQuery(null, "UPDATE sample SET id = 1")));
    }

}

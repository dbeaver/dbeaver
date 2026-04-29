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

import org.jkiss.dbeaver.ext.duckdb.model.DuckMetaModel;
import org.jkiss.dbeaver.ext.generic.model.GenericCatalog;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericSchema;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.GenericTableBase;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.Assert;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

public class DuckMetaModelTest extends DBeaverUnitTest {

    @Test
    public void readsForeignKeysFromDuckDBConstraints() throws Exception {
        DuckMetaModel metaModel = new DuckMetaModel();
        JDBCSession session = Mockito.mock(JDBCSession.class);
        JDBCPreparedStatement statement = Mockito.mock(JDBCPreparedStatement.class);
        Mockito.when(session.prepareStatement(Mockito.anyString())).thenReturn(statement);

        GenericStructContainer owner = Mockito.mock(GenericStructContainer.class);
        GenericDataSource dataSource = Mockito.mock(GenericDataSource.class);
        GenericCatalog catalog = Mockito.mock(GenericCatalog.class);
        GenericSchema schema = Mockito.mock(GenericSchema.class);
        GenericTableBase table = Mockito.mock(GenericTableBase.class);

        Mockito.when(owner.getDataSource()).thenReturn(dataSource);
        Mockito.when(owner.getCatalog()).thenReturn(catalog);
        Mockito.when(catalog.getName()).thenReturn("memory");
        Mockito.when(owner.getSchema()).thenReturn(schema);
        Mockito.when(schema.getName()).thenReturn("main");
        Mockito.when(table.getName()).thenReturn("table2");

        JDBCStatement result = metaModel.prepareForeignKeysLoadStatement(session, owner, table);

        Assert.assertSame(statement, result);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(session).prepareStatement(sqlCaptor.capture());
        String sql = sqlCaptor.getValue();
        Assert.assertTrue(sql.contains("duckdb_constraints()"));
        Assert.assertTrue(sql.contains("constraint_text"));
        Assert.assertFalse(sql.contains("constraint_name"));
        Assert.assertTrue(sql.contains(" AS PKTABLE_NAME"));
        Assert.assertTrue(sql.contains(" AS FKCOLUMN_NAME"));
        Mockito.verify(statement).setString(1, "memory");
        Mockito.verify(statement).setString(2, "memory");
        Mockito.verify(statement).setString(3, "main");
        Mockito.verify(statement).setString(4, "main");
        Mockito.verify(statement).setString(5, "table2");
    }
}

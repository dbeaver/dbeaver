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

import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.GenericStructContainer;
import org.jkiss.dbeaver.ext.generic.model.TableCache;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.junit.DBeaverUnitTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mockito;

public class FireBirdColumnDomainTest extends DBeaverUnitTest {

    @Test
    public void loadsDomainsOnColumnMetadataSessionBeforeOpeningColumns() throws Exception {
        GenericDataSource dataSource = Mockito.mock(GenericDataSource.class);
        GenericStructContainer owner = Mockito.mock(GenericStructContainer.class);
        Mockito.when(owner.getDataSource()).thenReturn(dataSource);
        Mockito.when(dataSource.getMetaModel()).thenReturn(Mockito.mock(GenericMetaModel.class));
        Mockito.when(dataSource.getAllObjectsPattern()).thenReturn("%");
        FireBirdTable table = new FireBirdTable(owner, "TEST_TABLE", "TABLE", null);

        JDBCSession session = Mockito.mock(JDBCSession.class);
        JDBCPreparedStatement domainStatement = Mockito.mock(JDBCPreparedStatement.class);
        JDBCResultSet domains = Mockito.mock(JDBCResultSet.class);
        JDBCDatabaseMetaData metadata = Mockito.mock(JDBCDatabaseMetaData.class);
        JDBCResultSet columns = Mockito.mock(JDBCResultSet.class);
        JDBCStatement columnStatement = Mockito.mock(JDBCStatement.class);
        Mockito.when(session.prepareStatement(Mockito.anyString())).thenReturn(domainStatement);
        Mockito.when(domainStatement.executeQuery()).thenReturn(domains);
        Mockito.when(domains.next()).thenReturn(true, false);
        Mockito.when(domains.getString(1)).thenReturn("COLUMN_A   ");
        Mockito.when(domains.getString(2)).thenReturn("DOMAIN_A   ");
        Mockito.when(session.getMetaData()).thenReturn(metadata);
        Mockito.when(metadata.getColumns(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(columns);
        Mockito.when(columns.getSourceStatement()).thenReturn(columnStatement);

        Assertions.assertSame(columnStatement, new FireBirdMetaModel().prepareTableColumnLoadStatement(session, owner, table));
        InOrder order = Mockito.inOrder(domainStatement, domains, metadata);
        order.verify(domainStatement).setString(1, "TEST_TABLE");
        order.verify(domainStatement).executeQuery();
        order.verify(domains).close();
        order.verify(metadata).getColumns(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        FireBirdTableColumn column = Mockito.mock(FireBirdTableColumn.class);
        Mockito.when(column.getName()).thenReturn("COLUMN_A");
        Assertions.assertEquals("DOMAIN_A", table.getColumnDomainType(Mockito.mock(DBRProgressMonitor.class), column));
    }

    @Test
    public void loadsDomainsForAllCachedTablesBeforeOpeningBulkColumns() throws Exception {
        GenericDataSource dataSource = Mockito.mock(GenericDataSource.class);
        GenericStructContainer owner = Mockito.mock(GenericStructContainer.class);
        TableCache tableCache = Mockito.mock(TableCache.class);
        Mockito.when(owner.getDataSource()).thenReturn(dataSource);
        Mockito.when(owner.getTableCache()).thenReturn(tableCache);
        Mockito.when(dataSource.getMetaModel()).thenReturn(Mockito.mock(GenericMetaModel.class));
        Mockito.when(dataSource.getAllObjectsPattern()).thenReturn("%");
        FireBirdTable table = new FireBirdTable(owner, "TEST_TABLE", "TABLE", null);
        FireBirdView view = new FireBirdView(owner, "TEST_VIEW", "VIEW", null);
        Mockito.when(tableCache.getCachedObject("TEST_TABLE")).thenReturn(table);
        Mockito.when(tableCache.getCachedObject("TEST_VIEW")).thenReturn(view);

        JDBCSession session = Mockito.mock(JDBCSession.class);
        JDBCPreparedStatement domainStatement = Mockito.mock(JDBCPreparedStatement.class);
        JDBCResultSet domains = Mockito.mock(JDBCResultSet.class);
        JDBCDatabaseMetaData metadata = Mockito.mock(JDBCDatabaseMetaData.class);
        JDBCResultSet columns = Mockito.mock(JDBCResultSet.class);
        JDBCStatement columnStatement = Mockito.mock(JDBCStatement.class);
        Mockito.when(session.prepareStatement(Mockito.anyString())).thenReturn(domainStatement);
        Mockito.when(domainStatement.executeQuery()).thenReturn(domains);
        Mockito.when(domains.next()).thenReturn(true, true, false);
        Mockito.when(domains.getString(1)).thenReturn("TEST_TABLE   ", "TEST_VIEW   ");
        Mockito.when(domains.getString(2)).thenReturn("COLUMN_A   ", "COLUMN_B   ");
        Mockito.when(domains.getString(3)).thenReturn("DOMAIN_A   ", "DOMAIN_B   ");
        Mockito.when(session.getMetaData()).thenReturn(metadata);
        Mockito.when(metadata.getColumns(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(columns);
        Mockito.when(columns.getSourceStatement()).thenReturn(columnStatement);

        Assertions.assertSame(columnStatement, new FireBirdMetaModel().prepareTableColumnLoadStatement(session, owner, null));
        InOrder order = Mockito.inOrder(domainStatement, domains, metadata);
        order.verify(domainStatement).executeQuery();
        order.verify(domains).close();
        order.verify(metadata).getColumns(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.any());
        FireBirdTableColumn tableColumn = Mockito.mock(FireBirdTableColumn.class);
        FireBirdTableColumn viewColumn = Mockito.mock(FireBirdTableColumn.class);
        Mockito.when(tableColumn.getName()).thenReturn("COLUMN_A");
        Mockito.when(viewColumn.getName()).thenReturn("COLUMN_B");
        DBRProgressMonitor monitor = Mockito.mock(DBRProgressMonitor.class);
        Assertions.assertEquals("DOMAIN_A", table.getColumnDomainType(monitor, tableColumn));
        Assertions.assertEquals("DOMAIN_B", view.getColumnDomainType(monitor, viewColumn));
        Mockito.verify(session, Mockito.times(1)).prepareStatement(Mockito.anyString());
    }
}

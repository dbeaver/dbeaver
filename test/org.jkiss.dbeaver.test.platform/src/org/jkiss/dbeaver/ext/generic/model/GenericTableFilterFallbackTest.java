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
package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class GenericTableFilterFallbackTest {

    @Test
    public void subtypeOnlyTableFilterDoesNotRestrictSharedMetadataQuery() throws Exception {
        GenericDataSource dataSource = Mockito.mock(GenericDataSource.class);
        GenericStructContainer owner = Mockito.mock(GenericStructContainer.class);
        DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class);
        JDBCSession session = Mockito.mock(JDBCSession.class);
        JDBCDatabaseMetaData metadata = Mockito.mock(JDBCDatabaseMetaData.class);
        JDBCResultSet resultSet = Mockito.mock(JDBCResultSet.class);
        Mockito.when(owner.getDataSource()).thenReturn(dataSource);
        Mockito.when(dataSource.getAllObjectsPattern()).thenReturn("%");
        Mockito.when(dataSource.getContainer()).thenReturn(container);
        Mockito.when(session.getDataSource()).thenReturn(dataSource);
        Mockito.when(session.getMetaData()).thenReturn(metadata);
        Mockito.when(metadata.getTables(null, null, "%", null)).thenReturn(resultSet);

        new GenericMetaModel().prepareTableLoadStatement(session, owner, null, null);
        Mockito.verify(metadata).getTables(null, null, "%", null);
        Mockito.verify(container).getObjectFilter(GenericTable.class, owner, false);
        Mockito.verify(container, Mockito.never()).getEffectiveObjectFilter(
            Mockito.any(), Mockito.any(), Mockito.anyBoolean());
    }

    @Test
    public void originalBaseTypeFilterRetainsItsMask() throws Exception {
        GenericDataSource dataSource = Mockito.mock(GenericDataSource.class);
        GenericStructContainer owner = Mockito.mock(GenericStructContainer.class);
        DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class);
        final JDBCSession session = Mockito.mock(JDBCSession.class);
        final JDBCDatabaseMetaData metadata = Mockito.mock(JDBCDatabaseMetaData.class);
        final JDBCResultSet resultSet = Mockito.mock(JDBCResultSet.class);
        DBSObjectFilter filter = new DBSObjectFilter();
        filter.addInclude("TABLE*");
        Mockito.when(owner.getDataSource()).thenReturn(dataSource);
        Mockito.when(dataSource.getContainer()).thenReturn(container);
        Mockito.when(session.getDataSource()).thenReturn(dataSource);
        Mockito.when(session.getMetaData()).thenReturn(metadata);
        Mockito.when(container.getObjectFilter(GenericTable.class, owner, false)).thenReturn(filter);
        Mockito.when(metadata.getTables(null, null, "TABLE%", null)).thenReturn(resultSet);

        new GenericMetaModel().prepareTableLoadStatement(session, owner, null, null);

        Mockito.verify(metadata).getTables(null, null, "TABLE%", null);
    }
}

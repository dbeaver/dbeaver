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
package org.jkiss.dbeaver.model.navigator;

import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPObjectWithLongId;
import org.jkiss.dbeaver.model.DBPUniqueObject;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeItem;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

public class DBNDatabaseNodeTest {
    @Test
    public void uniqueObjectNameUsedAsNodeId() {
        DBNDatabaseNode node = Mockito.mock(DBNDatabaseNode.class, Mockito.CALLS_REAL_METHODS);
        DBSObject object = Mockito.mock(
            DBSObject.class,
            Mockito.withSettings().extraInterfaces(DBPUniqueObject.class));
        Mockito.when(node.getObject()).thenReturn(object);
        Mockito.when(((DBPUniqueObject) object).getUniqueName()).thenReturn("procedure#2");

        Assertions.assertEquals("procedure#2", node.getNodeId());
    }

    @Test
    public void objectIdUsedAsNodeIdFallback() {
        DBNDatabaseNode node = Mockito.mock(DBNDatabaseNode.class, Mockito.CALLS_REAL_METHODS);
        DBSObject object = Mockito.mock(
            DBSObject.class,
            Mockito.withSettings().extraInterfaces(DBPObjectWithLongId.class));
        Mockito.when(node.getObject()).thenReturn(object);
        Mockito.doReturn("table").when(node).getNodeDisplayName();
        Mockito.when(((DBPObjectWithLongId) object).getObjectId()).thenReturn(42L);

        Assertions.assertEquals("table_42", node.getNodeId());
    }

    @Test
    public void connectionAndUserFiltersAreCombinedAtNavigatorLevel() {
        DBNDatabaseNode node = Mockito.mock(DBNDatabaseNode.class, Mockito.CALLS_REAL_METHODS);
        DBPDataSourceContainer dataSource = Mockito.mock(DBPDataSourceContainer.class);
        DBXTreeItem itemsMeta = Mockito.mock(DBXTreeItem.class);
        DBSObjectFilter connectionFilter = new DBSObjectFilter();
        connectionFilter.setInclude(List.of("prod_%"));
        DBSObjectFilter userFilter = new DBSObjectFilter();
        userFilter.setInclude(List.of("%_eu"));

        Mockito.doReturn(dataSource).when(node).getDataSourceContainer();
        Mockito.doReturn(String.class).when(node).getChildrenOrFolderClass(itemsMeta);
        Mockito.doReturn(null).when(node).getFilterObject();
        Mockito.when(dataSource.getConnectionObjectFilter(String.class, null, false)).thenReturn(connectionFilter);
        Mockito.when(dataSource.getUserObjectFilter(String.class, null, false)).thenReturn(userFilter);

        DBSObjectFilter effectiveFilter = node.getNodeFilter(itemsMeta, false);

        Assertions.assertNotNull(effectiveFilter);
        Assertions.assertTrue(effectiveFilter.matches("prod_eu"));
        Assertions.assertFalse(effectiveFilter.matches("test_eu"));
        Assertions.assertFalse(effectiveFilter.matches("prod_us"));
        Mockito.verify(dataSource, Mockito.never()).getObjectFilter(String.class, null, false);
    }
}

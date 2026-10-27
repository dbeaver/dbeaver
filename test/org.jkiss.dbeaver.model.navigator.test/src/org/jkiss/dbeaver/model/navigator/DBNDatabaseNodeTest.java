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

import org.jkiss.dbeaver.model.DBPObjectWithLongId;
import org.jkiss.dbeaver.model.DBPOverloadedObject;
import org.jkiss.dbeaver.model.DBPUniqueObject;
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
    public void connectionAndUserFiltersAreAppliedAtNavigatorLevel() {
        DBSObject object = Mockito.mock(DBSObject.class);
        Mockito.when(object.getName()).thenReturn("prod_eu");
        DBSObjectFilter connectionFilter = new DBSObjectFilter();
        connectionFilter.setInclude(List.of("prod_%"));
        DBSObjectFilter userFilter = new DBSObjectFilter();
        userFilter.setInclude(List.of("%_eu"));

        Assertions.assertTrue(DBNDatabaseNode.mergeFilters(connectionFilter, userFilter).test(object));

        Mockito.when(object.getName()).thenReturn("test_eu");
        Assertions.assertFalse(DBNDatabaseNode.mergeFilters(connectionFilter, userFilter).test(object));

        Mockito.when(object.getName()).thenReturn("prod_us");
        Assertions.assertFalse(DBNDatabaseNode.mergeFilters(connectionFilter, userFilter).test(object));
    }

    @Test
    public void overloadedObjectNamesAreMatchedByBothFilters() {
        DBSObject object = Mockito.mock(
            DBSObject.class,
            Mockito.withSettings().extraInterfaces(DBPOverloadedObject.class)
        );
        Mockito.when(object.getName()).thenReturn("base_name");
        Mockito.when(((DBPOverloadedObject) object).getOverloadedName()).thenReturn("overloaded_name");
        DBSObjectFilter connectionFilter = new DBSObjectFilter();
        connectionFilter.setInclude(List.of("base_name"));
        DBSObjectFilter userFilter = new DBSObjectFilter();
        userFilter.setInclude(List.of("overloaded_name"));

        Assertions.assertTrue(DBNDatabaseNode.mergeFilters(connectionFilter, userFilter).test(object));
    }

    @Test
    public void disabledAndEmptyFiltersAreIgnored() {
        DBSObject object = Mockito.mock(DBSObject.class);
        Mockito.when(object.getName()).thenReturn("prod_eu");
        DBSObjectFilter disabledFilter = new DBSObjectFilter();
        disabledFilter.setInclude(List.of("test_%"));
        disabledFilter.setEnabled(false);
        DBSObjectFilter emptyFilter = new DBSObjectFilter();

        Assertions.assertTrue(DBNDatabaseNode.mergeFilters(disabledFilter, emptyFilter).test(object));
    }

    @Test
    public void effectiveFilterPreservesCaseSensitivity() {
        DBSObject object = Mockito.mock(DBSObject.class);
        Mockito.when(object.getName()).thenReturn("prod_eu");
        DBSObjectFilter filter = new DBSObjectFilter();
        filter.setInclude(List.of("PROD_%"));
        filter.setCaseSensitive(true);

        Assertions.assertFalse(DBNDatabaseNode.mergeFilters(filter).test(object));
    }
}

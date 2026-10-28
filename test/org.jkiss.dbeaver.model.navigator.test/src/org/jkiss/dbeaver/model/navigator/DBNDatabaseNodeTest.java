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

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.impl.AbstractDescriptor;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeItem;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectFilter;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.List;
import java.util.function.Predicate;

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

        Predicate<DBSObject> filterMatcher = DBNObjectFilterProvider.createFilterMatcher(connectionFilter, userFilter);
        Assertions.assertNotNull(filterMatcher);
        Assertions.assertTrue(filterMatcher.test(object));

        Mockito.when(object.getName()).thenReturn("test_eu");
        Assertions.assertFalse(filterMatcher.test(object));

        Mockito.when(object.getName()).thenReturn("prod_us");
        Assertions.assertFalse(filterMatcher.test(object));
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

        Predicate<DBSObject> filterMatcher = DBNObjectFilterProvider.createFilterMatcher(connectionFilter, userFilter);
        Assertions.assertNotNull(filterMatcher);
        Assertions.assertTrue(filterMatcher.test(object));
    }

    @Test
    public void disabledAndEmptyFiltersAreIgnored() {
        DBSObject object = Mockito.mock(DBSObject.class);
        Mockito.when(object.getName()).thenReturn("prod_eu");
        DBSObjectFilter disabledFilter = new DBSObjectFilter();
        disabledFilter.setInclude(List.of("test_%"));
        disabledFilter.setEnabled(false);
        DBSObjectFilter emptyFilter = new DBSObjectFilter();

        Assertions.assertNull(DBNObjectFilterProvider.createFilterMatcher(disabledFilter, emptyFilter));
    }

    @Test
    public void effectiveFilterPreservesCaseSensitivity() {
        DBSObject object = Mockito.mock(DBSObject.class);
        Mockito.when(object.getName()).thenReturn("prod_eu");
        DBSObjectFilter filter = new DBSObjectFilter();
        filter.setInclude(List.of("PROD_%"));
        filter.setCaseSensitive(true);

        Predicate<DBSObject> filterMatcher = DBNObjectFilterProvider.createFilterMatcher(filter);
        Assertions.assertNotNull(filterMatcher);
        Assertions.assertFalse(filterMatcher.test(object));
    }

    @Test
    public void updateChildrenOrderRefiltersLoadedChildrenWithoutRefreshingMetadata() throws DBException {
        DBPProject project = Mockito.mock(DBPProject.class, Mockito.withSettings().extraInterfaces(DBPAdaptable.class));
        DBNObjectFilterProvider filterProvider = Mockito.mock(DBNObjectFilterProvider.class);
        Mockito.when(((DBPAdaptable) project).getAdapter(DBNObjectFilterProvider.class)).thenReturn(filterProvider);
        DBPDataSourceContainer container = Mockito.mock(DBPDataSourceContainer.class);
        Mockito.when(container.getProject()).thenReturn(project);
        Mockito.when(container.getNavigatorSettings()).thenReturn(Mockito.mock(DBNBrowseSettings.class));
        DBNModel model = Mockito.mock(DBNModel.class);
        DBNDataSource parent = Mockito.mock(DBNDataSource.class);
        Mockito.when(parent.getDataSourceContainer()).thenReturn(container);
        Mockito.when(parent.getModel()).thenReturn(model);

        AbstractDescriptor descriptor = Mockito.mock(AbstractDescriptor.class);
        DBXTreeItem rootMeta = new DBXTreeItem(
            descriptor, null, null, "root", "items", false, true, false, false, false, null, null);
        DBXTreeItem itemMeta = new DBXTreeItem(
            descriptor, rootMeta, null, "items", "items", false, true, false, false, false, null, null);
        new DBXTreeItem(
            descriptor, itemMeta, null, "children", "items", false, true, false, false, false, null, null);

        CachedObject visibleLeaf = cachedObject("keep_leaf", List.of());
        CachedObject hiddenLeaf = cachedObject("hide_leaf", List.of());
        List<CachedObject> cachedLeaves = List.of(visibleLeaf, hiddenLeaf);
        CachedObject loadedObject = cachedObject("keep_loaded", cachedLeaves);
        CachedObject unloadedObject = cachedObject("keep_unloaded", List.of(visibleLeaf));
        CachedObject hiddenObject = cachedObject("hide_branch", List.of());
        List<CachedObject> cachedItems = List.of(loadedObject, unloadedObject, hiddenObject);
        CachedObject rootObject = cachedObject("root", cachedItems);
        DBNDatabaseNode node = new DBNDatabaseItem(parent, rootMeta, rootObject);
        VoidProgressMonitor monitor = new VoidProgressMonitor();

        // VoidProgressMonitor checks shutdown state even when no application is running.
        try (MockedStatic<DBWorkbench> workbench = Mockito.mockStatic(
            DBWorkbench.class, Mockito.withSettings().mockMaker(MockMakers.INLINE))) {
            workbench.when(DBWorkbench::getPlatform).thenReturn(Mockito.mock(DBPPlatform.class));

            DBNDatabaseNode[] initialChildren = node.getChildren(monitor);
            Assertions.assertNotNull(initialChildren);
            Assertions.assertEquals(3, initialChildren.length);
            DBNDatabaseNode loadedNode = initialChildren[0];
            DBNDatabaseNode unloadedNode = initialChildren[1];
            DBNDatabaseNode[] initialLeaves = loadedNode.getChildren(monitor);
            Assertions.assertNotNull(initialLeaves);
            Assertions.assertEquals(2, initialLeaves.length);
            Assertions.assertFalse(node.isFiltered());
            Assertions.assertFalse(loadedNode.isFiltered());
            Assertions.assertTrue(unloadedNode.needsInitialization());

            Predicate<DBSObject> predicate = object -> object.getName().startsWith("keep_");
            Mockito.when(filterProvider.createFilter(Mockito.any(), Mockito.any(), Mockito.isNull())).thenReturn(predicate);
            node.updateChildrenOrder(true);

            Assertions.assertArrayEquals(new DBNDatabaseNode[] {loadedNode, unloadedNode}, node.getChildren(monitor));
            Assertions.assertSame(loadedNode, node.getChildren(monitor)[0]);
            Assertions.assertArrayEquals(new DBNDatabaseNode[] {initialLeaves[0]}, loadedNode.getChildren(monitor));
            Assertions.assertSame(initialLeaves[0], loadedNode.getChildren(monitor)[0]);
            Assertions.assertTrue(node.isFiltered());
            Assertions.assertTrue(loadedNode.isFiltered());
            Assertions.assertTrue(unloadedNode.needsInitialization());
            Assertions.assertNull(unloadedNode.getChildNodes());
            Mockito.verify(model, Mockito.atLeastOnce()).fireNodeUpdate(node, node, DBNEvent.NodeChange.REFRESH);

            // Removing the display predicate must reveal the same cached metadata again.
            Mockito.when(filterProvider.createFilter(Mockito.any(), Mockito.any(), Mockito.isNull())).thenReturn(null);
            node.updateChildrenOrder(true);

            DBNDatabaseNode[] restoredChildren = node.getChildren(monitor);
            Assertions.assertEquals(3, restoredChildren.length);
            Assertions.assertSame(loadedNode, restoredChildren[0]);
            Assertions.assertSame(unloadedNode, restoredChildren[1]);
            Assertions.assertSame(hiddenObject, restoredChildren[2].getObject());
            DBNDatabaseNode[] restoredLeaves = loadedNode.getChildren(monitor);
            Assertions.assertEquals(2, restoredLeaves.length);
            Assertions.assertSame(initialLeaves[0], restoredLeaves[0]);
            Assertions.assertSame(hiddenLeaf, restoredLeaves[1].getObject());
            Assertions.assertFalse(node.isFiltered());
            Assertions.assertFalse(loadedNode.isFiltered());
            Assertions.assertTrue(unloadedNode.needsInitialization());
            Assertions.assertNull(unloadedNode.getChildNodes());

            // Loaded collection getters are reread, but metadata refresh and lazy expansion are not requested.
            Mockito.verify(rootObject, Mockito.times(3)).getItems();
            Mockito.verify(loadedObject, Mockito.times(3)).getItems();
            Mockito.verify(unloadedObject, Mockito.never()).getItems();
            Mockito.verify(hiddenObject, Mockito.never()).getItems();
            for (CachedObject object : List.of(rootObject, loadedObject, unloadedObject, hiddenObject, visibleLeaf, hiddenLeaf)) {
                Mockito.verify(object, Mockito.never()).refreshObject(Mockito.any());
            }
            Assertions.assertSame(cachedItems, rootObject.getItems());
            Assertions.assertSame(cachedLeaves, loadedObject.getItems());
        }
    }

    @NotNull
    private static CachedObject cachedObject(@NotNull String name, @NotNull List<CachedObject> items) {
        CachedObject object = Mockito.mock(CachedObject.class);
        Mockito.when(object.getName()).thenReturn(name);
        Mockito.when(object.isPersisted()).thenReturn(true);
        Mockito.when(object.getItems()).thenReturn(items);
        return object;
    }

    // Public so DBXTreeItem's normal property reader can discover and invoke the getter.
    public interface CachedObject extends DBPRefreshableObject {
        @NotNull
        List<CachedObject> getItems();
    }
}

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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.navigator.meta.DBXTreeNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressListener;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

public class DBNDatabaseNodeSelectionTest {
    @Test
    public void checksUnloadedFolderWithoutLoadingChildren() {
        TestNode folder = new TestNode(null);
        DBNDatabaseNodeSelection selection = new DBNDatabaseNodeSelection(node -> true);

        selection.setChecked(folder, true);

        Assertions.assertTrue(selection.isChecked(folder));
        Assertions.assertFalse(selection.isGrayed(folder));
        Assertions.assertEquals(List.of(folder), selection.getSelectedNodes());
    }

    @Test
    public void newlyLoadedChildrenInheritSelection() {
        TestNode folder = new TestNode(null);
        DBNDatabaseNodeSelection selection = new DBNDatabaseNodeSelection(node -> true);
        selection.setChecked(folder, true);

        TestNode table = new TestNode(folder);
        folder.setCachedChildren(table);

        Assertions.assertTrue(selection.isChecked(table));
        Assertions.assertFalse(selection.isGrayed(table));
        Assertions.assertEquals(List.of(folder), selection.getSelectedNodes());
    }

    @Test
    public void unchecksNestedTableUsingOnlyCachedSiblings() {
        TestNode schema = new TestNode(null);
        TestNode tables = new TestNode(schema);
        TestNode views = new TestNode(schema);
        TestNode firstTable = new TestNode(tables);
        TestNode secondTable = new TestNode(tables);
        schema.setCachedChildren(tables, views);
        tables.setCachedChildren(firstTable, secondTable);
        DBNDatabaseNodeSelection selection = new DBNDatabaseNodeSelection(node -> true);
        selection.setChecked(schema, true);

        selection.setChecked(firstTable, false);

        Assertions.assertFalse(selection.isChecked(firstTable));
        Assertions.assertTrue(selection.isChecked(secondTable));
        Assertions.assertTrue(selection.isChecked(views));
        Assertions.assertTrue(selection.isGrayed(schema));
        Assertions.assertTrue(selection.isGrayed(tables));
        Assertions.assertEquals(Set.of(secondTable, views), Set.copyOf(selection.getSelectedNodes()));
    }

    @Test
    public void selectingAncestorReplacesIndividualSelections() {
        TestNode folder = new TestNode(null);
        TestNode table = new TestNode(folder);
        DBNDatabaseNodeSelection selection = new DBNDatabaseNodeSelection(node -> true);
        selection.setChecked(table, true);
        Assertions.assertTrue(selection.isGrayed(folder));

        selection.setChecked(folder, true);

        Assertions.assertEquals(List.of(folder), selection.getSelectedNodes());
        Assertions.assertFalse(selection.isGrayed(folder));
    }

    @Test
    public void uncheckingAncestorClearsDescendantSelections() {
        TestNode folder = new TestNode(null);
        TestNode table = new TestNode(folder);
        DBNDatabaseNodeSelection selection = new DBNDatabaseNodeSelection(node -> true);
        selection.setChecked(table, true);

        selection.setChecked(folder, false);

        Assertions.assertTrue(selection.getSelectedNodes().isEmpty());
        Assertions.assertFalse(selection.isChecked(table));
        Assertions.assertFalse(selection.isGrayed(folder));
    }

    @Test
    public void uncheckingLastChildClearsAncestorState() {
        TestNode folder = new TestNode(null);
        TestNode table = new TestNode(folder);
        folder.setCachedChildren(table);
        DBNDatabaseNodeSelection selection = new DBNDatabaseNodeSelection(node -> true);
        selection.setChecked(folder, true);

        selection.setChecked(table, false);

        Assertions.assertTrue(selection.getSelectedNodes().isEmpty());
        Assertions.assertFalse(selection.isChecked(folder));
    }

    @Test
    public void ignoresUnsupportedSiblingsWhenSplittingSelection() {
        TestNode schema = new TestNode(null);
        TestNode tables = new TestNode(schema);
        TestNode procedures = new TestNode(schema);
        schema.setCachedChildren(tables, procedures);
        DBNDatabaseNodeSelection selection = new DBNDatabaseNodeSelection(node -> node != procedures);
        selection.setChecked(schema, true);

        selection.setChecked(tables, false);

        Assertions.assertTrue(selection.getSelectedNodes().isEmpty());
        Assertions.assertFalse(selection.isChecked(procedures));
    }

    @Test
    public void keepsAncestorGrayedUntilLastSelectionIsRemoved() {
        TestNode folder = new TestNode(null);
        TestNode firstTable = new TestNode(folder);
        TestNode secondTable = new TestNode(folder);
        DBNDatabaseNodeSelection selection = new DBNDatabaseNodeSelection(node -> true);
        selection.setChecked(firstTable, true);
        selection.setChecked(secondTable, true);

        selection.setChecked(firstTable, false);

        Assertions.assertTrue(selection.isGrayed(folder));
        Assertions.assertEquals(List.of(secondTable), selection.getSelectedNodes());

        selection.setChecked(secondTable, false);

        Assertions.assertFalse(selection.isGrayed(folder));
        Assertions.assertFalse(selection.isChecked(folder));
    }

    @Test
    public void checkingAllCachedChildrenFullyChecksFolder() {
        TestNode folder = new TestNode(null);
        TestNode firstTable = new TestNode(folder);
        TestNode secondTable = new TestNode(folder);
        folder.setCachedChildren(firstTable, secondTable);
        DBNDatabaseNodeSelection selection = new DBNDatabaseNodeSelection(node -> true);
        selection.setChecked(firstTable, true);
        Assertions.assertTrue(selection.isGrayed(folder));

        selection.setChecked(secondTable, true);

        Assertions.assertFalse(selection.isGrayed(folder));
        Assertions.assertEquals(Set.of(firstTable, secondTable), Set.copyOf(selection.getSelectedNodes()));

        selection.setChecked(firstTable, false);
        Assertions.assertTrue(selection.isGrayed(folder));
        selection.setChecked(firstTable, true);

        Assertions.assertFalse(selection.isGrayed(folder));
        Assertions.assertEquals(Set.of(firstTable, secondTable), Set.copyOf(selection.getSelectedNodes()));
    }

    @Test
    public void ignoresUnsupportedChildrenWhenFullyCheckingFolder() {
        TestNode folder = new TestNode(null);
        TestNode table = new TestNode(folder);
        TestNode unsupported = new TestNode(folder);
        folder.setCachedChildren(table, unsupported);
        DBNDatabaseNodeSelection selection = new DBNDatabaseNodeSelection(node -> node != unsupported);

        selection.setChecked(table, true);

        Assertions.assertFalse(selection.isGrayed(folder));
        Assertions.assertEquals(List.of(table), selection.getSelectedNodes());
    }

    private static class TestNode extends DBNDatabaseNode {
        private TestNode(@Nullable DBNDatabaseNode parent) {
            super(parent);
        }

        private void setCachedChildren(@NotNull DBNDatabaseNode... children) {
            childNodes = children;
        }

        @NotNull
        @Override
        public DBNDatabaseNode[] getChildren(@NotNull DBRProgressMonitor monitor) {
            return Assertions.fail("Selection must not load children");
        }

        @Override
        public boolean initializeNode(@Nullable DBRProgressMonitor monitor, @Nullable DBRProgressListener onFinish) {
            return Assertions.fail("Selection must not initialize nodes");
        }

        @Nullable
        @Override
        public DBSObject getObject() {
            return null;
        }

        @Nullable
        @Override
        public Object getValueObject() {
            return null;
        }

        @NotNull
        @Override
        public DBXTreeNode getMeta() {
            return Assertions.fail("Selection must not read metadata");
        }

        @Override
        protected boolean reloadObject(@NotNull DBRProgressMonitor monitor, @NotNull DBSObject object) {
            return Assertions.fail("Selection must not reload objects");
        }
    }
}

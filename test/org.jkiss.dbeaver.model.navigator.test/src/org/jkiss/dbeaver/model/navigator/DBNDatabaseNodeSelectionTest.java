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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Set;

public class DBNDatabaseNodeSelectionTest {
    @Test
    public void checksUnloadedFolderWithoutReadingChildren() {
        DBNDatabaseNode folder = createNode(null);
        DBNDatabaseNodeSelection selection = new DBNDatabaseNodeSelection(node -> true);

        selection.setChecked(folder, true);

        Assertions.assertTrue(selection.isChecked(folder));
        Assertions.assertFalse(selection.isGrayed(folder));
        Assertions.assertEquals(List.of(folder), selection.getSelectedNodes());
        Mockito.verify(folder, Mockito.never()).getChildNodes();
    }

    @Test
    public void newlyLoadedChildrenInheritSelection() {
        DBNDatabaseNode folder = createNode(null);
        DBNDatabaseNodeSelection selection = new DBNDatabaseNodeSelection(node -> true);
        selection.setChecked(folder, true);

        DBNDatabaseNode table = createNode(folder);

        Assertions.assertTrue(selection.isChecked(table));
        Assertions.assertFalse(selection.isGrayed(table));
        Assertions.assertEquals(List.of(folder), selection.getSelectedNodes());
    }

    @Test
    public void unchecksNestedTableUsingOnlyCachedSiblings() {
        DBNDatabaseNode schema = createNode(null);
        DBNDatabaseNode tables = createNode(schema);
        DBNDatabaseNode views = createNode(schema);
        DBNDatabaseNode firstTable = createNode(tables);
        DBNDatabaseNode secondTable = createNode(tables);
        Mockito.when(schema.getChildNodes()).thenReturn(new DBNDatabaseNode[]{tables, views});
        Mockito.when(tables.getChildNodes()).thenReturn(new DBNDatabaseNode[]{firstTable, secondTable});
        DBNDatabaseNodeSelection selection = new DBNDatabaseNodeSelection(node -> true);
        selection.setChecked(schema, true);
        Mockito.clearInvocations(schema, tables, views, firstTable, secondTable);

        selection.setChecked(firstTable, false);

        Assertions.assertFalse(selection.isChecked(firstTable));
        Assertions.assertTrue(selection.isChecked(secondTable));
        Assertions.assertTrue(selection.isChecked(views));
        Assertions.assertTrue(selection.isGrayed(schema));
        Assertions.assertTrue(selection.isGrayed(tables));
        Assertions.assertEquals(Set.of(secondTable, views), Set.copyOf(selection.getSelectedNodes()));
        Mockito.verify(schema, Mockito.atLeastOnce()).getChildNodes();
        Mockito.verify(tables, Mockito.atLeastOnce()).getChildNodes();
        Mockito.verifyNoMoreInteractions(Mockito.ignoreStubs(schema, tables, views, firstTable, secondTable));
    }

    @Test
    public void selectingAncestorReplacesIndividualSelections() {
        DBNDatabaseNode folder = createNode(null);
        DBNDatabaseNode table = createNode(folder);
        DBNDatabaseNodeSelection selection = new DBNDatabaseNodeSelection(node -> true);
        selection.setChecked(table, true);
        Assertions.assertTrue(selection.isGrayed(folder));

        selection.setChecked(folder, true);

        Assertions.assertEquals(List.of(folder), selection.getSelectedNodes());
        Assertions.assertFalse(selection.isGrayed(folder));
    }

    @Test
    public void uncheckingAncestorClearsDescendantSelections() {
        DBNDatabaseNode folder = createNode(null);
        DBNDatabaseNode table = createNode(folder);
        DBNDatabaseNodeSelection selection = new DBNDatabaseNodeSelection(node -> true);
        selection.setChecked(table, true);

        selection.setChecked(folder, false);

        Assertions.assertTrue(selection.getSelectedNodes().isEmpty());
        Assertions.assertFalse(selection.isChecked(table));
        Assertions.assertFalse(selection.isGrayed(folder));
    }

    @Test
    public void uncheckingLastChildClearsAncestorState() {
        DBNDatabaseNode folder = createNode(null);
        DBNDatabaseNode table = createNode(folder);
        Mockito.when(folder.getChildNodes()).thenReturn(new DBNDatabaseNode[]{table});
        DBNDatabaseNodeSelection selection = new DBNDatabaseNodeSelection(node -> true);
        selection.setChecked(folder, true);

        selection.setChecked(table, false);

        Assertions.assertTrue(selection.getSelectedNodes().isEmpty());
        Assertions.assertFalse(selection.isChecked(folder));
    }

    @Test
    public void ignoresUnsupportedSiblingsWhenSplittingSelection() {
        DBNDatabaseNode schema = createNode(null);
        DBNDatabaseNode tables = createNode(schema);
        DBNDatabaseNode procedures = createNode(schema);
        Mockito.when(schema.getChildNodes()).thenReturn(new DBNDatabaseNode[]{tables, procedures});
        DBNDatabaseNodeSelection selection = new DBNDatabaseNodeSelection(node -> node != procedures);
        selection.setChecked(schema, true);

        selection.setChecked(tables, false);

        Assertions.assertTrue(selection.getSelectedNodes().isEmpty());
        Assertions.assertFalse(selection.isChecked(procedures));
    }

    @Test
    public void keepsAncestorGrayedUntilLastSelectionIsRemoved() {
        DBNDatabaseNode folder = createNode(null);
        DBNDatabaseNode firstTable = createNode(folder);
        DBNDatabaseNode secondTable = createNode(folder);
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
        DBNDatabaseNode folder = createNode(null);
        DBNDatabaseNode firstTable = createNode(folder);
        DBNDatabaseNode secondTable = createNode(folder);
        Mockito.when(folder.getChildNodes()).thenReturn(new DBNDatabaseNode[]{firstTable, secondTable});
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
        DBNDatabaseNode folder = createNode(null);
        DBNDatabaseNode table = createNode(folder);
        DBNDatabaseNode unsupported = createNode(folder);
        Mockito.when(folder.getChildNodes()).thenReturn(new DBNDatabaseNode[]{table, unsupported});
        DBNDatabaseNodeSelection selection = new DBNDatabaseNodeSelection(node -> node != unsupported);

        selection.setChecked(table, true);

        Assertions.assertFalse(selection.isGrayed(folder));
        Assertions.assertEquals(List.of(table), selection.getSelectedNodes());
    }

    @NotNull
    private static DBNDatabaseNode createNode(@Nullable DBNDatabaseNode parent) {
        DBNDatabaseNode node = Mockito.mock(DBNDatabaseNode.class);
        Mockito.when(node.getParentNode()).thenReturn(parent);
        return node;
    }
}

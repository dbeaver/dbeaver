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
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectSelection;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

class DBSObjectSelectionTest {
    @Test
    void checksUnloadedSchemaWithoutReadingChildren() {
        TestObject schema = new TestObject("schema", null);
        TestObject table = new TestObject("table", schema);
        DBSObjectSelection selection = new DBSObjectSelection(DBSObject::getParentObject,
            object -> Assertions.fail("Checking an unloaded container must not request its children"));

        selection.setChecked(schema, true);

        Assertions.assertTrue(selection.isChecked(table));
        Assertions.assertTrue(selection.isExplicitlyChecked(schema));
        Assertions.assertTrue(selection.isImplicitlyChecked(table));
        Assertions.assertFalse(selection.isExplicitlyChecked(table));
        Assertions.assertFalse(selection.isImplicitlyChecked(schema));
        Assertions.assertFalse(selection.isGrayed(schema));
        Assertions.assertEquals(List.of(schema), selection.getSelectedObjects());
    }

    @Test
    void uncheckingTablePreservesUnloadedSiblingSchemas() {
        TestObject catalog = new TestObject("catalog", null);
        TestObject schema = new TestObject("schema", catalog);
        TestObject otherSchema = new TestObject("otherSchema", catalog);
        TestObject table = new TestObject("table", schema);
        TestObject view = new TestObject("view", schema);
        Map<DBSObject, List<DBSObject>> cached = Map.of(catalog, List.of(schema, otherSchema), schema, List.of(table, view));
        DBSObjectSelection selection = new DBSObjectSelection(DBSObject::getParentObject, object -> {
            Assertions.assertTrue(cached.containsKey(object), "Do not read an unopened sibling's children");
            return cached.get(object);
        });
        selection.setChecked(catalog, true);

        selection.setChecked(table, false);

        Assertions.assertFalse(selection.isChecked(table));
        Assertions.assertTrue(selection.isChecked(view));
        Assertions.assertTrue(selection.isChecked(otherSchema));
        Assertions.assertEquals(Set.of(view, otherSchema), Set.copyOf(selection.getSelectedObjects()));
    }

    @Test
    void selectingAllCurrentTablesDoesNotSelectSchemaOrFutureTables() {
        TestObject schema = new TestObject("schema", null);
        TestObject table = new TestObject("table", schema);
        TestObject view = new TestObject("view", schema);
        DBSObjectSelection selection = new DBSObjectSelection(DBSObject::getParentObject,
            object -> object == schema ? List.of(table, view) : List.of());

        selection.setChecked(table, true);
        Assertions.assertTrue(selection.isGrayed(schema));
        selection.setChecked(view, true);
        Assertions.assertTrue(selection.isGrayed(schema));
        Assertions.assertFalse(selection.isExplicitlyChecked(schema));
        Assertions.assertFalse(selection.isImplicitlyChecked(schema));
        Assertions.assertTrue(selection.isExplicitlyChecked(table));
        Assertions.assertTrue(selection.isExplicitlyChecked(view));
        Assertions.assertFalse(selection.isImplicitlyChecked(table));
        Assertions.assertFalse(selection.isChecked(new TestObject("futureTable", schema)));
        Assertions.assertEquals(Set.of(table, view), Set.copyOf(selection.getSelectedObjects()));
        selection.setChecked(table, false);
        Assertions.assertTrue(selection.isGrayed(schema));
        Assertions.assertFalse(selection.isChecked(table));
    }

    @Test
    void splittingSelectedCatalogRespectsSystemObjectVisibility() {
        TestObject catalog = new TestObject("catalog", null);
        TestObject schema = new TestObject("schema", catalog);
        TestObject otherSchema = new TestObject("otherSchema", catalog);
        TestObject systemSchema = new TestObject("systemSchema", catalog);
        TestObject table = new TestObject("table", schema);
        TestObject view = new TestObject("view", schema);
        TestObject systemTable = new TestObject("systemTable", schema);
        Map<DBSObject, List<DBSObject>> cached = Map.of(
            catalog, List.of(schema, otherSchema, systemSchema), schema, List.of(table, view, systemTable));
        Set<DBSObject> systemObjects = Set.of(systemSchema, systemTable);
        // The tree supplies cached siblings filtered by the current Show system objects setting.
        AtomicBoolean showSystemObjects = new AtomicBoolean(true);
        DBSObjectSelection selection = new DBSObjectSelection(DBSObject::getParentObject,
            object -> cached.getOrDefault(object, List.of()).stream()
                .filter(child -> showSystemObjects.get() || !systemObjects.contains(child)).toList());

        selection.setChecked(catalog, true);
        showSystemObjects.set(false);
        selection.setChecked(table, false);

        Assertions.assertEquals(Set.of(otherSchema, view), Set.copyOf(selection.getSelectedObjects()));
        Assertions.assertFalse(selection.isChecked(systemSchema));
        Assertions.assertFalse(selection.isChecked(systemTable));

        selection.setChecked(catalog, true);
        selection.setChecked(schema, false);

        Assertions.assertEquals(List.of(otherSchema), selection.getSelectedObjects());

        showSystemObjects.set(true);
        selection.setChecked(catalog, true);
        selection.setChecked(table, false);

        Assertions.assertEquals(Set.of(otherSchema, systemSchema, view, systemTable),
            Set.copyOf(selection.getSelectedObjects()));

        selection.setChecked(catalog, true);
        selection.setChecked(schema, false);

        Assertions.assertEquals(Set.of(otherSchema, systemSchema), Set.copyOf(selection.getSelectedObjects()));
    }

    @Test
    void selectingSchemaReplacesExplicitTablesAndIncludesFutureTables() {
        TestObject schema = new TestObject("schema", null);
        TestObject table = new TestObject("table", schema);
        TestObject view = new TestObject("view", schema);
        DBSObjectSelection selection = new DBSObjectSelection(DBSObject::getParentObject,
            object -> object == schema ? List.of(table, view) : List.of());
        selection.setChecked(table, true);
        selection.setChecked(view, true);

        selection.setChecked(schema, true);

        Assertions.assertEquals(List.of(schema), selection.getSelectedObjects());
        Assertions.assertTrue(selection.isExplicitlyChecked(schema));
        Assertions.assertFalse(selection.isGrayed(schema));
        Assertions.assertFalse(selection.isExplicitlyChecked(table));
        Assertions.assertTrue(selection.isImplicitlyChecked(table));
        Assertions.assertTrue(selection.isImplicitlyChecked(view));
        Assertions.assertTrue(selection.isImplicitlyChecked(new TestObject("futureTable", schema)));

        selection.setChecked(schema, false);

        Assertions.assertTrue(selection.getSelectedObjects().isEmpty());
        Assertions.assertFalse(selection.isChecked(table));
        Assertions.assertFalse(selection.isImplicitlyChecked(table));
    }

    @Test
    void handlesFlattenedParentPaths() {
        TestObject schema = new TestObject("schema", null);
        TestObject hiddenContainer = new TestObject("hidden", schema);
        TestObject table = new TestObject("table", hiddenContainer);
        TestObject view = new TestObject("view", hiddenContainer);
        DBSObjectSelection selection = new DBSObjectSelection(
            object -> object.getParentObject() == hiddenContainer ? schema : object.getParentObject(),
            object -> object == schema ? List.of(table, view) : List.of());
        selection.setChecked(schema, true);

        selection.setChecked(table, false);

        Assertions.assertEquals(List.of(view), selection.getSelectedObjects());
        Assertions.assertFalse(selection.isChecked(table));
        Assertions.assertTrue(selection.isGrayed(schema));
    }

    @Test
    void replacingAndClearingSelectionRemovesStaleAncestorState() {
        TestObject schema = new TestObject("schema", null);
        TestObject table = new TestObject("table", schema);
        Map<DBSObject, List<DBSObject>> cached = new HashMap<>();
        cached.put(schema, List.of(table));
        DBSObjectSelection selection = new DBSObjectSelection(DBSObject::getParentObject,
            object -> cached.getOrDefault(object, List.of()));
        selection.setChecked(table, true);
        selection.setChecked(schema, true);
        Assertions.assertEquals(List.of(schema), selection.getSelectedObjects());

        selection.clear();

        Assertions.assertTrue(selection.getSelectedObjects().isEmpty());
        Assertions.assertFalse(selection.isChecked(schema));
        Assertions.assertFalse(selection.isChecked(table));
        Assertions.assertFalse(selection.isGrayed(schema));
    }

    private record TestObject(@NotNull String name, @Nullable DBSObject parent) implements DBSObject {
        @NotNull
        @Override
        public String getName() {
            return name;
        }

        @Nullable
        @Override
        public DBSObject getParentObject() {
            return parent;
        }

        @Nullable
        @Override
        public DBPDataSource getDataSource() {
            return null;
        }

        @Nullable
        @Override
        public String getDescription() {
            return null;
        }

        @Override
        public boolean isPersisted() {
            return true;
        }
    }
}

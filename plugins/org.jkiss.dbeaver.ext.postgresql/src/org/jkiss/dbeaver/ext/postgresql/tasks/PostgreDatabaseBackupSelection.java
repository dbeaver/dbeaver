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
package org.jkiss.dbeaver.ext.postgresql.tasks;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreDatabase;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreSchema;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableBase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PostgreDatabaseBackupSelection {
    // a null value selects the entire schema, independently of whether its tables have been loaded
    private final Map<PostgreSchema, Set<PostgreTableBase>> selectedObjects = new LinkedHashMap<>();

    public void clear() {
        selectedObjects.clear();
    }

    public void selectSchema(@NotNull PostgreSchema schema, boolean selected) {
        if (selected) {
            selectedObjects.put(schema, null);
        } else {
            selectedObjects.remove(schema);
        }
    }

    public void selectSchemas(@NotNull Collection<PostgreSchema> schemas, boolean selected) {
        for (PostgreSchema schema : schemas) {
            selectSchema(schema, selected);
        }
    }

    public void selectTables(@NotNull PostgreSchema schema, @NotNull Collection<PostgreTableBase> tables, boolean allTablesSelected) {
        if (tables.isEmpty()) {
            selectSchema(schema, false);
        } else if (allTablesSelected) {
            selectSchema(schema, true);
        } else {
            selectedObjects.put(schema, new LinkedHashSet<>(tables));
        }
    }

    public boolean isSchemaSelected(@NotNull PostgreSchema schema) {
        return selectedObjects.containsKey(schema);
    }

    public boolean isTableSelected(@NotNull PostgreSchema schema, @NotNull PostgreTableBase table) {
        Set<PostgreTableBase> tables = selectedObjects.get(schema);
        return isSchemaSelected(schema) && (tables == null || tables.contains(table));
    }

    public boolean isCompleteBackup(@NotNull Collection<PostgreSchema> schemas) {
        if (schemas.isEmpty() || selectedObjects.size() != schemas.size()) {
            return false;
        }
        for (PostgreSchema schema : schemas) {
            if (!isSchemaSelected(schema) || selectedObjects.get(schema) != null) {
                return false;
            }
        }
        return true;
    }

    @NotNull
    public List<PostgreDatabaseBackupInfo> getExportObjects(@NotNull PostgreDatabase database, boolean completeBackup) {
        List<PostgreDatabaseBackupInfo> objects = new ArrayList<>();
        if (completeBackup && !selectedObjects.isEmpty()) {
            objects.add(new PostgreDatabaseBackupInfo(database, new ArrayList<>(selectedObjects.keySet()), null));
            return objects;
        }
        List<PostgreSchema> entireSchemas = new ArrayList<>();
        for (Map.Entry<PostgreSchema, Set<PostgreTableBase>> entry : selectedObjects.entrySet()) {
            if (entry.getValue() == null) {
                entireSchemas.add(entry.getKey());
            } else {
                // pg_dump ignores -n when -t is present, so partial schemas need separate invocations
                objects.add(new PostgreDatabaseBackupInfo(database, List.of(entry.getKey()), new ArrayList<>(entry.getValue())));
            }
        }
        if (!entireSchemas.isEmpty()) {
            objects.add(new PostgreDatabaseBackupInfo(database, entireSchemas, null));
        }
        return objects;
    }
}

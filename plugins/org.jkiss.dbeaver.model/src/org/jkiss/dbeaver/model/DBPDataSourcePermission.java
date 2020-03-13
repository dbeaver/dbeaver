/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.model;

/**
 * Data-source permissions
 */
public enum DBPDataSourcePermission
{
    PERMISSION_EDIT_DATA("edit.data", "Restrict data edit", "Restrict and direct data modifications"),
    PERMISSION_EDIT_METADATA("edit.meta", "Restrict structure edit", "Restrict structure (metadata) changes, like tables create/drop"),
    PERMISSION_EXECUTE_SCRIPTS("edit.execute", "Restrict script execute", "Restruct custom user scripts (SQL) execution"),
    PERMISSION_IMPORT_DATA("import.data", "Restrict data import", "Restrict importing data");

    private final String id;
    private final String label;
    private final String description;

    DBPDataSourcePermission(String id, String label, String description) {
        this.id = id;
        this.label = label;
        this.description = description;
    }

    public String getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public String getDescription() {
        return description;
    }

    public static DBPDataSourcePermission getById(String id) {
        for (DBPDataSourcePermission permission : values()) {
            if (permission.id.equals(id)) {
                return permission;
            }
        }
        throw new IllegalArgumentException("Wrong permission id: " + id);
    }
}

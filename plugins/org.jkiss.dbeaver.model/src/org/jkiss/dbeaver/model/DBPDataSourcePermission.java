/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

import org.jkiss.dbeaver.model.messages.ModelMessages;

/**
 * Data-source permissions
 */
public enum DBPDataSourcePermission
{
    PERMISSION_EDIT_DATA("edit.data", ModelMessages.dbp_permission_edit_data_name, ModelMessages.dbp_permission_edit_data_description),
    PERMISSION_EDIT_METADATA("edit.meta", ModelMessages.dbp_permission_edit_metadata_name, ModelMessages.dbp_permission_edit_metadata_description),
    PERMISSION_EXECUTE_SCRIPTS("edit.execute", ModelMessages.dbp_permission_execute_scripts_name, ModelMessages.dbp_permission_execute_scripts_description),
    PERMISSION_IMPORT_DATA("import.data", ModelMessages.dbp_permission_import_data_name, ModelMessages.dbp_permission_import_data_description);

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

/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.rm;

/**
 * RM constants
 */
public interface RMConstants {
    String PROJECT_CONF_FOLDER = ".configuration";

    String PERMISSION_PROJECT_DATASOURCES_EDIT = "project-datasource-edit";
    String PERMISSION_PROJECT_DATASOURCES_VIEW = "project-datasource-view";

    String PERMISSION_PROJECT_RESOURCE_VIEW = "project-resource-view";
    String PERMISSION_PROJECT_RESOURCE_EDIT = "project-resource-edit";
    String PERMISSION_TASK_MANAGER = "task-manager";


    String PERMISSION_PROJECT_ADMIN = "project-admin";

    // RM admin can create/delete projects. It also can assign project permissions.
    String PERMISSION_RM_ADMIN = "rm-admin";

    String PERMISSION_DRIVER_MANAGER = "driver-manager";

    String PERMISSION_CONFIGURATION_MANAGER = "configuration-manager";
    String PERMISSION_DATABASE_DEVELOPER = "database-developer";
    String PERMISSION_METADATA_EDITOR = "metadata-editor";
    String PERMISSION_SQL_GENERATOR = "sql-generator";

    /**
     * <b>SQL script execution</b>: allows users to execute custom SQL scripts.
     *
     * @since 25.1.4
     */
    String PERMISSION_SQL_SCRIPT_EXECUTION = "permission.sql.script.execution";

    /**
     * <b>Data import</b>: allows users to import data using the Data Editor.
     *
     * @since 25.1.5
     */
    String PERMISSION_DATA_EDITOR_IMPORT = "permission.data-editor.import";

    /**
     * <b>Data copy</b>: allows users to copy data from the Data Editor.
     *
     * @since 25.1.5
     */
    String PERMISSION_DATA_EDITOR_COPY = "permission.data-editor.copy";

    /**
     * <b>Data export</b>: allows users to export data from the Data Editor.
     *
     * @since 25.1.5
     */
    String PERMISSION_DATA_EDITOR_EXPORT = "permission.data-editor.export";

    /**
     * <b>Data edit</b>: allows users to edit data in the Data Editor.
     *
     * @since 25.1.5
     */
    String PERMISSION_DATA_EDITOR_EDITING = "permission.data-editor.editing";
}

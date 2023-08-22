/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
}

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
package org.jkiss.dbeaver.ui;

/**
 * Help context IDs
 */
public interface IHelpContextIds
{

    String CTX_DRIVER_MANAGER = "database-drivers"; //$NON-NLS-1$
    String CTX_DRIVER_EDITOR = "database-drivers"; //$NON-NLS-1$

    String CTX_SQL_EDITOR = "sql-editor"; //$NON-NLS-1$
    String CTX_RESULT_SET_VIEWER  = "result-set-viewer"; //$NON-NLS-1$
    String CTX_DATA_FILTER = "data-filter"; //$NON-NLS-1$
    String CTX_ROW_COLORS = "row-colors"; //$NON-NLS-1$

    String CTX_ENTITY_EDITOR = "entity-editor"; //$NON-NLS-1$
    String CTX_DATABASE_NAVIGATOR = "view-database-navigator"; //$NON-NLS-1$
    String CTX_PROJECT_NAVIGATOR = "view-project-navigator"; //$NON-NLS-1$
    String CTX_PROJECT_EXPLORER = "view-project-explorer"; //$NON-NLS-1$
    String CTX_QUERY_MANAGER = "view-query-manager"; //$NON-NLS-1$
    String CTX_SHELL_PROCESS = "view-shell-process"; //$NON-NLS-1$
    String CTX_SEARCH_RESULTS = "view-search-results"; //$NON-NLS-1$

    String CTX_CON_WIZARD_DRIVER = "con-wizard-driver"; //$NON-NLS-1$
    String CTX_CON_WIZARD_SETTINGS = "con-wizard-settings"; //$NON-NLS-1$
    String CTX_CON_WIZARD_FINAL = "con-wizard-final"; //$NON-NLS-1$

    String CTX_SEARCH_OBJECTS = "search-objects"; //$NON-NLS-1$
    String CTX_EDIT_CONNECTION_EVENTS = "connection-events";
    String CTX_EDIT_CONNECTION_TUNNELS = "connection-tunnels";
    String CTX_EDIT_OBJECT_FILTERS = "object-filters";
}

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
package org.jkiss.dbeaver.ui.navigator.database;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;

public enum DatabaseNavigatorTreeFilterObjectType {
    connection(
        UINavigatorMessages.actions_navigator_search_filter_connection_name,
        UINavigatorMessages.actions_navigator_search_filter_connection_description,
        UIIcon.FILTER_CONNECTIONS,
        true
    ),
    container(
        UINavigatorMessages.actions_navigator_search_filter_container_name,
        UINavigatorMessages.actions_navigator_search_filter_container_description,
        UIIcon.FILTER_CONTAINERS,
        true
    ),
    table(
        UINavigatorMessages.actions_navigator_search_filter_object_name,
        UINavigatorMessages.actions_navigator_search_filter_object_description,
        UIIcon.FILTER_OBJECTS,
        true
    ),
    file(
        UINavigatorMessages.actions_navigator_search_filter_file_name,
        UINavigatorMessages.actions_navigator_search_filter_file_description,
        UIIcon.FILTER_OBJECTS,
        true
    );
    ;

    private final String name;
    private final String description;
    private final DBIcon icon;
    private final boolean databaseObjects;

    DatabaseNavigatorTreeFilterObjectType(@NotNull String name, @NotNull String description, @NotNull DBIcon icon, boolean databaseObjects) {
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.databaseObjects = databaseObjects;
    }

    @NotNull
    public String getName() {
        return name;
    }

    @NotNull
    public String getDescription() {
        return description;
    }

    @NotNull
    public DBIcon getIcon() {
        return icon;
    }

    public boolean isDatabaseObjects() {
        return databaseObjects;
    }
}

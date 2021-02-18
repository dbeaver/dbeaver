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

package org.jkiss.dbeaver.model.runtime.features;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.util.Map;

/**
 * DBeaver feature description
 */
public final class DBRFeature {

    static final DBRFeature ROOT = new DBRFeature("Root", "Root Feature");

    private final DBRFeature parentFeature;
    private String id;
    private final String name;
    private final String description;
    private final String helpURL;
    private final boolean isAbstract;
    private final DBRNotificationDescriptor notificationDefaults;
    private final String commandId;

    private DBRFeature(@NotNull String id, @NotNull String name) {
        this.parentFeature = null;
        this.id = id;
        this.name = name;
        this.description = null;
        this.helpURL = null;
        this.isAbstract = true;
        this.notificationDefaults = null;
        this.commandId = null;
    }

    private DBRFeature(
        @NotNull DBRFeature parentFeature,
        @NotNull String name,
        String description,
        String helpURL,
        boolean isAbstract,
        DBRNotificationDescriptor notificationDefaults,
        String commandId)
    {
        this.parentFeature = parentFeature;
        this.name = name;
        this.description = description;
        this.helpURL = helpURL;
        this.isAbstract = isAbstract;
        this.notificationDefaults = notificationDefaults;
        this.commandId = commandId;
    }

    private DBRFeature(@NotNull DBRFeature parentFeature, @NotNull String name) {
        this(parentFeature, name, null, null, false, null, null);
    }

    public static DBRFeature createCategory(@NotNull String name, String description) {
        return createCategory(ROOT, name, description);
    }

    public static DBRFeature createCategory(@NotNull DBRFeature parentFeature, @NotNull String name, @Nullable String description) {
        return new DBRFeature(parentFeature, name, description, null, true, null, null);
    }

    public static DBRFeature createFeature(@NotNull DBRFeature parentFeature, @NotNull String name) {
        return new DBRFeature(parentFeature, name);
    }

    public static DBRFeature createCommandFeature(@NotNull DBRFeature parentFeature, @NotNull String commandId) {
        return new DBRFeature(parentFeature, commandId, null, null, false, null, commandId);
    }

    public DBRFeature getParentFeature() {
        return parentFeature;
    }

    public String getId() {
        return id;
    }

    void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getHelpURL() {
        return helpURL;
    }

    public boolean isAbstract() {
        return isAbstract;
    }

    public DBRNotificationDescriptor getNotificationDefaults() {
        return notificationDefaults;
    }

    public String getCommandId() {
        return commandId;
    }

    public void use() {
        this.use(null);
    }

    public void use(Map<String, Object> parameters) {
        DBRFeatureRegistry.useFeature(this, parameters);
    }

    @Override
    public String toString() {
        return id + " (" + name + ")";
    }

}
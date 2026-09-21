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
package org.jkiss.dbeaver.model.sync;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;

import java.util.Map;

/**
 * Component whose configuration is a file or a directory inside the workspace or the project.
 */
public class DBPFileSyncUnit implements DBPSyncUnit {

    private final String id;
    private final String name;
    private final String path;
    private final DBPSyncScope scope;
    private final boolean enabledByDefault;

    public DBPFileSyncUnit(
        @NotNull String id,
        @NotNull String path,
        @NotNull DBPSyncScope scope,
        boolean enabledByDefault
    ) {
        this(id, id, path, scope, enabledByDefault);
    }

    public DBPFileSyncUnit(
        @NotNull String id,
        @NotNull String name,
        @NotNull String path,
        @NotNull DBPSyncScope scope,
        boolean enabledByDefault
    ) {
        this.id = id;
        this.name = name;
        this.path = path;
        this.scope = scope;
        this.enabledByDefault = enabledByDefault;
    }

    @NotNull
    @Override
    public String getId() {
        return id;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @NotNull
    @Override
    public DBPSyncScope getScope() {
        return scope;
    }

    @Override
    public boolean isEnabledByDefault() {
        return enabledByDefault;
    }

    @NotNull
    @Override
    public Map<String, byte[]> read(@NotNull DBPSyncTarget target) throws DBException {
        return files(target).read();
    }

    @Override
    public void write(@NotNull DBPSyncTarget target, @NotNull Map<String, byte[]> resources) throws DBException {
        files(target).write(resources);
    }

    @NotNull
    private DBPSyncFiles files(@NotNull DBPSyncTarget target) {
        return new DBPSyncFiles(target.root().resolve(path));
    }
}

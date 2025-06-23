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
package org.jkiss.dbeaver.model.sql.schema;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

public class SQLSchemaConfig {

    @NotNull
    private final String schemaId;

    @NotNull
    private final String createScriptPath;

    @NotNull
    private final String updateScriptPrefix;

    private final int schemaVersionActual;

    private final int schemaVersionObsolete;

    @NotNull
    private final SQLSchemaVersionManager versionManager;

    @NotNull
    private final ClassLoader classLoader;

    @Nullable
    private SQLInitialSchemaFiller initialSchemaFiller;

    public SQLSchemaConfig(
        @NotNull String schemaId,
        @NotNull String createScriptPath,
        @NotNull String updateScriptPrefix,
        int schemaVersionActual,
        int schemaVersionObsolete,
        @NotNull SQLSchemaVersionManager versionManager,
        @NotNull ClassLoader classLoader
    ) {
        this(schemaId, createScriptPath, updateScriptPrefix, schemaVersionActual, schemaVersionObsolete, versionManager, classLoader, null);
    }
    public SQLSchemaConfig(
        @NotNull String schemaId,
        @NotNull String createScriptPath,
        @NotNull String updateScriptPrefix,
        int schemaVersionActual,
        int schemaVersionObsolete,
        @NotNull SQLSchemaVersionManager versionManager,
        @NotNull ClassLoader classLoader,
        @Nullable SQLInitialSchemaFiller initialSchemaFiller
    ) {
        this.schemaId = schemaId;
        this.createScriptPath = createScriptPath;
        this.updateScriptPrefix = updateScriptPrefix;
        this.schemaVersionActual = schemaVersionActual;
        this.schemaVersionObsolete = schemaVersionObsolete;
        this.versionManager = versionManager;
        this.classLoader = classLoader;
        this.initialSchemaFiller = initialSchemaFiller;
    }

    public @NotNull String getSchemaId() {
        return schemaId;
    }

    public @NotNull String getCreateScriptPath() {
        return createScriptPath;
    }

    public @NotNull String getUpdateScriptPrefix() {
        return updateScriptPrefix;
    }

    public int getSchemaVersionActual() {
        return schemaVersionActual;
    }

    public int getSchemaVersionObsolete() {
        return schemaVersionObsolete;
    }

    public @NotNull SQLSchemaVersionManager getVersionManager() {
        return versionManager;
    }

    public @NotNull ClassLoader getClassLoader() {
        return classLoader;
    }

    public @Nullable SQLInitialSchemaFiller getInitialSchemaFiller() {
        return initialSchemaFiller;
    }

    public void setInitialSchemaFiller(@NotNull SQLInitialSchemaFiller initialSchemaFiller) {
        this.initialSchemaFiller = initialSchemaFiller;
    }
}
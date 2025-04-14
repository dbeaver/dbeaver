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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

/**
 * Script source which reads scripts from class loader
 */
public class ClassLoaderScriptSource implements SQLSchemaScriptSource {

    private static final Log log = Log.getLog(ClassLoaderScriptSource.class);
    private final ClassLoader classLoader;
    private final String createScriptPath;
    private final String updateScriptPrefix;

    public ClassLoaderScriptSource(ClassLoader classLoader, String createScriptPath, String updateScriptPrefix) {
        this.classLoader = classLoader;
        this.createScriptPath = createScriptPath;
        this.updateScriptPrefix = updateScriptPrefix;
    }

    @NotNull
    @Override
    public Reader openSchemaCreateScript(@NotNull DBRProgressMonitor monitor) throws IOException, DBException {
        InputStream resource = classLoader.getResourceAsStream(createScriptPath);
        if (resource == null) {
            throw new IOException("Resource '" + createScriptPath + "' not found in " + this.classLoader.getClass().getName());
        }
        log.info("Reading migration file: '" + createScriptPath + "'");
        return new InputStreamReader(resource);
    }

    @Nullable
    @Override
    public Reader openSchemaUpdateScript(
        @NotNull DBRProgressMonitor monitor,
        int versionNumber,
        @Nullable String specificPrefix
    ) throws IOException, DBException {

        String migrationFileNameWithSpecificPrefix = updateScriptPrefix + versionNumber + "_" + specificPrefix + ".sql";
        InputStream resource = classLoader.getResourceAsStream(migrationFileNameWithSpecificPrefix);
        if (resource != null) {
            log.info("Reading migration file: '" + migrationFileNameWithSpecificPrefix + "'");
            return new InputStreamReader(resource);
        }

        String migrationFileName = updateScriptPrefix + versionNumber + ".sql";
        resource = classLoader.getResourceAsStream(migrationFileName);
        if (resource != null) {
            log.info("Reading migration file: '" + migrationFileName + "'");
            return new InputStreamReader(resource);
        }
        return null;
    }
}

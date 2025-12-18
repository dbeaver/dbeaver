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
import java.util.List;

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
    public Reader openSchemaCreateScript(
        @NotNull DBRProgressMonitor monitor,
        @Nullable String specificPrefix
    ) throws IOException, DBException {
        List<String> attemptedPaths = specificPrefix == null ?
            List.of(createScriptPath + ".sql") :
            List.of(
                createScriptPath + "_" + specificPrefix + ".sql",
                createScriptPath + ".sql"
            );

        Reader reader = findScript(attemptedPaths.toArray(new String[0]));
        if (reader == null) {
            throw new IOException("Create script not found. Searched paths: " + String.join(", ", attemptedPaths));
        }

        return reader;
    }

    @Nullable
    @Override
    public Reader openSchemaUpdateScript(
        @NotNull DBRProgressMonitor monitor,
        int versionNumber,
        @Nullable String specificPrefix
    ) throws IOException, DBException {
        List<String> attemptedPaths = specificPrefix == null ?
            List.of(updateScriptPrefix + versionNumber + ".sql") :
            List.of(
                updateScriptPrefix + versionNumber + "_" + specificPrefix + ".sql",
                updateScriptPrefix + versionNumber + ".sql"
            );

        return findScript(attemptedPaths.toArray(new String[0]));
    }

    @Nullable
    public Reader findScript(
        String... paths
    ) {
        for (String path : paths) {
            InputStream resource = classLoader.getResourceAsStream(path);
            if (resource != null) {
                log.debug("Reading script file: '" + path + "'");
                return new InputStreamReader(resource);
            }
        }
        return null;
    }
}

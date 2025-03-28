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

package org.jkiss.dbeaver.model.connection;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.nio.file.Path;
import java.util.List;

/**
 * Driver loader. Contains class loader which loads all necessary dependencies.
 * Each driver may have several loaders.
 * Primary loader is a singleton and the default.
 * Additional loaders may be created for specific auth model or connection configuration.
 */
public interface DBPDriverLoader {

    @NotNull
    String getLoaderId();

    @NotNull
    List<DBPDriverLibraryProvider> getLibraryProviders();

    @Nullable
    ClassLoader getClassLoader();

    @NotNull
    <T> T getDriverInstance(@NotNull DBRProgressMonitor monitor) throws DBException;

    void loadDriver(DBRProgressMonitor monitor) throws DBException;

    /**
     * Flag that shows if a driver needs external dependencies (f.e. not all files are present).
     */
    boolean needsExternalDependencies();

    /**
     * Validates driver library files presence and download them if needed without creating a driver instance
     */
    void validateFilesPresence(@NotNull DBRProgressMonitor monitor);

    /**
     * Indicates whether the driver library files are installed.
     */
    boolean isDriverInstalled();

    /**
     * Downloads driver library files from external resources if it is possible.
     */
    boolean downloadDriverLibraries(@NotNull DBRProgressMonitor monitor, boolean resetVersions);

    boolean resolveDriverFiles(Path targetFileLocation);

}

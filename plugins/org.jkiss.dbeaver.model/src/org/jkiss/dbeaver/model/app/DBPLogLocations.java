/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.app;

import java.io.File;
import java.util.List;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

/**
 * Locations of app's log files.
 */
public interface DBPLogLocations {
    /**
     * Returns a file representing the current debug log file, or {@code null} if it is turned off.
     * 
     * @return debug log file or {@code null} if it is turned off
     */
    @Nullable
    File getDebugLog();

    /**
     * Returns a file representing the folder with the current debug log file, or {@code null} if it is turned off.
     * 
     * @return debug log folder or {@code null} if it is turned off
     */
    @Nullable
    default File getDebugLogFolder() {
        File debugLog = getDebugLog();
        if (debugLog == null) {
            return null;
        }
        return debugLog.getParentFile();
    }

    /**
     * Suggest a name for a new file with a debug log backup.
     * 
     * @return file with a new name for a backup of a debug log file
     */
    @Nullable
    File proposeDebugLogRotation();

    /**
     * Return all debug log files. The list will be empty if debug logs are disabled.
     * 
     * @return a list with all log files
     */
    @NotNull
    List<File> getDebugLogFiles();
}

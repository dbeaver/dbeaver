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
package org.jkiss.dbeaver.model.connection;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.io.File;

/**
 * LocalNativeClientLocation
 */
public class LocalNativeClientLocation implements DBPNativeClientLocation {
    private final String id;
    private final File path;
    private final String displayName;

    public LocalNativeClientLocation(String id, File path, String displayName) {
        this.id = id;
        this.path = path;
        this.displayName = displayName;
    }

    public LocalNativeClientLocation(String id, String path, String displayName) {
        this(id, new File(path != null ? path : id), displayName);
    }

    private LocalNativeClientLocation(String id, @NotNull File path) {
        this(id, path, path.getAbsolutePath());
    }

    public LocalNativeClientLocation(String id, String path) {
        this(id, new File(path != null ? path : id));
    }

    @NotNull
    @Override
    public String getName() {
        return id;
    }

    @NotNull
    @Override
    public File getPath() {
        return path;
    }

    @NotNull
    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean validateFilesPresence(DBRProgressMonitor progressMonitor) {
        return true;
    }

    @Override
    public String toString() {
        File path = getPath();
        return "Local: " + path.getAbsolutePath();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof DBPNativeClientLocation otherNativeClientLocation) {
            return this.path.equals(otherNativeClientLocation.getPath());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }
}

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
package org.jkiss.dbeaver.model.fs;

import org.jkiss.code.NotNull;

import java.io.IOException;
import java.nio.file.Path;

public final class DBFPath implements AutoCloseable {

    private final Path path;
    private final boolean ownsFileSystem;

    private DBFPath(Path path, boolean ownsFileSystem) {
        this.path = path;
        this.ownsFileSystem = ownsFileSystem;
    }

    public static DBFPath create(@NotNull Path path) {
        return new DBFPath(path, false);
    }

    public static DBFPath createExclusive(@NotNull Path path) {
        return new DBFPath(path, true);
    }

    public Path path() {
        return path;
    }

    @Override
    public void close() throws IOException {
        if (ownsFileSystem) {
            path.getFileSystem().close();
        }
    }

    @Override
    public String toString() {
        return path.toString();
    }

}
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
package org.jkiss.dbeaver.model.tracking.sync.core;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Resource set addressed by a single path: either one file or all files of a directory.
 */
public record DDSyncFiles(
    @NotNull Path root
) {
    private static final Log log = Log.getLog(DDSyncFiles.class);

    @NotNull
    public Map<String, byte[]> read() throws DBException {
        try {
            if (Files.isDirectory(root)) {
                Map<String, byte[]> resources = new LinkedHashMap<>();
                try (Stream<Path> list = Files.list(root)) {
                    for (Path file : list.filter(Files::isRegularFile).toList()) {
                        resources.put(file.getFileName().toString(), Files.readAllBytes(file));
                    }
                }
                return resources;
            }
            if (Files.isRegularFile(root)) {
                return Map.of("", Files.readAllBytes(root));
            }
            return Map.of();
        } catch (IOException e) {
            throw new DBException("Error reading " + root, e);
        }
    }

    public void write(@NotNull Map<String, byte[]> resources) throws DBException {
        for (Map.Entry<String, byte[]> resource : resources.entrySet()) {
            Path path = resolve(resource.getKey());
            if (path == null) {
                log.debug("Skip invalid resource name '" + resource.getKey() + "' for " + root);
                continue;
            }
            try {
                Files.createDirectories(path.getParent());
                Files.write(path, resource.getValue());
            } catch (IOException e) {
                throw new DBException("Error writing " + path, e);
            }
        }
    }

    @Nullable
    private Path resolve(@NotNull String name) {
        if (name.isEmpty()) {
            return root;
        }
        if (name.contains("/") || name.contains("..")) {
            return null;
        }
        return root.resolve(name);
    }
}

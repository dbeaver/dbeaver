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
package org.jkiss.dbeaver.tools.configuration;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * A file visitor that copies files from a source directory to a target directory, preserving the directory structure.
 */
final class CopyingFileVisitor extends SimpleFileVisitor<Path> {
    private static final Log log = Log.getLog(CopyingFileVisitor.class);

    private final Path sourceRoot;
    private final Path targetRoot;

    /**
     * Constructs a new CopyingFileVisitor with the specified source and target paths.
     *
     * @param sourceRoot the source root directory from which files will be copied
     * @param targetRoot the target root directory to which files will be copied
     */
    CopyingFileVisitor(@NotNull Path sourceRoot, @NotNull Path targetRoot) {
        this.sourceRoot = sourceRoot;
        this.targetRoot = targetRoot;
    }

    @NotNull
    @Override
    public FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) throws IOException {
        var result = resolveTargetPath(file);
        log.debug("Copying file " + result);
        Files.createDirectories(result.getParent());
        Files.copy(file, result, StandardCopyOption.REPLACE_EXISTING);
        return FileVisitResult.CONTINUE;
    }

    @NotNull
    private Path resolveTargetPath(@NotNull Path sourcePath) {
        var relative = sourceRoot.relativize(sourcePath);
        var target = targetRoot;
        for (Path element : relative) {
            target = target.resolve(element.toString());
        }
        return target;
    }
}

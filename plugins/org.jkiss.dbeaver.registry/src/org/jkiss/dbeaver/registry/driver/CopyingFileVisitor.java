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
package org.jkiss.dbeaver.registry.driver;

import org.jkiss.code.NotNull;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

final class CopyingFileVisitor extends SimpleFileVisitor<Path> {
    private final Path sourceRoot;
    private final Path targetRoot;

    CopyingFileVisitor(@NotNull Path sourceRoot, @NotNull Path targetRoot) {
        this.sourceRoot = sourceRoot;
        this.targetRoot = targetRoot;
    }

    @NotNull
    @Override
    public FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) throws IOException {
        Path result = resolveTargetPath(file);
        Files.createDirectories(result.getParent());
        Files.copy(file, result, StandardCopyOption.REPLACE_EXISTING);
        return FileVisitResult.CONTINUE;
    }

    @NotNull
    Path resolveTargetPath(@NotNull Path sourcePath) {
        Path relative = sourceRoot.relativize(sourcePath);
        Path target = targetRoot;
        for (Path element : relative) {
            target = target.resolve(element.toString());
        }
        return target;
    }
}

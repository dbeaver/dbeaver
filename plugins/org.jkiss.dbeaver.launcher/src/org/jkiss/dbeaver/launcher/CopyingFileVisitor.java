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
package org.jkiss.dbeaver.launcher;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

/**
 * Copy of {@code org.jkiss.utils.io.CopyingFileVisitor} because the launcher cannot depend on {@code org.jkiss.utils}.
 */
@SuppressWarnings({"CheckStyle", "NullableProblems"})
final class CopyingFileVisitor extends SimpleFileVisitor<Path> {
    private final Path sourceRoot;
    private final Path targetRoot;

    CopyingFileVisitor(Path sourceRoot, Path targetRoot) {
        this.sourceRoot = sourceRoot;
        this.targetRoot = targetRoot;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        Path result = resolveTargetPath(file);
        Files.createDirectories(result.getParent());
        Files.copy(file, result, StandardCopyOption.REPLACE_EXISTING);
        return FileVisitResult.CONTINUE;
    }

    Path resolveTargetPath(Path sourcePath) {
        Path relative = sourceRoot.relativize(sourcePath);
        Path target = targetRoot;
        for (Path element : relative) {
            target = target.resolve(element.toString());
        }
        // The target filesystem may interpret separators differently from the source filesystem.
        if (!target.toAbsolutePath().normalize().startsWith(targetRoot.toAbsolutePath().normalize())) {
            throw new InvalidPathException(sourcePath.toString(), "Path resolves outside the target directory");
        }
        return target;
    }
}

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
package org.jkiss.dbeaver.model.impl.app;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.util.function.Consumer;

final class InstallCacheCleaner {

    private InstallCacheCleaner() {
    }

    static void clean(
        @NotNull Path cacheRoot,
        @NotNull Path currentVersion,
        @NotNull Instant cutoff,
        @NotNull Consumer<IOException> onError
    ) {
        Path productDirectory = currentVersion.getParent();
        if (productDirectory == null || !cacheRoot.equals(productDirectory.getParent()) ||
            !Files.isDirectory(productDirectory, LinkOption.NOFOLLOW_LINKS)) {
            return;
        }
        // install-data/<product>/<version>: scan only this product's versions, excluding the active one.
        try (DirectoryStream<Path> versions = Files.newDirectoryStream(productDirectory)) {
            for (Path version : versions) {
                if (version.equals(currentVersion) || !Files.isDirectory(version, LinkOption.NOFOLLOW_LINKS)) {
                    continue;
                }
                try {
                    if (isOlderThan(version, cutoff)) {
                        delete(version);
                    }
                } catch (IOException e) {
                    onError.accept(e);
                }
            }
        } catch (IOException e) {
            onError.accept(e);
        }
    }

    private static boolean isOlderThan(@NotNull Path version, @NotNull Instant cutoff) throws IOException {
        Instant[] newest = {Instant.MIN};
        // Include directory timestamps for empty caches and do not traverse symbolic links outside the cache.
        Files.walkFileTree(version, new SimpleFileVisitor<>() {
            @Override
            @NotNull
            public FileVisitResult preVisitDirectory(@NotNull Path dir, @NotNull BasicFileAttributes attrs) {
                newest[0] = max(newest[0], attrs.lastModifiedTime().toInstant());
                return FileVisitResult.CONTINUE;
            }

            @Override
            @NotNull
            public FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) {
                newest[0] = max(newest[0], attrs.lastModifiedTime().toInstant());
                return FileVisitResult.CONTINUE;
            }
        });
        return newest[0].isBefore(cutoff);
    }

    @NotNull
    private static Instant max(@NotNull Instant first, @NotNull Instant second) {
        return first.isAfter(second) ? first : second;
    }

    private static void delete(@NotNull Path version) throws IOException {
        Files.walkFileTree(version, new SimpleFileVisitor<>() {
            @Override
            @NotNull
            public FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) throws IOException {
                Files.delete(file);
                return FileVisitResult.CONTINUE;
            }

            @Override
            @NotNull
            public FileVisitResult postVisitDirectory(@NotNull Path dir, @Nullable IOException error) throws IOException {
                if (error != null) {
                    throw error;
                }
                Files.delete(dir);
                return FileVisitResult.CONTINUE;
            }
        });
    }
}

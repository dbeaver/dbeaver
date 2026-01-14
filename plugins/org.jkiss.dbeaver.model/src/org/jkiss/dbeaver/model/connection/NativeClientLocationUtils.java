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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public final class NativeClientLocationUtils {
    private static final Log log = Log.getLog(NativeClientLocationUtils.class);

    private NativeClientLocationUtils() {
        // No instances for you!
    }

    /**
     * Finds native client locations. Designed to work on Unix and Unix-like systems.
     * It does so by examining some common disk locations for the clients. A user of this function
     * can supply extra folders to examine.
     *
     * @param extraFoldersToExamine extra folders to look for the clients
     * @param fileEndings the endings of full file paths to search for
     * @param grandparentPathToClientLocationMapper maps tool's grandparent folder to an instance of {@code DBPNativeClientLocation}
     * @return map of local client locations
     */
    public static Map<String, DBPNativeClientLocation> findLocalClientsOnUnix(
        Collection<String> extraFoldersToExamine,
        Iterable<String> fileEndings,
        Function<? super Path, ? extends DBPNativeClientLocation> grandparentPathToClientLocationMapper
    ) {
        Collection<String> foldersToExamine = unixFoldersToExamine();
        foldersToExamine.addAll(extraFoldersToExamine);
        Map<String, DBPNativeClientLocation> result = new HashMap<>();
        for (String folder : foldersToExamine) {
            Path folderPath = Path.of(folder);
            if (Files.notExists(folderPath)) {
                continue;
            }
            try {
                Files.walkFileTree(folderPath, new SimpleFileVisitor<>() {
                    @Override
                    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                        if (!somethingEndsWith(file, fileEndings)) {
                            return FileVisitResult.CONTINUE;
                        }
                        if (Files.isExecutable(file)) {
                            Path grandparent = getGrandparent(file.toRealPath());
                            if (grandparent != null) {
                                result.put(grandparent.toString(), grandparentPathToClientLocationMapper.apply(grandparent));
                            }
                        }
                        return FileVisitResult.SKIP_SIBLINGS;
                    }
                });
            } catch (IOException e) {
                log.warn("Unable to examine folder %s while looking for a client home".formatted(folder), e);
            }
        }
        return result;
    }

    @NotNull
    private static Collection<String> unixFoldersToExamine() {
        Collection<String> foldersToExamine = new ArrayList<>();
        foldersToExamine.add("/usr/bin");
        foldersToExamine.add("/usr/local/bin");
        if (RuntimeUtils.isLinux()) {
            foldersToExamine.add("/usr/lib"); // it seems like native clients never appear in this folder on macOS
            foldersToExamine.add("/etc/alternatives");
        } else if (RuntimeUtils.isMacOS()) {
            if (RuntimeUtils.isOSArchAMD64()) {
                foldersToExamine.add("/usr/local/Cellar/"); // homebrew on Intel-based macs
            } else if (RuntimeUtils.isOSArchAArch64()) {
                foldersToExamine.add("/opt/homebrew/bin");
                foldersToExamine.add("/opt/homebrew/Cellar");
                foldersToExamine.add("/opt/homebrew/opt");
            }
        }
        return foldersToExamine;
    }

    private static boolean somethingEndsWith(Path file, Iterable<String> fileEndings) {
        for (String endings : fileEndings) {
            if (file.endsWith(endings)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the <em>grandparent path</em>, or {@code null} if this path does not have a grandparent.
     *
     * @param path a path
     * @return a path representing the path's grandparent
     */
    @Nullable
    private static Path getGrandparent(@NotNull Path path) {
        Path parent = path.getParent();
        if (parent == null) {
            return null;
        }
        return parent.getParent();
    }
}

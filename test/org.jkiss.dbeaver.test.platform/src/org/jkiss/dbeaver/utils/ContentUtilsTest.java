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
package org.jkiss.dbeaver.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class ContentUtilsTest {
    @TempDir
    Path tempDirectory;

    @Test
    public void deleteFileRecursiveDoesNotFollowSymbolicLinks() throws IOException {
        Path target = Files.createDirectory(tempDirectory.resolve("target"));
        Path targetFile = Files.createFile(target.resolve("content.txt"));
        Path link = tempDirectory.resolve("link");
        try {
            Files.createSymbolicLink(link, target);
        } catch (UnsupportedOperationException | IOException | SecurityException e) {
            assumeTrue(false, "Symbolic links are not supported: " + e.getMessage());
        }

        assertTrue(ContentUtils.deleteFileRecursive(link));
        assertTrue(Files.exists(targetFile));
    }
}

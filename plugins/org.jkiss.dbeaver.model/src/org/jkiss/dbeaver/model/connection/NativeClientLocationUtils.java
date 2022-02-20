/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

public class NativeClientLocationUtils {
    public static final String USR_LOCAL = "/usr/local/";
    public static final String HOMEBREW_FORMULAE_LOCATION = USR_LOCAL + "Cellar/";
    public static final String BIN = "bin";

    private NativeClientLocationUtils() {}

    public static File[] getSubdirectories(File... dirs) {
        return getStreamOfSubdirectories(dirs).toArray(File[]::new);
    }

    public static File[] getSubdirectoriesWithNamesStartingWith(String prefix, File... dirs) {
        return getStreamOfSubdirectories(dirs)
                .filter(file -> file.getName().startsWith(prefix))
                .toArray(File[]::new);
    }

    private static Stream<File> getStreamOfSubdirectories(File... dirs) {
        if (dirs == null) {
            return Stream.empty();
        }
        return Arrays.stream(dirs)
                .filter(Objects::nonNull)
                .map(File::listFiles)
                .filter(Objects::nonNull)
                .flatMap(Arrays::stream)
                .filter(Objects::nonNull)
                .filter(File::isDirectory);
    }

    public static String getCanonicalPath(File file) {
        try {
            return file.getCanonicalPath();
        } catch (IOException e) {
            return "";
        }
    }
}

/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.Map;

class LauncherUtils {

    private static final Map<Character, String> CHARS_TO_ESCAPE =
        Map.of(
            '\b', "\\b",
            '\n', "\\n",
            '\t', "\\t",
            '\f', "\\f",
            '\r', "\\r",
            '\"', "\\\"",
            '\\', "\\\\",
            '/', "\\/"
        );

    static File toFileURL(String spec) {
        try {
            // Try to build it from a URI that will be properly decoded.
            return new File(new URI(spec));
        } catch (URISyntaxException | IllegalArgumentException e) {
            return new File(spec.substring(5));
        }
    }


    static URL adjustTrailingSlash(URL url, boolean trailingSlash) throws MalformedURLException {
        String file = url.getFile();
        if (trailingSlash == (file.endsWith("/"))) //$NON-NLS-1$
            return url;
        file = trailingSlash ? file + "/" : file.substring(0, file.length() - 1); //$NON-NLS-1$
        return new URL(url.getProtocol(), url.getHost(), file);
    }

    static boolean canWrite(File installDir) {
        if (!installDir.isDirectory())
            return false;

        if (Files.isWritable(installDir.toPath()))
            return true;

        File fileTest = null;
        try {
            // we use the .dll suffix to properly test on Vista virtual directories
            // on Vista you are not allowed to write executable files on virtual directories like "Program Files"
            fileTest = File.createTempFile("writableArea", ".dll", installDir); //$NON-NLS-1$ //$NON-NLS-2$
        } catch (IOException e) {
            //If an exception occured while trying to create the file, it means that it is not writtable
            return false;
        } finally {
            if (fileTest != null)
                fileTest.delete();
        }
        return true;
    }


    public static String escape(String original) {
        StringBuilder escaped = new StringBuilder();
        for (int i = 0; i < original.length(); i++) {
            char c = original.charAt(i);
            if (CHARS_TO_ESCAPE.containsKey(c)) {
                escaped.append(CHARS_TO_ESCAPE.get(c));
            } else {
                escaped.append(c);
            }
        }
        return escaped.toString();
    }
}

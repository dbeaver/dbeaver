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
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

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

    public static String substituteVars(String path) {
        StringBuilder buf = new StringBuilder(path.length());
        StringTokenizer st = new StringTokenizer(path, DBeaverLauncher.VARIABLE_DELIM_STRING, true);
        boolean varStarted = false; // indicates we are processing a var subtitute
        String var = null; // the current var key
        while (st.hasMoreElements()) {
            String tok = st.nextToken();
            if (DBeaverLauncher.VARIABLE_DELIM_STRING.equals(tok)) {
                if (!varStarted) {
                    varStarted = true; // we found the start of a var
                    var = ""; //$NON-NLS-1$
                } else {
                    // we have found the end of a var
                    String prop = null;
                    // get the value of the var from system properties
                    if (var != null && !var.isEmpty())
                        prop = System.getProperty(var);
                    if (prop == null) {
                        prop = System.getenv(var);
                    }
                    if (prop != null) {
                        // found a value; use it
                        buf.append(prop);
                    } else {
                        // could not find a value append the var; keep delemiters
                        buf.append(DBeaverLauncher.VARIABLE_DELIM_CHAR);
                        buf.append(var == null ? "" : var); //$NON-NLS-1$
                        buf.append(DBeaverLauncher.VARIABLE_DELIM_CHAR);
                    }
                    varStarted = false;
                    var = null;
                }
            } else {
                if (!varStarted)
                    buf.append(tok); // the token is not part of a var
                else
                    var = tok; // the token is the var key; save the key to process when we find the end token
            }
        }
        if (var != null)
            // found a case of $var at the end of the path with no trailing $; just append it as is.
            buf.append(DBeaverLauncher.VARIABLE_DELIM_CHAR).append(var);
        return buf.toString();
    }

    public static Properties substituteVars(Properties result) {
        if (result == null) {
            //nothing todo.
            return null;
        }
        for (Enumeration<?> eKeys = result.keys(); eKeys.hasMoreElements(); ) {
            Object key = eKeys.nextElement();
            if (key instanceof String) {
                String value = result.getProperty((String) key);
                if (value != null)
                    result.put(key, substituteVars(value));
            }
        }
        return result;
    }

    static URL buildURL(String spec, boolean trailingSlash) {
        if (spec == null)
            return null;
        if (File.separatorChar == '\\')
            spec = spec.trim();
        boolean isFile = spec.startsWith(DBeaverLauncher.FILE_SCHEME);
        try {
            if (isFile) {
                File toAdjust = toFileURL(spec);
                toAdjust = resolveFile(toAdjust);
                if (toAdjust.isDirectory())
                    return adjustTrailingSlash(toAdjust.toURL(), trailingSlash);
                return toAdjust.toURL();
            }
            return new URL(spec);
        } catch (MalformedURLException e) {
            // if we failed and it is a file spec, there is nothing more we can do
            // otherwise, try to make the spec into a file URL.
            if (isFile)
                return null;
            try {
                File toAdjust = new File(spec);
                if (toAdjust.isDirectory())
                    return adjustTrailingSlash(toAdjust.toURL(), trailingSlash);
                return toAdjust.toURL();
            } catch (MalformedURLException e1) {
                return null;
            }
        }
    }

    /**
     * Resolve the given file against  osgi.install.area.
     * If osgi.install.area is not set, or the file is not relative, then
     * the file is returned as is.
     */
    static File resolveFile(File toAdjust) {
        if (!toAdjust.isAbsolute()) {
            String installArea = System.getProperty(DBeaverLauncher.PROP_INSTALL_AREA);
            if (installArea != null) {
                if (installArea.startsWith(DBeaverLauncher.FILE_SCHEME))
                    toAdjust = new File(installArea.substring(5), toAdjust.getPath());
                else if (new File(installArea).exists())
                    toAdjust = new File(installArea, toAdjust.getPath());
            }
        }
        return toAdjust;
    }

    static String getWorkingDirectory(String defaultWorkspaceLocation) {
        String osName = (System.getProperty("os.name")).toUpperCase();
        String workingDirectory;
        if (osName.contains("WIN")) {
            String appData = System.getenv("AppData");
            if (appData == null) {
                appData = System.getProperty("user.home");
            }
            workingDirectory = appData + "\\" + defaultWorkspaceLocation;
        } else if (osName.contains("MAC")) {
            workingDirectory = System.getProperty("user.home") + "/Library/" + defaultWorkspaceLocation;
        } else {
            // Linux
            String dataHome = System.getProperty("XDG_DATA_HOME");
            if (dataHome == null) {
                dataHome = System.getProperty("user.home") + "/.local/share";
            }
            String badWorkingDir = dataHome + "/." + defaultWorkspaceLocation;
            String goodWorkingDir = dataHome + "/" + defaultWorkspaceLocation;
            if (!new File(goodWorkingDir).exists() && new File(badWorkingDir).exists()) {
                // Let's use bad working dir if it exists (#6316)
                workingDirectory = badWorkingDir;
            } else {
                workingDirectory = goodWorkingDir;
            }
        }
        return workingDirectory;
    }

    static void setSystemPropertyIfNotSet(String key, String value) {
        if (value == null || value.isBlank()) {
            return;
        }
        if (System.getProperty(key) == null) {
            System.setProperty(key, value);
        }
    }
}

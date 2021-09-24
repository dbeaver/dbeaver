/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui;

import org.eclipse.jface.util.Util;
import org.eclipse.swt.program.Program;
import org.eclipse.ui.internal.ide.IDEInternalPreferences;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;

import java.io.File;
import java.io.IOException;

/**
 * Utilities for interacting with the OS shell
 */
public final class ShellUtils {
    private static final Log log = Log.getLog(ShellUtils.class);

    private ShellUtils() {
        // prevent constructing utility class
    }

    public static boolean launchProgram(@NotNull String path) {
        return Program.launch(path);
    }

    public static void showInSystemExplorer(@NotNull String path) {
        final File file = new File(path);

        try {
            final String cmd = formShowInSystemExplorerCommand(file);
            final Process process;

            if (Util.isLinux() || Util.isMac()) {
                process = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", cmd}, null); //$NON-NLS-1$ //$NON-NLS-2$
            } else {
                process = Runtime.getRuntime().exec(cmd, null);
            }

            final int code = process.waitFor();

            if (code != 0 && !Util.isWindows()) {
                log.debug("Execution of '" + cmd + "' failed with return code: " + code);
            }
        } catch (IOException | InterruptedException e) {
            log.debug("Error opening file in explorer", e);

            if (file.isDirectory()) {
                launchProgram(file.getAbsolutePath());
            } else {
                launchProgram(file.getParent());
            }
        }
    }

    /**
     * Adopted from {@link org.eclipse.ui.internal.ide.handlers.ShowInSystemExplorerHandler}
     */
    @NotNull
    private static String formShowInSystemExplorerCommand(@NotNull File path) throws IOException {
        String command = IDEWorkbenchPlugin.getDefault().getPreferenceStore().getString(IDEInternalPreferences.WORKBENCH_SYSTEM_EXPLORER);
        command = Util.replaceAll(command, "${selected_resource_loc}", quotePath(path.getCanonicalPath()));
        command = Util.replaceAll(command, "${selected_resource_uri}", path.getCanonicalFile().toURI().toString());
        File parent = path.getParentFile();
        if (parent != null) {
            command = Util.replaceAll(command, "${selected_resource_parent_loc}", quotePath(parent.getCanonicalPath()));
        }
        return command;
    }

    /**
     * Adopted from {@link org.eclipse.ui.internal.ide.handlers.ShowInSystemExplorerHandler}
     */
    @NotNull
    private static String quotePath(@NotNull String path) {
        if (Util.isLinux() || Util.isMac()) {
            // Quote for usage inside "", man sh, topic QUOTING:
            return path.replaceAll("[\"$`]", "\\\\$0");
        } else {
            // Windows: Can't quote, since explorer.exe has a very special command line parsing strategy.
            return path;
        }
    }
}

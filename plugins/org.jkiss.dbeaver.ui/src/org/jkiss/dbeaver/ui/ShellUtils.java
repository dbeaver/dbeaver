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

import org.eclipse.swt.program.Program;
import org.eclipse.ui.internal.ide.handlers.ShowInSystemExplorerHandler;
import org.eclipse.ui.services.IServiceLocator;
import org.jkiss.code.NotNull;

import java.util.Collections;
import java.util.Map;

/**
 * Utilities for interacting with the OS shell
 */
public final class ShellUtils {
    private ShellUtils() {
        // prevent constructing utility class
    }

    public static boolean launchProgram(@NotNull String path) {
        return Program.launch(path);
    }

    public static void showInSystemExplorer(@NotNull String path) {
        final IServiceLocator serviceLocator = UIUtils.getActiveWorkbenchWindow();
        final Map<String, Object> parameters = Collections.singletonMap(ShowInSystemExplorerHandler.RESOURCE_PATH_PARAMETER, path);
        ActionUtils.runCommand(ShowInSystemExplorerHandler.ID, null, parameters, serviceLocator);
    }
}

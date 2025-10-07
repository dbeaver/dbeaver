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
package org.jkiss.dbeaver.model.impl.app;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.io.File;

public abstract class BaseSingleUserApplicationImpl extends BaseApplicationImpl {
    public static final String DEFAULT_WORKSPACE_FOLDER = "workspace6";
    public static final String DEFAULT_WORKSPACES_FILE = ".workspaces";

    protected final String WORKSPACE_DIR_CURRENT;

    protected BaseSingleUserApplicationImpl(
        @NotNull String defaultWorkspaceLocation,
        @NotNull String defaultAppWorkspaceName
    ) {

        // Explicitly set UTF-8 as default file encoding
        // In some places Eclipse reads this property directly.
        //System.setProperty(StandardConstants.ENV_FILE_ENCODING, GeneralUtils.UTF8_ENCODING);

        // Detect default workspace location
        // Since 6.1.3 it is different for different OSes
        // Windows: %AppData%/DBeaverData
        // MacOS: ~/Library/DBeaverData
        // Linux: $XDG_DATA_HOME/DBeaverData
        String workingDirectory = RuntimeUtils.getWorkingDirectory(defaultWorkspaceLocation);

        // Workspace dir
        WORKSPACE_DIR_CURRENT = new File(workingDirectory, defaultAppWorkspaceName).getAbsolutePath();
    }
}

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
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.nio.file.Path;

/**
 * Base application implementation
 */
public abstract class BaseApplicationImpl extends AbstractApplication {
    public static final String DBEAVER_DATA_DIR = "DBeaverData";
    public static final String DEFAULT_WORKSPACE_FOLDER = "workspace6";
    public static final String SECURE_DATA_FOLDER = "secure";

    public static final String ECLIPSE_EXIT_DATA = "eclipse.exitdata";

    private final Path workingDirectory;
    private Path workspacePath;

    protected BaseApplicationImpl() {
        this(DBEAVER_DATA_DIR, DEFAULT_WORKSPACE_FOLDER);
    }

    protected BaseApplicationImpl(
        @NotNull String defaultWorkspaceLocation,
        @NotNull String defaultAppWorkspaceName
    ) {
        workingDirectory = Path.of(RuntimeUtils.getWorkingDirectory(defaultWorkspaceLocation));
        // Workspace dir
        workspacePath = RuntimeUtils.getWorkspacePath(workingDirectory.toString(), defaultAppWorkspaceName);
    }

    @NotNull
    @Override
    public Path getGlobalDataPath() {
        return workingDirectory;
    }

    @NotNull
    public Path getWorkspacePath() {
        return workspacePath;
    }

    protected void setWorkspacePath(Path workspacePath) {
        this.workspacePath = workspacePath;
    }
}

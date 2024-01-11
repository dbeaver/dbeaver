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
package org.jkiss.dbeaver.dpi.app;

import org.eclipse.core.resources.IProject;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.auth.SMSessionContext;
import org.jkiss.dbeaver.registry.BaseProjectImpl;

import java.nio.file.Path;

public class DPIVirtualProject extends BaseProjectImpl {
    private final String projectName;

    public DPIVirtualProject(
        @NotNull DBPWorkspace workspace,
        @Nullable SMSessionContext sessionContext,
        String projectName
    ) {
        super(workspace, sessionContext);
        this.projectName = projectName;
    }

    @Override
    public boolean isVirtual() {
        return true;
    }

    @NotNull
    @Override
    public String getName() {
        return projectName;
    }

    @NotNull
    @Override
    public Path getAbsolutePath() {
        return Path.of(projectName);
    }

    @Nullable
    @Override
    public IProject getEclipseProject() {
        return null;
    }

    @Override
    public boolean isOpen() {
        return true;
    }

    @Override
    public void ensureOpen() {

    }

    @Override
    public boolean isUseSecretStorage() {
        return false;
    }
}

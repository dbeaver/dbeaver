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
package org.jkiss.dbeaver.registry;

import org.eclipse.core.resources.IProject;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.auth.SMSessionContext;

import java.nio.file.Path;

public class VirtualProjectImpl extends BaseProjectImpl {

    @NotNull
    private final String projectName;
    @NotNull
    private final Path projectPath;

    @Nullable
    private DataSourceConfigurationManager configurationManager;

    public VirtualProjectImpl(@NotNull DBPWorkspace workspace, @NotNull String projectName, @NotNull Path projectPath, @Nullable SMSessionContext sessionContext) {
        super(workspace, sessionContext);
        this.projectName = projectName;
        this.projectPath = projectPath;
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
        return projectPath;
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

    public void setConfigurationManager(DataSourceConfigurationManager configurationManager) {
        this.configurationManager = configurationManager;
    }

    @NotNull
    @Override
    public DataSourceRegistry createDataSourceRegistry() {
        if (configurationManager == null) {
            return new DataSourceRegistry(this);
        }
        return new DataSourceRegistry(this, configurationManager);
    }
}

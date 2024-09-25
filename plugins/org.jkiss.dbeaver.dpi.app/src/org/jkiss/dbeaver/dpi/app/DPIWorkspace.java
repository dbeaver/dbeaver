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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.impl.app.BaseWorkspaceImpl;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

/**
 * DPIWorkspace
 */
public class DPIWorkspace extends BaseWorkspaceImpl {
    private final DBPProject fakeProject;

    DPIWorkspace(DPIPlatform platform, Path workspacePath) {
        super(platform, workspacePath);
        this.fakeProject = new DPIVirtualProject(this, getAuthContext(), "dpi-virtual-project");
    }

    @Override
    public boolean isReadOnly() {
        return true;
    }

    @NotNull
    @Override
    public String getWorkspaceId() {
        return "DPI";
    }

    @NotNull
    @Override
    public List<? extends DBPProject> getProjects() {
        return Collections.singletonList(fakeProject);
    }

    @Override
    public void initializeProjects() {

    }

    @Nullable
    @Override
    public DBPProject getActiveProject() {
        return fakeProject;
    }

    @Nullable
    @Override
    public DBPProject getProject(@NotNull String projectName) {
        return null;
    }

}
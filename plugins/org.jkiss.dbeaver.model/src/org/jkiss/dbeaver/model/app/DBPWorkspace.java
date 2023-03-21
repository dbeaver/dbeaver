/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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

package org.jkiss.dbeaver.model.app;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.access.DBAPermissionRealm;
import org.jkiss.dbeaver.model.auth.SMAuthSpace;
import org.jkiss.dbeaver.model.auth.SMSession;
import org.jkiss.dbeaver.model.auth.SMSessionContext;

import java.nio.file.Path;
import java.util.List;

/**
 * DBPWorkspace
 */
public interface DBPWorkspace extends SMAuthSpace, DBAPermissionRealm
{
    String METADATA_FOLDER = ".metadata";

    @NotNull
    DBPPlatform getPlatform();

    @NotNull
    String getWorkspaceId();

    boolean isActive();

    @NotNull
    Path getAbsolutePath();

    @NotNull
    Path getMetadataFolder();

    @NotNull
    List<? extends DBPProject> getProjects();
    DBPProject getActiveProject();
    DBPProject getProject(@NotNull String projectName);

    /**
     * Finds project by ID
     */
    @Nullable
    DBPProject getProjectById(@NotNull String projectId);

    /**
     * Workspace auth context
     */
    @NotNull
    SMSessionContext getAuthContext();

    void dispose();

    default SMSession getWorkspaceSession() {
        return getAuthContext().findSpaceSession(this);
    }

}

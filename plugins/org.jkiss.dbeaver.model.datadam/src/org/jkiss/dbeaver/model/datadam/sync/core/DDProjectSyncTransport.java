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
package org.jkiss.dbeaver.model.datadam.sync.core;

import com.dbeaver.datadam.share.api.model.DDCreateProjectRequest;
import com.dbeaver.datadam.share.api.model.DDPushProjectConfigurationRequest;
import com.dbeaver.datadam.share.api.model.DDSharedProject;
import com.dbeaver.datadam.share.api.model.DDSharedProjectConfiguration;
import com.dbeaver.datadam.share.api.model.DDSharedProjectRevision;
import com.dbeaver.datadam.share.api.model.DDUpdateProjectRequest;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;

import java.util.List;

/**
 * Remote storage for standalone shared projects. Separate from DDSyncTransport (Configuration).
 */
public interface DDProjectSyncTransport {

    @NotNull
    List<DDSharedProject> listProjects() throws DBException;

    @NotNull
    DDSharedProject createProject(@NotNull DDCreateProjectRequest request) throws DBException;

    @Nullable
    DDSharedProject updateProject(@NotNull String projectId, @NotNull DDUpdateProjectRequest request) throws DBException;

    boolean deleteProject(@NotNull String projectId) throws DBException;

    @Nullable
    DDSharedProjectConfiguration pullProjectConfiguration(@NotNull String projectId) throws DBException;

    /**
     * Returns null if the project does not exist, or if the whole-project configuration
     * fingerprint no longer matches request.lastKnownConfigurationFingerprint - the caller
     * should pull the current state and reconcile.
     */
    @Nullable
    DDSharedProjectRevision pushProjectConfiguration(
        @NotNull String projectId,
        @NotNull DDPushProjectConfigurationRequest request
    ) throws DBException;
}

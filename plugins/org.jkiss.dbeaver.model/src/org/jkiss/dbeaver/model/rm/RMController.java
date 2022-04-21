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
package org.jkiss.dbeaver.model.rm;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;

/**
 * Resource manager API.
 */
public interface RMController {

    @NotNull
    RMProject[] listAccessibleProjects();

    @NotNull
    RMProject[] listSharedProjects();

    @NotNull
    RMProject getPrivateProject();

    void createProject(@NotNull RMProject project);

    void deleteProject(@NotNull String projectId);

    @NotNull
    RMResource[] listResources(
        @NotNull String projectId,
        @Nullable String folder,
        @Nullable String nameMask,
        boolean readProperties,
        boolean readHistory) throws DBException;

    String createResource(
        @NotNull String projectId,
        @NotNull String folder,
        @NotNull RMResource resource) throws DBException;

    String updateResource(
        @NotNull String projectId,
        @NotNull String folder,
        @NotNull RMResource resource) throws DBException;

    String moveResource(
        @NotNull String projectId,
        @NotNull String oldResourcePath,
        @NotNull String newResourcePath) throws DBException;

    void deleteResource(
        @NotNull String projectId,
        @NotNull String resourcePath,
        boolean recursive) throws DBException;

    @NotNull
    byte[] getResourceContents(
        @NotNull String projectId,
        @NotNull String resourcePath) throws DBException;

    @NotNull
    String setResourceContents(
        @NotNull String projectId,
        @NotNull String resourcePath,
        @NotNull String contentType,
        @NotNull byte[] data) throws DBException;

}

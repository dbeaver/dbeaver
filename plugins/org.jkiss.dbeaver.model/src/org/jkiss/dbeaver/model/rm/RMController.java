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

import java.util.Map;

/**
 * Resource manager API.
 */
public interface RMController {

    @NotNull
    RMProject[] listAccessibleProjects() throws DBException;

    @NotNull
    RMProject[] listSharedProjects() throws DBException;

    RMProject createProject(@NotNull String id, @NotNull String name, @NotNull String description) throws DBException;

    void deleteProject(@NotNull String projectId) throws DBException;

    RMProject getProject(@NotNull String projectId, boolean readResources) throws DBException;

    Object getProjectProperty(@NotNull String projectId, @NotNull String propName) throws DBException;

    /**
     * Returns datasources configuration in modern format
     */
    String getProjectsDataSources(@NotNull String projectId) throws DBException;

    /**
     * Save datasources. Not: it only adds or updates existing datasources.
     * @param configuration configuration in modern format.
     */
    void saveProjectDataSources(@NotNull String projectId, @NotNull String configuration) throws DBException;

    /**
     * Delete datasource by Ids
     */
    void deleteProjectDataSources(@NotNull String projectId, @NotNull String[] dataSourceIds) throws DBException;

    @NotNull
    RMResource[] listResources(
        @NotNull String projectId,
        @Nullable String folder,
        @Nullable String nameMask,
        boolean readProperties,
        boolean readHistory,
        boolean recursive) throws DBException;

    String createResource(
        @NotNull String projectId,
        @NotNull String resourcePath,
        boolean isFolder) throws DBException;

    String moveResource(
        @NotNull String projectId,
        @NotNull String oldResourcePath,
        @NotNull String newResourcePath) throws DBException;

    void deleteResource(
        @NotNull String projectId,
        @NotNull String resourcePath,
        boolean recursive) throws DBException;

    RMResource[] getResourcePath(
        @NotNull String projectId,
        @NotNull String resourcePath) throws DBException;

    @NotNull
    byte[] getResourceContents(
        @NotNull String projectId,
        @NotNull String resourcePath) throws DBException;

    @NotNull
    String setResourceContents(
        @NotNull String projectId,
        @NotNull String resourcePath,
        @NotNull byte[] data) throws DBException;

}

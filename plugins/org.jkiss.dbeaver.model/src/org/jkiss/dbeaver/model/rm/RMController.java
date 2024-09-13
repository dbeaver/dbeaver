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
package org.jkiss.dbeaver.model.rm;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPObjectController;
import org.jkiss.dbeaver.model.app.DBPPingController;

import java.util.List;
import java.util.Map;

/**
 * Resource manager API.
 */
public interface RMController extends DBPObjectController, DBPPingController {

    ////////////////////////////////////////////
    // Projects

    /**
     * Returns list of all projects accessible to calling user
     */
    @NotNull
    RMProject[] listAccessibleProjects() throws DBException;

    /**
     * Returns list of all shared projects
     */
    @NotNull
    RMProject[] listAllSharedProjects() throws DBException;

    /**
     * Creates new shared project
     */
    RMProject createProject(@NotNull String name, @Nullable String description) throws DBException;

    /**
     * Deletes shared project
     */
    void deleteProject(@NotNull String projectId) throws DBException;

    /**
     * Reads project information
     */
    RMProject getProject(@NotNull String projectId, boolean readResources, boolean readProperties) throws DBException;

    /**
     * Reads single project property
     */
    Object getProjectProperty(@NotNull String projectId, @NotNull String propName) throws DBException;

    /**
     * Sets project property
     */
    void setProjectProperty(@NotNull String projectId, @NotNull String propName, @NotNull Object propValue) throws DBException;

    ////////////////////////////////////////////
    // DataSources

    /**
     * Returns datasources configuration in modern format
     */
    String getProjectsDataSources(@NotNull String projectId, @Nullable String[] dataSourceIds) throws DBException;

    /**
     * Save datasources. Note: it only adds datasources.
     *
     * @param configuration configuration in modern format.
     */
    void createProjectDataSources(
        @NotNull String projectId,
        @NotNull String configuration,
        @Nullable List<String> dataSourceIds) throws DBException;


    /**
     * Save datasources. Note: it only updates existing datasources.
     *
     * @param configuration configuration in modern format.
     */
    boolean updateProjectDataSources(
        @NotNull String projectId,
        @NotNull String configuration,
        @Nullable List<String> dataSourceIds) throws DBException;

    /**
     * Delete datasource by Ids
     */
    void deleteProjectDataSources(@NotNull String projectId, @NotNull String[] dataSourceIds) throws DBException;

    void createProjectDataSourceFolder(
        @NotNull String projectId,
        @NotNull String folderPath
    ) throws DBException;

    /**
     * Delete project datasource folders.
     *
     * @param projectId    the project id
     * @param folderPaths  the folder paths
     * @param dropContents the drop contents
     * @throws DBException the db exception
     */
    void deleteProjectDataSourceFolders(
        @NotNull String projectId,
        @NotNull String[] folderPaths,
        boolean dropContents
    ) throws DBException;

    /**
     * Moves project datasource folder
     *
     * @param projectId  id of the project
     * @param oldPath old path of the moving folder
     * @param newPath new path of the moving folder
     */
    void moveProjectDataSourceFolder(
        @NotNull String projectId,
        @NotNull String oldPath,
        @NotNull String newPath
    ) throws DBException;

    ////////////////////////////////////////////
    // Resources

    /**
     * Reads resources by path
     */
    @NotNull
    RMResource[] listResources(
        @NotNull String projectId,
        @Nullable String folder,
        @Nullable String nameMask,
        boolean readProperties,
        boolean readHistory,
        boolean recursive) throws DBException;

    /**
     * Creates new empty resource
     */
    String createResource(
        @NotNull String projectId,
        @NotNull String resourcePath,
        boolean isFolder) throws DBException;

    /**
     * Moves resource to another folder
     */
    String moveResource(
        @NotNull String projectId,
        @NotNull String oldResourcePath,
        @NotNull String newResourcePath) throws DBException;

    /**
     * Deletes resource by path
     */
    void deleteResource(
        @NotNull String projectId,
        @NotNull String resourcePath,
        boolean recursive) throws DBException;

    /**
     * Resources hierarchy
     */
    RMResource[] getResourcePath(
        @NotNull String projectId,
        @NotNull String resourcePath
    ) throws DBException;


    /**
     * Resource metainfo
     */
    @Nullable
    RMResource getResource(
        @NotNull String projectId,
        @NotNull String resourcePath
    ) throws DBException;

    /**
     * Reads resource data
     */
    @NotNull
    byte[] getResourceContents(
        @NotNull String projectId,
        @NotNull String resourcePath) throws DBException;

    /**
     * Writes resource data
     */
    @NotNull
    String setResourceContents(
        @NotNull String projectId,
        @NotNull String resourcePath,
        @NotNull byte[] data,
        boolean forceOverwrite) throws DBException;

    /**
     * Sets resource property
     */
    @NotNull
    String setResourceProperty(
        @NotNull String projectId,
        @NotNull String resourcePath,
        @NotNull String propertyName,
        @Nullable Object propertyValue) throws DBException;


    /**
     * Sets all resource properties
     */
    @NotNull
    String setResourceProperties(
        @NotNull String projectId,
        @NotNull String resourcePath,
        @NotNull Map<String, Object> properties
    ) throws DBException;
}

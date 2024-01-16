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

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.app.DBPWorkspaceEclipse;
import org.jkiss.dbeaver.model.fs.DBFUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class RMUtils {

    public static final String USER_PROJECTS_FOLDER = "user-projects";
    public static final String SHARED_PROJECTS_FOLDER = "shared-projects";

    public static Path getRootPath() {
        return DBWorkbench.getPlatform().getWorkspace().getAbsolutePath();
    }

    public static Path getUserProjectsPath() {
        return getRootPath().resolve(USER_PROJECTS_FOLDER);
    }

    public static Path getSharedProjectsPath() {
        return getRootPath().resolve(SHARED_PROJECTS_FOLDER);
    }

    @NotNull
    public static Path getProjectPath(RMProject project) {
        switch (project.getType()) {
            case GLOBAL:
                return getRootPath().resolve(DBWorkbench.getPlatform().getApplication().getDefaultProjectName());
            case SHARED:
                return getSharedProjectsPath().resolve(project.getName());
            default:
                return getUserProjectsPath().resolve(project.getName());
        }
    }

    public static String getProjectName(@NotNull String projectId) throws DBException {
        int divPos = projectId.indexOf("_");
        if (divPos <= 0) {
            throw new DBException("Bad project ID");
        }
        return projectId.substring(divPos + 1);
    }

    /**
     * Different types of project have different location on the workspace. Method returns path to the project.
     *
     * @param projectId project name or other identifier
     * @return path to the project based on the name of projects and prefixes that it contains
     */
    @NotNull
    public static Path getProjectPathById(@NotNull String projectId) throws DBException {
        int divPos = projectId.indexOf("_");
        if (divPos <= 0) {
            throw new DBException("Bad project ID");
        }

        String prefix = projectId.substring(0, divPos);
        String projectName = projectId.substring(divPos + 1);
        switch (RMProjectType.getByPrefix(prefix)) {
            case GLOBAL:
                String defaultProjectName = DBWorkbench.getPlatform().getApplication().getDefaultProjectName();
                if (CommonUtils.isEmpty(defaultProjectName)) {
                    throw new DBException("Global projects are not supported");
                }
                return getRootPath().resolve(defaultProjectName);
            case SHARED:
                return getSharedProjectsPath().resolve(projectName);
            default:
                return getUserProjectsPath().resolve(projectName);
        }
    }

    public static Set<String> parseProjectPermissions(Set<String> permissions) {
        return permissions.stream()
            .map(RMProjectPermission::fromPermission).filter(Objects::nonNull)
            .flatMap(permission -> permission.getAllPermissions().stream())
            .collect(Collectors.toSet());
    }

    public static RMProject createAnonymousProject() {
        RMProject project = new RMProject("anonymous");
        project.setId("anonymous");
        project.setType(RMProjectType.USER);
        project.setProjectPermissions(RMProjectPermission.DATA_SOURCES_EDIT.getAllPermissions().toArray(new String[0]));
        return project;
    }

    public static String readScriptContents(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBPProject project,
        @NotNull String filePath
    ) throws DBException, IOException {
        Path nioPath = DBFUtils.resolvePathFromString(monitor, project, filePath);
        if (!IOUtils.isLocalPath(nioPath)) {
            // Remote file
            return Files.readString(nioPath);
        }

        RMControllerProvider rmControllerProvider = DBUtils.getAdapter(RMControllerProvider.class, project);
        if (rmControllerProvider != null) {
            var rmController = rmControllerProvider.getResourceController();
            return new String(rmController.getResourceContents(project.getId(), filePath), StandardCharsets.UTF_8);
        }
        var projectRootResource = project.getRootResource();
        if (projectRootResource == null) {
            throw new DBException("Root resource is not found in project " + project.getId());
        }
        var sqlFile = findEclipseProjectFile(project, filePath);
        if (sqlFile == null) {
            throw new DBException("File " + filePath + " is not found in project " + project.getId());
        }
        try (InputStream sqlStream = sqlFile.getContents(true)) {
            try (Reader fileReader = new InputStreamReader(sqlStream, sqlFile.getCharset())) {
                return IOUtils.readToString(fileReader);
            }
        } catch (CoreException e) {
            throw new IOException(e);
        }
    }

    public static IFile findEclipseProjectFile(@NotNull DBPProject project, @NotNull String filePath) {
        var rootResource = project.getRootResource();
        if (rootResource == null) {
            return null;
        }
        var file = rootResource.findMember(filePath);
        if (file != null && file.exists() && file instanceof IFile) {
            return (IFile) file;
        }
        DBPWorkspace workspace = project.getWorkspace();
        if (workspace instanceof DBPWorkspaceEclipse) {
            file = ((DBPWorkspaceEclipse) workspace).getEclipseWorkspace().getRoot().getFile(new org.eclipse.core.runtime.Path(filePath));
            if (file.exists()) {
                return (IFile) file;
            }
        }
        return null;
    }

}

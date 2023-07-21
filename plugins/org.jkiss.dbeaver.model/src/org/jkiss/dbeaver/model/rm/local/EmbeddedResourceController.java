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
package org.jkiss.dbeaver.model.rm.local;

import org.eclipse.core.runtime.IPath;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceFolder;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspace;
import org.jkiss.dbeaver.model.exec.DBCFeatureNotSupportedException;
import org.jkiss.dbeaver.model.rm.*;
import org.jkiss.dbeaver.model.rm.file.UniversalFileVisitor;
import org.jkiss.dbeaver.model.rm.lock.RMFileLockController;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;
import org.jkiss.utils.Pair;

import java.io.IOException;
import java.nio.file.*;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Stream;

/**
 * Resource manager API
 */
public class EmbeddedResourceController implements RMController {

    private static final Log log = Log.getLog(EmbeddedResourceController.class);

    private static final String FILE_REGEX = "(?U)[\\w.$()@/\\\\ -]+";
    private static final String PROJECT_REGEX = "(?U)[\\w.$()@ -]+"; // slash not allowed in project name
    public static final String DEFAULT_CHANGE_ID = "0";

    private final DBPWorkspace workspace;
    protected final RMFileLockController lockController;

    public EmbeddedResourceController() {
        this.workspace = DBWorkbench.getPlatform().getWorkspace();
        this.lockController = new RMFileLockController(DBWorkbench.getPlatform().getApplication());
    }

    protected DBPProject getProject(String projectId) throws DBException {
        return workspace.getProjectById(projectId);
    }

    @NotNull
    @Override
    public RMProject[] listAccessibleProjects() throws DBException {
        List<RMProject> projects = new ArrayList<>();
        for (DBPProject project : workspace.getProjects()) {
            RMProject rmProject = makeProjectFromPath(project.getAbsolutePath(), Set.of(), RMProjectType.SHARED, true);
            projects.add(rmProject);
        }
        projects.sort(Comparator.comparing(RMProject::getDisplayName));
        return projects.toArray(new RMProject[0]);
    }

    @NotNull
    @Override
    public RMProject[] listAllSharedProjects() throws DBException {
        return listAccessibleProjects();
    }

    @Override
    public RMProject createProject(@NotNull String name, @Nullable String description) throws DBException {
        validateResourcePath(name, PROJECT_REGEX);
        RMProject project;
        var projectPath = workspace.getAbsolutePath().resolve(name);
        if (Files.exists(projectPath)) {
            throw new DBException("Project '" + name + "' already exists");
        }
        project = makeProjectFromPath(projectPath, Set.of(), RMProjectType.SHARED, false);
        if (project == null) {
            throw new DBException("Project '" + name + "' not created");
        }
        try {
            Files.createDirectories(projectPath);
            return project;
        } catch (IOException e) {
            throw new DBException("Error creating project path", e);
        }
    }

    @Override
    public void deleteProject(@NotNull String projectId) throws DBException {
        try (var projectLock = lockController.lockProject(projectId, "deleteProject")) {
            RMProject project = makeProjectFromId(projectId);
            Path targetPath = getProjectPath(projectId);
            if (!Files.exists(targetPath)) {
                throw new DBException("Project '" + project.getName() + "' doesn't exists");
            }
            try {
                IOUtils.deleteDirectory(targetPath);
            } catch (IOException e) {
                throw new DBException("Error deleting project '" + project.getName() + "'", e);
            }
        }
    }

    @Override
    public RMProject getProject(@NotNull String projectId, boolean readResources, boolean readProperties) throws DBException {
        RMProject project = makeProjectFromId(projectId);
        if (readResources) {
            project.setChildren(
                listResources(projectId, null, null, readProperties, false, true)
            );
        }
        return project;
    }

    @Override
    public Object getProjectProperty(@NotNull String projectId, @NotNull String propName) throws DBException {
        return getProject(projectId).getProjectProperty(propName);
    }

    @Override
    public void setProjectProperty(
        @NotNull String projectId,
        @NotNull String propName,
        @NotNull Object propValue
    ) throws DBException {
        getProject(projectId).setProjectProperty(propName, propValue);
    }

    @Override
    public String getProjectsDataSources(@NotNull String projectId, @Nullable String[] dataSourceIds) throws DBException {
        throw new DBCFeatureNotSupportedException();
    }

    @Override
    public void createProjectDataSources(
        @NotNull String projectId,
        @NotNull String configuration,
        @Nullable List<String> dataSourceIds
    ) throws DBException {
        throw new DBCFeatureNotSupportedException();
    }

    @Override
    public boolean updateProjectDataSources(
        @NotNull String projectId,
        @NotNull String configuration,
        @Nullable List<String> dataSourceIds
    ) throws DBException {
        throw new DBCFeatureNotSupportedException();
    }

    @Override
    public void deleteProjectDataSources(@NotNull String projectId,
                                         @NotNull String[] dataSourceIds) throws DBException {
        throw new DBCFeatureNotSupportedException();
    }

    @Override
    public void createProjectDataSourceFolder(@NotNull String projectId,
                                              @NotNull String folderPath) throws DBException {
        try (var projectLock = lockController.lockProject(projectId, "createDatasourceFolder")) {
            DBPProject project = getProject(projectId);
            DBPDataSourceRegistry registry = project.getDataSourceRegistry();
            var result = Path.of(folderPath);
            var newName = result.getFileName().toString();
            var parent = result.getParent();
            var parentFolder = parent == null ? null : registry.getFolder(parent.toString().replace("\\", "/"));
            DBPDataSourceFolder newFolder = registry.addFolder(parentFolder, newName);
            registry.checkForErrors();
        }
    }

    @Override
    public void deleteProjectDataSourceFolders(
        @NotNull String projectId,
        @NotNull String[] folderPaths,
        boolean dropContents
    ) throws DBException {
        try (var projectLock = lockController.lockProject(projectId, "createDatasourceFolder")) {
            DBPProject project = getProject(projectId);
            DBPDataSourceRegistry registry = project.getDataSourceRegistry();
            for (String folderPath : folderPaths) {
                DBPDataSourceFolder folder = registry.getFolder(folderPath);
                if (folder != null) {
                    registry.removeFolder(folder, dropContents);
                } else {
                    log.warn("Can not find folder by path [" + folderPath + "] for deletion");
                }
            }
            registry.checkForErrors();
        }
    }

    @Override
    public void moveProjectDataSourceFolder(
        @NotNull String projectId,
        @NotNull String oldPath,
        @NotNull String newPath
    ) throws DBException {
        try (var projectLock = lockController.lockProject(projectId, "createDatasourceFolder")) {
            DBPProject project = getProject(projectId);
            DBPDataSourceRegistry registry = project.getDataSourceRegistry();
            registry.moveFolder(oldPath, newPath);
            registry.checkForErrors();
        }
    }

    @NotNull
    @Override
    public RMResource[] listResources(
        @NotNull String projectId,
        @Nullable String folder,
        @Nullable String nameMask,
        boolean readProperties,
        boolean readHistory,
        boolean recursive
    ) throws DBException {
        Path projectPath = getProjectPath(projectId);
        if (!Files.exists(projectPath)) {
            return new RMResource[0];
        }
        try {
            Path folderPath = CommonUtils.isEmpty(folder) ?
                projectPath :
                projectPath.resolve(folder);
            folderPath = folderPath.normalize();
            // Test that folder is inside the project
            if (!folderPath.startsWith(projectPath)) {
                throw new DBException("Invalid folder path");
            }
            createFolder(folderPath);
            return readChildResources(projectId, folderPath, nameMask, readProperties, readHistory, recursive);
        } catch (NoSuchFileException e) {
            throw new DBException("Invalid resource folder " + folder);
        } catch (IOException e) {
            throw new DBException("Error reading resources", e);
        }
    }

    @NotNull
    private RMResource[] readChildResources(
        @NotNull String projectId,
        @NotNull Path folderPath,
        @Nullable String nameMask,
        boolean readProperties,
        boolean readHistory,
        boolean recursive
    ) throws IOException {
        try (Stream<Path> files = Files.list(folderPath)) {
            return files.filter(path -> {
                    String fileName = path.getFileName().toString();
                    return (nameMask == null || nameMask.equals(fileName)) && !fileName.startsWith(".");
                }) // skip hidden files
                .sorted(Comparator.comparing(path -> path.getFileName().toString(), String.CASE_INSENSITIVE_ORDER))
                .map((Path path) -> makeResourceFromPath(projectId, path, nameMask, readProperties, readHistory, recursive))
                .filter(Objects::nonNull)
                .toArray(RMResource[]::new);
        }
    }

    @Override
    public String createResource(
        @NotNull String projectId,
        @NotNull String resourcePath,
        boolean isFolder
    ) throws DBException {
        try (var projectLock = lockController.lockProject(projectId, "createResource")) {
            validateResourcePath(resourcePath);
            Path targetPath = getTargetPath(projectId, resourcePath);
            if (Files.exists(targetPath)) {
                throw new DBException("Resource '" + resourcePath + "' already exists");
            }
            createFolder(targetPath.getParent());
            try {
                if (isFolder) {
                    Files.createDirectories(targetPath);
                } else {
                    Files.createFile(targetPath);
                }
            } catch (IOException e) {
                throw new DBException("Error creating resource '" + resourcePath + "'", e);
            }
        }
        return DEFAULT_CHANGE_ID;
    }


    @Override
    public String moveResource(
        @NotNull String projectId,
        @NotNull String oldResourcePath,
        @NotNull String newResourcePath
    ) throws DBException {
        try (var projectLock = lockController.lockProject(projectId, "moveResource")) {
            var normalizedOldResourcePath = CommonUtils.normalizeResourcePath(oldResourcePath);
            var normalizedNewResourcePath = CommonUtils.normalizeResourcePath(newResourcePath);
            if (log.isDebugEnabled()) {
                log.debug("Moving resource from '" + normalizedOldResourcePath + "' to '" + normalizedNewResourcePath + "'");
            }
            Path oldTargetPath = getTargetPath(projectId, normalizedOldResourcePath);

            if (!Files.exists(oldTargetPath)) {
                throw new DBException("Resource '" + oldTargetPath + "' doesn't exists");
            }
            Path newTargetPath = getTargetPath(projectId, normalizedNewResourcePath);
            validateResourcePath(newTargetPath.toString());
            try {
                Files.move(oldTargetPath, newTargetPath);
            } catch (IOException e) {
                throw new DBException("Error moving resource '" + normalizedOldResourcePath + "'", e);
            }

            log.debug("Moving resource properties");
            try {
                movePropertiesRecursive(projectId, newTargetPath, normalizedOldResourcePath, normalizedNewResourcePath);
            } catch (IOException | DBException e) {
                throw new DBException("Unable to move resource properties", e);
            }
        }

        return DEFAULT_CHANGE_ID;
    }

    /**
     * Iterates the tree starting at {@code rootResourcePath}.
     * Calculates for each file/folder {@code newResourcePropertiesPath} and restores {@code oldResourcePropertiesPath}
     * by replacing the first {@code newRootPropertiesPath} with {@code oldRootPropertiesPath} in {@code newResourcePropertiesPath}.
     * Gathers the old-new properties paths pairs and updates properties via BaseProjectImpl#moveResourcePropertiesBatch()
     */
    private void movePropertiesRecursive(
        @NotNull String projectId,
        @NotNull Path rootResourcePath,
        @NotNull String oldRootPropertiesPath,
        @NotNull String newRootPropertiesPath
    ) throws IOException, DBException {
        var project = getProject(projectId);
        var projectPath = getProjectPath(projectId);
        var propertiesPathsList = new ArrayList<Pair<String, String>>();
        Files.walkFileTree(rootResourcePath, (UniversalFileVisitor<Path>) (path, attrs) -> {
            var newResourcePropertiesPath = CommonUtils.normalizeResourcePath(projectPath.relativize(path.toAbsolutePath()).toString());
            var oldResourcePropertiesPath = newResourcePropertiesPath.replaceFirst(newRootPropertiesPath, oldRootPropertiesPath);
            propertiesPathsList.add(new Pair<>(oldResourcePropertiesPath, newResourcePropertiesPath));
            return FileVisitResult.CONTINUE;
        });
        if (log.isDebugEnabled()) {
            log.debug("Move resources properties:\n" + propertiesPathsList);
        }
        project.moveResourceProperties(oldRootPropertiesPath, newRootPropertiesPath);
    }

    @Override
    public void deleteResource(@NotNull String projectId, @NotNull String resourcePath, boolean recursive) throws DBException {
        try (var projectLock = lockController.lockProject(projectId, "deleteResource")) {
            if (log.isDebugEnabled()) {
                log.debug("Removing resource from '" + resourcePath + "'" + (recursive ? " recursive" : ""));
            }
            validateResourcePath(resourcePath);
            Path targetPath = getTargetPath(projectId, resourcePath);
            if (!Files.exists(targetPath)) {
                throw new DBException("Resource '" + resourcePath + "' doesn't exists");
            }
            try {
                if (targetPath.toFile().isDirectory()) {
                    IOUtils.deleteDirectory(targetPath);
                } else {
                    Files.delete(targetPath);
                }
            } catch (IOException e) {
                throw new DBException("Error deleting resource '" + resourcePath + "'", e);
            }
        }
    }

    @Override
    public RMResource[] getResourcePath(@NotNull String projectId, @NotNull String resourcePath) throws DBException {
        return makeResourcePath(projectId, getTargetPath(projectId, resourcePath), false).toArray(RMResource[]::new);
    }

    @NotNull
    @Override
    public byte[] getResourceContents(@NotNull String projectId, @NotNull String resourcePath) throws DBException {
        validateResourcePath(resourcePath);
        Path targetPath = getTargetPath(projectId, resourcePath);
        if (!Files.exists(targetPath)) {
            throw new DBException("Resource '" + resourcePath + "' doesn't exists");
        }
        try {
            return Files.readAllBytes(targetPath);
        } catch (IOException e) {
            throw new DBException("Error reading resource '" + resourcePath + "'", e);
        }
    }

    @NotNull
    @Override
    public String setResourceContents(
        @NotNull String projectId,
        @NotNull String resourcePath,
        @NotNull byte[] data,
        boolean forceOverwrite
    ) throws DBException {
        try (var lock = lockController.lockProject(projectId, "setResourceContents")) {
            validateResourcePath(resourcePath);
            Path targetPath = getTargetPath(projectId, resourcePath);
            if (!forceOverwrite && Files.exists(targetPath)) {
                throw new DBException("Resource '" + IOUtils.getFileNameWithoutExtension(targetPath) + "' already exists");
            }
            createFolder(targetPath.getParent());
            try {
                Files.write(targetPath, data);
            } catch (IOException e) {
                throw new DBException("Error writing resource '" + resourcePath + "'", e);
            }
        }
        return DEFAULT_CHANGE_ID;
    }

    protected void createFolder(Path targetPath) throws DBException {
        if (!Files.exists(targetPath)) {
            try {
                Files.createDirectories(targetPath);
            } catch (IOException e) {
                throw new DBException("Error creating folder '" + targetPath + "'");
            }
        }
    }

    @NotNull
    @Override
    public String setResourceProperty(
        @NotNull String projectId,
        @NotNull String resourcePath,
        @NotNull String propertyName,
        @Nullable Object propertyValue
    ) throws DBException {
        try (var projectLock = lockController.lockProject(projectId, "resourcePropertyUpdate")) {
            validateResourcePath(resourcePath);
            DBPProject project = getProject(projectId);
            project.setResourceProperty(resourcePath, propertyName, propertyValue);
            return DEFAULT_CHANGE_ID;
        }
    }

    private void validateResourcePath(String resourcePath) throws DBException {
        validateResourcePath(resourcePath, FILE_REGEX);
    }

    private void validateResourcePath(String resourcePath, String regex) throws DBException {
        var fullPath = Paths.get(resourcePath);
        for (Path path : fullPath) {
            if (path.toString().startsWith(".")) {
                throw new DBException("Resource path '" + resourcePath + "' can't start with dot");
            }
        }
        if (!resourcePath.matches(regex)) {
            String illegalCharacters = resourcePath.replaceAll(regex, " ").strip();
            throw new DBException("Resource path '" + resourcePath + "' contains illegal characters: " + illegalCharacters);
        }
    }

    @NotNull
    private Path getTargetPath(@NotNull String projectId, @NotNull String resourcePath) throws DBException {
        Path projectPath = getProjectPath(projectId);
        if (!Files.exists(projectPath)) {
            try {
                Files.createDirectories(projectPath);
            } catch (IOException e) {
                throw new DBException("Error creating project path", e);
            }
        }
        try {
            while (resourcePath.startsWith("/")) resourcePath = resourcePath.substring(1);
            Path targetPath = projectPath.resolve(resourcePath).normalize();
            if (!targetPath.startsWith(projectPath)) {
                throw new DBException("Invalid resource path");
            }
            return targetPath;
        } catch (InvalidPathException e) {
            throw new DBException("Resource path contains invalid characters");
        }
    }


    private String makeProjectIdFromPath(Path path, RMProjectType type) {
        String projectName = path.getFileName().toString();
        return type.getPrefix() + "_" + projectName;
    }

    private RMProject makeProjectFromId(String projectId) throws DBException {
        var projectName = parseProjectName(projectId);
        var projectPath = getProjectPath(projectId);
        return makeProjectFromPath(projectPath, Set.of(), projectName.getType(), false);
    }

    private RMProject makeProjectFromPath(Path path, Set<RMProjectPermission> permissions, RMProjectType type, boolean checkExistence) {
        if (path == null) {
            return null;
        }
        if (Files.exists(path)) {
            if (!Files.isDirectory(path)) {
                log.error("Project path " + path + " is not a directory");
                return null;
            }
        } else if (checkExistence) {
            return null;
        }

        String[] allProjectPermissions = permissions.stream()
            .flatMap(rmProjectPermission -> rmProjectPermission.getAllPermissions().stream())
            .toArray(String[]::new);

        RMProject project = new RMProject();
        String projectName = path.getFileName().toString();
        project.setName(projectName);
        project.setId(makeProjectIdFromPath(path, type));
        project.setType(type);
        project.setProjectPermissions(allProjectPermissions);
        if (Files.exists(path)) {
            try {
                project.setCreateTime(
                    OffsetDateTime.ofInstant(Files.getLastModifiedTime(path).toInstant(), ZoneId.of("UTC")).toInstant().toEpochMilli());
            } catch (IOException e) {
                log.error(e);
            }
        }
        return project;
    }

    protected Path getProjectPath(String projectId) throws DBException {
        return getProject(projectId).getAbsolutePath();
    }

    private @NotNull List<RMResource> makeResourcePath(@NotNull String projectId, @NotNull Path targetPath, boolean recursive) throws DBException {
        var projectPath = getProjectPath(projectId);
        var relativeResourcePath = projectPath.relativize(targetPath.toAbsolutePath());
        var resourcePath = projectPath;

        var result = new ArrayList<RMResource>();

        for (var resourceName : relativeResourcePath) {
            resourcePath = resourcePath.resolve(resourceName);
            result.add(makeResourceFromPath(projectId, resourcePath, null, false, false, recursive));
        }

        return result;
    }

    private RMResource makeResourceFromPath(
        @NotNull String projectId,
        @NotNull Path path,
        @Nullable String nameMask,
        boolean readProperties,
        boolean readHistory,
        boolean recursive
    ) {
        if (Files.notExists(path)) {
            return null;
        }
        RMResource resource = new RMResource();
        resource.setName(path.getFileName().toString());
        resource.setFolder(Files.isDirectory(path));
        if (!resource.isFolder()) {
            try {
                resource.setLastModified(
                    Files.getLastModifiedTime(path).toMillis());
            } catch (IOException e) {
                log.debug("Error getting last modified time: " + e.getMessage());
            }
        }
        try {
            if (!resource.isFolder()) {
                resource.setLength(Files.size(path));
            }
            if (readHistory) {
                resource.setChanges(
                    Collections.singletonList(
                        new RMResourceChange(
                            DEFAULT_CHANGE_ID,
                            new Date(Files.getLastModifiedTime(path).toMillis()),
                            null
                        ))
                );
            }
            if (readProperties) {
                final DBPProject project = getProject(projectId);
                final String resourcePath = getProjectRelativePath(projectId, path);
                final Map<String, Object> properties = project.getResourceProperties(resourcePath);

                if (properties != null && !properties.isEmpty()) {
                    resource.setProperties(new LinkedHashMap<>(properties));
                }
            }
        } catch (Exception e) {
            log.error(e);
        }

        if (recursive && resource.isFolder()) {
            try {
                resource.setChildren(readChildResources(projectId, path, nameMask, readProperties, readHistory, true));
            } catch (IOException e) {
                log.error(e);
            }
        }

        return resource;
    }

    @NotNull
    private String getProjectRelativePath(@NotNull String projectId, @NotNull Path path) throws DBException {
        return getProjectPath(projectId).toAbsolutePath().relativize(path).toString().replace('\\', IPath.SEPARATOR);
    }

    public static class RMProjectName {
        String prefix;
        String name;
        private RMProjectName(String prefix, String name) {
            this.prefix = prefix;
            this.name = name;
        }

        public String getPrefix() {
            return prefix;
        }

        public String getName() {
            return name;
        }

        public RMProjectType getType() {
            return RMProjectType.getByPrefix(prefix);
        }
    }

    public static RMProjectName parseProjectName(String projectId) throws DBException {
        if (CommonUtils.isEmpty(projectId)) {
            throw new DBException("Project id is empty");
        }
        return parseProjectNameUnsafe(projectId);
    }

    private static RMProjectName parseProjectNameUnsafe(String projectId) {
        String prefix;
        String name;
        int divPos = projectId.indexOf("_");
        if (divPos < 0) {
            prefix = RMProjectType.USER.getPrefix();
            name = projectId;
        } else {
            prefix = projectId.substring(0, divPos);
            name = projectId.substring(divPos + 1);
        }
        return new RMProjectName(prefix, name);
    }
}

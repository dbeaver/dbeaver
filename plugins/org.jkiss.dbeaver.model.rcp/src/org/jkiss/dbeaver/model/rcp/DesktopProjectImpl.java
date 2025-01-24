/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.model.rcp;

import org.eclipse.core.internal.localstore.Bucket;
import org.eclipse.core.internal.localstore.BucketTree;
import org.eclipse.core.internal.properties.PropertyBucket;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPWorkspaceEclipse;
import org.jkiss.dbeaver.model.auth.SMSessionContext;
import org.jkiss.dbeaver.model.fs.DBFResourceAdapter;
import org.jkiss.dbeaver.model.fs.DBFVirtualFileSystemRoot;
import org.jkiss.dbeaver.model.fs.nio.EFSNIOFile;
import org.jkiss.dbeaver.model.fs.nio.EFSNIOFileSystemRoot;
import org.jkiss.dbeaver.model.fs.nio.EFSNIOFolder;
import org.jkiss.dbeaver.model.impl.app.BaseProjectImpl;
import org.jkiss.dbeaver.model.impl.app.BaseWorkspaceImpl;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.task.DBTTaskManager;
import org.jkiss.dbeaver.registry.DesktopDataSourceRegistry;
import org.jkiss.dbeaver.registry.task.TaskConstants;
import org.jkiss.dbeaver.registry.task.TaskManagerImpl;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public class DesktopProjectImpl extends BaseProjectImpl implements RCPProject, DBFResourceAdapter {

    private static final Log log = Log.getLog(DesktopProjectImpl.class);

    private static final String SETTINGS_FOLDER = ".settings";
    private static final String PROJECT_FILE = ".project";

    private static final String EMPTY_PROJECT_TEMPLATE = """
        <?xml version="1.0" encoding="UTF-8"?>
        <projectDescription>
        <name>${project-name}</name>
        <comment></comment>
        <projects>
        </projects>
        <buildSpec>
        </buildSpec>
        <natures>
        </natures>
        </projectDescription>""";

    @NotNull
    private final IProject project;
    protected volatile DBTTaskManager taskManager;

    private volatile boolean projectInvalidated;

    public DesktopProjectImpl(@NotNull BaseWorkspaceImpl workspace, @NotNull IProject project, @Nullable SMSessionContext sessionContext) {
        super(workspace, sessionContext);
        this.project = project;
    }

    @Override
    public boolean isVirtual() {
        return false;
    }

    @NotNull
    @Override
    public String getName() {
        return project.getName();
    }

    @NotNull
    @Override
    public Path getAbsolutePath() {
        if (project.getLocation() == null) {
            throw new IllegalStateException("Can't determine the workspace path for project " + project.getName());
        }
        return project.getLocation().toFile().toPath();
    }

    @Nullable
    @Override
    public IProject getEclipseProject() {
        return project;
    }

    @Nullable
    @Override
    public IContainer getRootResource() {
        return getEclipseProject();
    }

    @Override
    @NotNull
    public String getResourcePath(@NotNull IResource resource) {
        return resource.getProjectRelativePath().toString();
    }

    @Override
    public boolean isOpen() {
        return project.isOpen();
    }

    @Override
    public void ensureOpen() throws IllegalStateException {
        if (!project.isOpen()) {
            NullProgressMonitor monitor = new NullProgressMonitor();
            try {
                project.open(monitor);
                if (!DBWorkbench.isDistributed()) {
                    project.refreshLocal(IFile.DEPTH_ONE, monitor);
                }
            } catch (CoreException e) {
                if (getWorkspace().getPlatform().getApplication().isStandalone() &&
                    e.getMessage().contains(IProjectDescription.DESCRIPTION_FILE_NAME)) {
                    try {
                        recoverProjectDescription();
                        project.open(monitor);
                        hideConfigurationFiles();
                        project.refreshLocal(IFile.DEPTH_ONE, monitor);
                    } catch (Exception e2) {
                        log.error("Error opening project", e2);
                        return;
                    }
                }
            }
        }
        if (!projectInvalidated) {
            try {
                if (isInMemory()) {
                    setFormat(ProjectFormat.MODERN);
                    return;
                }

                if (DBWorkbench.getPlatform().getApplication() instanceof DesktopApplicationImpl && !DBWorkbench.isDistributed()) {
                    // Validate project structure only for local desktop apps
                    Path mdFolder = getMetadataFolder(false);

                    Path dsConfig = getAbsolutePath().resolve(DBPDataSourceRegistry.LEGACY_CONFIG_FILE_NAME);
                    if (!Files.exists(mdFolder) && Files.exists(dsConfig)) {
                        setFormat(ProjectFormat.LEGACY);
                    } else {
                        setFormat(ProjectFormat.MODERN);
                    }

                    // Check project structure and migrate
                    checkAndUpdateProjectStructure();
                }

                // Now project is in modern format
                setFormat(ProjectFormat.MODERN);
            } finally {
                projectInvalidated = true;
            }
        }
    }

    @Override
    public boolean isUseSecretStorage() {
        return false;
    }

    @NotNull
    protected DBPDataSourceRegistry createDataSourceRegistry() {
        return new DesktopDataSourceRegistry<>(this);
    }

    @Nullable
    @Override
    public <T> T adaptResource(DBFVirtualFileSystemRoot fsRoot, Path path, Class<T> adapter) {
        if (adapter == IResource.class) {
            return adapter.cast(createResourceFromPath(fsRoot, path));
        }
        return null;
    }

    @NotNull
    private IResource createResourceFromPath(DBFVirtualFileSystemRoot fsRoot, Path path) {
        EFSNIOFileSystemRoot root = new EFSNIOFileSystemRoot(
            getEclipseProject(),
            fsRoot,
            fsRoot.getFileSystem().getType() + "/" + fsRoot.getFileSystem().getId() + "/" + fsRoot.getRootId()
        );
        if (Files.isDirectory(path)) {
            return new EFSNIOFolder(root, path);
        } else {
            return new EFSNIOFile(root, path);
        }
    }

    @Nullable
    @Override
    public DBNModel getNavigatorModel() {
        return getWorkspace().getPlatform().getNavigatorModel();
    }

    @NotNull
    @Override
    public DBTTaskManager getTaskManager() {
        ensureOpen();
        if (taskManager == null) {
            synchronized (metadataSync) {
                if (taskManager == null) {
                    taskManager = createTaskManager();
                }
            }
        }
        return taskManager;
    }

    @Nullable
    @Override
    public DBTTaskManager getTaskManager(boolean create) {
        if (taskManager != null) {
            return taskManager;
        }
        return create ? getTaskManager() : null;
    }

    @NotNull
    protected DBTTaskManager createTaskManager() {
        return new TaskManagerImpl(
            this,
            getWorkspace().getMetadataFolder().resolve(TaskConstants.TASK_STATS_FOLDER)
        );
    }

    /**
     * Validates project files structure.
     * If project was created in older DBeaver version then converts it to newer format
     */
    private void checkAndUpdateProjectStructure() {
        if (getFormat() == ProjectFormat.UNKNOWN || getFormat() == ProjectFormat.MODERN) {
            return;
        }

        Path mdConfig = getMetadataPath().resolve(BaseProjectImpl.METADATA_STORAGE_FILE);
        if (!Files.exists(mdConfig)) {
            // Migrate
            Map<String, Map<String, Object>> projectResourceProperties = extractProjectResourceProperties();
            synchronized (metadataSync) {
                setResourceProperties(projectResourceProperties);
            }
            flushMetadata();
        }
    }

    private Map<String, Map<String, Object>> extractProjectResourceProperties() {
        Map<String, Map<String, Object>> result = new LinkedHashMap<>();

        DBPWorkspaceEclipse workspaceEclipse;
        if (getWorkspace() instanceof DBPWorkspaceEclipse) {
            workspaceEclipse = (DBPWorkspaceEclipse) getWorkspace();
        } else {
            return result;
        }
        try {
            BucketTree bucketTree = new BucketTree((Workspace) workspaceEclipse.getEclipseWorkspace(), new PropertyBucket());
            try {
                final IPath projectPath = project.getFullPath();
                bucketTree.accept(new Bucket.Visitor() {
                                      @Override
                                      public int visit(Bucket.Entry entry) {
                                          Object value = entry.getValue();
                                          if (value instanceof String[][]) {
                                              String[][] bucketProps = (String[][]) value;
                                              for (String[] resProps : bucketProps) {
                                                  if (resProps.length == 3) {
                                                      if ("org.jkiss.dbeaver".equals(resProps[0])) {
                                                          if ("sql-editor-project-id".equals(resProps[1])) {
                                                              continue;
                                                          }
                                                          Map<String, Object> propsMap = result.computeIfAbsent(
                                                              entry.getPath().makeRelativeTo(projectPath).toString(), s -> new LinkedHashMap<>());
                                                          propsMap.put(resProps[1], resProps[2]);
                                                      }
                                                  }
                                              }
                                          }
                                          return CONTINUE;
                                      }
                                  },
                    projectPath,
                    BucketTree.DEPTH_INFINITE);
            } catch (CoreException e) {
                log.error(e);
            }
        } catch (Throwable e) {
            log.error("Error extracting project metadata", e);
        }

        return result;
    }

    public void hideConfigurationFiles() {
        if (project.isOpen() && DBWorkbench.getPlatform().getApplication().isStandalone()) {
            // To avoid accidental corruption of the workspace configuration by search/replace commands,
            // we need to mark metadata folder as hidden (see dbeaver/dbeaver#20759)
            IFolder metadataFolder = project.getFolder(DBPProject.METADATA_FOLDER);
            hideResource(metadataFolder);
            IFolder settingsFolder = project.getFolder(SETTINGS_FOLDER);
            hideResource(settingsFolder);
            IFile file = project.getFile(PROJECT_FILE);
            hideResource(file);
        }
    }

    private void hideResource(IResource file) {
        if (file.exists() && !file.isHidden()) {
            try {
                file.setHidden(true);
            } catch (CoreException e) {
                log.error("Error hiding metadata folder", e);
            }
        }
    }

    public void recoverProjectDescription() throws IOException {
        // .project file missing. Let's try to create an empty project config
        Path mdFile = getAbsolutePath().resolve(IProjectDescription.DESCRIPTION_FILE_NAME);
        log.debug("Recovering project '" + project.getName() + "' metadata " + mdFile.toAbsolutePath());

        IOUtils.writeFileFromString(
            mdFile.toFile(),
            EMPTY_PROJECT_TEMPLATE.replace("${project-name}", project.getName()));
    }

    @NotNull
    public Map<String, Map<String, Object>> getAllResourceProperties() {
        this.loadMetadata();
        synchronized (resourcesSync) {
            return new TreeMap<>(this.resourceProperties);
        }
    }

    void removeResourceFromCache(IPath path) {
        boolean cacheChanged = false;
        synchronized (resourcesSync) {
            if (resourceProperties != null) {
                String resPath = CommonUtils.normalizeResourcePath(path.toString());
                cacheChanged = (resourceProperties.remove(resPath) != null);
            }
        }
        if (cacheChanged) {
            flushMetadata();
        }
    }

    void moveResourceCache(IPath oldPath, IPath newPath) {
        boolean cacheChanged = false;
        synchronized (resourcesSync) {
            if (resourceProperties != null) {
                String oldResPath = CommonUtils.normalizeResourcePath(oldPath.toString());
                Map<String, Object> props = resourceProperties.remove(oldResPath);
                if (props != null) {
                    String newResPath = CommonUtils.normalizeResourcePath(newPath.toString());
                    resourceProperties.put(newResPath, props);
                    cacheChanged = true;
                }
            }
        }
        if (cacheChanged) {
            flushMetadata();
        }
    }

}

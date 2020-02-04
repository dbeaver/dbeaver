/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPExternalFileManager;
import org.jkiss.dbeaver.model.app.*;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.LoggingProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.virtual.DBVModel;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.resource.DBeaverNature;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.SecurityUtils;

import java.io.*;
import java.util.*;

/**
 * BaseWorkspaceImpl.
 */
public abstract class BaseWorkspaceImpl implements DBPWorkspace, DBPExternalFileManager {

    private static final Log log = Log.getLog(BaseWorkspaceImpl.class);

    public static final String DEFAULT_RESOURCES_ROOT = "Resources"; //$NON-NLS-1$

    private static final String PROP_PROJECT_ACTIVE = "project.active";
    private static final String EXT_FILES_PROPS_STORE = "dbeaver-external-files.data";

    private static final String WORKSPACE_ID = "workspace-id";

    private final DBPPlatform platform;
    private final IWorkspace eclipseWorkspace;

    private final Map<IProject, ProjectMetadata> projects = new LinkedHashMap<>();
    private final ProjectListener projectListener;
    private ProjectMetadata activeProject;
    private final List<DBPProjectListener> projectListeners = new ArrayList<>();
    private final List<ResourceHandlerDescriptor> handlerDescriptors = new ArrayList<>();
    private final Map<String, Map<String, Object>> externalFileProperties = new HashMap<>();

    private final AbstractJob externalFileSaver = new WorkspaceFilesMetadataJob();

    protected BaseWorkspaceImpl(DBPPlatform platform, IWorkspace eclipseWorkspace) {
        this.platform = platform;
        this.eclipseWorkspace = eclipseWorkspace;

        String activeProjectName = platform.getPreferenceStore().getString(PROP_PROJECT_ACTIVE);

        IWorkspaceRoot root = eclipseWorkspace.getRoot();
        IProject[] allProjects = root.getProjects();
        if (ArrayUtils.isEmpty(allProjects)) {
            try {
                refreshWorkspaceContents(new LoggingProgressMonitor());
            } catch (Throwable e) {
                log.error(e);
            }
            allProjects = root.getProjects();
        }
        for (IProject project : allProjects) {
            if (project.exists() && !project.isHidden()) {
                ProjectMetadata projectMetadata = new ProjectMetadata(this, project);
                this.projects.put(project, projectMetadata);

                if (activeProject == null || (!CommonUtils.isEmpty(activeProjectName) && project.getName().equals(activeProjectName))) {
                    activeProject = projectMetadata;
                }
            }
        }

        if (activeProject != null && !activeProject.isOpen()) {
            try {
                activeProject.ensureOpen();
            } catch (IllegalStateException e) {
                log.error("Error opening active project", e);
            }
        }

        projectListener = new ProjectListener();
        eclipseWorkspace.addResourceChangeListener(projectListener);

        loadExtensions(Platform.getExtensionRegistry());
        loadExternalFileProperties();
    }

    public void initializeProjects() {
        if (DBWorkbench.getPlatform().getApplication().isStandalone() && CommonUtils.isEmpty(projects)) {
            try {
                createDefaultProject(new NullProgressMonitor());
            } catch (CoreException e) {
                log.error("Can't create default project", e);
            }
        }
        if (activeProject == null && !projects.isEmpty()) {
            // Set active project
            activeProject = projects.values().iterator().next();
            platform.getPreferenceStore().setValue(PROP_PROJECT_ACTIVE, activeProject.getName());
        }
    }

    public static Properties readWorkspaceInfo(File metadataFolder) {
        Properties props = new Properties();

        File versionFile = new File(metadataFolder, DBConstants.WORKSPACE_PROPS_FILE);
        if (versionFile.exists()) {
            try (InputStream is = new FileInputStream(versionFile)) {
                props.load(is);
            } catch (Exception e) {
                log.error(e);
            }
        }
        return props;
    }

    public static void writeWorkspaceInfo(File metadataFolder, Properties props) {
        File versionFile = new File(metadataFolder, DBConstants.WORKSPACE_PROPS_FILE);

        try (OutputStream os = new FileOutputStream(versionFile)) {
            props.store(os, "DBeaver workspace version");
        } catch (Exception e) {
            log.error(e);
        }
    }

    private void loadExtensions(IExtensionRegistry registry) {
        {
            IConfigurationElement[] extElements = registry.getConfigurationElementsFor(ResourceHandlerDescriptor.EXTENSION_ID);
            for (IConfigurationElement ext : extElements) {
                ResourceHandlerDescriptor handlerDescriptor = new ResourceHandlerDescriptor(ext);
                handlerDescriptors.add(handlerDescriptor);
            }
        }
    }

    @Override
    public void dispose() {
        this.eclipseWorkspace.removeResourceChangeListener(projectListener);

        synchronized (projects) {
            // Dispose all DS registries
            for (ProjectMetadata project : this.projects.values()) {
                project.dispose();
            }
            this.projects.clear();
        }
        DBVModel.checkGlobalCacheIsEmpty();

        // Dispose resource handlers
        for (ResourceHandlerDescriptor handlerDescriptor : this.handlerDescriptors) {
            handlerDescriptor.dispose();
        }
        this.handlerDescriptors.clear();

        if (!projectListeners.isEmpty()) {
            log.warn("Some project listeners are still register: " + projectListeners);
            projectListeners.clear();
        }
    }

    @NotNull
    @Override
    public IWorkspace getEclipseWorkspace() {
        return eclipseWorkspace;
    }

    @Override
    public List<DBPProject> getProjects() {
        return new ArrayList<>(projects.values());
    }

    @Override
    public DBPProject getActiveProject() {
        return activeProject;
    }

    @Override
    public void setActiveProject(DBPProject project) {
        ProjectMetadata oldActiveProject = this.activeProject;
        this.activeProject = (ProjectMetadata) project;

        platform.getPreferenceStore().setValue(
            PROP_PROJECT_ACTIVE, project == null ? "" : project.getName());

        fireActiveProjectChange(oldActiveProject, this.activeProject);
    }

    @Override
    public DBPProject getProject(IProject project) {
        return projects.get(project);
    }

    @Override
    public DBPProject getProject(String projectName) {
        IProject eProject = eclipseWorkspace.getRoot().getProject(projectName);
        if (!eProject.exists()) {
            return null;
        }
        return getProject(eProject);
    }

    @Override
    public void refreshWorkspaceContents(DBRProgressMonitor monitor) throws DBException {
        try {
            IWorkspaceRoot root = eclipseWorkspace.getRoot();

            root.refreshLocal(IResource.DEPTH_ONE, monitor.getNestedMonitor());

            File workspaceLocation = root.getLocation().toFile();
            if (!workspaceLocation.exists()) {
                // Nothing to refresh
                return;
            }

            // Remove unexistent projects
            for (IProject project : root.getProjects()) {
                File projectDir = project.getLocation().toFile();
                if (!projectDir.exists()) {
                    monitor.subTask("Removing unexistent project '" + project.getName() + "'");
                    project.delete(false, true, monitor.getNestedMonitor());
                }
            }

            File[] wsFiles = workspaceLocation.listFiles();
            if (!ArrayUtils.isEmpty(wsFiles)) {
                // Add missing projects
                monitor.beginTask("Refreshing workspace contents", wsFiles.length);
                for (File wsFile : wsFiles) {
                    if (!wsFile.isDirectory() || wsFile.isHidden() || wsFile.getName().startsWith(".")) {
                        // skip regular files
                        continue;
                    }
                    File projectConfig = new File(wsFile, ".project");
                    if (projectConfig.exists()) {
                        String projectName = wsFile.getName();
                        IProject project = root.getProject(projectName);
                        if (project.exists()) {
                            continue;
                        }
                        try {
                            monitor.subTask("Adding project '" + projectName + "'");
                            project.create(monitor.getNestedMonitor());
                        } catch (CoreException e) {
                            log.error("Error adding project '" + projectName + "' to workspace");
                        }
                    }
                }
            }

        } catch (Throwable e) {
            log.error("Error refreshing workspce contents", e);
        }
    }

    @Override
    public void addProjectListener(DBPProjectListener listener) {
        synchronized (projectListeners) {
            projectListeners.add(listener);
        }
    }

    @Override
    public void removeProjectListener(DBPProjectListener listener) {
        synchronized (projectListeners) {
            projectListeners.remove(listener);
        }
    }

    @NotNull
    @Override
    public DBPPlatform getPlatform() {
        return platform;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @NotNull
    @Override
    public File getAbsolutePath() {
        return eclipseWorkspace.getRoot().getLocation().toFile();
    }

    @NotNull
    @Override
    public File getMetadataFolder() {
        return GeneralUtils.getMetadataFolder(getAbsolutePath());
    }

    public void save(DBRProgressMonitor monitor) throws DBException {
        try {
            eclipseWorkspace.save(true, monitor.getNestedMonitor());
        } catch (CoreException e) {
            throw new DBException("Error saving Eclipse workspace", e);
        }
    }

    private IProject createDefaultProject(IProgressMonitor monitor) throws CoreException {
        final String baseProjectName = DBWorkbench.getPlatform().getApplication().getDefaultProjectName();
        String projectName = baseProjectName;
        for (int i = 1; ; i++) {
            final IProject project = eclipseWorkspace.getRoot().getProject(projectName);
            if (project.exists()) {
                projectName = baseProjectName + i;
                continue;
            }
            project.create(monitor);
            project.open(monitor);
            final IProjectDescription description = eclipseWorkspace.newProjectDescription(project.getName());
            description.setComment("General DBeaver project");
            description.setNatureIds(new String[]{DBeaverNature.NATURE_ID});
            project.setDescription(description, monitor);

            return project;
        }
    }

    private ResourceHandlerDescriptor getHandlerDescriptor(String id) {
        for (ResourceHandlerDescriptor rhd : handlerDescriptors) {
            if (rhd.getId().equals(id)) {
                return rhd;
            }
        }
        return null;
    }

    private ResourceHandlerDescriptor getHandlerDescriptorByRootPath(DBPProject project, String path) {
        for (ResourceHandlerDescriptor rhd : handlerDescriptors) {
            String defaultRoot = rhd.getDefaultRoot(project);
            if (defaultRoot != null && defaultRoot.equals(path)) {
                return rhd;
            }
        }
        return null;
    }

    @Override
    public DBPResourceHandler getResourceHandler(IResource resource) {
        if (resource == null || resource.isHidden() || resource.isPhantom()) {
            // Skip not accessible hidden and phantom resources
            return null;
        }
        if (resource.getParent() instanceof IProject && resource.getName().startsWith(DataSourceRegistry.LEGACY_CONFIG_FILE_PREFIX)) {
            // Skip connections settings file
            // TODO: remove in some older version
            return null;
        }
        // Check resource is synced
        if (resource instanceof IFile && !resource.isSynchronized(IResource.DEPTH_ZERO)) {
            ContentUtils.syncFile(new VoidProgressMonitor(), resource);
        }

        // Find handler
        DBPResourceHandler handler = null;
        for (ResourceHandlerDescriptor rhd : handlerDescriptors) {
            if (rhd.canHandle(resource)) {
                handler = rhd.getHandler();
                break;
            }
        }
        if (handler == null && resource instanceof IFolder) {
            final IProject eclipseProject = resource.getProject();
            DBPProject project = projects.get(eclipseProject);
            IPath relativePath = resource.getFullPath().makeRelativeTo(eclipseProject.getFullPath());
            while (relativePath.segmentCount() > 0) {
                String folderPath = relativePath.toString();
                ResourceHandlerDescriptor handlerDescriptor = getHandlerDescriptorByRootPath(project, folderPath);
                if (handlerDescriptor != null) {
                    handler = handlerDescriptor.getHandler();
                }
                relativePath = relativePath.removeLastSegments(1);
            }
        }
        if (handler == null) {
            handler = DBWorkbench.getPlatform().getDefaultResourceHandler();
        }
        return handler;
    }

    @Override
    public DBPResourceHandlerDescriptor[] getAllResourceHandlers() {
        return handlerDescriptors.toArray(new DBPResourceHandlerDescriptor[0]);
    }

    @Override
    public IFolder getResourceDefaultRoot(DBPProject project, DBPResourceHandlerDescriptor rhd, boolean forceCreate) {
        if (project == null) {
            return null;
        }
        String defaultRoot = rhd.getDefaultRoot(project);
        if (defaultRoot == null) {
            // No root
            return null;
        }
        final IFolder realFolder = project.getEclipseProject().getFolder(defaultRoot);

        if (forceCreate && !realFolder.exists()) {
            try {
                realFolder.create(true, true, new NullProgressMonitor());
            } catch (CoreException e) {
                log.error("Can't create '" + rhd.getName() + "' root folder '" + realFolder.getName() + "'", e);
                return realFolder;
            }
        }
        return realFolder;
    }

    @Override
    public IFolder getResourceDefaultRoot(DBPProject project, Class<? extends DBPResourceHandler> handlerType, boolean forceCreate) {
        if (project == null) {
            return null;
        }
        for (ResourceHandlerDescriptor rhd : handlerDescriptors) {
            DBPResourceHandler handler = rhd.getHandler();
            if (handler != null && handler.getClass() == handlerType) {
                String defaultRoot = rhd.getDefaultRoot(project);
                if (defaultRoot == null) {
                    // No root
                    return null;
                }
                final IFolder realFolder = project.getEclipseProject().getFolder(defaultRoot);

                if (forceCreate && !realFolder.exists()) {
                    try {
                        realFolder.create(true, true, new NullProgressMonitor());
                    } catch (CoreException e) {
                        log.error("Can't create '" + rhd.getName() + "' root folder '" + realFolder.getName() + "'", e);
                        return realFolder;
                    }
                }
                return realFolder;
            }
        }
        return project.getEclipseProject().getFolder(DEFAULT_RESOURCES_ROOT);
    }

/*
    @Override
    @Nullable
    public DataSourceRegistry getDataSourceRegistry(DBPProject project)
    {
        if (project == null) {
            log.warn("No active project - can't get datasource registry");
            return null;
        }
        if (!project.isOpen()) {
            log.warn("Project '" + project.getName() + "' is not open - can't get datasource registry");
            return null;
        }
        DataSourceRegistry registry;
        DataSourceRegistry registry2 = null;
        synchronized (projectDatabases) {
            registry = projectDatabases.get(project);
        }
        if (registry == null) {
            registry = new DataSourceRegistry(DBWorkbench.getPlatform(), project);
            synchronized (projectDatabases) {
                registry2 = projectDatabases.get(project);
                if (registry2 == null) {
                    projectDatabases.put(project, registry);
                }
            }
        }
        if (registry2 != null) {
            registry.dispose();
            return registry2;
        }
        return registry;
    }
*/

    @Override
    public DBPDataSourceRegistry getDefaultDataSourceRegistry() {
        return activeProject == null ? null : activeProject.getDataSourceRegistry();
    }

    @Override
    public DBPResourceHandlerDescriptor[] getResourceHandlerDescriptors() {
        DBPResourceHandlerDescriptor[] result = new DBPResourceHandlerDescriptor[handlerDescriptors.size()];
        for (int i = 0; i < handlerDescriptors.size(); i++) {
            result[i] = handlerDescriptors.get(i);
        }
        return result;
    }

    @Override
    public Map<String, Object> getFileProperties(File file) {
        synchronized (externalFileProperties) {
            return externalFileProperties.get(file.getAbsolutePath());
        }
    }

    @Override
    public Object getFileProperty(File file, String property) {
        synchronized (externalFileProperties) {
            final Map<String, Object> fileProps = externalFileProperties.get(file.getAbsolutePath());
            return fileProps == null ? null : fileProps.get(property);
        }
    }

    @Override
    public void setFileProperty(File file, String property, Object value) {
        synchronized (externalFileProperties) {
            final String filePath = file.getAbsolutePath();
            Map<String, Object> fileProps = externalFileProperties.get(filePath);
            if (fileProps == null) {
                fileProps = new HashMap<>();
                externalFileProperties.put(filePath, fileProps);
            }
            if (value == null) {
                fileProps.remove(property);
            } else {
                fileProps.put(property, value);
            }
        }

        saveExternalFileProperties();
    }

    @Override
    public Map<String, Map<String, Object>> getAllFiles() {
        synchronized (externalFileProperties) {
            return new LinkedHashMap<>(externalFileProperties);
        }
    }

    private void loadExternalFileProperties() {
        synchronized (externalFileProperties) {
            externalFileProperties.clear();
            File propsFile = new File(
                GeneralUtils.getMetadataFolder(),
                EXT_FILES_PROPS_STORE);
            if (propsFile.exists()) {
                try (InputStream is = new FileInputStream(propsFile)) {
                    try (ObjectInputStream ois = new ObjectInputStream(is)) {
                        final Object object = ois.readObject();
                        if (object instanceof Map) {
                            externalFileProperties.putAll((Map) object);
                        } else {
                            log.error("Bad external files properties data format: " + object);
                        }
                    }
                } catch (Exception e) {
                    log.error("Error saving external files properties", e);
                }
            }
        }
    }

    private void saveExternalFileProperties() {
        synchronized (externalFileProperties) {
            externalFileSaver.schedule(100);
        }
    }

    private void fireActiveProjectChange(ProjectMetadata oldActiveProject, ProjectMetadata activeProject) {
        for (DBPProjectListener listener : getListenersCopy()) {
            listener.handleActiveProjectChange(oldActiveProject, activeProject);
        }
    }

    private void fireProjectAdd(ProjectMetadata project) {
        for (DBPProjectListener listener : getListenersCopy()) {
            listener.handleProjectAdd(project);
        }
    }

    private void fireProjectRemove(ProjectMetadata project) {
        for (DBPProjectListener listener : getListenersCopy()) {
            listener.handleProjectRemove(project);
        }
    }

    @NotNull
    private DBPProjectListener[] getListenersCopy() {
        DBPProjectListener[] listeners;
        synchronized (projectListeners) {
            listeners = projectListeners.toArray(new DBPProjectListener[0]);
        }
        return listeners;
    }

    private class ProjectListener implements IResourceChangeListener {
        @Override
        public void resourceChanged(IResourceChangeEvent event) {
            if (event.getType() == IResourceChangeEvent.POST_CHANGE) {
                IResourceDelta delta = event.getDelta();
                for (IResourceDelta childDelta : delta.getAffectedChildren()) {
                    if (childDelta.getResource() instanceof IProject) {
                        IProject project = (IProject) childDelta.getResource();
                        if (!projects.containsKey(project)) {
                            if (childDelta.getKind() == IResourceDelta.ADDED) {
                                ProjectMetadata projectMetadata = new ProjectMetadata(BaseWorkspaceImpl.this, project);
                                projects.put(project, projectMetadata);
                                fireProjectAdd(projectMetadata);
                                if (activeProject == null) {
                                    activeProject = projectMetadata;
                                    fireActiveProjectChange(null, activeProject);
                                }
                            } else {
                                // Project not found - report an error
                                log.error("Project '" + childDelta.getResource().getName() + "' not found in workspace");
                            }
                        } else {
                            if (childDelta.getKind() == IResourceDelta.REMOVED) {
                                // Project deleted
                                ProjectMetadata projectMetadata = projects.remove(project);
                                fireProjectRemove(projectMetadata);
                                if (projectMetadata == activeProject) {
                                    activeProject = null;
                                    fireActiveProjectChange(projectMetadata, null);
                                }
                            } else {
                                // Some changes within project - reflect them in metadata cache
                                ProjectMetadata projectMetadata = projects.get(project);
                                if (projectMetadata != null) {
                                    handleResourceChange(projectMetadata, childDelta);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void handleResourceChange(ProjectMetadata projectMetadata, IResourceDelta delta) {
        if (delta.getKind() == IResourceDelta.REMOVED) {
            IPath movedToPath = delta.getMovedToPath();
            if (movedToPath != null) {
                IPath oldPath = delta.getProjectRelativePath();
                IPath newPath = movedToPath.makeRelativeTo(projectMetadata.getEclipseProject().getFullPath());
                projectMetadata.updateResourceCache(oldPath, newPath);
            } else {
                projectMetadata.removeResourceFromCache(delta.getProjectRelativePath());
            }
        } else {
            for (IResourceDelta childDelta : delta.getAffectedChildren(IResourceDelta.ALL_WITH_PHANTOMS, IContainer.INCLUDE_HIDDEN)) {
                handleResourceChange(projectMetadata, childDelta);
            }
        }
    }

    public static String readWorkspaceId() {
        // Check workspace ID
        Properties workspaceInfo = BaseWorkspaceImpl.readWorkspaceInfo(GeneralUtils.getMetadataFolder());
        String workspaceId = workspaceInfo.getProperty(WORKSPACE_ID);
        if (CommonUtils.isEmpty(workspaceId)) {
            // Generate new UUID
            workspaceId = "D" + Long.toString(
                Math.abs(SecurityUtils.generateRandomLong()),
                36).toUpperCase();
            workspaceInfo.setProperty(WORKSPACE_ID, workspaceId);
            BaseWorkspaceImpl.writeWorkspaceInfo(GeneralUtils.getMetadataFolder(), workspaceInfo);
        }
        return workspaceId;
    }

    private class WorkspaceFilesMetadataJob extends AbstractJob {
        public WorkspaceFilesMetadataJob() {
            super("External files metadata saver");
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            synchronized (externalFileProperties) {
                File propsFile = new File(
                    GeneralUtils.getMetadataFolder(),
                    EXT_FILES_PROPS_STORE);
                try (OutputStream os = new FileOutputStream(propsFile)) {
                    try (ObjectOutputStream oos = new ObjectOutputStream(os)) {
                        oos.writeObject(externalFileProperties);
                    }
                } catch (Exception e) {
                    log.error("Error saving external files properties", e);
                }
            }
            return Status.OK_STATUS;
        }
    }
}

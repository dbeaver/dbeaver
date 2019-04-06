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
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPExternalFileManager;
import org.jkiss.dbeaver.model.app.*;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.resource.DBeaverNature;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectRegistry implements DBPProjectManager, DBPExternalFileManager {
    private static final Log log = Log.getLog(ProjectRegistry.class);

    public static final String DEFAULT_RESOURCES_ROOT = "Resources"; //$NON-NLS-1$

    private static final String PROP_PROJECT_ACTIVE = "project.active";
    private static final String EXT_FILES_PROPS_STORE = "dbeaver-external-files.data";

    private final List<ResourceHandlerDescriptor> handlerDescriptors = new ArrayList<>();

    private final Map<IProject, DataSourceRegistry> projectDatabases = new HashMap<>();
    private IProject activeProject;
    private IWorkspace workspace;

    private final Map<String, Map<String, Object>> externalFileProperties = new HashMap<>();

    private final List<DBPProjectListener> projectListeners = new ArrayList<>();

    public ProjectRegistry(IWorkspace workspace)
    {
        this.workspace = workspace;
        loadExtensions(Platform.getExtensionRegistry());
        loadExternalFileProperties();
    }

    public void loadExtensions(IExtensionRegistry registry)
    {
        {
            IConfigurationElement[] extElements = registry.getConfigurationElementsFor(ResourceHandlerDescriptor.EXTENSION_ID);
            for (IConfigurationElement ext : extElements) {
                ResourceHandlerDescriptor handlerDescriptor = new ResourceHandlerDescriptor(ext);
                handlerDescriptors.add(handlerDescriptor);
            }
        }
    }

    public void loadProjects(IProgressMonitor monitor)
        throws DBException
    {
        DBPPlatform platform = DBWorkbench.getPlatform();

        String activeProjectName = platform.getPreferenceStore().getString(PROP_PROJECT_ACTIVE);
        List<IProject> projects = platform.getLiveProjects();

        monitor.beginTask("Open active project", projects.size());
        try {
            for (IProject project : projects) {
                if (project.exists() && !project.isHidden()) {
                    if (project.getName().equals(activeProjectName)) {
                        activeProject = project;
                        break;
                    } else if (activeProject == null) {
                        // By default use first project
                        activeProject = project;
                    }
                }
                monitor.worked(1);
            }
            if (activeProject != null) {
                try {
                    activeProject.open(monitor);
                    activeProject.refreshLocal(IFile.DEPTH_ONE, monitor);
                    setActiveProject(activeProject);
                } catch (CoreException e) {
                    // Project seems to be corrupted
                    projects.remove(activeProject);
                    activeProject = null;
                }
            }
        } finally {
            monitor.done();
        }

        if (DBWorkbench.getPlatform().getApplication().isStandalone() && CommonUtils.isEmpty(projects)) {
            // Create initial project (only for standalone version)
            monitor.beginTask("Create general project", 1);
            try {
                activeProject = createGeneralProject(monitor);
                activeProject.open(monitor);
                setActiveProject(activeProject);
            } catch (CoreException e) {
                throw new DBException("Can't create default project", e);
            } finally {
                monitor.done();
            }
        }
    }

    public void dispose()
    {
        if (!this.projectDatabases.isEmpty()) {
            log.warn("Some projects are still open: " + this.projectDatabases.keySet());
        }
        // Dispose all DS registries
        for (DataSourceRegistry dataSourceRegistry : this.projectDatabases.values()) {
            dataSourceRegistry.dispose();
        }
        this.projectDatabases.clear();

        // Dispose resource handlers
        for (ResourceHandlerDescriptor handlerDescriptor : this.handlerDescriptors) {
            handlerDescriptor.dispose();
        }
        this.handlerDescriptors.clear();

        // Remove listeners
        this.workspace = null;

        if (!projectListeners.isEmpty()) {
            log.warn("Some project listeners are still register: " + projectListeners);
            projectListeners.clear();
        }
    }

    @Override
    public void addProjectListener(DBPProjectListener listener)
    {
        synchronized (projectListeners) {
            projectListeners.add(listener);
        }
    }

    @Override
    public void removeProjectListener(DBPProjectListener listener)
    {
        synchronized (projectListeners) {
            projectListeners.remove(listener);
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

    private ResourceHandlerDescriptor getHandlerDescriptorByRootPath(IProject project, String path) {
        for (ResourceHandlerDescriptor rhd : handlerDescriptors) {
            String defaultRoot = rhd.getDefaultRoot(project);
            if (defaultRoot != null && defaultRoot.equals(path)) {
                return rhd;
            }
        }
        return null;
    }

    @Override
    public DBPResourceHandler getResourceHandler(IResource resource)
    {
        if (resource == null || resource.isHidden() || resource.isPhantom()) {
            // Skip not accessible hidden and phantom resources
            return null;
        }
        if (resource.getParent() instanceof IProject && resource.getName().startsWith(DataSourceRegistry.CONFIG_FILE_PREFIX)) {
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
            final IProject project = resource.getProject();
            IPath relativePath = resource.getFullPath().makeRelativeTo(project.getFullPath());
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
    public DBPResourceHandlerDescriptor[] getAllResourceHandlers()
    {
        return handlerDescriptors.toArray(new DBPResourceHandlerDescriptor[0]);
    }

    @Override
    public IFolder getResourceDefaultRoot(IProject project, DBPResourceHandlerDescriptor rhd, boolean forceCreate)
    {
    	if (project == null) {
			return null;
		}
        String defaultRoot = rhd.getDefaultRoot(project);
        if (defaultRoot == null) {
            // No root
            return null;
        }
        final IFolder realFolder = project.getFolder(defaultRoot);

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
    public IFolder getResourceDefaultRoot(IProject project, Class<? extends DBPResourceHandler> handlerType, boolean forceCreate)
    {
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
                final IFolder realFolder = project.getFolder(defaultRoot);

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
        return project.getFolder(DEFAULT_RESOURCES_ROOT);
    }

    @Override
    @Nullable
    public DataSourceRegistry getDataSourceRegistry(IProject project)
    {
        if (project == null) {
            log.warn("No active project - can't get datasource registry");
            return null;
        }
        if (!project.isOpen()) {
            log.warn("Project '" + project.getName() + "' is not open - can't get datasource registry");
            return null;
        }
        DataSourceRegistry dataSourceRegistry = projectDatabases.get(project);
        if (dataSourceRegistry == null) {
            log.warn("Project '" + project.getName() + "' not found in registry");
        }
        return dataSourceRegistry;
    }

    @Override
    public DBPResourceHandlerDescriptor[] getResourceHandlerDescriptors()
    {
        DBPResourceHandlerDescriptor[] result = new DBPResourceHandlerDescriptor[handlerDescriptors.size()];
        for (int i = 0; i < handlerDescriptors.size(); i++) {
            result[i] = handlerDescriptors.get(i);
        }
        return result;
    }

    public DataSourceRegistry getActiveDataSourceRegistry()
    {
        if (activeProject == null) {
            return null;
        }
        final DataSourceRegistry dataSourceRegistry = projectDatabases.get(activeProject);
        if (dataSourceRegistry == null) {
            throw new IllegalStateException("No registry for active project found");
        }
        return dataSourceRegistry;
    }

    @Override
    public IProject getActiveProject()
    {
        return activeProject;
    }

    public void setActiveProject(IProject project)
    {
        final IProject oldValue = this.activeProject;
        this.activeProject = project;
        DBWorkbench.getPlatform().getPreferenceStore().setValue(PROP_PROJECT_ACTIVE, project == null ? "" : project.getName());

        DBPProjectListener[] listeners;
        synchronized (projectListeners) {
            listeners = projectListeners.toArray(new DBPProjectListener[projectListeners.size()]);
        }
        for (DBPProjectListener listener : listeners) {
            listener.handleActiveProjectChange(oldValue, activeProject);
        }
    }

    private IProject createGeneralProject(IProgressMonitor monitor) throws CoreException
    {
        final String baseProjectName = DBWorkbench.getPlatform().getApplication().isStandalone() ? "General" : "DBeaver";
        String projectName = baseProjectName;
        for (int i = 1; ; i++) {
            final IProject project = workspace.getRoot().getProject(projectName);
            if (project.exists()) {
                projectName = baseProjectName + i;
                continue;
            }
            project.create(monitor);
            project.open(monitor);
            final IProjectDescription description = workspace.newProjectDescription(project.getName());
            description.setComment("General DBeaver project");
            description.setNatureIds(new String[] {DBeaverNature.NATURE_ID});
            project.setDescription(description, monitor);

            return project;
        }
    }

    /**
     * We do not use resource listener in project registry because project should be added/removedhere
     * only after all other event handlers were finished and project was actually created/deleted.
     * Otherwise set of workspace synchronize problems occur
     * @param project project
     */
    @Override
    public void addProject(IProject project)
    {
        if (projectDatabases.containsKey(project)) {
            log.warn("Project [" + project + "] already added");
            return;
        }
        projectDatabases.put(project, new DataSourceRegistry(DBWorkbench.getPlatform(), project));
    }

    @Override
    public void removeProject(IProject project)
    {
        // Remove project from registry
        if (project != null) {
            DataSourceRegistry dataSourceRegistry = projectDatabases.get(project);
            if (dataSourceRegistry == null) {
                log.warn("Project '" + project.getName() + "' not found in the registry");
            } else {
                dataSourceRegistry.dispose();
                projectDatabases.remove(project);
            }
        }
    }

    @Override
    public Map<String, Object> getFileProperties(File file) {
        return externalFileProperties.get(file.getAbsolutePath());
    }

    @Override
    public Object getFileProperty(File file, String property) {
        final Map<String, Object> fileProps = externalFileProperties.get(file.getAbsolutePath());
        return fileProps == null ? null : fileProps.get(property);
    }

    @Override
    public void setFileProperty(File file, String property, Object value) {
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

        saveExternalFileProperties();
    }

    @Override
    public Map<String, Map<String, Object>> getAllFiles() {
        return externalFileProperties;
    }

    private void loadExternalFileProperties() {
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

    private void saveExternalFileProperties() {
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

}

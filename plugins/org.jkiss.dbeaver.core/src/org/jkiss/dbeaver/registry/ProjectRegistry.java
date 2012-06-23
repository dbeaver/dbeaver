/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.registry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.impl.project.DefaultResourceHandlerImpl;
import org.jkiss.dbeaver.model.project.DBPProjectListener;
import org.jkiss.dbeaver.model.project.DBPResourceHandler;
import org.jkiss.dbeaver.ui.actions.GlobalPropertyTester;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.SecurityUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectRegistry implements IResourceChangeListener {
    static final Log log = LogFactory.getLog(ProjectRegistry.class);

    private final List<ResourceHandlerDescriptor> handlerDescriptors = new ArrayList<ResourceHandlerDescriptor>();
    private final List<DBPResourceHandler> resourceHandlerList = new ArrayList<DBPResourceHandler>();
    private final Map<String, DBPResourceHandler> resourceHandlerMap = new HashMap<String, DBPResourceHandler>();
    private final Map<String, DBPResourceHandler> extensionsMap = new HashMap<String, DBPResourceHandler>();

    private final Map<String, DataSourceRegistry> projectDatabases = new HashMap<String, DataSourceRegistry>();
    private String activeProjectId;
    private IProject activeProject;
    private IWorkspace workspace;

    private final List<DBPProjectListener> projectListeners = new ArrayList<DBPProjectListener>();

    public ProjectRegistry()
    {
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
        for (ResourceHandlerDescriptor descriptor : handlerDescriptors) {
            try {
                final DBPResourceHandler handler = descriptor.getHandler();
                if (handler == null) {
                    continue;
                }
                resourceHandlerMap.put(descriptor.getResourceType(), handler);
                resourceHandlerList.add(handler);
                for (String ext : descriptor.getFileExtensions()) {
                    extensionsMap.put(ext, handler);
                }
            } catch (Exception e) {
                log.error("Can't instantiate resource handler " + descriptor.getResourceType(), e);
            }
        }
    }

    public void loadProjects(IWorkspace workspace, IProgressMonitor monitor) throws CoreException
    {
        final DBeaverCore core = DBeaverCore.getInstance();
        this.activeProjectId = core.getGlobalPreferenceStore().getString("project.active");

        List<IProject> projects = core.getLiveProjects();
        if (CommonUtils.isEmpty(projects)) {
            // Create initial project
            monitor.beginTask("Create general project", 1);
            try {
                createGeneralProject(workspace, monitor);
            } finally {
                monitor.done();
            }
            projects = core.getLiveProjects();
        }

        monitor.beginTask("Open projects", projects.size());
        try {
            List<IProject> initializedProjects = new ArrayList<IProject>();
            for (IProject project : projects) {
                if (project.exists() && !project.isHidden()) {
                    monitor.subTask("Initialize project " + project.getName());
                    try {
                        initializeProject(project, monitor);
                        initializedProjects.add(project);
                    } catch (CoreException e) {
                        log.error("Can't open project '" + project.getName() + "'", e);
                    }
                }
                monitor.worked(1);
            }
            if (getActiveProject() == null && !initializedProjects.isEmpty()) {
                setActiveProject(initializedProjects.get(0));
            }
        } finally {
            monitor.done();
        }

        this.workspace = workspace;
        workspace.addResourceChangeListener(this);
    }

    public void dispose()
    {
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
        if (workspace != null) {
            workspace.removeResourceChangeListener(this);
            workspace = null;
        }

        if (!projectListeners.isEmpty()) {
            log.warn("Some project listeners are still register: " + projectListeners);
            projectListeners.clear();
        }
    }

    public void addProjectListener(DBPProjectListener listener)
    {
        synchronized (projectListeners) {
            projectListeners.add(listener);
        }
    }

    public void removeProjectListener(DBPProjectListener listener)
    {
        synchronized (projectListeners) {
            projectListeners.remove(listener);
        }
    }

    public DBPResourceHandler getResourceHandler(IResource resource)
    {
        if (resource == null || !resource.isAccessible() || resource.isPhantom()) {
            // Skip not accessible hidden and phantom resources
            return null;
        }
        if (resource instanceof IContainer && resource.isHidden()) {
            // Skip hidden folders and projects
            return null;
        }
        DBPResourceHandler handler = null;
        String resourceType = getResourceType(resource);
        if (resourceType != null) {
            handler = getResourceHandler(resourceType);
        }
        if (handler == null) {
            if (!CommonUtils.isEmpty(resource.getFileExtension())) {
                handler = getResourceHandlerByExtension(resource.getFileExtension());
            }
        }
        if (handler == null) {
            handler = DefaultResourceHandlerImpl.INSTANCE;
        }
        return handler;
    }

    public static String getResourceType(IResource resource)
    {
        String resourceType;
        try {
            resourceType = resource.getPersistentProperty(DBPResourceHandler.PROP_RESOURCE_TYPE);
        } catch (CoreException e) {
            log.warn(e);
            return null;
        }
        if (CommonUtils.isEmpty(resourceType) && resource instanceof IFolder && !(resource.getParent() instanceof IProject)) {
            // For folders try to get parent's handler
            return getResourceType(resource.getParent());
        }
        return resourceType;
    }

    public DBPResourceHandler getResourceHandler(String resourceType)
    {
        return resourceHandlerMap.get(resourceType);
    }

    public DBPResourceHandler getResourceHandlerByExtension(String extension)
    {
        return extensionsMap.get(extension);
    }

    public DataSourceRegistry getDataSourceRegistry(IProject project)
    {
        if (!project.isOpen()) {
            log.warn("Project '" + project.getName() + "' is not open - can't get datasource registry");
            return null;
        }
        String projectId = getProjectId(project);
        DataSourceRegistry dataSourceRegistry = projectId == null ? null : projectDatabases.get(projectId);
        if (dataSourceRegistry == null) {
            throw new IllegalStateException("Project '" + project.getName() + "' not found in registry");
        }
        return dataSourceRegistry;
    }

    public DataSourceRegistry getActiveDataSourceRegistry()
    {
        final DataSourceRegistry dataSourceRegistry = projectDatabases.get(activeProjectId);
        if (dataSourceRegistry == null) {
            throw new IllegalStateException("No registry for active project found");
        }
        return dataSourceRegistry;
    }

    public static String getProjectId(IProject project)
    {
        try {
            String projectId = project.getPersistentProperty(DBPResourceHandler.PROP_PROJECT_ID);
            if (projectId == null) {
                project.setPersistentProperty(DBPResourceHandler.PROP_PROJECT_ID, projectId = SecurityUtils.generateGUID(false));
                log.warn("Project '" + project.getName() + "' didn't has project ID");
            }
            return projectId;
        } catch (CoreException e) {
            log.error(e);
            return null;
        }
    }

    public IProject getActiveProject()
    {
        return activeProject;
    }

    public void setActiveProject(IProject project)
    {
        IProject oldValue = this.activeProject;
        this.activeProject = project;
        this.activeProjectId = getProjectId(project);
        DBeaverCore.getInstance().getGlobalPreferenceStore().setValue("project.active", activeProjectId);

        synchronized (projectListeners) {
            for (DBPProjectListener listener : projectListeners) {
                listener.handleActiveProjectChange(oldValue, activeProject);
            }
        }
    }

    private IProject createGeneralProject(IWorkspace workspace, IProgressMonitor monitor) throws CoreException
    {
        final IProject project = workspace.getRoot().getProject(
            DBeaverCore.getInstance().isStandalone() ?
                "General" : "DBeaver");
        project.create(monitor);
        project.open(monitor);
        final IProjectDescription description = workspace.newProjectDescription(project.getName());
        description.setComment("General DBeaver project");
        project.setDescription(description, monitor);
        project.setPersistentProperty(DBPResourceHandler.PROP_PROJECT_ID, SecurityUtils.generateGUID(false));

        return project;
    }

    private IProject initializeProject(final IProject project, IProgressMonitor monitor) throws CoreException
    {
        // Open project
        if (!project.isOpen()) {
            project.open(monitor);
        }
        // Set project ID
        String projectId = project.getPersistentProperty(DBPResourceHandler.PROP_PROJECT_ID);
        if (projectId == null) {
            project.setPersistentProperty(DBPResourceHandler.PROP_PROJECT_ID, projectId = SecurityUtils.generateGUID(false));
        }
        if (projectId.equals(activeProjectId)) {
            activeProject = project;
        }
        // Init all resource handlers
        for (DBPResourceHandler handler : resourceHandlerList) {
            try {
                handler.initializeProject(project, monitor);
            } catch (DBException e) {
                log.warn("Can't initialize project using resource handler", e);
            }
        }

        // Init DS registry
        DataSourceRegistry dataSourceRegistry = new DataSourceRegistry(project);
        projectDatabases.put(projectId, dataSourceRegistry);

        return project;
    }

    /**
     * We do not use resource listener in project registry because project should be added/removedhere
     * only after all other event handlers were finished and project was actually created/deleted.
     * Otherwise set of workspace synchronize problems occur
     * @param project project
     */
    public void addProject(IProject project)
    {
        try {
            initializeProject(project, new NullProgressMonitor());
        } catch (CoreException e) {
            log.error("Can't add project to registry", e);
        }
    }

    public void removeProject(String projectId)
    {
        // Remove project from registry
        if (projectId != null) {
            DataSourceRegistry dataSourceRegistry = projectDatabases.get(projectId);
            if (dataSourceRegistry == null) {
                log.warn("Project '" + projectId + "' not found in the registry");
            } else {
                dataSourceRegistry.dispose();
                projectDatabases.remove(projectId);
            }
        }
    }

    @Override
    public void resourceChanged(IResourceChangeEvent event)
    {
        IResourceDelta delta = event.getDelta();
        if (delta == null) {
            return;
        }
        for (IResourceDelta projectDelta : delta.getAffectedChildren()) {
            if (projectDelta.getResource() instanceof IProject) {
                IProject project = (IProject)projectDelta.getResource();
                if (projectDelta.getKind() == IResourceDelta.REMOVED) {
                    if (project == activeProject) {
                        activeProject = null;
                    }
                    GlobalPropertyTester.firePropertyChange(GlobalPropertyTester.PROP_HAS_MULTI_PROJECTS);
                } else if (projectDelta.getKind() == IResourceDelta.ADDED) {
                    if (project.isOpen() && activeProjectId.equals(getProjectId(project))) {
                        this.activeProject = project;
                    }
                    GlobalPropertyTester.firePropertyChange(GlobalPropertyTester.PROP_HAS_MULTI_PROJECTS);
                }
            }
        }
    }

}

/*
                    // Add new project in registry
                    if (projectDatabases.get(project) != null) {
                        log.warn("Project '" + project.getName() + "' is already in the registry");
                    } else {
                        try {
                            initializeProject(project, VoidProgressMonitor.INSTANCE.getNestedMonitor());
                        } catch (CoreException e) {
                            log.error(e);
                        }
                    }


                } else if (projectDelta.getKind() == IResourceDelta.REMOVED) {
                    // Remove project from registry
                    DataSourceRegistry dataSourceRegistry = projectDatabases.get(project);
                    if (dataSourceRegistry == null) {
                        log.warn("Project '" + project.getName() + "' not found in the registry");
                    } else {
                        dataSourceRegistry.dispose();
                        projectDatabases.remove(project);
                    }
                }
            }
        }
    }


}
*/
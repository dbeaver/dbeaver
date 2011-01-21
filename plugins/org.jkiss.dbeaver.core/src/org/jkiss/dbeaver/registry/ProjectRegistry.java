/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.registry;

import net.sf.jkiss.utils.CommonUtils;
import net.sf.jkiss.utils.SecurityUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IProgressMonitor;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.project.DBPResourceHandler;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProjectRegistry implements IResourceChangeListener {
    static final Log log = LogFactory.getLog(ProjectRegistry.class);

    private final List<ResourceHandlerDescriptor> handlerDescriptors = new ArrayList<ResourceHandlerDescriptor>();
    private final List<DBPResourceHandler> resourceHandlerList = new ArrayList<DBPResourceHandler>();
    private final Map<String, DBPResourceHandler> resourceHandlerMap = new HashMap<String, DBPResourceHandler>();
    private Map<String, DBPResourceHandler> extensionsMap = new HashMap<String, DBPResourceHandler>();

    private final Map<IProject, DataSourceRegistry> projectDatabases = new HashMap<IProject, DataSourceRegistry>();
    private IProject activeProject;
    private IWorkspace workspace;

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
        List<IProject> projects = DBeaverCore.getInstance().getLiveProjects();
        if (CommonUtils.isEmpty(projects)) {
            // Create initial project
            monitor.beginTask("Create general project", 1);
            try {
                createGeneralProject(workspace, monitor);
            } finally {
                monitor.done();
            }
            projects = DBeaverCore.getInstance().getLiveProjects();
        }

        monitor.beginTask("Open projects", projects.size());
        try {
            for (IProject project : projects) {
                if (project.exists() && !project.isHidden()) {
                    monitor.subTask("Initialize project " + project.getName());
                    try {
                        initializeProject(project, monitor);

                        if (activeProject == null || "true".equals(project.getPersistentProperty(DBPResourceHandler.PROP_PROJECT_ACTIVE))) {
                            activeProject = project;
                        }
                    } catch (CoreException e) {
                        log.error("Can't open project '" + project.getName() + "'", e);
                    }
                }
                monitor.worked(1);
            }
            if (activeProject != null) {
                activeProject.setPersistentProperty(DBPResourceHandler.PROP_PROJECT_ACTIVE, "true");
            }
        } finally {
            monitor.done();
        }

        this.workspace = workspace;
        workspace.addResourceChangeListener(this);
    }

    public void dispose()
    {
        for (ResourceHandlerDescriptor handlerDescriptor : this.handlerDescriptors) {
            handlerDescriptor.dispose();
        }
        this.handlerDescriptors.clear();

        if (workspace != null) {
            workspace.removeResourceChangeListener(this);
            workspace = null;
        }
    }

    public List<ResourceHandlerDescriptor> getHandlerDescriptors()
    {
        return handlerDescriptors;
    }

    public DBPResourceHandler getResourceHandler(IResource resource)
    {
        DBPResourceHandler handler = null;
        String resourceType = null;
        try {
            resourceType = resource.getPersistentProperty(DBPResourceHandler.PROP_RESOURCE_TYPE);
        } catch (CoreException e) {
            log.debug(e);
        }
        if (resourceType != null) {
            handler = getResourceHandler(resourceType);
        }
        if (handler == null) {
            if (!CommonUtils.isEmpty(resource.getFileExtension())) {
                handler = getResourceHandlerByExtension(resource.getFileExtension());
            }
        }
        return handler;
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
        return projectDatabases.get(project);
    }

    public IProject getActiveProject()
    {
        return activeProject;
    }

    private IProject createGeneralProject(IWorkspace workspace, IProgressMonitor monitor) throws CoreException
    {
        final IProject project = workspace.getRoot().getProject("General");
        project.create(monitor);
        project.open(monitor);
        final IProjectDescription description = workspace.newProjectDescription(project.getName());
        description.setComment("General project");
        project.setDescription(description, monitor);
        project.setPersistentProperty(DBPResourceHandler.PROP_PROJECT_ID, SecurityUtils.generateGUID(false));
        project.setPersistentProperty(DBPResourceHandler.PROP_PROJECT_ACTIVE, "true");

        return project;
    }

    private IProject initializeProject(final IProject project, IProgressMonitor monitor) throws CoreException
    {
        // Open project
        if (!project.isOpen()) {
            project.open(monitor);
        }
        // Set project ID
        if (project.getPersistentProperty(DBPResourceHandler.PROP_PROJECT_ID) == null) {
            project.setPersistentProperty(DBPResourceHandler.PROP_PROJECT_ID, SecurityUtils.generateGUID(false));
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
        projectDatabases.put(project, dataSourceRegistry);

        return project;
    }

    public void addProject(IProject project)
    {
        try {
            initializeProject(project, VoidProgressMonitor.INSTANCE.getNestedMonitor());
        } catch (CoreException e) {
            log.error("Can't add project to registry", e);
        }
    }

    public void resourceChanged(IResourceChangeEvent event)
    {
        IResourceDelta delta = event.getDelta();
        if (delta == null) {
            return;
        }
        for (IResourceDelta projectDelta : delta.getAffectedChildren()) {
            if (projectDelta.getResource() instanceof IProject) {
                IProject project = (IProject)projectDelta.getResource();
                if (projectDelta.getKind() == IResourceDelta.ADDED) {
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
*/
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

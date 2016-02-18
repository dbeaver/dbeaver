/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.registry;

import org.jkiss.dbeaver.Log;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.swt.widgets.Display;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.DBPApplication;
import org.jkiss.dbeaver.model.DBPProjectManager;
import org.jkiss.dbeaver.ui.resources.DefaultResourceHandlerImpl;
import org.jkiss.dbeaver.model.project.DBPProjectListener;
import org.jkiss.dbeaver.model.project.DBPResourceHandler;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.actions.GlobalPropertyTester;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

public class ProjectRegistry implements DBPProjectManager {
    static final Log log = Log.getLog(ProjectRegistry.class);

    private static final String PROP_PROJECT_ACTIVE = "project.active";
    private static final String[] IGNORED_FILES = {
        DataSourceRegistry.CONFIG_FILE_NAME,
        DataSourceRegistry.OLD_CONFIG_FILE_NAME
    };

    private final List<ResourceHandlerDescriptor> handlerDescriptors = new ArrayList<>();
    private final Map<String, ResourceHandlerDescriptor> rootMapping = new HashMap<>();

    private final Map<IProject, DataSourceRegistry> projectDatabases = new HashMap<>();
    private final Set<IProject> busyProjects = new HashSet<>();
    private IProject activeProject;
    private IWorkspace workspace;

    private final List<DBPProjectListener> projectListeners = new ArrayList<>();

    public ProjectRegistry(IWorkspace workspace)
    {
        this.workspace = workspace;
        loadExtensions(Platform.getExtensionRegistry());
    }

    public void loadExtensions(IExtensionRegistry registry)
    {
        {
            IConfigurationElement[] extElements = registry.getConfigurationElementsFor(ResourceHandlerDescriptor.EXTENSION_ID);
            for (IConfigurationElement ext : extElements) {
                ResourceHandlerDescriptor handlerDescriptor = new ResourceHandlerDescriptor(ext);
                handlerDescriptors.add(handlerDescriptor);
            }
            for (ResourceHandlerDescriptor rhd : handlerDescriptors) {
                for (String root : rhd.getRoots()) {
                    rootMapping.put(root, rhd);
                }
            }
        }
    }

    public void loadProjects(IProgressMonitor monitor)
        throws DBException
    {
        final DBeaverCore core = DBeaverCore.getInstance();
        String activeProjectName = DBeaverCore.getGlobalPreferenceStore().getString(PROP_PROJECT_ACTIVE);

        List<IProject> projects = core.getLiveProjects();

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

        if (DBeaverCore.isStandalone() && CommonUtils.isEmpty(projects)) {
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
        this.rootMapping.clear();

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

    @Override
    public DBPApplication geApplication() {
        return DBeaverCore.getInstance();
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
            ContentUtils.syncFile(VoidProgressMonitor.INSTANCE, resource);
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
            IPath relativePath = resource.getFullPath().makeRelativeTo(resource.getProject().getFullPath());
            while (relativePath.segmentCount() > 0) {
                ResourceHandlerDescriptor handlerDescriptor = rootMapping.get(relativePath.toString());
                if (handlerDescriptor != null) {
                    handler = handlerDescriptor.getHandler();
                }
                relativePath = relativePath.removeLastSegments(1);
            }
        }
        if (handler == null) {
            handler = DefaultResourceHandlerImpl.INSTANCE;
        }
        return handler;
    }

    public Collection<ResourceHandlerDescriptor> getResourceHandlers()
    {
        return handlerDescriptors;
    }

    public IFolder getResourceDefaultRoot(IProject project, Class<? extends DBPResourceHandler> handlerType)
    {
    	if (project == null) {
			return null;
		}
        for (ResourceHandlerDescriptor rhd : handlerDescriptors) {
            DBPResourceHandler handler = rhd.getHandler();
            if (handler != null && handler.getClass() == handlerType) {
                return project.getFolder(rhd.getDefaultRoot());
            }
        }
        return project.getFolder(DefaultResourceHandlerImpl.DEFAULT_ROOT);
    }

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
        DBeaverCore.getGlobalPreferenceStore().setValue(PROP_PROJECT_ACTIVE, project == null ? "" : project.getName());

        GlobalPropertyTester.firePropertyChange(GlobalPropertyTester.PROP_HAS_ACTIVE_PROJECT);

        Display.getDefault().asyncExec(new Runnable() {
            @Override
            public void run()
            {
                synchronized (projectListeners) {
                    for (DBPProjectListener listener : projectListeners) {
                        listener.handleActiveProjectChange(oldValue, activeProject);
                    }
                }
            }
        });
    }

    private IProject createGeneralProject(IProgressMonitor monitor) throws CoreException
    {
        final String baseProjectName = DBeaverCore.isStandalone() ? "General" : "DBeaver";
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
            project.setDescription(description, monitor);

            return project;
        }
    }

    /**
     * Busy projects is a workaround to avoid to early project opening (e.g. by project import wizard)
     * while project's contents are still creating
     */
    public void projectBusy(IProject project, boolean busy) {
        if (busy) {
            busyProjects.add(project);
        } else {
            busyProjects.remove(project);
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
        if (busyProjects.contains(project)) {
            // Just ignore busy projects
            return;
        }
        if (projectDatabases.containsKey(project)) {
            log.warn("Project [" + project + "] already added");
            return;
        }
        projectDatabases.put(project, new DataSourceRegistry(DBeaverCore.getInstance(), project));
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

}

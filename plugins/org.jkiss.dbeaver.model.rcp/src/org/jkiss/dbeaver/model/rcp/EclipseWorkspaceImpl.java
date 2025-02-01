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

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.access.DBAPermissionRealm;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPProjectListener;
import org.jkiss.dbeaver.model.app.DBPWorkspaceEclipse;
import org.jkiss.dbeaver.model.impl.app.BaseProjectImpl;
import org.jkiss.dbeaver.model.impl.app.BaseWorkspaceImpl;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.LoggingProgressMonitor;
import org.jkiss.dbeaver.registry.internal.RegistryMessages;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * DBeaver workspace.
 *
 * Basically just a wrapper around Eclipse workspace.
 * Additionally, holds information about remote workspace.
 * Identified by unique ID (random UUID).
 */
public abstract class EclipseWorkspaceImpl extends BaseWorkspaceImpl implements DBPWorkspaceEclipse {

    private static final Log log = Log.getLog(EclipseWorkspaceImpl.class);

    private final String workspaceId;
    private final ProjectListener projectListener;
    private final IWorkspace eclipseWorkspace;
    protected final Map<IProject, DesktopProjectImpl> projects = new LinkedHashMap<>();

    private final List<DBPProjectListener> projectListeners = new ArrayList<>();

    public EclipseWorkspaceImpl(DBPPlatform platform, IWorkspace eclipseWorkspace) {
        super(platform, eclipseWorkspace.getRoot().getLocation().toPath());
        this.eclipseWorkspace = eclipseWorkspace;
        this.workspaceId = initWorkspaceId();

        if (!isReadOnly()) {
            this.projectListener = new ProjectListener();
            this.getEclipseWorkspace().addResourceChangeListener(projectListener);
        } else {
            this.projectListener = null;
        }
    }


    @NotNull
    @Override
    public IWorkspace getEclipseWorkspace() {
        return eclipseWorkspace;
    }

    @Override
    public final void initializeProjects() {
        initializeWorkspaceSession();
        try {
            loadWorkspaceProjects();
        } catch (DBException ex) {
            log.error("Can't load workspace projects", ex);
        }
        
        if (DBWorkbench.getPlatform().getApplication().isStandalone() && CommonUtils.isEmpty(projects) && isDefaultProjectNeeded() && !isReadOnly()) {
            try {
                createDefaultProject();
            } catch (CoreException e) {
                log.error("Can't create default project", e);
            }
        }
        if (getActiveProject() == null && !projects.isEmpty() && !isReadOnly()) {
            // Set active project
            setActiveProject(projects.values().iterator().next());
        }

        if (activeProject != null && !activeProject.isOpen()) {
            try {
                activeProject.ensureOpen();
            } catch (IllegalStateException e) {
                log.error("Error opening active project", e);
            }
        }
    }


    @Override
    public void setActiveProject(@NotNull DBPProject project) {
        DBPProject oldActiveProject = this.activeProject;
        this.activeProject = project;

        if (!CommonUtils.equalObjects(oldActiveProject, project)) {
            platform.getPreferenceStore().setValue(
                PROP_PROJECT_ACTIVE, project == null ? "" : project.getName());

            fireActiveProjectChange(oldActiveProject, this.activeProject);
        }
    }

    @NotNull
    @Override
    public List<DBPProject> getProjects() {
        return new ArrayList<>(projects.values());
    }

    @Nullable
    @Override
    public DBPProject getProject(@NotNull IProject project) {
        return projects.get(project);
    }

    @Nullable
    @Override
    public DBPProject getProject(@NotNull String projectName) {
        IProject eProject = eclipseWorkspace.getRoot().getProject(projectName);
        if (!eProject.exists()) {
            return null;
        }
        return getProject(eProject);
    }

    protected boolean isDefaultProjectNeeded() {
        return DBWorkbench.getPlatform().getApplication().getDefaultProjectName() != null;
    }

    @Override
    public void addProjectListener(@NotNull DBPProjectListener listener) {
        synchronized (projectListeners) {
            projectListeners.add(listener);
        }
    }

    @Override
    public void removeProjectListener(@NotNull DBPProjectListener listener) {
        synchronized (projectListeners) {
            projectListeners.remove(listener);
        }
    }

    @Override
    public void dispose() {
        if (projectListener != null) {
            this.getEclipseWorkspace().removeResourceChangeListener(projectListener);
        }

        if (!projectListeners.isEmpty()) {
            log.warn("Some project listeners are still register: " + projectListeners);
            projectListeners.clear();
        }

        synchronized (projects) {
            // Dispose all DS registries
            for (BaseProjectImpl project : this.projects.values()) {
                project.dispose();
            }
            this.projects.clear();
        }

        super.dispose();
    }

    protected void loadWorkspaceProjects() throws DBException {
        String activeProjectName = getPlatform().getPreferenceStore().getString(PROP_PROJECT_ACTIVE);

        IWorkspaceRoot root = getEclipseWorkspace().getRoot();
        IProject[] allProjects = root.getProjects();
        if (ArrayUtils.isEmpty(allProjects)) {
            try {
                reloadWorkspace(new LoggingProgressMonitor(log));
            } catch (Throwable e) {
                log.error(e);
            }
            allProjects = root.getProjects();
        }
        for (IProject project : allProjects) {
            if (project.exists() && !project.isHidden() && isProjectAccessible(project)) {
                DesktopProjectImpl projectMetadata = projects.get(project);
                if (projectMetadata == null) {
                    projectMetadata = createProjectFrom(project);
                }
                this.projects.put(project, projectMetadata);

                if (activeProject == null || (!CommonUtils.isEmpty(activeProjectName) && project.getName().equals(activeProjectName))) {
                    activeProject = projectMetadata;
                }
                projectMetadata.hideConfigurationFiles();
            }
        }
    }

    protected void reloadWorkspace(DBRProgressMonitor monitor) {

    }

    protected boolean isProjectAccessible(IProject project) {
        return true;
    }

    protected DesktopProjectImpl createProjectFrom(IProject project) {
        return new DesktopProjectImpl(this, project, this.getAuthContext());
    }

    private IProject createDefaultProject() throws CoreException {
        final String baseProjectName = DBWorkbench.getPlatform().getApplication().getDefaultProjectName();
        String projectName = baseProjectName;
        for (int i = 1; ; i++) {
            final IProject project = getEclipseWorkspace().getRoot().getProject(projectName);
            if (project.exists()) {
                projectName = baseProjectName + i;
                continue;
            }
            NullProgressMonitor monitor = new NullProgressMonitor();
            project.create(monitor);
            project.open(monitor);
            final IProjectDescription description = getEclipseWorkspace().newProjectDescription(project.getName());
            description.setComment(RegistryMessages.project_description_comment);
            description.setNatureIds(new String[]{DBeaverNature.NATURE_ID});
            project.setDescription(description, monitor);

            return project;
        }
    }

    @NotNull
    @Override
    public String getWorkspaceId() {
        return workspaceId;
    }

    public void save(DBRProgressMonitor monitor) throws DBException {
        try {
            eclipseWorkspace.save(true, monitor.getNestedMonitor());
        } catch (CoreException e) {
            throw new DBException("Error saving Eclipse workspace", e);
        }
    }

    protected void fireProjectAdd(BaseProjectImpl project) {
        for (DBPProjectListener listener : getListenersCopy()) {
            listener.handleProjectAdd(project);
        }
    }

    protected void fireProjectRemove(BaseProjectImpl project) {
        for (DBPProjectListener listener : getListenersCopy()) {
            listener.handleProjectRemove(project);
        }
    }

    protected void fireActiveProjectChange(DBPProject oldActiveProject, DBPProject activeProject) {
        for (DBPProjectListener listener : getListenersCopy()) {
            listener.handleActiveProjectChange(oldActiveProject, activeProject);
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
                // Process removed projects first and added projects afterwards to properly update current active project
                // Higher delta kind is processed first. See IResourceDelta constants
                Arrays.stream(event.getDelta().getAffectedChildren())
                    .filter(delta -> delta.getResource() instanceof IProject)
                    .sorted(Comparator.comparingInt(IResourceDelta::getKind).reversed())
                    .forEach(delta -> {
                        IProject project = (IProject) delta.getResource();
                        if (!projects.containsKey(project)) {
                            if (delta.getKind() == IResourceDelta.ADDED) {
                                DesktopProjectImpl projectMetadata = createProjectFrom(project);
                                projects.put(project, projectMetadata);
                                fireProjectAdd(projectMetadata);
                                if (activeProject == null) {
                                    activeProject = projectMetadata;
                                    fireActiveProjectChange(null, activeProject);
                                }
                            }
                        } else {
                            if (delta.getKind() == IResourceDelta.REMOVED) {
                                // Project deleted
                                DesktopProjectImpl projectMetadata = projects.remove(project);
                                projectMetadata.dispose();
                                fireProjectRemove(projectMetadata);
                                if (projectMetadata == activeProject) {
                                    activeProject = null;
                                    fireActiveProjectChange(projectMetadata, null);
                                }
                            } else {
                                // Some changes within project - reflect them in metadata cache
                                DesktopProjectImpl projectMetadata = projects.get(project);
                                if (projectMetadata != null) {
                                    handleResourceChange(projectMetadata, delta);
                                }
                            }
                        }
                    });
            }
        }
    }

    private void handleResourceChange(DesktopProjectImpl projectMetadata, IResourceDelta delta) {
        if (delta.getKind() == IResourceDelta.REMOVED) {
            IPath movedToPath = delta.getMovedToPath();
            if (movedToPath != null) {
                IPath oldPath = delta.getProjectRelativePath();
                IPath newPath = movedToPath.makeRelativeTo(projectMetadata.getEclipseProject().getFullPath());
                projectMetadata.moveResourceCache(oldPath, newPath);
            } else {
                projectMetadata.removeResourceFromCache(delta.getProjectRelativePath());
            }
        } else {
            for (IResourceDelta childDelta : delta.getAffectedChildren(IResourceDelta.ALL_WITH_PHANTOMS, IContainer.INCLUDE_HIDDEN)) {
                handleResourceChange(projectMetadata, childDelta);
            }
        }
    }

    protected String initWorkspaceId() {
        // Try to read workspace config from workspace root and from metadata
        // Metedata is the default path but we keep backward compatibility
        // and read from workspace if config exists
        Path workspaceConfigPath = getAbsolutePath();
        if (!Files.exists(workspaceConfigPath.resolve(DBConstants.WORKSPACE_PROPS_FILE))) {
            workspaceConfigPath = getMetadataFolder();
        }
        return readWorkspaceId(workspaceConfigPath);
    }

    public boolean isAdmin() {
        return hasRealmPermission(DBAPermissionRealm.PERMISSION_ADMIN);
    }

}
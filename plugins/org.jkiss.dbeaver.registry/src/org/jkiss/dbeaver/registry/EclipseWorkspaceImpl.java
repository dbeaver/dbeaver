/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPPlatform;
import org.jkiss.dbeaver.model.app.DBPWorkspaceEclipse;
import org.jkiss.dbeaver.model.runtime.LoggingProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.resource.DBeaverNature;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

/**
 * DBeaver workspace.
 *
 * Basically just a wrapper around Eclipse workspace.
 * Additionally, holds information about remote workspace.
 * Identified by unique ID (random UUID).
 */
public class EclipseWorkspaceImpl extends BaseWorkspaceImpl implements DBPWorkspaceEclipse {

    private static final Log log = Log.getLog(EclipseWorkspaceImpl.class);

    private final String workspaceId;

    public EclipseWorkspaceImpl(DBPPlatform platform, IWorkspace eclipseWorkspace) {
        super(platform, eclipseWorkspace);

        workspaceId = readWorkspaceId();
    }

    @Override
    public void initializeProjects() {
        loadWorkspaceProjects();

        if (DBWorkbench.getPlatform().getApplication().isStandalone() && CommonUtils.isEmpty(projects)) {
            try {
                createDefaultProject();
            } catch (CoreException e) {
                log.error("Can't create default project", e);
            }
        }
        if (getActiveProject() == null && !projects.isEmpty()) {
            // Set active project
            setActiveProject(projects.values().iterator().next());
        }
    }

    private void loadWorkspaceProjects() {
        try {
            this.getAuthContext().addSession(acquireWorkspaceSession(new VoidProgressMonitor()));
        } catch (DBException e) {
            log.debug(e);
            DBWorkbench.getPlatformUI().showMessageBox(
                "Authentication error",
                "Error authenticating application user: " +
                    "\n" + e.getMessage(),
                true);
            System.exit(101);
        }

        String activeProjectName = getPlatform().getPreferenceStore().getString(PROP_PROJECT_ACTIVE);

        IWorkspaceRoot root = getEclipseWorkspace().getRoot();
        IProject[] allProjects = root.getProjects();
        if (ArrayUtils.isEmpty(allProjects)) {
            try {
                refreshWorkspaceContents(new LoggingProgressMonitor(log));
            } catch (Throwable e) {
                log.error(e);
            }
            allProjects = root.getProjects();
        }
        for (IProject project : allProjects) {
            if (project.exists() && !project.isHidden()) {
                ProjectMetadata projectMetadata = new ProjectMetadata(this, project, this.getAuthContext());
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
            description.setComment("General DBeaver project");
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

}
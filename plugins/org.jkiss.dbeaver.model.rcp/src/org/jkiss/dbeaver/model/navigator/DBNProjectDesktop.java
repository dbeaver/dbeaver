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
package org.jkiss.dbeaver.model.navigator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.app.DBPResourceHandler;
import org.jkiss.dbeaver.model.rcp.RCPProject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.List;

/**
 * DBNProjectDesktop
 */
public class DBNProjectDesktop extends DBNProject {
    private static final Log log = Log.getLog(DBNProjectDesktop.class);

    private final DBPResourceHandler handler;

    public DBNProjectDesktop(DBNNode parentNode, RCPProject project, DBPResourceHandler handler) {
        super(parentNode, project);
        this.handler = handler;
    }

    @NotNull
    public RCPProject getProject() {
        return (RCPProject) super.getProject();
    }

    private IProject getEclipseProject() {
        return getProject().getEclipseProject();
    }

    @Override
    public String getNodeDescription() {
        IProject iProject = getEclipseProject();
        if (iProject != null) {
            getProject().ensureOpen();
            try {
                return iProject.getDescription().getComment();
            } catch (CoreException e) {
                log.debug(e);
                return null;
            }
        } else {
            return super.getNodeDescription();
        }
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == IResource.class) {
            return adapter.cast(getEclipseProject());
        }
        return super.getAdapter(adapter);
    }

    @Override
    public void rename(DBRProgressMonitor monitor, String newName) throws DBException {
        GeneralUtils.validateResourceNameUnconditionally(newName);

        RCPProject project = getProject();
        project.ensureOpen();

        try {
            IProject eclipseProject = project.getEclipseProject();
            if (eclipseProject == null) {
                throw new DBException("Eclipse project is null");
            }
            final IProjectDescription description = eclipseProject.getDescription();
            description.setName(newName);
            eclipseProject.move(description, true, monitor.getNestedMonitor());
        } catch (Exception e) {
            throw new DBException("Can't rename project: " + e.getMessage(), e);
        }
    }

    @Override
    protected void addProjectNodes(DBRProgressMonitor monitor, List<DBNNode> children) throws DBException {
        children.addAll(List.of(DBNResource.readChildResourceNodes(monitor, this)));
        super.addProjectNodes(monitor, children);
    }

    @Override
    protected void filterChildren(List<DBNNode> children) {
        if (!DBWorkbench.getPlatform().getPreferenceStore().getBoolean(ModelPreferences.NAVIGATOR_SHOW_FOLDER_PLACEHOLDERS)) {
            // Remove non-existing resources (placeholders)
            children.removeIf(node -> node instanceof DBNResource && !((DBNResource) node).isResourceExists());
        }
    }

    @Override
    public DBNNode refreshNode(DBRProgressMonitor monitor, Object source) throws DBException {
        NavigatorResources.refreshThisResource(monitor, this);
        return super.refreshNode(monitor, source);
    }

}

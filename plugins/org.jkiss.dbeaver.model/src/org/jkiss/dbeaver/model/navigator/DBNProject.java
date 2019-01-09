/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.app.DBPDataSourceRegistry;
import org.jkiss.dbeaver.model.app.DBPProjectManager;
import org.jkiss.dbeaver.model.app.DBPResourceHandler;
import org.jkiss.dbeaver.model.app.DBPResourceHandlerDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.utils.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * DBNProject
 */
public class DBNProject extends DBNResource
{
    private static final Log log = Log.getLog(DBNProject.class);

    public DBNProject(DBNNode parentNode, IProject project, DBPResourceHandler handler)
    {
        super(parentNode, project, handler);
        getModel().getPlatform().getProjectManager().addProject(project);
    }

    @Override
    protected void dispose(boolean reflect)
    {
        IProject project = getProject();
        super.dispose(reflect);
        getModel().getPlatform().getProjectManager().removeProject(project);
    }

    public IProject getProject()
    {
        return (IProject)getResource();
    }

    public DBNProjectDatabases getDatabases()
    {
        try {
            for (DBNNode db : getChildren(new VoidProgressMonitor())) {
                if (db instanceof DBNProjectDatabases) {
                    return (DBNProjectDatabases) db;
                }
            }
        } catch (DBException e) {
            throw new IllegalStateException("Can't read project contents", e);
        }
        throw new IllegalStateException("No databases resource in project");
    }

    @Override
    public String getNodeDescription()
    {
        try {
            final IProject project = getProject();
            return project == null ? null : project.getDescription().getComment();
        } catch (CoreException e) {
            log.debug(e);
            return null;
        }
    }

    @Override
    public DBPImage getNodeIcon()
    {
        return DBIcon.PROJECT;
    }

    @Override
    public boolean allowsOpen()
    {
        return true;
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        if (adapter == DBNProject.class) {
            return adapter.cast(this);
        }
        return super.getAdapter(adapter);
    }

    @Override
    public boolean supportsRename()
    {
        // Do not rename active projects
        return getModel().getPlatform().getProjectManager().getActiveProject() != getProject();
    }

    @Override
    public void rename(DBRProgressMonitor monitor, String newName) throws DBException
    {
        try {
            final IProjectDescription description = getProject().getDescription();
            description.setName(newName);
            getProject().move(description, true, monitor.getNestedMonitor());
        } catch (CoreException e) {
            throw new DBException("Can't rename project", e);
        }
    }

    @Override
    protected DBNNode[] readChildNodes(DBRProgressMonitor monitor) throws DBException
    {
        IProject project = getProject();
        if (!project.isOpen()) {
            try {
                project.open(monitor.getNestedMonitor());
                project.refreshLocal(IFile.DEPTH_ONE, monitor.getNestedMonitor());
            } catch (CoreException e) {
                throw new DBException("Can't open project '" + project.getName() + "'", e);
            }
        }
        DBPDataSourceRegistry dataSourceRegistry = getModel().getPlatform().getProjectManager().getDataSourceRegistry(project);
        DBNNode[] children = super.readChildNodes(monitor);
        if (dataSourceRegistry != null) {
            children = ArrayUtils.insertArea(DBNNode.class, children, 0, new Object[] {new DBNProjectDatabases(this, dataSourceRegistry)});
        }
        return children;
    }

    @Override
    protected IResource[] addImplicitMembers(IResource[] members) {
        DBPProjectManager projectManager = getModel().getPlatform().getProjectManager();
        for (DBPResourceHandlerDescriptor rh : projectManager.getAllResourceHandlers()) {
            IFolder rhDefaultRoot = projectManager.getResourceDefaultRoot(getProject(), rh, false);
            if (rhDefaultRoot != null && !rhDefaultRoot.exists()) {
                // Add as explicit member
                members = ArrayUtils.add(IResource.class, members, rhDefaultRoot);
            }
        }
        return super.addImplicitMembers(members);
    }

    public DBNResource findResource(IResource resource)
    {
        List<IResource> path = new ArrayList<>();
        for (IResource parent = resource; !(parent instanceof IProject); parent = parent.getParent()) {
            path.add(0, parent);
        }

        DBNResource resNode = this;
        for (IResource res : path) {
            try {
                resNode.getChildren(new VoidProgressMonitor());
            } catch (DBException e) {
                log.error(e);
            }
            resNode = resNode.getChild(res);
            if (resNode == null) {
                return null;
            }
        }
        return resNode;
    }

    @Override
    protected void handleChildResourceChange(IResourceDelta delta) {
        final String name = delta.getResource().getName();
        if (name.startsWith(DBPDataSourceRegistry.CONFIG_FILE_PREFIX) && name.endsWith(DBPDataSourceRegistry.CONFIG_FILE_EXT)) {
            // DS registry configuration changed
            getDatabases().getDataSourceRegistry().refreshConfig();
        } else {
            super.handleChildResourceChange(delta);
        }
    }

    public void openProject() {
        final DBNProjectDatabases databases = getDatabases();
        if (databases != null) {
            databases.getDataSourceRegistry().refreshConfig();
        }
    }
}

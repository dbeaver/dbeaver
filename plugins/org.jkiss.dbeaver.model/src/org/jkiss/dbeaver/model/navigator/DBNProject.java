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

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBIconComposite;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.app.*;
import org.jkiss.dbeaver.model.navigator.registry.DBNRegistry;
import org.jkiss.dbeaver.model.rcp.RCPProject;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * DBNProject
 */
public class DBNProject extends DBNResource implements DBNNodeExtendable {
    private static final Log log = Log.getLog(DBNProject.class);

    private final DBPProject project;
    private List<DBNNode> extraNodes;

    public DBNProject(DBNNode parentNode, DBPProject project, DBPResourceHandler handler) {
        super(parentNode, project instanceof RCPProject rcpProject ? rcpProject.getEclipseProject() : null, handler);
        this.project = project;
        if (DBWorkbench.getPlatform().getApplication().isMultiuser()) {
            DBNRegistry.getInstance().extendNode(this, false);
        }
    }

    @NotNull
    public DBPProject getProject() {
        return project;
    }

    private IProject getEclipseProject() {
        return project instanceof RCPProject rcpProject ? rcpProject.getEclipseProject() : null;
    }

    public DBNProjectDatabases getDatabases() {
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

    @NotNull
    @Override
    public String getName() {
        return project.getId();
    }

    @Override
    public String getNodeDisplayName() {
        return project.getDisplayName();
    }

    protected String getResourceNodeType() {
        return "project";
    }

    @Override
    public String getNodeDescription() {
        IProject iProject = getEclipseProject();
        if (iProject == null) {
            return null;
        }
        project.ensureOpen();
        try {
            return iProject.getDescription().getComment();
        } catch (CoreException e) {
            log.debug(e);
            return null;
        }
    }

    @Override
    public String getLocalizedName(String locale) {
        return getNodeDisplayName();
    }

    @NotNull
    @Override
    public DBPImage getNodeIcon() {
        DBPImage image = DBIcon.PROJECT;
        if (getProject().isPrivateProject()) {
            image = new DBIconComposite(image, false, null, null, null, DBIcon.OVER_LAMP);
        } else if (!getProject().hasRealmPermission(RMConstants.PERMISSION_PROJECT_DATASOURCES_EDIT)) {
            image = new DBIconComposite(image, false, null, null, null, DBIcon.OVER_LOCK);
        }

        return image;
    }

    @Override
    public boolean allowsOpen() {
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
    public DBPProject getOwnerProject() {
        return project;
    }

    @Override
    public Throwable getLastLoadError() {
        return getProject().getDataSourceRegistry().getLastError();
    }

    @Override
    public boolean supportsRename() {
        return !project.isVirtual();
    }

    @Override
    public void rename(DBRProgressMonitor monitor, String newName) throws DBException {
        GeneralUtils.validateResourceNameUnconditionally(newName);

        project.ensureOpen();

        try {
            IProject eclipseProject = getEclipseProject();
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
    protected DBNNode[] readChildNodes(DBRProgressMonitor monitor) throws DBException {
        if (getModel().isGlobal() && !project.isOpen()) {
            project.ensureOpen();
        }

        final DBPDataSourceRegistry dataSourceRegistry = project.getDataSourceRegistry();

        try {
            dataSourceRegistry.checkForErrors();
        } catch (Throwable e) {
            project.dispose();
            throw e;
        }

        final List<DBNNode> children = new ArrayList<>();

        children.add(new DBNProjectDatabases(this, dataSourceRegistry));
        children.addAll(List.of(super.readChildNodes(monitor)));

        if (!DBWorkbench.getPlatform().getPreferenceStore().getBoolean(ModelPreferences.NAVIGATOR_SHOW_FOLDER_PLACEHOLDERS)) {
            // Remove non-existing resources (placeholders)
            children.removeIf(node -> node instanceof DBNResource && !((DBNResource) node).isResourceExists());
        }

        if (!CommonUtils.isEmpty(extraNodes)) {
            children.addAll(extraNodes);
        }

        return children.toArray(DBNNode[]::new);
    }

    @Override
    protected IResource[] addImplicitMembers(IResource[] members) {
        DBPWorkspace workspace = project.getWorkspace();
        if (workspace instanceof DBPWorkspaceDesktop) {
            for (DBPResourceHandlerDescriptor rh : ((DBPWorkspaceDesktop)workspace).getAllResourceHandlers()) {
                IFolder rhDefaultRoot = ((DBPWorkspaceDesktop)workspace).getResourceDefaultRoot(getProject(), rh, false);
                if (rhDefaultRoot != null && !rhDefaultRoot.exists()) {
                    // Add as explicit member
                    members = ArrayUtils.add(IResource.class, members, rhDefaultRoot);
                }
            }
        }
        return super.addImplicitMembers(members);
    }

    @Override
    public DBNNode refreshNode(DBRProgressMonitor monitor, Object source) throws DBException {
        project.getDataSourceRegistry().refreshConfig();
        super.refreshThisResource(monitor);
        return this;
    }

    public DBNResource findResource(IResource resource) {
        try {
            return findResource(new VoidProgressMonitor(), resource);
        } catch (Exception e) {
            log.debug(e);
            return null;
        }
    }

    public DBNResource findResource(DBRProgressMonitor monitor, IResource resource) throws DBException {
        if (!(project instanceof RCPProject rcpProject)) {
            return null;
        }
        List<IResource> path = new ArrayList<>();
        for (IResource parent = resource;
             !(parent instanceof IProject) && !CommonUtils.equalObjects(parent, rcpProject.getRootResource());
             parent = parent.getParent())
        {
            path.add(0, parent);
        }

        DBNResource resNode = this;
        for (IResource res : path) {
            resNode.getChildren(monitor);
            resNode = resNode.getChild(res);
            if (resNode == null) {
                return null;
            }
        }
        return resNode;
    }

    public DBNResource findResource(DBRProgressMonitor monitor, Path path) throws DBException {
        Path relativePath = getProject().getAbsolutePath().relativize(path);

        DBNResource resNode = this;
        for (Path fileName : relativePath) {
            DBNNode node = DBUtils.findObject(resNode.getChildren(monitor), fileName.toString());
            if (node instanceof DBNResource resource) {
                resNode = resource;
            } else {
                break;
            }
        }
        return resNode;
    }

    @Override
    protected void handleChildResourceChange(IResourceDelta delta) {
        if (CommonUtils.equalObjects(delta.getResource(), ((RCPProject)project).getRootResource())) {
            // Go inside root resource
            for (IResourceDelta cChild : delta.getAffectedChildren()) {
                handleChildResourceChange(cChild);
            }
            return;
        }
        final String name = delta.getResource().getName();
        if (name.equals(DBPProject.METADATA_FOLDER)) {
            // Metadata configuration changed
            IResourceDelta[] configFiles = delta.getAffectedChildren();
            boolean dsChanged = false;
            if (configFiles != null) {
                for (IResourceDelta rd : configFiles) {
                    IResource childRes = rd.getResource();
                    if (childRes instanceof IFile && childRes.getName().startsWith(DBPDataSourceRegistry.MODERN_CONFIG_FILE_PREFIX)) {
                        dsChanged = true;
                    }
                }
            }
            if (dsChanged) {
                getDatabases().getDataSourceRegistry().refreshConfig();
            }
        } else {
            super.handleChildResourceChange(delta);
        }
    }

    @NotNull
    @Override
    public List<DBNNode> getExtraNodes() {
        if (extraNodes == null) {
            return Collections.emptyList();
        }
        return extraNodes;
    }

    public <T> T getExtraNode(Class<T> nodeType) {
        if (extraNodes == null) {
            DBNRegistry.getInstance().extendNode(this, false);
        }
        if (extraNodes != null) {
            for (DBNNode node : extraNodes) {
                if (nodeType.isAssignableFrom(node.getClass())) {
                    return nodeType.cast(node);
                }
            }
        }
        return null;
    }

    @Override
    public void addExtraNode(@NotNull DBNNode node, boolean reflect) {
        if (extraNodes == null) {
            extraNodes = new ArrayList<>();
        }
        extraNodes.add(node);
        extraNodes.sort(Comparator.comparing(DBNNode::getNodeDisplayName));
        if (reflect) {
            getModel().fireNodeEvent(new DBNEvent(this, DBNEvent.Action.ADD, node));
        }
    }

    @Override
    public void removeExtraNode(@NotNull DBNNode node) {
        if (extraNodes != null && extraNodes.remove(node)) {
            getModel().fireNodeEvent(new DBNEvent(this, DBNEvent.Action.REMOVE, node));
        }
    }

    @Override
    protected IResource getContentLocationResource() {
        return project instanceof RCPProject rcpProject ? rcpProject.getRootResource() : null;
    }

    @Override
    protected void dispose(boolean reflect) {
        if (extraNodes != null) {
            for (DBNNode node : extraNodes) {
                node.dispose(reflect);
            }
            extraNodes.clear();
        }
        super.dispose(reflect);
    }

    @NotNull
    @Override
    public String getNodeId() {
        return project.getId();
    }

    @Deprecated
    @Override
    public String getNodeItemPath() {
        return NodePathType.resource.getPrefix() + project.getId();
    }

    @Override
    public boolean hasChildren(boolean navigableOnly) {
        return true;
    }
}

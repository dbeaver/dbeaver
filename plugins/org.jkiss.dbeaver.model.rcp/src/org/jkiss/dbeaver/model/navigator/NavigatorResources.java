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

import org.eclipse.core.internal.resources.Resource;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPPlatformDesktop;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPResourceHandler;
import org.jkiss.dbeaver.model.fs.DBFRemoteFileStore;
import org.jkiss.dbeaver.model.rcp.RCPProject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.AlphanumericComparator;
import org.jkiss.utils.CommonUtils;

import java.util.*;

public class NavigatorResources {

    private static final Log log = Log.getLog(NavigatorResources.class);

    static final Comparator<DBNNode> COMPARATOR = (o1, o2) -> {
        if (o1 instanceof DBNProjectDatabases) {
            return -1;
        } else if (o2 instanceof DBNProjectDatabases) {
            return 1;
        } else if (o1 instanceof DBNResource && o2 instanceof DBNResource) {
            boolean folder1 = DBNUtils.isFolderNode(o1);
            boolean folder2 = DBNUtils.isFolderNode(o2);
            if (folder1 && !folder2) {
                return -1;
            } else if (folder2 && !folder1) {
                return 1;
            }
        }

        return AlphanumericComparator.getInstance().compare(o1.getNodeDisplayName(), o2.getNodeDisplayName());
    };

    //////////////////////////////////
    // RCP resources

    public static DBNProject getProjectNode(DBNRoot root, IProject project) {
        for (DBNProject node : root.getProjects()) {
            if (node.getProject() instanceof RCPProject rcpProject && rcpProject.getEclipseProject() == project) {
                return node;
            }
        }
        return null;
    }

    public static DBNResource getNodeByResource(DBNModel model, IResource resource) {
        return getNodeByResource(model.getRoot(), resource);
    }

    public static DBNResource getNodeByResource(DBNRoot root, IResource resource) {
        final IProject project = resource.getProject();
        if (project == null) {
            return null;
        }
        final DBNProject projectNode = getProjectNode(root, project);
        if (projectNode == null) {
            return null;
        }
        List<IResource> path = new ArrayList<>();
        for (IResource parent = resource; parent != null && parent != project; parent = parent.getParent()) {
            path.add(0, parent);
        }
        DBNNode curResNode = projectNode;
        for (IResource res : path) {
            curResNode = getChild(curResNode, res);
            if (curResNode == null) {
                return null;
            }
        }
        return curResNode instanceof DBNResource dbnResource ? dbnResource : null;
    }

    public static void refreshNavigatorResource(@NotNull DBPProject project, @NotNull IResource resource, Object source) {
        DBNModel navigatorModel = project.getNavigatorModel();
        if (navigatorModel == null) {
            return;
        }
        final DBNProject projectNode = getProjectNode(navigatorModel.getRoot(), resource.getProject());
        final DBNResource fileNode = NavigatorResources.findResource(projectNode, resource);
        if (fileNode != null) {
            fileNode.refreshResourceState(source);
        }
    }

    public static DBNResource findResource(@Nullable DBNNode node, @NotNull IResource resource) {
        try {
            return findResource(new VoidProgressMonitor(), node, resource);
        } catch (Exception e) {
            log.debug(e);
            return null;
        }
    }

    public static DBNResource findResource(
        @NotNull DBRProgressMonitor monitor,
        @Nullable DBNNode node,
        @NotNull IResource resource
    ) throws DBException {
        if (node == null || !(node.getOwnerProject() instanceof RCPProject rcpProject)) {
            return null;
        }
        List<IResource> path = new ArrayList<>();
        for (IResource parent = resource;
             !(parent instanceof IProject) && !CommonUtils.equalObjects(parent, rcpProject.getRootResource());
             parent = parent.getParent())
        {
            path.add(0, parent);
        }

        DBNNode resNode = node;
        for (IResource res : path) {
            resNode.getChildren(monitor);
            resNode = getChild(resNode, res);
            if (resNode == null) {
                return null;
            }
        }
        if (resNode instanceof DBNResource dbnResource) {
            return dbnResource;
        }
        log.warn("Node '" + resNode + "' is not a local resource");
        return null;
    }


    public static boolean isRootResource(DBPProject ownerProject, IResource resource) {
        return ownerProject instanceof RCPProject rcpProject &&
               (CommonUtils.equalObjects(resource.getParent(), rcpProject.getRootResource()) ||
                CommonUtils.equalObjects(resource.getParent(), rcpProject.getEclipseProject()));
    }

    static void sortChildren(DBNNode[] list) {
        Arrays.sort(list, COMPARATOR);
    }

    public static DBNNode makeNode(DBNNode parentNode, IResource resource) {
        boolean isRootResource = isRootResource(parentNode.getOwnerProject(), resource);
        if (isRootResource && resource.getName().startsWith(".")) {
            // Skip project config
            return null;
        }
        try {
            if (parentNode instanceof DBNResource resourceNode && resource instanceof IFolder && !isRootResource) {
                // Sub folder
                return resourceNode.getHandler().makeNavigatorNode(parentNode, resource);
            }
            DBPResourceHandler resourceHandler = DBPPlatformDesktop.getInstance().getWorkspace().getResourceHandler(resource);
            if (resourceHandler == null) {
                log.debug("Skip resource '" + resource.getName() + "'");
                return null;
            }

            return resourceHandler.makeNavigatorNode(parentNode, resource);
        } catch (Exception e) {
            log.error("Error creating navigator node for resource '" + resource.getName() + "'", e);
            return null;
        }
    }

    public static DBNNode getChild(DBNNode parentNode, IResource resource) {
        if (parentNode instanceof DBNNodeWithCache nodeWithCache && !nodeWithCache.needsInitialization()) {
            for (DBNNode child : nodeWithCache.getCachedChildren()) {
                if (Objects.equals(child.getAdapter(IResource.class), resource)) {
                    return child;
                }
            }
        }
        return null;
    }

    public static void refreshThisResource(DBRProgressMonitor monitor, DBNNode resNode) throws DBException {
        IResource resource = resNode.getAdapter(IResource.class);
        if (resource == null) {
            return;
        }
        try {
            refreshFileStore(monitor, resource);
            resource.refreshLocal(IResource.DEPTH_INFINITE, monitor.getNestedMonitor());

            IPath resourceLocation = resource.getLocation();
            if (resourceLocation != null && !resourceLocation.toFile().exists()) {
                log.debug("Resource '" + resource.getName() + "' doesn't exists on file system");
                //resource.delete(true, monitor.getNestedMonitor());
            }
        } catch (CoreException e) {
            throw new DBException("Can't refresh resource", e);
        }
    }

    public static void refreshFileStore(@NotNull DBRProgressMonitor monitor, IResource resource) throws DBException {
        if (resource instanceof Resource) {
            final DBFRemoteFileStore remoteFileStore = GeneralUtils.adapt(((Resource) resource).getStore(), DBFRemoteFileStore.class);
            if (remoteFileStore != null) {
                remoteFileStore.refresh(monitor);
            }
        }
    }
}

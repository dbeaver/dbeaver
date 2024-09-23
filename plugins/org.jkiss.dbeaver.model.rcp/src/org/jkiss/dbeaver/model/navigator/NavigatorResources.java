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
import org.eclipse.core.resources.IResource;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.rcp.RCPProject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

public class NavigatorResources {

    private static final Log log = Log.getLog(NavigatorResources.class);

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
        DBNResource curResNode = projectNode;
        for (IResource res : path) {
            curResNode = curResNode.getChild(res);
            if (curResNode == null) {
                return null;
            }
        }
        return curResNode;
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

    public static DBNResource findResource(@Nullable DBNResource node, @NotNull IResource resource) {
        try {
            return findResource(new VoidProgressMonitor(), node, resource);
        } catch (Exception e) {
            log.debug(e);
            return null;
        }
    }

    public static DBNResource findResource(
        @NotNull DBRProgressMonitor monitor,
        @Nullable DBNResource node,
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

        DBNResource resNode = node;
        for (IResource res : path) {
            resNode.getChildren(monitor);
            resNode = resNode.getChild(res);
            if (resNode == null) {
                return null;
            }
        }
        return resNode;
    }


}

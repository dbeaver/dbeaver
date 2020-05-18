/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.navigator.database;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.navigator.DBNEmptyNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIExecutionQueue;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.INavigatorFilter;
import org.jkiss.utils.CommonUtils;

public class DatabaseBrowserView extends NavigatorViewBase {
    public static final String VIEW_ID = "org.jkiss.dbeaver.core.databaseBrowser";

    private static final Log log = Log.getLog(DatabaseBrowserView.class);

    public DatabaseBrowserView()
    {
        super();
    }

    @Override
    protected INavigatorFilter getNavigatorFilter() {
        return new DatabaseNavigatorTreeFilter();
    }

    @Override
    public DBNNode getRootNode()
    {
        String secondaryId = getViewSite().getSecondaryId();
        if (!CommonUtils.isEmpty(secondaryId)) {
            try {
                return getNodeFromSecondaryId(secondaryId);
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError("Open database browser", "Can't find database navigator node", e);
            }
        }
        DBNProject projectNode = getModel().getRoot().getProjectNode(DBWorkbench.getPlatform().getWorkspace().getActiveProject());
        return projectNode == null ? new DBNEmptyNode() : projectNode.getDatabases();
    }

    @Override
    public void createPartControl(Composite parent)
    {
        super.createPartControl(parent);
        UIUtils.setHelp(parent, IHelpContextIds.CTX_DATABASE_NAVIGATOR);

        String secondaryId = getViewSite().getSecondaryId();
        if (!CommonUtils.isEmpty(secondaryId)) {
            UIExecutionQueue.queueExec(() -> {
                try {
                    DBNNode node = getNodeFromSecondaryId(secondaryId);
                    setPartName(node.getNodeName());
                    setTitleImage(DBeaverIcons.getImage(node.getNodeIconDefault()));
                } catch (DBException e) {
                    // ignore
                }
            });
        }
    }

    @Override
    public void init(IViewSite site) throws PartInitException {
        super.init(site);

    }

    @Override
    public void saveState(IMemento memento) {
        super.saveState(memento);
    }

    public static String getSecondaryIdFromNode(DBNNode node) {
        DBPProject project = null;
        for (DBNNode dn = node; dn != null; dn = dn.getParentNode()) {
            if (dn instanceof DBNProject) {
                project = ((DBNProject) dn).getProject();
                break;
            }
        }
        if (project == null) {
            throw new IllegalStateException("Navigator node " + node.getNodeItemPath() + " doesn't belong to a project");
        }
        // We can't use colon in secondary ID
        return project.getName() + "|" + node.getNodeItemPath().replace(":", "~");
    }

    public static DBNNode getNodeFromSecondaryId(String id) throws DBException {
        int divPos = id.indexOf('|');
        if (divPos == -1) {
            throw new DBException("Bad secondary ID: " + id);
        }
        String projectName = id.substring(0, divPos);
        String nodePath = id.substring(divPos + 1).replace("~", ":");
        IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
        if (project == null) {
            throw new DBException("Project '" + projectName + "' not found");
        }
        final DBNModel navigatorModel = DBWorkbench.getPlatform().getNavigatorModel();
        DBNNode node = null;
        DBPProject projectMeta = DBWorkbench.getPlatform().getWorkspace().getProject(project);
        if (projectMeta != null) {
            navigatorModel.ensureProjectLoaded(projectMeta);
            node = navigatorModel.getNodeByPath(new VoidProgressMonitor(), projectMeta, nodePath);
        }
        if (node == null) {
            log.error("Node " + nodePath + " not found for browse view");
            node = new DBNEmptyNode();
        }
        return node;
    }

}

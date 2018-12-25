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
package org.jkiss.dbeaver.ui.navigator.database;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.navigator.DBNEmptyNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.navigator.INavigatorFilter;
import org.jkiss.utils.CommonUtils;

public class DatabaseBrowserView extends NavigatorViewBase {
    public static final String VIEW_ID = "org.jkiss.dbeaver.core.databaseBrowser";

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
                DBUserInterface.getInstance().showError("Open database browser", "Can't find database navigator node", e);
            }
        }
        DBNProject projectNode = getModel().getRoot().getProject(DBWorkbench.getPlatform().getProjectManager().getActiveProject());
        return projectNode == null ? new DBNEmptyNode() : projectNode.getDatabases();
    }

    @Override
    public void createPartControl(Composite parent)
    {
        super.createPartControl(parent);
        UIUtils.setHelp(parent, IHelpContextIds.CTX_DATABASE_NAVIGATOR);

        String secondaryId = getViewSite().getSecondaryId();
        if (!CommonUtils.isEmpty(secondaryId)) {
            try {
                DBNNode node = getNodeFromSecondaryId(secondaryId);
                setPartName(node.getNodeName());
                setTitleImage(DBeaverIcons.getImage(node.getNodeIconDefault()));
            } catch (DBException e) {
                // ignore
            }
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
        IProject project = null;
        for (DBNNode dn = node; dn != null; dn = dn.getParentNode()) {
            if (dn instanceof DBNProject) {
                project = ((DBNProject) dn).getProject();
                break;
            }
        }
        if (project == null) {
            throw new IllegalStateException("Bad navigator node: " + node.getNodeItemPath());
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
        final DBNModel navigatorModel = DBeaverCore.getInstance().getNavigatorModel();
        navigatorModel.ensureProjectLoaded(project);
        return navigatorModel.getNodeByPath(new VoidProgressMonitor(), project, nodePath);
    }

}

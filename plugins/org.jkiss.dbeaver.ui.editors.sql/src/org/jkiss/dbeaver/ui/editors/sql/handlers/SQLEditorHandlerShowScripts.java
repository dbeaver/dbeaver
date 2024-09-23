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
package org.jkiss.dbeaver.ui.editors.sql.handlers;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.app.DBPResourceTypeDescriptor;
import org.jkiss.dbeaver.model.navigator.DBNProject;
import org.jkiss.dbeaver.model.navigator.DBNResource;
import org.jkiss.dbeaver.model.navigator.NavigatorResources;
import org.jkiss.dbeaver.model.rcp.RCPProject;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.registry.ResourceTypeRegistry;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditorUtils;
import org.jkiss.dbeaver.ui.editors.sql.scripts.ScriptsHandlerImpl;
import org.jkiss.dbeaver.ui.navigator.database.NavigatorViewBase;
import org.jkiss.dbeaver.ui.navigator.project.ProjectExplorerView;

public class SQLEditorHandlerShowScripts extends SQLEditorHandlerOpenEditor {

    private static final Log log = Log.getLog(SQLEditorHandlerShowScripts.class);

    public SQLEditorHandlerShowScripts()
    {
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        DBPProject project = DBWorkbench.getPlatform().getWorkspace().getActiveProject();
        if (!(project instanceof RCPProject rcpProject)) {
            return null;
        }
        try {
            DBNProject projectNode = NavigatorViewBase.getGlobalNavigatorModel()
                .getRoot().getProjectNode(rcpProject);

            if (projectNode == null || !rcpProject.hasRealmPermission(RMConstants.PERMISSION_PROJECT_RESOURCE_VIEW)) {
                return null;
            }

            DBPDataSourceContainer ds = getActiveDataSourceContainer(event, true);
            if (ds != null) {
                SQLNavigatorContext context = new SQLNavigatorContext(ds);
                SQLEditorUtils.ResourceInfo res = SQLEditorUtils.findRecentScript(rcpProject, context);
                if (res != null) {
                    showResourceInExplorer(event, projectNode, res.getResource());
                }
            } else {
                DBPResourceTypeDescriptor resourceType = ResourceTypeRegistry.getInstance().getResourceType(ScriptsHandlerImpl.RESOURCE_TYPE_ID_SQL_SCRIPT);
                String defaultRoot = resourceType.getDefaultRoot(rcpProject);
                if (defaultRoot != null) {
                    IContainer rootResource = rcpProject.getRootResource();
                    if (rootResource != null) {
                        IResource scriptsRoot = rootResource.findMember(defaultRoot);
                        if (scriptsRoot instanceof IFolder) {
                            showResourceInExplorer(event, projectNode, scriptsRoot);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error(e);
        }
        return null;
    }

    private static boolean showResourceInExplorer(ExecutionEvent event, DBNProject projectNode, IResource resource1) throws PartInitException {
        DBNResource resource = NavigatorResources.findResource(projectNode, resource1);
        if (resource != null) {
            ProjectExplorerView projectExplorer = getProjectExplorerView(event);
            if (projectExplorer != null) {
                projectExplorer.showNode(resource);
            }
            return true;
        }
        return false;
    }

    @Nullable
    private static ProjectExplorerView getProjectExplorerView(ExecutionEvent event) throws PartInitException {
        IWorkbenchWindow ww = HandlerUtil.getActiveWorkbenchWindow(event);
        ProjectExplorerView projectExplorer = ww == null ? null : (ProjectExplorerView)ww.getActivePage().showView(
            ProjectExplorerView.VIEW_ID);
        if (projectExplorer == null) {
            return null;
        }
        return projectExplorer;
    }
}

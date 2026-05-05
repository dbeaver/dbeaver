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
package org.jkiss.dbeaver.ui.navigator.actions;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.app.DBPProject;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBEObjectManager;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.navigator.DBNContainer;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.rm.RMConstants;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.TasksJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.LinkedHashMap;
import java.util.Map;

public class NavigatorHandlerObjectRename extends NavigatorHandlerObjectBase {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);

        if (selection instanceof IStructuredSelection structSelection) {
            Object element = structSelection.getFirstElement();
            DBNNode node = RuntimeUtils.getObjectAdapter(element, DBNNode.class);
            if (node != null) {
                DBPProject nodeProject = node.getOwnerProject();
                if (nodeProject == null || nodeProject.hasRealmPermission(RMConstants.PERMISSION_PROJECT_RESOURCE_EDIT)) {
                    renameNode(HandlerUtil.getActiveWorkbenchWindow(event), HandlerUtil.getActiveShell(event), node, null, this);
                }
            }
        }
        return null;
    }

    public static boolean renameNode(IWorkbenchWindow workbenchWindow, Shell shell, final DBNNode node, String newName, Object uiSource)
    {
        String oldName = node instanceof DBNDatabaseNode ? ((DBNDatabaseNode) node).getPlainNodeName(true,
            false) : node.getNodeDisplayName();
        if (oldName == null) {
            oldName = "?";
        }
        if (newName == null) {
            newName = EnterNameDialog.chooseName(shell,
                NLS.bind(UINavigatorMessages.actions_navigator_rename_object, node.getNodeTypeLabel()), oldName);
        }
        if (CommonUtils.isEmpty(newName) || newName.equals(oldName)) {
            return false;
        }

        if (node.supportsRename()) {
            try {
                // Rename with null monitor because it is some local resource
                String finalNewName = newName;
                UIUtils.runInProgressService(monitor -> {
                    try {
                        node.rename(monitor, finalNewName);
                    } catch (DBException e) {
                        throw new InvocationTargetException(e);
                    }
                });
                return true;
            } catch (Exception e) {
                DBWorkbench.getPlatformUI().showError(
                    UINavigatorMessages.actions_navigator_rename_object_exception_title,
                    NLS.bind(UINavigatorMessages.actions_navigator_rename_object_exception_message, oldName), e);
            }
        }
        if (node instanceof DBNDatabaseNode dbNode) {
            return renameDatabaseObject(
                workbenchWindow,
                dbNode,
                CommonUtils.toString(UIUtils.normalizePropertyValue(newName)), uiSource);
        }
        return false;
    }

    public static boolean renameDatabaseObject(IWorkbenchWindow workbenchWindow, DBNDatabaseNode node, String newName, Object uiSource)
    {
        try {
            if (node.getParentNode() instanceof DBNContainer) {
                DBSObject object = node.getObject();
                if (object != null) {
                    DBEObjectRenamer objectRenamer = DBWorkbench.getPlatform().getEditorsRegistry().getObjectManager(object.getClass(), DBEObjectRenamer.class);
                    if (objectRenamer != null) {
                        CommandTarget commandTarget = getCommandTarget(
                            workbenchWindow,
                            node.getParentNode(),
                            node,
                            object.getClass(),
                            false);

                        Map<String, Object> options = new LinkedHashMap<>();
                        options.put(DBEObjectManager.OPTION_UI_SOURCE, uiSource);
                        objectRenamer.renameObject(commandTarget.getContext(), object, options, newName);
                        if (object.isPersisted() && commandTarget.getEditor() == null) {
                            if (!showScript(workbenchWindow, commandTarget.getContext(), DBPScriptObject.EMPTY_OPTIONS,
                                UINavigatorMessages.actions_navigator_rename_script)) {
                                commandTarget.getContext().resetChanges(true);
                                return false;
                            } else {
                                ObjectSaver renamer = new ObjectSaver(commandTarget.getContext(), DBPScriptObject.EMPTY_OPTIONS);
                                TasksJob.runTask(NLS.bind(UINavigatorMessages.actions_navigator_rename_database_object,
                                    object.getName()), renamer);
                            }
                        } else {
                            for (DBECommand command : commandTarget.getContext().getFinalCommands()) {
                                //System.out.println(command);
                            }
                        }
                        return true;
                    }
                }
            }
        } catch (Throwable e) {
            DBWorkbench.getPlatformUI().showError(
                UINavigatorMessages.actions_navigator_rename_database_object_exception_title,
                NLS.bind(UINavigatorMessages.actions_navigator_rename_database_object_exception_message,
                    node.getNodeDisplayName()),
                e);
            return false;
        }
        return false;
    }

}
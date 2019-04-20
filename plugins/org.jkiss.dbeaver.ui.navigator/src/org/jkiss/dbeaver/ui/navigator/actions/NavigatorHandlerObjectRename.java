/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBEObjectRenamer;
import org.jkiss.dbeaver.model.navigator.DBNContainer;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.TasksJob;
import org.jkiss.dbeaver.ui.dialogs.EnterNameDialog;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

public class NavigatorHandlerObjectRename extends NavigatorHandlerObjectBase {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);

        if (selection instanceof IStructuredSelection) {
            IStructuredSelection structSelection = (IStructuredSelection) selection;
            Object element = structSelection.getFirstElement();
            if (element instanceof DBNNode) {
                renameNode(HandlerUtil.getActiveWorkbenchWindow(event), (DBNNode) element, null);
            }
        }
        return null;
    }

    public static boolean renameNode(IWorkbenchWindow workbenchWindow, final DBNNode node, String newName)
    {
        String oldName = node instanceof DBNDatabaseNode ? ((DBNDatabaseNode) node).getPlainNodeName(true, false) : node.getNodeName();
        if (oldName == null) {
            oldName = "?";
        }
        if (newName == null) {
            newName = EnterNameDialog.chooseName(workbenchWindow.getShell(), "Rename " + node.getNodeType(), oldName);
        }
        if (CommonUtils.isEmpty(newName) || newName.equals(oldName)) {
            return false;
        }

        if (node.supportsRename()) {
            try {
                // Rename with null monitor because it is some local resource
                node.rename(new VoidProgressMonitor(), newName);
                return true;
            } catch (DBException e) {
                DBWorkbench.getPlatformUI().showError("Rename", "Can't rename object '" + oldName + "'", e);
            }
        }
        if (node instanceof DBNDatabaseNode) {
            return renameDatabaseObject(
                workbenchWindow,
                (DBNDatabaseNode) node,
                newName);
        }
        return false;
    }

    public static boolean renameDatabaseObject(IWorkbenchWindow workbenchWindow, DBNDatabaseNode node, String newName)
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
                            object.getClass(),
                            false);

                        objectRenamer.renameObject(commandTarget.getContext(), object, newName);
                        if (object.isPersisted() && commandTarget.getEditor() == null) {
                            Map<String, Object> options = DBPScriptObject.EMPTY_OPTIONS;
                            if (!showScript(workbenchWindow, commandTarget.getContext(), options, "Rename script")) {
                                commandTarget.getContext().resetChanges(true);
                                return false;
                            } else {
                                ObjectSaver renamer = new ObjectSaver(commandTarget.getContext(), options);
                                TasksJob.runTask("Rename object '" + object.getName() + "'", renamer);
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
            DBWorkbench.getPlatformUI().showError("Rename object", "Can't rename object '" + node.getNodeName() + "'", e);
            return false;
        }
        return false;
    }

}
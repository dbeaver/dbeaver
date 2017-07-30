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
package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverActivator;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.editor.EntityEditorsRegistry;
import org.jkiss.dbeaver.runtime.TasksJob;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditor;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;

import java.util.*;

public class NavigatorHandlerObjectDelete extends NavigatorHandlerObjectBase implements IElementUpdater {
    private static final Log log = Log.getLog(NavigatorHandlerObjectDelete.class);

    private IStructuredSelection structSelection;
    private Boolean deleteAll;
    private List<DBRRunnableWithProgress> tasksToExecute = new ArrayList<>();

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        this.structSelection = null;
        this.deleteAll = null;
        this.tasksToExecute.clear();

        final IWorkbenchWindow activeWorkbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
        final ISelection selection = HandlerUtil.getCurrentSelection(event);

        if (selection instanceof IStructuredSelection) {
            structSelection = (IStructuredSelection)selection;
            for (Iterator<?> iter = structSelection.iterator(); iter.hasNext(); ) {
                Object element = iter.next();
                if (element instanceof DBNDatabaseNode) {
                    deleteObject(activeWorkbenchWindow, (DBNDatabaseNode)element);
                } else if (element instanceof DBNResource) {
                    deleteResource(activeWorkbenchWindow, (DBNResource)element);
                } else if (element instanceof DBNLocalFolder) {
                    deleteLocalFolder(activeWorkbenchWindow, (DBNLocalFolder) element);
                } else {
                    log.warn("Don't know how to delete element '" + element + "'"); //$NON-NLS-1$ //$NON-NLS-2$
                }
                if (deleteAll != null && !deleteAll) {
                    break;
                }
            }
        }

        if (!tasksToExecute.isEmpty()) {
            TasksJob.runTasks(tasksToExecute.size() > 1 ? "Delete " + tasksToExecute.size() + " objects" : "Delete object", tasksToExecute);
            tasksToExecute.clear();
        }

        return null;
    }

    private void deleteLocalFolder(IWorkbenchWindow workbenchWindow, DBNLocalFolder localFolder)
    {
        ConfirmResult confirmResult = confirmObjectDelete(workbenchWindow, localFolder, false);
        if (confirmResult == ConfirmResult.NO) {
            return;
        }
        localFolder.getDataSourceRegistry().removeFolder(localFolder.getFolder(), false);
        DBNModel.updateConfigAndRefreshDatabases(localFolder);
    }

    private boolean deleteResource(IWorkbenchWindow workbenchWindow, final DBNResource resourceNode)
    {
        ConfirmResult confirmResult = confirmObjectDelete(workbenchWindow, resourceNode, false);
        if (confirmResult == ConfirmResult.NO) {
            return false;
        }

        final IResource resource = resourceNode.getResource();
        try {
            if (resource instanceof IFolder) {
                ((IFolder)resource).delete(true, false, new NullProgressMonitor());
            } else if (resource instanceof IProject) {
                // Delete project (with all contents)
                final boolean deleteContent = UIUtils.confirmAction(workbenchWindow.getShell(), "Delete project", "Delete project '" + resource.getName() + "' contents?");
                ((IProject) resource).delete(deleteContent, true, new NullProgressMonitor());
            } else if (resource != null) {
                resource.delete(true, new NullProgressMonitor());
            }
        } catch (CoreException e) {
            log.error(e);
            return false;
        }
        return true;
    }

    private boolean deleteObject(IWorkbenchWindow workbenchWindow, DBNDatabaseNode node)
    {
        try {
            if (!(node.getParentNode() instanceof DBNContainer)) {
                throw new DBException("Node '" + node + "' doesn't have a container");
            }
            final DBNContainer container = (DBNContainer) node.getParentNode();

            // Try to delete object using object manager
            DBSObject object = node.getObject();
            if (object == null) {
                throw new DBException("Can't delete node with null object");
            }
            DBEObjectMaker objectMaker = EntityEditorsRegistry.getInstance().getObjectManager(object.getClass(), DBEObjectMaker.class);
            if (objectMaker == null) {
                throw new DBException("Object maker not found for type '" + object.getClass().getName() + "'"); //$NON-NLS-2$
            }

            Map<String, Object> deleteOptions = null;

            CommandTarget commandTarget = getCommandTarget(
                workbenchWindow,
                container,
                object.getClass(),
                false);

            ConfirmResult confirmResult = ConfirmResult.YES;
            if (!object.isPersisted() || commandTarget.getEditor() != null) {
                // Not a real object delete because it's not persisted
                // There should be command context somewhere
                if (deleteNewObject(workbenchWindow, node)) {
                    return true;
                }
                // No direct editor host found for this object -
                // try to find corresponding command context
                // and execute command within it
            } else {
                // Persisted object - confirm delete
                // Show "View script" only if we are not in some editor (because it have its own "View script" button)
                confirmResult = confirmObjectDelete(workbenchWindow, node, commandTarget.getContext() != null && commandTarget.getEditor() == null );
                if (confirmResult == ConfirmResult.NO) {
                    return false;
                }
            }

            objectMaker.deleteObject(commandTarget.getContext(), node.getObject(), deleteOptions);
            if (confirmResult == ConfirmResult.DETAILS) {
                if (!showScript(workbenchWindow, commandTarget.getContext(), CoreMessages.actions_navigator_delete_script)) {
                    commandTarget.getContext().resetChanges();
                    // Show confirmation again
                    return deleteObject(workbenchWindow, node);
                }
            }

            if (commandTarget.getEditor() == null && commandTarget.getContext() != null) {
                // Persist object deletion - only if there is no host editor and we have a command context
                ObjectSaver deleter = new ObjectSaver(commandTarget.getContext());
//                DBeaverUI.runInProgressDialog(deleter);
                tasksToExecute.add(deleter);
            }

        } catch (Throwable e) {
            DBUserInterface.getInstance().showError(CoreMessages.actions_navigator_error_dialog_delete_object_title, "Can't delete object '" + node.getNodeName() + "'", e);
            return false;
        }

        return true;
    }

    private boolean deleteNewObject(IWorkbenchWindow workbenchWindow, DBNDatabaseNode node) throws DBException
    {
        for (final IEditorReference editorRef : workbenchWindow.getActivePage().getEditorReferences()) {
            final IEditorPart editor = editorRef.getEditor(false);

            if (editor instanceof IDatabaseEditor) {
                final IDatabaseEditorInput editorInput = ((IDatabaseEditor)editor).getEditorInput();
                if (editorInput.getDatabaseObject() == node.getObject()) {

                    ConfirmResult confirmResult = confirmObjectDelete(workbenchWindow, node, false);
                    if (confirmResult == ConfirmResult.NO) {
                        return true;
                    }
                    // Just close editor
                    // It should dismiss new object and remove navigator node
                    workbenchWindow.getActivePage().closeEditor(editor, false);
                    return true;
                }
            }
        }
        return false;
    }

    enum ConfirmResult {
        YES,
        NO,
        DETAILS,
    }

    private ConfirmResult confirmObjectDelete(final IWorkbenchWindow workbenchWindow, final DBNNode node, final boolean viewScript)
    {
        if (deleteAll != null) {
            return deleteAll ? ConfirmResult.YES : ConfirmResult.NO;
        }
        ResourceBundle bundle = DBeaverActivator.getCoreResourceBundle();
        String objectType = node instanceof DBNLocalFolder ? DBeaverPreferences.CONFIRM_LOCAL_FOLDER_DELETE : DBeaverPreferences.CONFIRM_ENTITY_DELETE;
        String titleKey = ConfirmationDialog.RES_CONFIRM_PREFIX + objectType + "_" + ConfirmationDialog.RES_KEY_TITLE; //$NON-NLS-1$
        String messageKey = ConfirmationDialog.RES_CONFIRM_PREFIX + objectType + "_" + ConfirmationDialog.RES_KEY_MESSAGE; //$NON-NLS-1$

        String nodeTypeName = node.getNodeType();

        MessageDialog dialog = new MessageDialog(
            workbenchWindow.getShell(),
            UIUtils.formatMessage(bundle.getString(titleKey), nodeTypeName, node.getNodeName()),
            DBeaverIcons.getImage(UIIcon.REJECT),
                UIUtils.formatMessage(bundle.getString(messageKey), nodeTypeName.toLowerCase(), node.getNodeName()),
                MessageDialog.CONFIRM, null, 0)
        {
            @Override
            protected void createButtonsForButtonBar(Composite parent)
            {
                createButton(parent, IDialogConstants.YES_ID, IDialogConstants.YES_LABEL, true);
                createButton(parent, IDialogConstants.NO_ID, IDialogConstants.NO_LABEL, false);
                if (structSelection.size() > 1) {
                    createButton(parent, IDialogConstants.YES_TO_ALL_ID, IDialogConstants.YES_TO_ALL_LABEL, false);
                    createButton(parent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
                }
                if (viewScript) {
                    createButton(parent, IDialogConstants.DETAILS_ID, CoreMessages.actions_navigator_view_script_button, false);
                }
            }
        };
        int result = dialog.open();
        switch (result) {
            case IDialogConstants.YES_ID:
                return ConfirmResult.YES;
            case IDialogConstants.YES_TO_ALL_ID:
                deleteAll = true;
                return ConfirmResult.YES;
            case IDialogConstants.NO_ID:
                return ConfirmResult.NO;
            case IDialogConstants.CANCEL_ID:
            case -1:
                deleteAll = false;
                return ConfirmResult.NO;
            case IDialogConstants.DETAILS_ID:
                return ConfirmResult.DETAILS;
            default:
                log.warn("Unsupported confirmation dialog result: " + result); //$NON-NLS-1$
                return ConfirmResult.NO;
        }
    }

    @Override
    public void updateElement(UIElement element, Map parameters)
    {
        if (!updateUI) {
            return;
        }
/*
        final ISelectionProvider selectionProvider = UIUtils.getSelectionProvider(element.getServiceLocator());
        if (selectionProvider != null) {
            ISelection selection = selectionProvider.getSelection();

            if (selection instanceof IStructuredSelection && ((IStructuredSelection) selection).size() > 1) {
                element.setText(CoreMessages.actions_navigator_delete_objects);
            } else {
                DBNNode node = NavigatorUtils.getSelectedNode(selection);
                if (node != null) {
                    element.setText(CoreMessages.actions_navigator_delete_ + " " + node.getNodeType()  + " '" + node.getNodeName() + "'");
                }
            }
        }
*/

    }


}
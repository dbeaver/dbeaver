/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.*;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.edit.DBEObjectEditor;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.project.DBPResourceHandler;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.registry.editor.EntityEditorsRegistry;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.dbeaver.ui.navigator.NavigatorUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.folders.IFolderContainer;
import org.jkiss.dbeaver.ui.dialogs.connection.EditConnectionDialog;
import org.jkiss.dbeaver.ui.dialogs.connection.EditConnectionWizard;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorInput;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorInputFactory;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditorInput;
import org.jkiss.dbeaver.ui.editors.entity.FolderEditor;
import org.jkiss.dbeaver.ui.editors.entity.FolderEditorInput;
import org.jkiss.dbeaver.ui.editors.object.ObjectEditorInput;
import org.jkiss.utils.CommonUtils;

import java.util.Iterator;
import java.util.Map;

public class NavigatorHandlerObjectOpen extends NavigatorHandlerObjectBase implements IElementUpdater {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);

        if (selection instanceof IStructuredSelection) {
            final IStructuredSelection structSelection = (IStructuredSelection)selection;
            for (Iterator<?> iter = structSelection.iterator(); iter.hasNext(); ) {
                Object element = iter.next();
                DBNDatabaseNode node = null;
                if (element instanceof DBNResource) {
                    openResource(((DBNResource)element).getResource(), HandlerUtil.getActiveWorkbenchWindow(event));
                    continue;
                } else if (element instanceof IResource) {
                    openResource((IResource)element, HandlerUtil.getActiveWorkbenchWindow(event));
                    continue;
                } else if (element instanceof DBNDatabaseNode) {
                    node = (DBNDatabaseNode)element;
                } else {
                    DBSObject object = RuntimeUtils.getObjectAdapter(element, DBSObject.class);
                    if (object != null) {
                        node = getNodeByObject(object);
                    }
                }
                if (node != null) {
                    openEntityEditor(node, null, HandlerUtil.getActiveWorkbenchWindow(event));
                }
            }
        }
        return null;
    }

    public static void openResource(IResource resource, IWorkbenchWindow window)
    {
        try {
            DBPResourceHandler handler = DBeaverCore.getInstance().getProjectRegistry().getResourceHandler(resource);
            if (handler != null) {
                handler.openResource(resource);
            }
        } catch (Exception e) {
            UIUtils.showErrorDialog(window.getShell(), CoreMessages.actions_navigator_error_dialog_open_resource_title, "Can't open resource '" + resource.getName() + "'", e); //$NON-NLS-3$
        }
    }

    @Nullable
    public static IEditorPart openEntityEditor(DBSObject object)
    {
        DBNDatabaseNode node = NavigatorHandlerObjectOpen.getNodeByObject(object);
        if (node != null) {
            return NavigatorHandlerObjectOpen.openEntityEditor(node, null, DBeaverUI.getActiveWorkbenchWindow());
        }
        return null;
    }

    public static IEditorPart openEntityEditor(
        DBNDatabaseNode selectedNode,
        @Nullable String defaultPageId,
        IWorkbenchWindow workbenchWindow)
    {
        return openEntityEditor(selectedNode, defaultPageId, null , workbenchWindow);
    }

    public static IEditorPart openEntityEditor(
        @NotNull DBNDatabaseNode selectedNode,
        @Nullable String defaultPageId,
        @Nullable Map<String, Object> attributes,
        IWorkbenchWindow workbenchWindow)
    {
        if (selectedNode instanceof DBNDataSource) {
            EditConnectionDialog dialog = new EditConnectionDialog(
                workbenchWindow,
                new EditConnectionWizard((DataSourceDescriptor) selectedNode.getDataSourceContainer()));
            dialog.open();
            return null;
        }
        if (!selectedNode.isPersisted()) {
            return null;
        }
        try {
            String defaultFolderId = null;
            if (selectedNode instanceof DBNDatabaseFolder && !(selectedNode.getParentNode() instanceof DBNDatabaseFolder) && selectedNode.getParentNode() instanceof DBNDatabaseNode) {
                defaultFolderId = selectedNode.getNodeType();
                selectedNode = (DBNDatabaseNode) selectedNode.getParentNode();
            }

            DatabaseEditorInputFactory.setLookupEditor(true);
            try {
                for (IEditorReference ref : workbenchWindow.getActivePage().getEditorReferences()) {
                    IEditorInput editorInput;
                    try {
                        editorInput = ref.getEditorInput();
                    } catch (PartInitException e) {
                        continue;
                    }
                    if (editorInput instanceof EntityEditorInput && ((EntityEditorInput) editorInput).getNavigatorNode() == selectedNode) {
                        final IEditorPart editor = ref.getEditor(true);
                        if (editor instanceof IFolderContainer && defaultFolderId != null) {
                            // Activate default folder
                            ((IFolderContainer)editor).switchFolder(defaultFolderId);
                        }
                        workbenchWindow.getActivePage().activate(editor);
                        return editor;
                    }
                }
            }
            finally {
                DatabaseEditorInputFactory.setLookupEditor(false);
            }

            IWorkbenchPart oldActivePart = workbenchWindow.getActivePage().getActivePart();
            try {
                if (selectedNode instanceof DBNDatabaseFolder) {
                    FolderEditorInput folderInput = new FolderEditorInput((DBNDatabaseFolder)selectedNode);
                    folderInput.setDefaultPageId(defaultPageId);
                    setInputAttributes(folderInput, defaultPageId, defaultFolderId, attributes);
                    return workbenchWindow.getActivePage().openEditor(
                        folderInput,
                        FolderEditor.class.getName());
                } else if (selectedNode instanceof DBNDatabaseObject) {
                    DBNDatabaseObject objectNode = (DBNDatabaseObject) selectedNode;
                    ObjectEditorInput objectInput = new ObjectEditorInput(objectNode);
                    setInputAttributes(objectInput, defaultPageId, defaultFolderId, attributes);
                    return workbenchWindow.getActivePage().openEditor(
                        objectInput,
                        objectNode.getMeta().getEditorId());
                } else if (selectedNode.getObject() != null) {
                    EntityEditorInput editorInput = new EntityEditorInput(selectedNode);
                    setInputAttributes(editorInput, defaultPageId, defaultFolderId, attributes);
                    return workbenchWindow.getActivePage().openEditor(
                        editorInput,
                        EntityEditor.class.getName());
                } else {
                    throw new DBException("Don't know how to open object '" + selectedNode.getNodeName() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
                }
            }
            finally {
                // Reactivate navigator
                // Actually it still focused but we need to use it's selection
                // I think it is an eclipse bug
                if (!(oldActivePart instanceof IEditorPart)) {
                    //workbenchWindow.getActivePage().activate(oldActivePart);
                }
            }
        } catch (Exception ex) {
            UIUtils.showErrorDialog(workbenchWindow.getShell(), CoreMessages.actions_navigator_error_dialog_open_entity_title, "Can't open entity '" + selectedNode.getNodeName() + "'", ex);
            return null;
        }
    }

    private static void setInputAttributes(DatabaseEditorInput<?> editorInput, String defaultPageId, String defaultFolderId, Map<String, Object> attributes) {
        editorInput.setDefaultPageId(defaultPageId);
        editorInput.setDefaultFolderId(defaultFolderId);
        if (!CommonUtils.isEmpty(attributes)) {
            for (Map.Entry<String, Object> attr : attributes.entrySet()) {
                editorInput.setAttribute(attr.getKey(), attr.getValue());
            }
        }
    }

    @Override
    public void updateElement(UIElement element, Map parameters)
    {
        if (!updateUI) {
            return;
        }
        final ISelectionProvider selectionProvider = UIUtils.getSelectionProvider(element.getServiceLocator());
        if (selectionProvider != null) {
            ISelection selection = selectionProvider.getSelection();
            DBNNode node = NavigatorUtils.getSelectedNode(selection);
            if (node != null) {
                String actionName = CoreMessages.actions_navigator_open;
                if (node instanceof DBNDataSource) {
                    actionName = CoreMessages.actions_navigator_edit;
                } else if (node instanceof DBNDatabaseNode) {
                    DBSObject object = ((DBNDatabaseNode) node).getObject();
                    DBEObjectEditor objectManager = EntityEditorsRegistry.getInstance().getObjectManager(
                        object.getClass(),
                        DBEObjectEditor.class);
                    actionName = objectManager == null || !objectManager.canEditObject(object)? CoreMessages.actions_navigator_view : CoreMessages.actions_navigator_edit;
                }
                String label;
                if (selection instanceof IStructuredSelection && ((IStructuredSelection) selection).size() > 1) {
                    label = actionName + CoreMessages.actions_navigator__objects;
                } else {
                    label = actionName + " " + node.getNodeType(); //$NON-NLS-1$
                }
                element.setText(label);
            }
        }
    }
}
/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.ui.IFolderedPart;
import org.jkiss.dbeaver.model.edit.DBEObjectManager;
import org.jkiss.dbeaver.model.edit.DBEPrivateObjectEditor;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.project.DBPResourceHandler;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.NavigatorUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditorInput;
import org.jkiss.dbeaver.ui.editors.entity.FolderEditor;
import org.jkiss.dbeaver.ui.editors.entity.FolderEditorInput;
import org.jkiss.dbeaver.ui.editors.object.ObjectEditorInput;

import java.util.Iterator;
import java.util.Map;

public class NavigatorHandlerObjectOpen extends NavigatorHandlerObjectBase implements IElementUpdater {

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
                handler.openResource(resource, window);
            }
        } catch (Exception e) {
            UIUtils.showErrorDialog(window.getShell(), CoreMessages.actions_navigator_error_dialog_open_resource_title, "Can't open resource '" + resource.getName() + "'", e); //$NON-NLS-3$
        }
    }

    public static IEditorPart openEntityEditor(DBSObject object)
    {
        DBNDatabaseNode node = NavigatorHandlerObjectOpen.getNodeByObject(object);
        if (node != null) {
            return NavigatorHandlerObjectOpen.openEntityEditor(node, null, DBeaverCore.getActiveWorkbenchWindow());
        }
        return null;
    }

    public static IEditorPart openEntityEditor(DBNDatabaseNode selectedNode, String defaultPageId, IWorkbenchWindow workbenchWindow)
    {
        if (selectedNode.getObject() instanceof DBEPrivateObjectEditor) {
            ((DBEPrivateObjectEditor)selectedNode.getObject()).editObject(workbenchWindow);
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
            for (IEditorReference ref : workbenchWindow.getActivePage().getEditorReferences()) {
                if (ref.getEditorInput() instanceof EntityEditorInput && ((EntityEditorInput)ref.getEditorInput()).getTreeNode() == selectedNode) {
                    final IEditorPart editor = ref.getEditor(false);
                    if (editor instanceof IFolderedPart) {
                        ((IFolderedPart)editor).switchFolder(defaultFolderId);
                    }
                    workbenchWindow.getActivePage().activate(editor);
                    return editor;
                }
            }
            if (selectedNode instanceof DBNDatabaseFolder) {
                FolderEditorInput folderInput = new FolderEditorInput((DBNDatabaseFolder)selectedNode);
                folderInput.setDefaultPageId(defaultPageId);
                return workbenchWindow.getActivePage().openEditor(
                    folderInput,
                    FolderEditor.class.getName());
            } else if (selectedNode instanceof DBNDatabaseObject) {
                DBNDatabaseObject objectNode = (DBNDatabaseObject) selectedNode;
                ObjectEditorInput objectInput = new ObjectEditorInput(objectNode);
                return workbenchWindow.getActivePage().openEditor(
                    objectInput,
                    objectNode.getMeta().getEditorId());
            } else if (selectedNode.getObject() != null) {
                EntityEditorInput editorInput = new EntityEditorInput(selectedNode);
                editorInput.setDefaultPageId(defaultPageId);
                editorInput.setDefaultFolderId(defaultFolderId);
                return workbenchWindow.getActivePage().openEditor(
                    editorInput,
                    EntityEditor.class.getName());
            } else {
                throw new DBException("Don't know how to open object '" + selectedNode.getNodeName() + "'"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        } catch (Exception ex) {
            UIUtils.showErrorDialog(workbenchWindow.getShell(), CoreMessages.actions_navigator_error_dialog_open_entity_title, "Can't open entity '" + selectedNode.getNodeName() + "'", ex);
            return null;
        }
    }

    public void updateElement(UIElement element, Map parameters)
    {
        if (!updateUI) {
            return;
        }
        IWorkbenchPartSite partSite = (IWorkbenchPartSite) element.getServiceLocator().getService(IWorkbenchPartSite.class);
        if (partSite != null) {
            final ISelectionProvider selectionProvider = partSite.getSelectionProvider();
            if (selectionProvider != null) {
                ISelection selection = selectionProvider.getSelection();
                DBNNode node = NavigatorUtils.getSelectedNode(selection);
                if (node != null) {
                    String actionName = CoreMessages.actions_navigator_open;
                    if (node instanceof DBNDatabaseNode) {
                        DBEObjectManager<?> objectManager = DBeaverCore.getInstance().getEditorsRegistry().getObjectManager(((DBNDatabaseNode) node).getObject().getClass());
                        actionName = objectManager == null ? CoreMessages.actions_navigator_view : CoreMessages.actions_navigator_edit;
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
}
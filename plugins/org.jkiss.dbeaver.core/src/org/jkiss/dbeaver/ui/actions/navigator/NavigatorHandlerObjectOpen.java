/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.ui.IFolderedPart;
import org.jkiss.dbeaver.model.edit.DBEPrivateObjectEditor;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.project.DBPResourceHandler;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditorInput;
import org.jkiss.dbeaver.ui.editors.entity.FolderEditor;
import org.jkiss.dbeaver.ui.editors.entity.FolderEditorInput;
import org.jkiss.dbeaver.ui.editors.object.ObjectEditorInput;

import java.util.Iterator;

public class NavigatorHandlerObjectOpen extends NavigatorHandlerObjectBase {

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
                    DBSObject object = (DBSObject) Platform.getAdapterManager().getAdapter(element, DBSObject.class);
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
            UIUtils.showErrorDialog(window.getShell(), "Open resource", "Can't open resource", e);
        }
    }

    public static void openEntityEditor(DBNDatabaseNode selectedNode, String defaultPageId, IWorkbenchWindow workbenchWindow)
    {
        if (selectedNode.getObject() instanceof DBEPrivateObjectEditor) {
            ((DBEPrivateObjectEditor)selectedNode.getObject()).editObject(workbenchWindow);
            return;
        }
        IWorkbenchPart oldActivePart = workbenchWindow.getActivePage().getActivePart();
        try {
            String defaultFolderId = null;
            if (selectedNode instanceof DBNDatabaseFolder && !(selectedNode.getParentNode() instanceof DBNDatabaseFolder) && selectedNode.getParentNode() instanceof DBNDatabaseNode) {
                defaultFolderId = selectedNode.getMeta().getLabel();
                selectedNode = (DBNDatabaseNode) selectedNode.getParentNode();
            }
            for (IEditorReference ref : workbenchWindow.getActivePage().getEditorReferences()) {
                if (ref.getEditorInput() instanceof EntityEditorInput && ((EntityEditorInput)ref.getEditorInput()).getTreeNode() == selectedNode) {
                    final IEditorPart editor = ref.getEditor(false);
                    if (editor instanceof IFolderedPart) {
                        ((IFolderedPart)editor).switchFolder(defaultFolderId);
                    }
                    workbenchWindow.getActivePage().activate(editor);
                    return;
                }
            }
            if (selectedNode instanceof DBNDatabaseFolder) {
                FolderEditorInput folderInput = new FolderEditorInput((DBNDatabaseFolder)selectedNode);
                folderInput.setDefaultPageId(defaultPageId);
                workbenchWindow.getActivePage().openEditor(
                    folderInput,
                    FolderEditor.class.getName());
            } else if (selectedNode instanceof DBNDatabaseObject) {
                DBNDatabaseObject objectNode = (DBNDatabaseObject) selectedNode;
                ObjectEditorInput objectInput = new ObjectEditorInput(objectNode);
                workbenchWindow.getActivePage().openEditor(
                    objectInput,
                    objectNode.getMeta().getEditorId());
            } else if (selectedNode.getObject() != null) {
                EntityEditorInput editorInput = new EntityEditorInput(selectedNode);
                editorInput.setDefaultPageId(defaultPageId);
                editorInput.setDefaultFolderId(defaultFolderId);
                workbenchWindow.getActivePage().openEditor(
                    editorInput,
                    EntityEditor.class.getName());
            }
        } catch (Exception ex) {
            UIUtils.showErrorDialog(workbenchWindow.getShell(), "Open entity", "Can't open entity", ex);
        }
        finally {
            // Reactivate navigator
            // Actually it still focused but we need to use it's selection
            // I think it is an eclipse bug
            if (!(oldActivePart instanceof IEditorPart)) {
                workbenchWindow.getActivePage().activate(oldActivePart);
            }
        }
    }

}
/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.edit.DBOEditorInline;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNTreeFolder;
import org.jkiss.dbeaver.model.navigator.DBNTreeObject;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditorInput;
import org.jkiss.dbeaver.ui.editors.folder.FolderEditor;
import org.jkiss.dbeaver.ui.editors.folder.FolderEditorInput;
import org.jkiss.dbeaver.ui.editors.object.ObjectEditorInput;

import java.util.Iterator;

public class NavigatorHandlerObjectOpen extends NavigatorHandlerObjectBase {

    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);

        if (selection instanceof IStructuredSelection) {
            final IStructuredSelection structSelection = (IStructuredSelection)selection;
            for (Iterator iter = structSelection.iterator(); iter.hasNext(); ) {
                Object element = iter.next();
                DBNNode node = null;
                if (element instanceof DBNNode) {
                    node = (DBNNode)element;
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

    public static void openEntityEditor(DBNNode selectedNode, String defaultPageId, IWorkbenchWindow workbenchWindow)
    {
        if (selectedNode.getObject() instanceof DBOEditorInline) {
            ((DBOEditorInline)selectedNode.getObject()).editObject(workbenchWindow);
            return;
        }
        IWorkbenchPart oldActivePart = workbenchWindow.getActivePage().getActivePart();
        try {
            for (IEditorReference ref : workbenchWindow.getActivePage().getEditorReferences()) {
                if (ref.getEditorInput() instanceof EntityEditorInput && ((EntityEditorInput)ref.getEditorInput()).getTreeNode() == selectedNode) {
                    workbenchWindow.getActivePage().activate(ref.getEditor(false));
                    return;
                }
            }
            if (selectedNode instanceof DBNTreeFolder) {
                FolderEditorInput folderInput = new FolderEditorInput((DBNTreeFolder)selectedNode);
                folderInput.setDefaultPageId(defaultPageId);
                workbenchWindow.getActivePage().openEditor(
                    folderInput,
                    FolderEditor.class.getName());
            } else if (selectedNode instanceof DBNTreeObject) {
                DBNTreeObject objectNode = (DBNTreeObject) selectedNode;
                ObjectEditorInput objectInput = new ObjectEditorInput(objectNode);
                workbenchWindow.getActivePage().openEditor(
                    objectInput,
                    objectNode.getMeta().getEditorId());

            } else if (selectedNode.getObject() != null) {
                EntityEditorInput editorInput = new EntityEditorInput(selectedNode);
                editorInput.setDefaultPageId(defaultPageId);
                workbenchWindow.getActivePage().openEditor(
                    editorInput,
                    EntityEditor.class.getName());
            }
        } catch (Exception ex) {
            log.error("Can't open editor", ex);
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
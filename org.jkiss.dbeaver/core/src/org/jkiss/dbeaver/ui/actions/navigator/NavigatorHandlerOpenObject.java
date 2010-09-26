/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNTreeFolder;
import org.jkiss.dbeaver.model.navigator.DBNTreeObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditorInput;
import org.jkiss.dbeaver.ui.editors.folder.FolderEditor;
import org.jkiss.dbeaver.ui.editors.folder.FolderEditorInput;
import org.jkiss.dbeaver.ui.editors.object.ObjectEditorInput;
import org.jkiss.dbeaver.ui.views.navigator.database.DatabaseNavigatorView;

import java.lang.reflect.InvocationTargetException;

public class NavigatorHandlerOpenObject extends AbstractHandler {

    static final Log log = LogFactory.getLog(NavigatorHandlerOpenObject.class);

    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);

        if (selection instanceof IStructuredSelection) {
            final IStructuredSelection structSelection = (IStructuredSelection)selection;
            DBSObject object = (DBSObject) Platform.getAdapterManager().getAdapter(structSelection.getFirstElement(), DBSObject.class);
            if (object != null) {
                DBNModel model = DBeaverCore.getInstance().getNavigatorModel();
                DBNNode node = model.findNode(object);
                if (node == null) {
                    NodeLoader nodeLoader = new NodeLoader(model, object);
                    try {
                        DBeaverCore.getInstance().runAndWait2(nodeLoader);
                    } catch (InvocationTargetException e) {
                        log.warn("Could not load node for object '" + object.getName() + "'", e.getTargetException());
                    } catch (InterruptedException e) {
                        // do nothing
                    }
                    node = nodeLoader.node;
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
            if (oldActivePart instanceof DatabaseNavigatorView) {
                workbenchWindow.getActivePage().activate(oldActivePart);
            }
        }
    }


    private static class NodeLoader implements DBRRunnableWithProgress {
        private final DBNModel model;
        private final DBSObject object;
        private DBNNode node;

        public NodeLoader(DBNModel model, DBSObject object)
        {
            this.model = model;
            this.object = object;
        }

        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
        {
            node = model.getNodeByObject(monitor, object, true);
        }
    }
}
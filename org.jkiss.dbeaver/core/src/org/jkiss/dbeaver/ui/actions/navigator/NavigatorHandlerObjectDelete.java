/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IDatabaseObjectManager;
import org.jkiss.dbeaver.ext.IDatabaseObjectManagerEx;
import org.jkiss.dbeaver.model.DBPDeletableObject;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNTreeItem;
import org.jkiss.dbeaver.model.navigator.DBNTreeNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.EntityManagerDescriptor;
import org.jkiss.dbeaver.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.preferences.PrefConstants;

import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Map;

public class NavigatorHandlerObjectDelete extends NavigatorHandlerObjectBase {

    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);

        if (selection instanceof IStructuredSelection) {
            final IStructuredSelection structSelection = (IStructuredSelection)selection;
            for (Iterator iter = structSelection.iterator(); iter.hasNext(); ) {
                Object element = iter.next();
                if (element instanceof DBNTreeNode) {
                    deleteObject(HandlerUtil.getActiveWorkbenchWindow(event), (DBNTreeNode)element);
                }
            }
        }
        return null;
    }

    private boolean deleteObject(IWorkbenchWindow workbenchWindow, DBNTreeNode node)
    {
        // Check for deletable object
        if (node instanceof DBPDeletableObject) {
            if (!confirmObjectDelete(workbenchWindow, node)) {
                return false;
            }

            ((DBPDeletableObject) node).deleteObject(workbenchWindow);
            return true;
        }

        if (!(node instanceof DBNTreeItem)) {
            log.error("Only tree items could be deleted");
            return false;
        }

        // Try to delete object using object manager
        DBSObject object = node.getObject();
        if (object == null) {
            log.error("Can't delete node with null object");
            return false;
        }
        EntityManagerDescriptor entityManager = DBeaverCore.getInstance().getEditorsRegistry().getEntityManager(object.getClass());
        if (entityManager == null) {
            log.error("Object manager not found for type '" + object.getClass().getName() + "'");
            return false;
        }
        IDatabaseObjectManager<?> objectManager = entityManager.createManager();
        if (!(objectManager instanceof IDatabaseObjectManagerEx<?>)) {
            log.error("Object manager '" + objectManager.getClass().getName() + "' do not supports object deletion");
            return false;
        }

        IDatabaseObjectManagerEx objectManagerEx = (IDatabaseObjectManagerEx)objectManager;
        Map<String, Object> deleteOptions = null;

        objectManagerEx.deleteObject(node.getObject(), deleteOptions);

        if (!confirmObjectDelete(workbenchWindow, node)) {
            objectManagerEx.resetChanges();
            return false;
        }

        // Delete object
        ObjectDeleter deleter = new ObjectDeleter(objectManagerEx);
        try {
            workbenchWindow.run(true, true, deleter);
        } catch (InvocationTargetException e) {
            log.error("Can't delete object", e);
            return false;
        } catch (InterruptedException e) {
            // do nothing
        }

        // Remove node
        DBNNode parent = node.getParentNode();
        if (parent instanceof DBNTreeNode) {
            try {
                ((DBNTreeNode)parent).removeChildItem((DBNTreeItem) node);
            } catch (DBException e) {
                log.error(e);
            }
        }

        return true;
    }

    private boolean confirmObjectDelete(IWorkbenchWindow workbenchWindow, DBNTreeNode node)
    {
        return ConfirmationDialog.confirmActionWithParams(
            workbenchWindow.getShell(),
            PrefConstants.CONFIRM_ENTITY_DELETE,
            node.getMeta().getLabel(),
            node.getNodeName());
    }

    private static class ObjectDeleter implements IRunnableWithProgress {
        private final IDatabaseObjectManagerEx objectManager;

        public ObjectDeleter(IDatabaseObjectManagerEx objectManagerEx)
        {
            this.objectManager = objectManagerEx;
        }

        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
        {
            try {
                objectManager.saveChanges(new DefaultProgressMonitor(monitor));
            } catch (DBException e) {
                throw new InvocationTargetException(e);
            }
        }
    }

}
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
import org.jkiss.dbeaver.model.navigator.DBNTreeNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.EntityManagerDescriptor;
import org.jkiss.dbeaver.runtime.DefaultProgressMonitor;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;

public class NavigatorHandlerObjectDelete extends NavigatorHandlerObjectBase {

    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);

        if (selection instanceof IStructuredSelection) {
            final IStructuredSelection structSelection = (IStructuredSelection)selection;
            Object element = structSelection.getFirstElement();
            if (element instanceof DBNTreeNode) {
                deleteObject(HandlerUtil.getActiveWorkbenchWindow(event), (DBNTreeNode)element);
            }
        }
        return null;
    }

    private boolean deleteObject(IWorkbenchWindow workbenchWindow, DBNTreeNode node)
    {
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

        ObjectDeleter deleter = new ObjectDeleter(objectManagerEx, node, deleteOptions);
        try {
            workbenchWindow.run(true, true, deleter);
        } catch (InvocationTargetException e) {
            log.error("Can't delete object", e);
            return false;
        } catch (InterruptedException e) {
            // do nothing
        }

        return true;
    }

    private static class ObjectDeleter implements IRunnableWithProgress {
        private final IDatabaseObjectManagerEx objectManager;
        private final DBNTreeNode node;
        private final Map<String, Object> deleteOptions;

        public ObjectDeleter(IDatabaseObjectManagerEx objectManagerEx, DBNTreeNode node, Map<String, Object> deleteOptions)
        {
            this.objectManager = objectManagerEx;
            this.node = node;
            this.deleteOptions = deleteOptions;
        }

        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
        {
            try {
                objectManager.deleteObject(node.getObject(), null);

                objectManager.saveChanges(new DefaultProgressMonitor(monitor));
            } catch (DBException e) {
                throw new InvocationTargetException(e);
            }
        }
    }

}
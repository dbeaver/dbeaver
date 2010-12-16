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
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IDatabaseObjectManager;
import org.jkiss.dbeaver.ext.IDatabaseObjectManagerEx;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNTreeFolder;
import org.jkiss.dbeaver.model.navigator.DBNTreeItem;
import org.jkiss.dbeaver.model.navigator.DBNTreeNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.EntityManagerDescriptor;
import org.jkiss.dbeaver.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditorInput;
import org.jkiss.dbeaver.ui.views.navigator.database.DatabaseNavigatorView;

import java.lang.reflect.InvocationTargetException;

public class NavigatorHandlerObjectCreate extends NavigatorHandlerObjectBase {

    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);

        if (selection instanceof IStructuredSelection) {
            final IStructuredSelection structSelection = (IStructuredSelection)selection;
            Object element = structSelection.getFirstElement();
            if (element instanceof DBNNode) {
                createNewObject(HandlerUtil.getActiveWorkbenchWindow(event), (DBNNode) element, null);
            } else {
                log.error("Can't create new object based on selected element '" + element + "'");
            }
        }
        return null;
    }

    protected boolean createNewObject(IWorkbenchWindow workbenchWindow, DBNNode element, DBNTreeNode copyFrom)
    {
        DBNTreeFolder folder = null;
        if (element instanceof DBNTreeFolder) {
            folder = (DBNTreeFolder) element;
        } else if (element instanceof DBNTreeItem) {
            DBNNode parentNode = element.getParentNode();
            if (parentNode instanceof DBNTreeFolder) {
                folder = (DBNTreeFolder) parentNode;
            }
        }
        if (folder == null || !(folder.getValueObject() instanceof DBSObject)) {
            log.error("Can't create child object for folder " + folder);
            return false;
        }
        Class childType = folder.getChildrenType();
        if (childType == null) {
            log.error("Can't determine child element type for folder '" + folder.getNodeName() + "'");
            return false;
        }
        DBSObject sourceObject = copyFrom == null ? null : copyFrom.getObject();
        if (sourceObject != null && sourceObject.getClass() != childType) {
            log.error("Can't create '" + childType.getName() + "' from '" + sourceObject.getClass().getName() + "'");
            return false;
        }
        EntityManagerDescriptor entityManager = DBeaverCore.getInstance().getEditorsRegistry().getEntityManager(childType);
        if (entityManager == null) {
            log.error("Object manager not found for type '" + childType.getName() + "'");
            return false;
        }
        IDatabaseObjectManager<?> objectManager = entityManager.createManager();
        if (!(objectManager instanceof IDatabaseObjectManagerEx<?>)) {
            log.error("Object manager '" + objectManager.getClass().getName() + "' do not supports object creation");
            return false;
        }
        IWorkbenchPart oldActivePart = workbenchWindow.getActivePage().getActivePart();
        try {
            IDatabaseObjectManagerEx objectManagerEx = (IDatabaseObjectManagerEx) objectManager;
            objectManagerEx.createNewObject((DBSObject) folder.getValueObject(), sourceObject);
            ObjectCreator objectCreator = new ObjectCreator(folder, objectManagerEx);
            try {
                workbenchWindow.run(true, true, objectCreator);
            } catch (InvocationTargetException e) {
                log.error("Can't create new object", e);
                return false;
            } catch (InterruptedException e) {
                // do nothing
            }
            DBNTreeItem newChild = objectCreator.getNewChild();
            EntityEditorInput editorInput = new EntityEditorInput(newChild, objectManager);
            try {
                workbenchWindow.getActivePage().openEditor(
                    editorInput,
                    EntityEditor.class.getName());
            } catch (PartInitException e) {
                log.error("Can't open editor for new object", e);
                return false;
            }
        }
        finally {
            if (oldActivePart instanceof DatabaseNavigatorView) {
                workbenchWindow.getActivePage().activate(oldActivePart);
            }
        }

        return true;
    }

    private static class ObjectCreator implements IRunnableWithProgress {
        private final DBNTreeFolder folder;
        private final IDatabaseObjectManagerEx<?> objectManager;
        DBNTreeItem newChild;

        public ObjectCreator(DBNTreeFolder folder, IDatabaseObjectManagerEx<?> objectManager)
        {
            this.folder = folder;
            this.objectManager = objectManager;
        }

        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
        {
            try {
                DBSObject newObject = objectManager.getObject();

                newChild = folder.addChildItem(new DefaultProgressMonitor(monitor), newObject);
            } catch (DBException e) {
                throw new InvocationTargetException(e);
            }
        }

        public DBNTreeItem getNewChild()
        {
            return newChild;
        }
    }
}
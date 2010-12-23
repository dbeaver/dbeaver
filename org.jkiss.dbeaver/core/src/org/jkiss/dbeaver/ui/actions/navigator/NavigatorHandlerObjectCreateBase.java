/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.edit.DBOCreator;
import org.jkiss.dbeaver.model.edit.DBOManager;
import org.jkiss.dbeaver.model.navigator.DBNContainer;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNTreeNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.EntityManagerDescriptor;
import org.jkiss.dbeaver.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditorInput;

import java.lang.reflect.InvocationTargetException;

public abstract class NavigatorHandlerObjectCreateBase extends NavigatorHandlerObjectBase {

    protected boolean createNewObject(IWorkbenchWindow workbenchWindow, DBNNode element, DBNTreeNode copyFrom)
    {
        DBNContainer container = null;
        if (element instanceof DBNContainer) {
            container = (DBNContainer) element;
        } else if (element instanceof DBNNode) {
            DBNNode parentNode = element.getParentNode();
            if (parentNode instanceof DBNContainer) {
                container = (DBNContainer) parentNode;
            }
        }
        if (container == null || !(container.getValueObject() instanceof DBSObject)) {
            log.error("Can't create child object for container " + container);
            return false;
        }
        Class childType = container.getItemsClass();
        if (childType == null) {
            log.error("Can't determine child element type for container '" + container + "'");
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
        DBOManager<?> objectManager = entityManager.createManager();
        if (!(objectManager instanceof DBOCreator<?>)) {
            log.error("Object manager '" + objectManager.getClass().getName() + "' do not supports object creation");
            return false;
        }

        DBOCreator objectCreator = (DBOCreator) objectManager;
        if (!objectCreator.createNewObject(workbenchWindow, (DBSObject) container.getValueObject(), sourceObject)) {
            // Object created by manager itself
            return true;
        }

        // Save object manager's content
        IWorkbenchPart oldActivePart = workbenchWindow.getActivePage().getActivePart();
        try {
            ObjectSaver objectSaver = new ObjectSaver(container, objectCreator);
            try {
                workbenchWindow.run(true, true, objectSaver);
            } catch (InvocationTargetException e) {
                log.error("Can't create new object", e);
                return false;
            } catch (InterruptedException e) {
                // do nothing
            }
            DBNNode newChild = objectSaver.getNewChild();
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
            if (!(oldActivePart instanceof IEditorPart)) {
                workbenchWindow.getActivePage().activate(oldActivePart);
            }
        }

        return true;
    }

    private static class ObjectSaver implements IRunnableWithProgress {
        private final DBNContainer container;
        private final DBOCreator<?> objectManager;
        DBNNode newChild;

        public ObjectSaver(DBNContainer container, DBOCreator<?> objectManager)
        {
            this.container = container;
            this.objectManager = objectManager;
        }

        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
        {
            try {
                DBSObject newObject = objectManager.getObject();

                newChild = container.addChildItem(new DefaultProgressMonitor(monitor), newObject);
            } catch (DBException e) {
                throw new InvocationTargetException(e);
            }
        }

        public DBNNode getNewChild()
        {
            return newChild;
        }
    }
}
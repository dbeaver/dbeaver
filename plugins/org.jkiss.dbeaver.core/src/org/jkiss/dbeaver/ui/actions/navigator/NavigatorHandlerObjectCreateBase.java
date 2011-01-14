/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.edit.DBOCreator;
import org.jkiss.dbeaver.model.edit.DBOEditor;
import org.jkiss.dbeaver.model.edit.DBOManager;
import org.jkiss.dbeaver.model.navigator.DBNContainer;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.EntityManagerDescriptor;
import org.jkiss.dbeaver.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditorInput;
import org.jkiss.dbeaver.ui.views.navigator.database.DatabaseNavigatorView;
import org.jkiss.dbeaver.utils.ViewUtils;

import java.lang.reflect.InvocationTargetException;

public abstract class NavigatorHandlerObjectCreateBase extends NavigatorHandlerObjectBase {

    protected boolean createNewObject(final IWorkbenchWindow workbenchWindow, DBNNode element, DBNDatabaseNode copyFrom)
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
        if (container == null) {
            log.error("Can't create child object - no container found");
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
        DBOCreator.CreateResult result = objectCreator.createNewObject(workbenchWindow, container.getValueObject(), sourceObject);
        if (result == DBOCreator.CreateResult.CANCEL) {
            return false;
        }

        // Save object manager's content
        IWorkbenchPart oldActivePart = workbenchWindow.getActivePage().getActivePart();
        try {
            ObjectSaver objectSaver = new ObjectSaver(container, objectCreator, result == DBOCreator.CreateResult.SAVE);
            try {
                workbenchWindow.run(true, true, objectSaver);
            } catch (InvocationTargetException e) {
                UIUtils.showErrorDialog(workbenchWindow.getShell(), "Can't create new object", null, e.getTargetException());
                return false;
            } catch (InterruptedException e) {
                // do nothing
            }
            final DBNNode newChild = objectSaver.getNewChild();
            if (newChild != null) {
                Display.getDefault().asyncExec(new Runnable() {
                    public void run()
                    {
                        DatabaseNavigatorView view = ViewUtils.findView(workbenchWindow, DatabaseNavigatorView.class);
                        if (view != null) {
                            view.showNode(newChild);
                        }
                    }
                });
                if (result == DBOCreator.CreateResult.OPEN_EDITOR) {
                    // Can open editor only for database nodes
                    if (newChild instanceof DBNDatabaseNode) {
                        EntityEditorInput editorInput = new EntityEditorInput((DBNDatabaseNode)newChild, objectManager);
                        try {
                            workbenchWindow.getActivePage().openEditor(
                                editorInput,
                                EntityEditor.class.getName());
                        } catch (PartInitException e) {
                            UIUtils.showErrorDialog(workbenchWindow.getShell(), "Can't open editor for new object", null, e);
                            return false;
                        }
                    }
                }
            } else {
                UIUtils.showErrorDialog(workbenchWindow.getShell(), "Navigator node not found", "Can't find node corresponding to new object");
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
        private final boolean saveObject;
        DBNNode newChild;

        public ObjectSaver(DBNContainer container, DBOCreator<?> objectManager, boolean saveObject)
        {
            this.container = container;
            this.objectManager = objectManager;
            this.saveObject = saveObject;
        }

        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
        {
            try {
                DefaultProgressMonitor progressMonitor = new DefaultProgressMonitor(monitor);
                if (saveObject && objectManager instanceof DBOEditor) {
                    ((DBOEditor)objectManager).saveChanges(progressMonitor);
                }

                DBSObject newObject = objectManager.getObject();

                newChild = container.addChildItem(progressMonitor, newObject);
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
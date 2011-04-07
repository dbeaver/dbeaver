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
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.edit.DBEObjectManager;
import org.jkiss.dbeaver.model.impl.edit.DBECommandContextImpl;
import org.jkiss.dbeaver.model.navigator.DBNContainer;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;
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
        Class<?> childType = container.getItemsClass();
        if (childType == null) {
            log.error("Can't determine child element type for container '" + container + "'");
            return false;
        }

        DBSObject sourceObject = copyFrom == null ? null : copyFrom.getObject();
        if (sourceObject != null && sourceObject.getClass() != childType) {
            log.error("Can't create '" + childType.getName() + "' from '" + sourceObject.getClass().getName() + "'");
            return false;
        }
        DBEObjectManager<?> objectManager = DBeaverCore.getInstance().getEditorsRegistry().getObjectManager(childType);
        if (objectManager == null) {
            log.error("Object manager not found for type '" + childType.getName() + "'");
            return false;
        }
        DBEObjectMaker objectMaker = (DBEObjectMaker) objectManager;
        DBECommandContext commandContext = null;
        if (container instanceof DBNDatabaseNode) {
            DBSDataSourceContainer dsContainer = ((DBNDatabaseNode) container).getObject().getDataSource().getContainer();
            commandContext = new DBECommandContextImpl(dsContainer);
        }

        DBSObject result = objectMaker.createNewObject(workbenchWindow, commandContext, container.getValueObject(), sourceObject);
        if (result == null) {
            return true;
        }
        if (commandContext == null) {
            log.error("Non-database container '" + container + "' must save new objects itself - command context is not accessible");
            return false;
        }

        // Save object manager's content
        IWorkbenchPart oldActivePart = workbenchWindow.getActivePage().getActivePart();
        try {
            ObjectSaver objectSaver = new ObjectSaver(
                container,
                commandContext,
                result,
                (objectMaker.getMakerOptions() & DBEObjectMaker.FEATURE_SAVE_IMMEDIATELY) != 0);
            try {
                workbenchWindow.run(true, true, objectSaver);
            } catch (InvocationTargetException e) {
                UIUtils.showErrorDialog(workbenchWindow.getShell(), "New object", "Can't create new object", e.getTargetException());
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
                if ((objectMaker.getMakerOptions() & DBEObjectMaker.FEATURE_EDITOR_ON_CREATE) != 0) {
                    // Can open editor only for database nodes
                    if (newChild instanceof DBNDatabaseNode) {
                        EntityEditorInput editorInput = new EntityEditorInput((DBNDatabaseNode)newChild);
                        try {
                            workbenchWindow.getActivePage().openEditor(
                                editorInput,
                                EntityEditor.class.getName());
                        } catch (PartInitException e) {
                            UIUtils.showErrorDialog(workbenchWindow.getShell(), "Open editor", "Can't open editor for new object", e);
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
                //workbenchWindow.getActivePage().activate(oldActivePart);
            }
        }

        return true;
    }

    private static class ObjectSaver implements IRunnableWithProgress {
        private final DBNContainer container;
        private final DBECommandContext commandContext;
        private final DBSObject newObject;
        private final boolean saveObject;
        DBNNode newChild;

        public ObjectSaver(DBNContainer container, DBECommandContext commandContext, DBSObject newObject, boolean saveObject)
        {
            this.container = container;
            this.commandContext = commandContext;
            this.newObject = newObject;
            this.saveObject = saveObject;
        }

        public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
        {
            try {
                DefaultProgressMonitor progressMonitor = new DefaultProgressMonitor(monitor);
                if (saveObject) {
                    commandContext.saveChanges(progressMonitor);
                }
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
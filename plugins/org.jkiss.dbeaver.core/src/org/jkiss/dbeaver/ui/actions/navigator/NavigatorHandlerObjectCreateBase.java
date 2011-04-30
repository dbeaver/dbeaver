/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditor;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.edit.DBEObjectManager;
import org.jkiss.dbeaver.model.navigator.DBNContainer;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.EntityEditorsRegistry;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditorInput;
import org.jkiss.dbeaver.ui.views.navigator.database.DatabaseNavigatorView;
import org.jkiss.dbeaver.utils.ViewUtils;

import java.lang.reflect.InvocationTargetException;

public abstract class NavigatorHandlerObjectCreateBase extends NavigatorHandlerObjectBase {

    protected boolean createNewObject(final IWorkbenchWindow workbenchWindow, DBNNode element, DBNDatabaseNode copyFrom)
    {
        try {
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
                throw new DBException("Can't determine child element type for container '" + container + "'");
            }

            DBSObject sourceObject = copyFrom == null ? null : copyFrom.getObject();
            if (sourceObject != null && sourceObject.getClass() != childType) {
                throw new DBException("Can't create '" + childType.getName() + "' from '" + sourceObject.getClass().getName() + "'");
            }

            final EntityEditorsRegistry editorsRegistry = DBeaverCore.getInstance().getEditorsRegistry();
            DBEObjectManager<?> objectManager = editorsRegistry.getObjectManager(childType);
            if (objectManager == null) {
                throw new DBException("Object manager not found for type '" + childType.getName() + "'");
            }
            DBEObjectMaker objectMaker = (DBEObjectMaker) objectManager;
            final boolean openEditor = (objectMaker.getMakerOptions() & DBEObjectMaker.FEATURE_EDITOR_ON_CREATE) != 0;
            CommandTarget commandTarget = getCommandTarget(
                workbenchWindow,
                container,
                childType,
                openEditor);

            final Object parentObject = container.getValueObject();
            DBSObject result = objectMaker.createNewObject(workbenchWindow, commandTarget.getContext(), parentObject, sourceObject);
            if (result == null) {
                return true;
            }
            if (commandTarget == null) {
                throw new DBException("Non-database container '" + container + "' must save new objects itself - command context is not accessible");
            }

            // Save object manager's content
            ObjectSaver objectSaver = new ObjectSaver(
                container,
                commandTarget.getContext(),
                result,
                (objectMaker.getMakerOptions() & DBEObjectMaker.FEATURE_SAVE_IMMEDIATELY) != 0);
            if (!objectSaver.isLazy()) {
                objectSaver.run(VoidProgressMonitor.INSTANCE);
            } else {
                DBeaverCore.getInstance().runInProgressService(objectSaver);
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
                IDatabaseNodeEditor editor = commandTarget.getEditor();
                if (editor != null) {
                    // Just activate existing editor
                    workbenchWindow.getActivePage().activate(editor);
                } else if (openEditor && newChild instanceof DBNDatabaseNode) {
                    // Open new one with existing context
                    EntityEditorInput editorInput = new EntityEditorInput(
                        (DBNDatabaseNode) newChild,
                        commandTarget.getContext());
                    workbenchWindow.getActivePage().openEditor(
                        editorInput,
                        EntityEditor.class.getName());
                }
            } else {
                throw new DBException("Can't find node corresponding to new object");
            }
        } catch (InterruptedException e) {
            // do nothing
        }
        catch (Throwable e) {
            if (e instanceof InvocationTargetException) {
                e = ((InvocationTargetException)e).getTargetException();
            }
            UIUtils.showErrorDialog(workbenchWindow.getShell(), "Create object", null, e);
            return false;
        }

        return true;
    }

    private static class ObjectSaver implements DBRRunnableWithProgress {
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

        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
        {
            try {
                if (saveObject) {
                    commandContext.saveChanges(monitor);
                }
                newChild = container.addChildItem(monitor, newObject);
            } catch (DBException e) {
                throw new InvocationTargetException(e);
            }
        }

        public DBNNode getNewChild()
        {
            return newChild;
        }

        boolean isLazy()
        {
            if (!saveObject && container instanceof DBNDatabaseNode && !((DBNDatabaseNode) container).isLazyNode()) {
                return false;
            }
            return true;
        }
    }

}
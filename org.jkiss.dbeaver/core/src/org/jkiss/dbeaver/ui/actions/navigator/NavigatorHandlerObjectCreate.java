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
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IDatabaseObjectManager;
import org.jkiss.dbeaver.ext.IDatabaseObjectManagerEx;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNTreeFolder;
import org.jkiss.dbeaver.model.navigator.DBNTreeItem;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.EntityManagerDescriptor;
import org.jkiss.dbeaver.runtime.DefaultProgressMonitor;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditorInput;

import java.lang.reflect.InvocationTargetException;

public class NavigatorHandlerObjectCreate extends NavigatorHandlerObjectBase {

    public Object execute(ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);

        if (selection instanceof IStructuredSelection) {
            final IStructuredSelection structSelection = (IStructuredSelection)selection;
            Object element = structSelection.getFirstElement();
            DBNTreeFolder folder = null;
            if (element instanceof DBNTreeFolder) {
                folder = (DBNTreeFolder) element;
            } else if (element instanceof DBNTreeItem) {
                DBNNode parentNode = ((DBNTreeItem) element).getParentNode();
                if (parentNode instanceof DBNTreeFolder) {
                    folder = (DBNTreeFolder) parentNode;
                }
            }
            if (folder == null || !(folder.getValueObject() instanceof DBSObject)) {
                log.error("Can't create child object for folder " + folder);
                return null;
            }
            Class childType = folder.getChildrenType();
            if (childType != null) {
                EntityManagerDescriptor entityManager = DBeaverCore.getInstance().getEditorsRegistry().getEntityManager(childType);
                if (entityManager != null) {
                    IDatabaseObjectManager<?> objectManager = entityManager.createManager();
                    if (objectManager instanceof IDatabaseObjectManagerEx<?>) {
                        IWorkbenchWindow workbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
                        ObjectCreator objectCreator = new ObjectCreator(folder, (IDatabaseObjectManagerEx<?>) objectManager);
                        try {
                            workbenchWindow.run(true, true, objectCreator);
                        } catch (InvocationTargetException e) {
                            log.error("Can't create new object", e);
                        } catch (InterruptedException e) {
                            // do nothing
                        }
                        DBNTreeItem newChild = objectCreator.getNewChild();
                        if (newChild != null) {
                            EntityEditorInput editorInput = new EntityEditorInput(newChild, objectManager);
                            try {
                                workbenchWindow.getActivePage().openEditor(
                                    editorInput,
                                    EntityEditor.class.getName());
                            } catch (PartInitException e) {
                                log.error("Can't open editor for new object", e);
                            }
                        }
                    }
                }
            }
        }
        return null;
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
                objectManager.createNewObject((DBSObject) folder.getValueObject(), null);
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
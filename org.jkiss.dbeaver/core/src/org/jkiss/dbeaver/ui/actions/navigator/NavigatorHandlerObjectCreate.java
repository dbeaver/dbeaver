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
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IDatabaseObjectManager;
import org.jkiss.dbeaver.ext.IDatabaseObjectManagerEx;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.navigator.DBNTreeFolder;
import org.jkiss.dbeaver.model.navigator.DBNTreeItem;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.EntityManagerDescriptor;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditorInput;

import java.util.Iterator;

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
            if (folder != null && folder.getValueObject() instanceof DBSObject) {
                Class childType = folder.getChildrenType();
                if (childType != null) {
                    EntityManagerDescriptor entityManager = DBeaverCore.getInstance().getEditorsRegistry().getEntityManager(childType);
                    if (entityManager != null) {
                        IDatabaseObjectManager<?> objectManager = entityManager.createManager();
                        if (objectManager instanceof IDatabaseObjectManagerEx<?>) {
                            IDatabaseObjectManagerEx<?> exManager = (IDatabaseObjectManagerEx<?>)objectManager;
                            exManager.createNewObject((DBSObject) folder.getValueObject(), null);
                            DBSObject newObject = exManager.getObject();
                            EntityEditorInput editorInput = null;//new EntityEditorInput(folder, objectManager);
                            try {
                                HandlerUtil.getActiveWorkbenchWindow(event).getActivePage().openEditor(
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
}
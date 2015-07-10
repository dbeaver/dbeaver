/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditor;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.edit.DBEObjectManager;
import org.jkiss.dbeaver.model.navigator.DBNContainer;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.registry.editor.EntityEditorsRegistry;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditor;
import org.jkiss.dbeaver.ui.editors.entity.EntityEditorInput;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorView;

import java.lang.reflect.InvocationTargetException;

public abstract class NavigatorHandlerObjectCreateBase extends NavigatorHandlerObjectBase {

    protected boolean createNewObject(final IWorkbenchWindow workbenchWindow, DBNNode element, DBNDatabaseNode copyFrom)
    {
        try {
            DBNContainer container = null;
            if (element instanceof DBNContainer) {
                container = (DBNContainer) element;
            } else {
                DBNNode parentNode = element.getParentNode();
                if (parentNode instanceof DBNContainer) {
                    container = (DBNContainer) parentNode;
                }
            }
            if (container == null) {
                throw new DBException("Can't detect container for '" + element.getNodeName() + "'");
            }
            Class<?> childType = container.getChildrenClass();
            if (childType == null) {
                throw new DBException("Can't determine child element type for container '" + container + "'");
            }

            DBSObject sourceObject = copyFrom == null ? null : copyFrom.getObject();
            if (sourceObject != null && !childType.isAssignableFrom(sourceObject.getClass())) {
                throw new DBException("Can't create '" + childType.getName() + "' from '" + sourceObject.getClass().getName() + "'");
            }

            final EntityEditorsRegistry editorsRegistry = EntityEditorsRegistry.getInstance();
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
            DBSObject result = objectMaker.createNewObject(commandTarget.getContext(), parentObject, sourceObject);
            if (result == null) {
                return true;
            }

            if ((objectMaker.getMakerOptions() & DBEObjectMaker.FEATURE_SAVE_IMMEDIATELY) != 0) {
                // Save object manager's content
                ObjectSaver objectSaver = new ObjectSaver(commandTarget.getContext());
                DBeaverUI.runInProgressService(objectSaver);
            }

            final DBNNode newChild = DBeaverCore.getInstance().getNavigatorModel().findNode(result);
            if (newChild != null) {
                Display.getDefault().asyncExec(new Runnable() {
                    @Override
                    public void run()
                    {
                        DatabaseNavigatorView view = UIUtils.findView(workbenchWindow, DatabaseNavigatorView.class);
                        if (view != null) {
                            view.showNode(newChild);
                        }
                    }
                });
                IDatabaseEditor editor = commandTarget.getEditor();
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

}
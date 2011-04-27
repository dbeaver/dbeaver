/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditor;
import org.jkiss.dbeaver.ext.IDatabaseNodeEditorInput;
import org.jkiss.dbeaver.model.DBPPersistedObject;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.impl.edit.DBECommandContextImpl;
import org.jkiss.dbeaver.model.navigator.DBNContainer;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.lang.reflect.InvocationTargetException;

public abstract class NavigatorHandlerObjectBase extends AbstractHandler {

    static final Log log = LogFactory.getLog(NavigatorHandlerObjectBase.class);

    protected DBECommandContext getCommandContext(IWorkbenchWindow workbenchWindow, DBNContainer container, Object parentObject) throws DBException
    {
        if (parentObject instanceof DBPPersistedObject && !((DBPPersistedObject)parentObject).isPersisted()) {
            // Parent is not yet persisted
            // It seems that child object is created within some editor
            // Obtain command context from it
            for (final IEditorReference editorRef : workbenchWindow.getActivePage().getEditorReferences()) {
                final IEditorPart editor = editorRef.getEditor(false);
                if (editor instanceof IDatabaseNodeEditor) {
                    final IDatabaseNodeEditorInput editorInput = ((IDatabaseNodeEditor) editor).getEditorInput();
                    if (editorInput.getDatabaseObject() == parentObject) {
                        return editorInput.getCommandContext();
                    }
                }
            }

            throw new DBException("Can't find host editor for object " + parentObject);
        } else if (container instanceof DBNDatabaseNode) {
            DBSDataSourceContainer dsContainer = ((DBNDatabaseNode) container).getObject().getDataSource().getContainer();
            return new DBECommandContextImpl(dsContainer);
        } else {
            // No command context supported for this object
            return null;
        }
    }

    public static DBNDatabaseNode getNodeByObject(DBSObject object)
    {
        DBNModel model = DBeaverCore.getInstance().getNavigatorModel();
        DBNDatabaseNode node = model.findNode(object);
        if (node == null) {
            NodeLoader nodeLoader = new NodeLoader(model, object);
            try {
                DBeaverCore.getInstance().runInProgressService(nodeLoader);
            } catch (InvocationTargetException e) {
                log.warn("Could not load node for object '" + object.getName() + "'", e.getTargetException());
            } catch (InterruptedException e) {
                // do nothing
            }
            node = nodeLoader.node;
        }
        return node;
    }

    private static class NodeLoader implements DBRRunnableWithProgress {
        private final DBNModel model;
        private final DBSObject object;
        private DBNDatabaseNode node;

        public NodeLoader(DBNModel model, DBSObject object)
        {
            this.model = model;
            this.object = object;
        }

        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
        {
            node = model.getNodeByObject(monitor, object, true);
        }
    }
}
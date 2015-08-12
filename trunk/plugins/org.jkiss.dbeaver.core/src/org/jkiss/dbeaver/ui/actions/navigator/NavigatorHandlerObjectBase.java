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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEStructEditor;
import org.jkiss.dbeaver.model.navigator.DBNContainer;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseFolder;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNModel;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.registry.editor.EntityEditorsRegistry;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.SimpleCommandContext;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.folders.IFolderContainer;
import org.jkiss.dbeaver.ui.dialogs.sql.ViewSQLDialog;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditor;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.ui.navigator.database.DatabaseNavigatorView;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class NavigatorHandlerObjectBase extends AbstractHandler {

    static final Log log = Log.getLog(NavigatorHandlerObjectBase.class);

    static boolean updateUI = true;

    protected static class CommandTarget {
        private DBECommandContext context;
        private IDatabaseEditor editor;
        private CommandTarget()
        {
        }
        private CommandTarget(DBECommandContext context)
        {
            this.context = context;
        }
        public CommandTarget(IDatabaseEditor editor)
        {
            this.editor = editor;
            this.context = editor.getEditorInput().getCommandContext();
        }

        public DBECommandContext getContext()
        {
            return context;
        }
        public IDatabaseEditor getEditor()
        {
            return editor;
        }
    }

    protected static CommandTarget getCommandTarget(
        IWorkbenchWindow workbenchWindow,
        DBNContainer container,
        Class<?> childType,
        boolean openEditor)
        throws DBException
    {
        final Object parentObject = container.getValueObject();

        DBSObject objectToSeek = null;
        if (parentObject instanceof DBSObject) {
            final DBEStructEditor parentStructEditor = EntityEditorsRegistry.getInstance().getObjectManager(parentObject.getClass(), DBEStructEditor.class);
            if (parentStructEditor != null && RuntimeUtils.isTypeSupported(childType, parentStructEditor.getChildTypes())) {
                objectToSeek = (DBSObject) parentObject;
            }
        }
        if (objectToSeek != null) {
            for (final IEditorReference editorRef : workbenchWindow.getActivePage().getEditorReferences()) {
                final IEditorPart editor = editorRef.getEditor(false);
                if (editor instanceof IDatabaseEditor) {
                    final IDatabaseEditorInput editorInput = ((IDatabaseEditor) editor).getEditorInput();
                    if (editorInput.getDatabaseObject() == objectToSeek) {
                        workbenchWindow.getActivePage().activate(editor);
                        switchEditorFolder(container, editor);
                        return new CommandTarget((IDatabaseEditor) editor);
                    }
                }
            }

            if (openEditor && container instanceof DBNDatabaseNode) {
                final IDatabaseEditor editor = (IDatabaseEditor) NavigatorHandlerObjectOpen.openEntityEditor(
                    (DBNDatabaseNode) container,
                    null,
                    workbenchWindow);
                if (editor != null) {
                    switchEditorFolder(container, editor);
                    return new CommandTarget(editor);
                }
            }
        }
        if (container instanceof DBNDatabaseNode) {
            // No editor found - create new command context
            DBPDataSource dataSource = ((DBNDatabaseNode) container).getObject().getDataSource();
            if (dataSource != null) {
                return new CommandTarget(new SimpleCommandContext(dataSource.getDefaultContext(true), !openEditor));
            }
        }
        return new CommandTarget();
    }

    private static void switchEditorFolder(DBNContainer container, IEditorPart editor)
    {
        if (editor instanceof IFolderContainer && container instanceof DBNDatabaseFolder) {
            ((IFolderContainer) editor).switchFolder(container.getChildrenType());
        }
    }

    public static DBNDatabaseNode getNodeByObject(DBSObject object)
    {
        DBNModel model = DBeaverCore.getInstance().getNavigatorModel();
        DBNDatabaseNode node = model.findNode(object);
        if (node == null) {
            NodeLoader nodeLoader = new NodeLoader(model, Collections.singleton(object));
            try {
                DBeaverUI.runInProgressService(nodeLoader);
            } catch (InvocationTargetException e) {
                log.warn("Can't load node for object '" + object.getName() + "'", e.getTargetException());
            } catch (InterruptedException e) {
                // do nothing
            }
            if (!nodeLoader.nodes.isEmpty()) {
                node = nodeLoader.nodes.get(0);
            }
        }
        return node;
    }

    public static Collection<DBNDatabaseNode> getNodesByObjects(Collection<Object> objects)
    {
        DBNModel model = DBeaverCore.getInstance().getNavigatorModel();
        List<DBNDatabaseNode> result = new ArrayList<DBNDatabaseNode>();
        List<Object> missingObjects = new ArrayList<Object>();
        for (Object object : objects) {
            if (object instanceof DBSObject) {
                DBNDatabaseNode node = model.findNode((DBSObject) object);
                if (node != null) {
                    result.add(node);
                    continue;
                }
            }
            missingObjects.add(object);
        }
        if (!missingObjects.isEmpty()) {
            NodeLoader nodeLoader = new NodeLoader(model, missingObjects);
            try {
                DBeaverUI.runInProgressService(nodeLoader);
            } catch (InvocationTargetException e) {
                log.warn("Can't load node for objects " + missingObjects.size(), e.getTargetException());
            } catch (InterruptedException e) {
                // do nothing
            }
            result.addAll(nodeLoader.nodes);
        }
        return result;
    }

    protected static boolean showScript(IWorkbenchWindow workbenchWindow, DBECommandContext commandContext, String dialogTitle)
    {
        Collection<? extends DBECommand> commands = commandContext.getFinalCommands();
        StringBuilder script = new StringBuilder();
        for (DBECommand command : commands) {
            script.append(DBUtils.generateScript(command.getPersistActions(), false));
        }
        DatabaseNavigatorView view = UIUtils.findView(workbenchWindow, DatabaseNavigatorView.class);
        if (view != null) {
            ViewSQLDialog dialog = new ViewSQLDialog(
                view.getSite(),
                commandContext.getExecutionContext(),
                dialogTitle,
                DBeaverIcons.getImage(UIIcon.SQL_PREVIEW),
                    script.toString());
            dialog.setShowSaveButton(true);
            return dialog.open() == IDialogConstants.PROCEED_ID;
        } else {
            return false;
        }
    }

    private static class NodeLoader implements DBRRunnableWithProgress {
        private final DBNModel model;
        private final Collection<? extends Object> objects;
        private List<DBNDatabaseNode> nodes;

        public NodeLoader(DBNModel model, Collection<? extends Object> objects)
        {
            this.model = model;
            this.objects = objects;
            this.nodes = new ArrayList<DBNDatabaseNode>(objects.size());
        }

        @Override
        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
        {
            for (Object object : objects) {
                DBSObject structObject;
                if (object instanceof DBSObject) {
                    structObject = (DBSObject) object;
                } else if (object instanceof DBSObjectReference) {
                    try {
                        structObject = ((DBSObjectReference) object).resolveObject(monitor);
                    } catch (DBException e) {
                        log.error("Can't resolve object reference", e);
                        continue;
                    }
                } else {
                    log.warn("Unsupported object type: " + object);
                    continue;
                }
                DBNDatabaseNode node = model.getNodeByObject(monitor, structObject, true);
                if (node != null) {
                    nodes.add(node);
                }
            }
        }
    }

    protected static class ObjectSaver implements DBRRunnableWithProgress {
        private final DBECommandContext commander;

        public ObjectSaver(DBECommandContext commandContext)
        {
            this.commander = commandContext;
        }

        @Override
        public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
        {
            try {
                commander.saveChanges(monitor);
            } catch (DBException e) {
                throw new InvocationTargetException(e);
            }
        }
    }

}
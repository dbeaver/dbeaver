package org.jkiss.dbeaver.ui.navigator.actions;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.TasksJob;
import org.jkiss.dbeaver.runtime.ui.UIServiceSQL;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditor;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

class NavigatorObjectsDeleter {
    private static final Log log = Log.getLog(NavigatorObjectsDeleter.class);

    /**
     * A map with delete options.
     *
     * <p>Only contains cascade option
     */
    private static final Map<String, Object> OPTIONS_CASCADE = Collections.singletonMap(DBEObjectMaker.OPTION_DELETE_CASCADE, Boolean.TRUE);

    private final List<DBRRunnableWithProgress> tasksToExecute = new ArrayList<>();

    /**
     * Active window.
     */
    private final IWorkbenchWindow window;

    /**
     * A list containing objects to delete.
     */
    private final List<Object> selection;

    /**
     * {@code true} if 'Cascade delete' button should be shown
     */
    private final boolean showCascade;

    /**
     * {@code true} if 'View Script' button should be shown
     */
    private final boolean showViewScript;

    /**
     * {@code true} in case of attempt to delete database nodes which belong to different data sources
     */
    private final boolean hasNodesFromDifferentDataSources;

    @Nullable
    private DBECommandContext commandContext;

    private boolean checkCascade = false;

    private NavigatorObjectsDeleter(final List<Object> selection, final IWorkbenchWindow window,
                                    final boolean hasNodesFromDifferentDataSources, final boolean showCascade,
                                    final boolean showViewScript) {
        this.selection = selection;
        this.window = window;
        this.hasNodesFromDifferentDataSources = hasNodesFromDifferentDataSources;
        this.showCascade = showCascade;
        this.showViewScript = showViewScript;
    }

    static NavigatorObjectsDeleter of(final List<Object> selection, final IWorkbenchWindow window) {
        DBPDataSource dataSource = null;
        boolean hasNodesFromDifferentDataSources = false;
        boolean showCascade = false;
        boolean showViewScript = false;
        for (Object obj: selection) {
            if (!(obj instanceof DBNDatabaseNode)) {
                continue;
            }
            final DBNDatabaseNode node = (DBNDatabaseNode) obj;
            final DBPDataSource currentDatasource = node.getDataSource();
            if (dataSource == null) {
                dataSource = currentDatasource;
            } else if (!dataSource.equals(currentDatasource)) {
                hasNodesFromDifferentDataSources = true;
            }
            if (!(node.getParentNode() instanceof DBNContainer)) {
                continue;
            }
            final DBSObject object = node.getObject();
            if (object == null) {
                continue;
            }
            @SuppressWarnings("rawtypes") final DBEObjectMaker objectMaker =
                    DBWorkbench.getPlatform().getEditorsRegistry().getObjectManager(object.getClass(), DBEObjectMaker.class);
            if (objectMaker == null) {
                continue;
            }
            showCascade |= (objectMaker.getMakerOptions(object.getDataSource()) & DBEObjectMaker.FEATURE_DELETE_CASCADE) != 0;
            final NavigatorHandlerObjectBase.CommandTarget commandTarget;
            try {
                commandTarget = NavigatorHandlerObjectBase.getCommandTarget(
                        window,
                        node.getParentNode(),
                        object.getClass(),
                        false
                );
            } catch (DBException e) {
                continue;
            }
            if (object.isPersisted() && commandTarget.getEditor() == null && commandTarget.getContext() != null) {
                showViewScript = true;
            }
        }
        return new NavigatorObjectsDeleter(selection, window, hasNodesFromDifferentDataSources, showCascade, showViewScript);
    }

    boolean hasNodesFromDifferentDataSources() {
        return hasNodesFromDifferentDataSources;
    }

    public boolean getShowCascade() {
        return showCascade;
    }

    public boolean getShowViewScript() {
        return showViewScript;
    }

    void delete() {
        for (Object obj: selection) {
            if (obj instanceof DBNDatabaseNode) {
                deleteDatabaseNode((DBNDatabaseNode)obj);
            } else if (obj instanceof DBNResource) {
                deleteResource((DBNResource)obj);
            } else if (obj instanceof DBNLocalFolder) {
                deleteLocalFolder((DBNLocalFolder) obj);
            } else {
                log.warn("Don't know how to delete element '" + obj + "'"); //$NON-NLS-1$ //$NON-NLS-2$
            }
        }
        if (!tasksToExecute.isEmpty()) {
            TasksJob.runTasks(tasksToExecute.size() > 1 ? "Delete " + tasksToExecute.size() + " objects" : "Delete object", tasksToExecute);
        }
    }

    private void deleteLocalFolder(final DBNLocalFolder folder) {
        folder.getDataSourceRegistry().removeFolder(folder.getFolder(), false);
        DBNModel.updateConfigAndRefreshDatabases(folder);
    }

    private void deleteResource(final DBNResource resource) {
        final IResource iResource = resource.getResource();
        try {
            if (iResource instanceof IFolder) {
                ((IFolder)iResource).delete(true, true, new NullProgressMonitor());
            } else if (iResource instanceof IProject) {
                // Delete project (with all contents)
                ((IProject) iResource).delete(true, true, new NullProgressMonitor());
            } else if (iResource != null) {
                iResource.delete(IResource.FORCE | IResource.KEEP_HISTORY, new NullProgressMonitor());
            }
        } catch (CoreException e) {
            DBWorkbench.getPlatformUI().showError(
                    UINavigatorMessages.error_deleting_resource_title,
                    NLS.bind(UINavigatorMessages.error_deleting_resource_message, iResource.getFullPath().toString()),
                    e
            );
        }
    }

    private void deleteDatabaseNode(final DBNDatabaseNode node) {
        try {
            if (!(node.getParentNode() instanceof DBNContainer)) {
                throw new DBException("Node '" + node + "' doesn't have a container");
            }
            final DBSObject object = node.getObject();
            if (object == null) {
                throw new DBException("Can't delete node with null object");
            }
            @SuppressWarnings("rawtypes") final DBEObjectMaker objectMaker =
                    DBWorkbench.getPlatform().getEditorsRegistry().getObjectManager(object.getClass(), DBEObjectMaker.class);
            if (objectMaker == null) {
                throw new DBException("Object maker not found for type '" + object.getClass().getName() + "'"); //$NON-NLS-2$
            }
            final boolean supportsCascade = (objectMaker.getMakerOptions(object.getDataSource()) & DBEObjectMaker.FEATURE_DELETE_CASCADE) != 0;
            final NavigatorHandlerObjectBase.CommandTarget commandTarget =
                    NavigatorHandlerObjectBase.getCommandTarget(
                    window,
                    node.getParentNode(),
                    object.getClass(),
                    false
            );
            if (!object.isPersisted() || commandTarget.getEditor() != null) {
                // Not a real object delete because it's not persisted
                // There should be command context somewhere
                if (deleteNewObject(node)) {
                    return;
                }
                // No direct editor host found for this object -
                // try to find corresponding command context
                // and execute command within it
            }
            @SuppressWarnings("unchecked") Map<String, Object> deleteOptions = Collections.EMPTY_MAP;
            if (checkCascade && supportsCascade) {
                deleteOptions = OPTIONS_CASCADE;
            }
            objectMaker.deleteObject(commandTarget.getContext(), node.getObject(), deleteOptions);
            if (commandTarget.getEditor() == null && commandTarget.getContext() != null) {
                // Persist object deletion - only if there is no host editor and we have a command context
                final NavigatorHandlerObjectBase.ObjectSaver deleter = new NavigatorHandlerObjectBase.ObjectSaver(commandTarget.getContext(), deleteOptions);
                tasksToExecute.add(deleter);
            }
        } catch (Throwable e) {
            DBWorkbench.getPlatformUI().showError(
                    UINavigatorMessages.actions_navigator_error_dialog_delete_object_title,
                    NLS.bind(UINavigatorMessages.actions_navigator_error_dialog_delete_object_message, node.getNodeName()),
                    e
            );
        }
    }

    private boolean deleteNewObject(final DBNDatabaseNode node) {
        for (final IEditorReference editorRef : window.getActivePage().getEditorReferences()) {
            final IEditorPart editor = editorRef.getEditor(false);
            if (editor instanceof IDatabaseEditor) {
                final IEditorInput editorInput = editor.getEditorInput();
                if (editorInput instanceof IDatabaseEditorInput && ((IDatabaseEditorInput) editorInput).getDatabaseObject() == node.getObject()) {
                    window.getActivePage().closeEditor(editor, false);
                    return true;
                }
            }
        }
        return false;
    }

    boolean showScriptWindow() {
        final String sql = collectSQL();
        boolean result = false;
        if (sql.length() > 0) {
            UIServiceSQL serviceSQL = DBWorkbench.getService(UIServiceSQL.class);
            if (serviceSQL != null) {
                result = serviceSQL.openSQLViewer(
                        //command context is not null because sql is not empty, see appendSQL()
                        commandContext.getExecutionContext(),
                        UINavigatorMessages.actions_navigator_delete_script,
                        UIIcon.SQL_PREVIEW,
                        sql,
                        true,
                        false
                ) == IDialogConstants.PROCEED_ID;
            }
        } else {
            result = UIUtils.confirmAction(
                    window.getShell(),
                    UINavigatorMessages.actions_navigator_delete_script,
                    UINavigatorMessages.question_no_sql_available
            );
        }
        commandContext.resetChanges(!result);
        return result;
    }

    private String collectSQL() {
        final StringBuilder sql = new StringBuilder();
        try {
            UIUtils.runInProgressService(monitor -> {
                for (Object obj: selection) {
                    if (obj instanceof DBNDatabaseNode) {
                        appendScript(monitor, sql, (DBNDatabaseNode) obj);
                    }
                }
            });
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError(
                    UINavigatorMessages.error_sql_generation_title,
                    UINavigatorMessages.error_sql_generation_message,
                    e.getTargetException()
            );
        } catch (InterruptedException ignored) {
        }
        return sql.toString();
    }

    private void appendScript(final DBRProgressMonitor monitor, final StringBuilder sql, final DBNDatabaseNode node) throws InvocationTargetException {
        if (!(node.getParentNode() instanceof DBNContainer)) {
            return;
        }
        final DBSObject object = node.getObject();
        if (object == null) {
            return;
        }
        @SuppressWarnings("rawtypes") final DBEObjectMaker objectMaker =
                DBWorkbench.getPlatform().getEditorsRegistry().getObjectManager(object.getClass(), DBEObjectMaker.class);
        if (objectMaker == null) {
            return;
        }
        final boolean supportsCascade = (objectMaker.getMakerOptions(object.getDataSource()) & DBEObjectMaker.FEATURE_DELETE_CASCADE) != 0;
        final NavigatorHandlerObjectBase.CommandTarget commandTarget;
        try {
            commandTarget = NavigatorHandlerObjectBase.getCommandTarget(
                    window,
                    node.getParentNode(),
                    object.getClass(),
                    false
            );
        } catch (DBException e) {
            log.warn(e);
            return;
        }
        if (commandContext == null) {
            commandContext = commandTarget.getContext();
        }
        if (!object.isPersisted() || commandTarget.getEditor() != null) {
            return;
        }
        final Map<String, Object> deleteOptions;
        if (supportsCascade && checkCascade) {
            deleteOptions = OPTIONS_CASCADE;
        } else {
            deleteOptions = Collections.EMPTY_MAP;
        }
        try {
            objectMaker.deleteObject(commandTarget.getContext(), node.getObject(), deleteOptions);
        } catch (DBException e) {
            log.warn(e);
            return;
        }
        final StringBuilder script = new StringBuilder();
        final DBECommandContext commandContext = commandTarget.getContext();
        final @SuppressWarnings("rawtypes") Collection<? extends DBECommand> commands = commandContext.getFinalCommands();
        try {
            for(@SuppressWarnings("rawtypes") DBECommand command : commands) {
                final DBEPersistAction[] persistActions = command.getPersistActions(monitor, commandContext.getExecutionContext(), deleteOptions);
                script.append(
                        SQLUtils.generateScript(commandContext.getExecutionContext().getDataSource(),
                                persistActions,
                                false));
                if (script.length() == 0) {
                    script.append(
                            SQLUtils.generateComments(commandContext.getExecutionContext().getDataSource(),
                                    persistActions,
                                    false));
                }
            }
        } catch (DBException e) {
            throw new InvocationTargetException(e);
        }
        commandTarget.getContext().resetChanges(true);
        if (sql.length() != 0) {
            sql.append("\n");
        }
        sql.append(script);
    }

    void setCheckCascade(final boolean checkCascade) {
        this.checkCascade = checkCascade;
    }
}

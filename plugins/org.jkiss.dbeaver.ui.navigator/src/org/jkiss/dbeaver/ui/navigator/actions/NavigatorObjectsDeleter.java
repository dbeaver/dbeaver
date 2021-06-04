/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.*;
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
import org.jkiss.dbeaver.ui.navigator.database.NavigatorViewBase;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class NavigatorObjectsDeleter {
    private static final Log log = Log.getLog(NavigatorObjectsDeleter.class);

    private final List<DBRRunnableWithProgress> tasksToExecute = new ArrayList<>();

    /**
     * Active window.
     */
    private final IWorkbenchWindow window;

    @Nullable
    private DBECommandContext commandContext;

    /**
     * A list containing objects to delete.
     */
    private final List<?> selection;

    private final boolean selectedFromNavigator;
    private final boolean supportsShowViewScript;
    private final boolean supportsDeleteContents;
    private boolean deleteContent;

    private final Set<Option> supportedOptions;
    private final Set<Option> enabledOptions = new HashSet<>();

    private NavigatorObjectsDeleter(IWorkbenchWindow window, List<?> selection, boolean selectedFromNavigator, boolean supportsShowViewScript,
                                    boolean supportsDeleteContents, Set<Option> supportedOptions) {
        this.window = window;
        this.selection = selection;
        this.selectedFromNavigator = selectedFromNavigator;
        this.supportsShowViewScript = supportsShowViewScript;
        this.supportsDeleteContents = supportsDeleteContents;
        this.supportedOptions = supportedOptions;
    }

    static NavigatorObjectsDeleter of(List<?> selection, IWorkbenchWindow window) {
        boolean supportsShowViewScript = false;
        boolean supportsDeleteContents = false;
        boolean selectedFromNavigator = window.getPartService().getActivePart() instanceof NavigatorViewBase;
        Set<Option> supportedOptions = new HashSet<>();

        for (Object obj: selection) {
            if (obj instanceof DBNProject) {
                supportsDeleteContents = true;
                continue;
            }
            if (!(obj instanceof DBNDatabaseNode)) {
                continue;
            }
            final DBNDatabaseNode node = (DBNDatabaseNode) obj;
            if (!(node.getParentNode() instanceof DBNContainer)) {
                continue;
            }

            final DBSObject object = node.getObject();
            if (object == null) {
                continue;
            }
            DBEObjectMaker<?, ?> objectMaker = DBWorkbench.getPlatform().getEditorsRegistry().getObjectManager(object.getClass(), DBEObjectMaker.class);
            if (objectMaker == null) {
                continue;
            }
            for (Option option: Option.values()) {
                if (supportsFeature(objectMaker, object.getDataSource(), option.featureValue)) {
                    supportedOptions.add(option);
                }
            }
            final NavigatorHandlerObjectBase.CommandTarget commandTarget;
            try {
                commandTarget = NavigatorHandlerObjectBase.getCommandTarget(
                        window,
                        node.getParentNode(),
                    null, object.getClass(),
                        false
                );
            } catch (DBException e) {
                continue;
            }
            if (object.isPersisted() && commandTarget.getEditor() == null && commandTarget.getContext() != null) {
                supportsShowViewScript = true;
            }
        }

        return new NavigatorObjectsDeleter(
            window,
            selection,
            selectedFromNavigator,
            supportsShowViewScript,
            supportsDeleteContents,
            supportedOptions
        );
    }

    private static boolean supportsFeature(@NotNull DBEObjectMaker<?, ?> objectMaker, DBPDataSource dataSource, long feature) {
        return (objectMaker.getMakerOptions(dataSource) & feature) != 0;
    }

    public IProject getProjectToDelete() {
        for (Object obj: selection) {
            if (obj instanceof DBNProject) {
                return ((DBNProject) obj).getProject().getEclipseProject();
            }
        }
        return null;
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

    private void deleteResource(final DBNResource resourceNode) {
        final IResource resource = resourceNode.getResource();
        try {
            NullProgressMonitor monitor = new NullProgressMonitor();
            if (resource instanceof IFolder) {
                ((IFolder)resource).delete(true, true, monitor);
            } else if (resource instanceof IProject) {
                // Delete project
                ((IProject) resource).delete(deleteContent, true, monitor);
            } else if (resource != null) {
                resource.delete(IResource.FORCE | IResource.KEEP_HISTORY, monitor);
            }
        } catch (CoreException e) {
            DBWorkbench.getPlatformUI().showError(
                    UINavigatorMessages.error_deleting_resource_title,
                    NLS.bind(UINavigatorMessages.error_deleting_resource_message, resource.getFullPath().toString()),
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
            final DBEObjectMaker objectMaker = DBWorkbench.getPlatform().getEditorsRegistry().getObjectManager(object.getClass(), DBEObjectMaker.class);
            if (objectMaker == null) {
                throw new DBException("Object maker not found for type '" + object.getClass().getName() + "'"); //$NON-NLS-2$
            }
            final NavigatorHandlerObjectBase.CommandTarget commandTarget =
                    NavigatorHandlerObjectBase.getCommandTarget(
                    window,
                    node.getParentNode(),
                        null, object.getClass(),
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
            Map<String, Object> deleteOptions = collectObjectMakerOptionsMap(object, objectMaker);
            objectMaker.deleteObject(commandTarget.getContext(), node.getObject(), deleteOptions);
            if (commandTarget.getEditor() == null && commandTarget.getContext() != null) {
                // Persist object deletion - only if there is no host editor and we have a command context
                final NavigatorHandlerObjectBase.ObjectSaver deleter = new NavigatorHandlerObjectBase.ObjectSaver(commandTarget.getContext(), deleteOptions);
                tasksToExecute.add(deleter);
            }
            if (commandTarget.getEditor() != null && selectedFromNavigator) {
                UIUtils.getActiveWorkbenchWindow().getActivePage().activate(commandTarget.getEditor());
                DBWorkbench.getPlatformUI().showMessageBox(
                    UINavigatorMessages.actions_navigator_persist_delete_in_the_editor_title,
                    NLS.bind(UINavigatorMessages.actions_navigator_persist_delete_in_the_editor_message, commandTarget.getEditor().getTitle()),
                    false
                );
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
        final DBEObjectMaker objectMaker =
                DBWorkbench.getPlatform().getEditorsRegistry().getObjectManager(object.getClass(), DBEObjectMaker.class);
        if (objectMaker == null) {
            return;
        }
        final NavigatorHandlerObjectBase.CommandTarget commandTarget;
        try {
            commandTarget = NavigatorHandlerObjectBase.getCommandTarget(
                    window,
                    node.getParentNode(),
                null, object.getClass(),
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
        Map<String, Object> deleteOptions = collectObjectMakerOptionsMap(object, objectMaker);
        try {
            objectMaker.deleteObject(commandTarget.getContext(), node.getObject(), deleteOptions);
        } catch (DBException e) {
            log.warn(e);
            return;
        }
        final StringBuilder script = new StringBuilder();
        final DBECommandContext commandContext = commandTarget.getContext();
        Collection<? extends DBECommand> commands = commandContext.getFinalCommands();
        try {
            for(DBECommand command : commands) {
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

    public boolean supportsShowViewScript() {
        return supportsShowViewScript;
    }

    public boolean supportsDeleteContents() {
        return supportsDeleteContents;
    }

    public void setDeleteContents(boolean deleteContents) {
        this.deleteContent = deleteContents;
    }

    public Set<Option> getSupportedOptions() {
        return Collections.unmodifiableSet(supportedOptions);
    }

    public void enableOption(@NotNull Option option) {
        enabledOptions.add(option);
    }

    public void disableOption(@NotNull Option option) {
        enabledOptions.remove(option);
    }

    private Map<String, Object> collectObjectMakerOptionsMap(@NotNull DBSObject object, @NotNull DBEObjectMaker<?, ?> objectMaker) {
        Map<String, Object> objectMakerOptionsMap = new HashMap<>();
        DBPDataSource dataSource = object.getDataSource();
        for (Option option: enabledOptions) {
            if (supportsFeature(objectMaker, dataSource, option.featureValue)) {
                objectMakerOptionsMap.put(option.optionValue, Boolean.TRUE);
            }
        }
        return objectMakerOptionsMap;
    }

    public enum Option {
        DELETE_CASCADE(
            DBEObjectMaker.FEATURE_DELETE_CASCADE,
            DBEObjectManager.OPTION_DELETE_CASCADE,
            UINavigatorMessages.confirm_deleting_delete_cascade_checkbox_label,
            UINavigatorMessages.confirm_deleting_delete_cascade_checkbox_tooltip
        ),

        CLOSE_EXISTING_CONNECTIONS(
                DBEObjectMaker.FEATURE_CLOSE_EXISTING_CONNECTIONS,
                DBEObjectManager.OPTION_CLOSE_EXISTING_CONNECTIONS,
                UINavigatorMessages.confirm_deleting_close_existing_connections_checkbox_label,
                UINavigatorMessages.confirm_deleting_close_existing_connections_checkbox_tooltip
        );

        private final long featureValue;
        private final String optionValue;
        private final String label;
        private final String tip;

        Option(long featureValue, @NotNull String optionValue, @NotNull String label, @NotNull String tip) {
            this.featureValue = featureValue;
            this.optionValue = optionValue;
            this.label = label;
            this.tip = tip;
        }

        public String getLabel() {
            return label;
        }

        public String getTip() {
            return tip;
        }
    }
}

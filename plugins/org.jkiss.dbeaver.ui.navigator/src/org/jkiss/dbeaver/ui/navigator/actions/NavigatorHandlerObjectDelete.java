/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.edit.DBECommand;
import org.jkiss.dbeaver.model.edit.DBECommandContext;
import org.jkiss.dbeaver.model.edit.DBEObjectMaker;
import org.jkiss.dbeaver.model.edit.DBEPersistAction;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.TasksJob;
import org.jkiss.dbeaver.runtime.ui.UIServiceSQL;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditor;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.List;

@SuppressWarnings({"unchecked", "rawtypes"})
public class NavigatorHandlerObjectDelete extends NavigatorHandlerObjectBase implements IElementUpdater {
    private static final Log log = Log.getLog(NavigatorHandlerObjectDelete.class);

    private IStructuredSelection structSelection;

    private final List<DBRRunnableWithProgress> tasksToExecute = new ArrayList<>();

    /**
     * Active window.
     */
    private IWorkbenchWindow window;

    /**
     * {@code true} if 'Cascade delete' button should be shown
     */
    private boolean showCascade;

    /**
     * {@code true} if 'View Script' button should be shown
     */
    private boolean showViewScript;

    /**
     * {@code true} in case of attempt to delete database nodes which belong to different data sources
     */
    private boolean multipleDataSources;

    private @Nullable DBECommandContext commandContext;

    /**
     * A map with delete option.
     *
     * <p>Only contains cascade option
     */
    private static final Map<String, Object> OPTIONS_CASCADE;

    static {
        final Map<String, Object> map = new HashMap<>(1);
        map.put(DBEObjectMaker.OPTION_DELETE_CASCADE, Boolean.TRUE);
        OPTIONS_CASCADE = Collections.unmodifiableMap(map);
    }

    @Override
    public Object execute(final ExecutionEvent event) throws ExecutionException {
        reset();
        final ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (!(selection instanceof IStructuredSelection)) {
            return null;
        }
        window = HandlerUtil.getActiveWorkbenchWindow(event);
        structSelection = (IStructuredSelection)selection;
        resolveOptions();
        if (multipleDataSources) {
            // attempt to delete database nodes from different databases
            DBWorkbench.getPlatformUI().
                    showError(
                        UINavigatorMessages.error_deleting_multiple_objects_from_different_datasources_title,
                        UINavigatorMessages.error_deleting_multiple_objects_from_different_datasources_message
                    );
            return null;
        }
        final ConfirmationDialog dialog = ConfirmationDialog.of(window.getShell(), structSelection.toList(), showCascade, showViewScript);
        final int result = dialog.open();
        if (result == IDialogConstants.YES_ID) {
            delete(dialog.cascadeCheck);
        } else if (result == IDialogConstants.DETAILS_ID) {
            final boolean persistCheck = showScriptWindow(dialog.cascadeCheck);
            if (persistCheck) {
                delete(dialog.cascadeCheck);
            } else {
                commandContext.resetChanges(true);
                execute(event);
            }
        }
        return null;
    }

    /**
     * Resolves boolean flags from this class.
     *
     * @see this.showViewScript, this.showCascade, this.multipleDataSources
     */
    private void resolveOptions() {
        DBPDataSource dataSource = null;
        for (Object obj: structSelection.toList()) {
            if (!(obj instanceof DBNDatabaseNode)) {
                continue;
            }
            final DBNDatabaseNode node = (DBNDatabaseNode) obj;
            final DBPDataSource currentDatasource = node.getDataSource();
            if (dataSource == null) {
                dataSource = currentDatasource;
            } else if (!dataSource.equals(currentDatasource)) {
                multipleDataSources = true;
            }
            if (!(node.getParentNode() instanceof DBNContainer)) {
                continue;
            }
            final DBSObject object = node.getObject();
            if (object == null) {
                continue;
            }
            final DBEObjectMaker objectMaker = DBWorkbench.getPlatform().getEditorsRegistry().getObjectManager(object.getClass(), DBEObjectMaker.class);
            if (objectMaker == null) {
                continue;
            }
            showCascade |= (objectMaker.getMakerOptions(object.getDataSource()) & DBEObjectMaker.FEATURE_DELETE_CASCADE) != 0;
            final CommandTarget commandTarget;
            try {
                commandTarget = getCommandTarget(
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
    }

    private static class ConfirmationDialog extends MessageDialog {
        private final List selectedObjects;

        private final boolean showCascade;

        private final boolean showViewScript;

        private boolean cascadeCheck;

        private ConfirmationDialog(final Shell shell, final String title, final String message,
                                   final List selectedObjects, final boolean showCascade, final boolean showViewScript) {
            super(
                    shell,
                    title,
                    DBeaverIcons.getImage(UIIcon.REJECT),
                    message,
                    MessageDialog.WARNING,
                    null,
                    0
            );
            this.selectedObjects = selectedObjects;
            this.showCascade = showCascade;
            this.showViewScript = showViewScript;
        }

        static ConfirmationDialog of(final Shell shell, final List selectedObjects,
                                     final boolean showCascade, final boolean showViewScript) {
            if (selectedObjects.size() > 1) {
                return new ConfirmationDialog(
                        shell,
                        UINavigatorMessages.confirm_deleting_multiple_objects_title,
                        UINavigatorMessages.confirm_deleting_multiple_objects_message,
                        selectedObjects,
                        showCascade,
                        showViewScript
                );
            }
            final DBNNode node = (DBNNode) selectedObjects.get(0);
            final String title = NLS.bind(node instanceof DBNLocalFolder ? UINavigatorMessages.confirm_local_folder_delete_title : UINavigatorMessages.confirm_entity_delete_title, node.getNodeType(), node.getNodeName());
            final String message = NLS.bind(node instanceof DBNLocalFolder ? UINavigatorMessages.confirm_local_folder_delete_message : UINavigatorMessages.confirm_entity_delete_message, node.getNodeType(), node.getNodeName());
            return new ConfirmationDialog(shell, title, message, selectedObjects, showCascade, showViewScript);
        }

        @Override
        protected Control createCustomArea(final Composite parent) {
            if (selectedObjects.size() > 1) {
                setUpObjectsTable(parent);
            }
            setUpCascadeButton(parent);
            return super.createCustomArea(parent);
        }

        private void setUpObjectsTable(final Composite parent) {
            final Composite placeholder = UIUtils.createComposite(parent, 1);
            placeholder.setLayoutData(new GridData(GridData.FILL_BOTH));
            final Group tableGroup = UIUtils.createControlGroup(placeholder, UINavigatorMessages.confirm_deleting_multiple_objects_table_group_name, 1, GridData.FILL_BOTH, 0);
            tableGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
            final Table objectsTable = new Table(tableGroup, SWT.BORDER | SWT.FULL_SELECTION);
            objectsTable.setHeaderVisible(false);
            objectsTable.setLinesVisible(true);
            objectsTable.setLayoutData(new GridData(GridData.FILL_BOTH));
            UIUtils.createTableColumn(objectsTable, SWT.RIGHT, UINavigatorMessages.confirm_deleting_multiple_objects_column_name);
            UIUtils.createTableColumn(objectsTable, SWT.RIGHT, UINavigatorMessages.confirm_deleting_multiple_objects_column_description);
            for (Object obj: selectedObjects) {
                if (!(obj instanceof DBNNode)) {
                    continue;
                }
                final DBNNode node = (DBNNode) obj;
                final TableItem item = new TableItem(objectsTable, SWT.NONE);
                item.setImage(DBeaverIcons.getImage(node.getNodeIcon()));
                item.setText(0, node.getNodeFullName());
                item.setText(1, CommonUtils.toString(node.getNodeDescription()));
            }
            UIUtils.asyncExec(() -> UIUtils.packColumns(objectsTable, true));
        }

        private void setUpCascadeButton(final Composite parent) {
            if (!showCascade) {
                return;
            }
            final Composite ph = UIUtils.createPlaceholder(parent, 1, 5);
            final Button cascadeCheckButton =
                    UIUtils.createCheckbox(
                            ph,
                            UINavigatorMessages.confirm_deleting_multiple_objects_cascade_checkbox,
                            UINavigatorMessages.confirm_deleting_multiple_objects_cascade_checkbox_tooltip,
                            false,
                            0
                    );
            cascadeCheckButton.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    cascadeCheck = cascadeCheckButton.getSelection();
                }
            });
        }

        @Override
        protected void createButtonsForButtonBar(final Composite parent) {
            createButton(parent, IDialogConstants.YES_ID, IDialogConstants.YES_LABEL, false);
            createButton(parent, IDialogConstants.NO_ID, IDialogConstants.NO_LABEL, true);
            if (showViewScript) {
                createButton(parent, IDialogConstants.DETAILS_ID, UINavigatorMessages.actions_navigator_view_script_button, false);
            }
        }
    }

    /**
     * Deletes all objects in selection.
     *
     * @param cascadeCheck {@code true} if Cascade Delete checkbox was checked
     */
    private void delete(final boolean cascadeCheck) {
        for (Object obj: structSelection.toList()) {
            if (obj instanceof DBNDatabaseNode) {
                deleteDatabaseNode((DBNDatabaseNode)obj, cascadeCheck);
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

    private void deleteDatabaseNode(final DBNDatabaseNode node, final boolean cascadeCheck) {
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
            final boolean supportsCascade = (objectMaker.getMakerOptions(object.getDataSource()) & DBEObjectMaker.FEATURE_DELETE_CASCADE) != 0;
            final CommandTarget commandTarget = getCommandTarget(
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
            Map<String, Object> deleteOptions = Collections.EMPTY_MAP;
            if (cascadeCheck && supportsCascade) {
                deleteOptions = OPTIONS_CASCADE;
            }
            objectMaker.deleteObject(commandTarget.getContext(), node.getObject(), deleteOptions);
            if (commandTarget.getEditor() == null && commandTarget.getContext() != null) {
                // Persist object deletion - only if there is no host editor and we have a command context
                final ObjectSaver deleter = new ObjectSaver(commandTarget.getContext(), deleteOptions);
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

    private boolean showScriptWindow(final boolean checkCascade) {
        final String sql = collectSQL(checkCascade);
        if (sql.length() > 0) {
            UIServiceSQL serviceSQL = DBWorkbench.getService(UIServiceSQL.class);
            if (serviceSQL != null) {
                return serviceSQL.openSQLViewer(
                        commandContext.getExecutionContext(),
                        UINavigatorMessages.actions_navigator_delete_script,
                        UIIcon.SQL_PREVIEW,
                        sql,
                        true,
                        false
                ) == IDialogConstants.PROCEED_ID;
            }
        } else {
            return UIUtils.confirmAction(
                    window.getShell(),
                    UINavigatorMessages.actions_navigator_delete_script,
                    UINavigatorMessages.question_no_sql_available
            );
        }
        return false;
    }

    private String collectSQL(final boolean checkCascade) {
        final StringBuilder sql = new StringBuilder();
        for (Object obj: structSelection.toList()) {
            if (obj instanceof DBNDatabaseNode) {
                appendScript(sql, (DBNDatabaseNode) obj, checkCascade);
            }
        }
        return sql.toString();
    }

    private void appendScript(final StringBuilder sql, final DBNDatabaseNode node, final boolean checkCascade) {
        if (!(node.getParentNode() instanceof DBNContainer)) {
            return;
        }
        // Try to delete object using object manager
        final DBSObject object = node.getObject();
        if (object == null) {
            return;
        }
        final DBEObjectMaker objectMaker = DBWorkbench.getPlatform().getEditorsRegistry().getObjectManager(object.getClass(), DBEObjectMaker.class);
        if (objectMaker == null) {
            return;
        }
        final boolean supportsCascade = (objectMaker.getMakerOptions(object.getDataSource()) & DBEObjectMaker.FEATURE_DELETE_CASCADE) != 0;
        final CommandTarget commandTarget;
        try {
            commandTarget = getCommandTarget(
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
        final Collection<? extends DBECommand> commands = commandContext.getFinalCommands();
        try {
            UIUtils.runInProgressService(monitor -> {
                try {
                    for (DBECommand command : commands) {
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
            });
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError(
                    UINavigatorMessages.error_sql_generation_title,
                    UINavigatorMessages.error_sql_generation_message,
                    e.getTargetException()
            );
        } catch (InterruptedException ignored) {
        }
        commandTarget.getContext().resetChanges(true);
        if (sql.length() != 0) {
            sql.append("\n");
        }
        sql.append(script);
    }

    /**
     * Zeroes all fields of the class, making it ready for reuse
     */
    private void reset() {
        structSelection = null;
        tasksToExecute.clear();
        showCascade = false;
        showViewScript = false;
        multipleDataSources = false;
        commandContext = null;
        window = null;
    }

    @Override
    public void updateElement(UIElement element, Map parameters) {
//        if (!updateUI) {
//            return;
//        }
//        final ISelectionProvider selectionProvider = UIUtils.getSelectionProvider(element.getServiceLocator());
//        if (selectionProvider != null) {
//            ISelection selection = selectionProvider.getSelection();
//
//            if (selection instanceof IStructuredSelection && ((IStructuredSelection) selection).size() > 1) {
//                element.setText(UINavigatorMessages.actions_navigator_delete_objects);
//            } else {
//                DBNNode node = NavigatorUtils.getSelectedNode(selection);
//                if (node != null) {
//                    element.setText(UINavigatorMessages.actions_navigator_delete_ + " " + node.getNodeTypeLabel()  + " '" + node.getNodeName() + "'");
//                }
//            }
//        }
    }
}
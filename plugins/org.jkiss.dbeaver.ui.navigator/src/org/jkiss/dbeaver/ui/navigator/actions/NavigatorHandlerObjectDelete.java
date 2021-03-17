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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IPath;
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
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEObjectManager;
import org.jkiss.dbeaver.model.edit.DBEObjectWithDependencies;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableConstraint;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIIcon;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class NavigatorHandlerObjectDelete extends NavigatorHandlerObjectBase implements IElementUpdater {
    private static final Log log = Log.getLog(NavigatorHandlerObjectDelete.class);

    @Override
    public Object execute(final ExecutionEvent event) throws ExecutionException {
        final ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (!(selection instanceof IStructuredSelection)) {
            return null;
        }
        final IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
        tryDeleteObjects(window, (IStructuredSelection) selection);
        return null;
    }

    public static boolean tryDeleteObjects(IWorkbenchWindow window, IStructuredSelection selection) {
        @SuppressWarnings("unchecked")
        final List<DBNNode> selectedObjects = selection.toList();
        final NavigatorObjectsDeleter deleter = NavigatorObjectsDeleter.of(selectedObjects, window);
        return makeDeletionAttempt(window, selectedObjects, deleter);
    }

    private static boolean makeDeletionAttempt(final IWorkbenchWindow window, final List<?> selectedObjects, final NavigatorObjectsDeleter deleter) {
        if (deleter.hasNodesFromDifferentDataSources()) {
            // attempt to delete database nodes from different databases
            DBWorkbench.getPlatformUI().
                    showError(
                            UINavigatorMessages.error_deleting_multiple_objects_from_different_datasources_title,
                            UINavigatorMessages.error_deleting_multiple_objects_from_different_datasources_message
                    );
            return false;
        }
        final ConfirmationDialog dialog = ConfirmationDialog.of(
                window.getShell(),
                selectedObjects,
                deleter);
        final int result = dialog.open();
        if (result == IDialogConstants.YES_ID) {
            return deleteObjects(window, deleter, selectedObjects);
        } else if (result == IDialogConstants.DETAILS_ID) {
            final boolean persistCheck = deleter.showScriptWindow();
            if (persistCheck) {
                return deleteObjects(window, deleter, selectedObjects);
            } else {
                return makeDeletionAttempt(window, selectedObjects, deleter);
            }
        } else {
            return false;
        }
    }

    private static boolean deleteObjects(final IWorkbenchWindow window, NavigatorObjectsDeleter deleter, final List<?> selectedObjects) {
        if (confirmDependenciesDelete(window, selectedObjects)) {
            deleter.delete();
            return true;
        }
        return false;
    }

    private static boolean confirmDependenciesDelete(final IWorkbenchWindow window, final List<?> selectedObjects) {
        List<DBNNode> dependentObjectsListNodes = new ArrayList<>();
        try {
            UIUtils.runInProgressService(monitor -> {
                for (Object obj : selectedObjects) {
                    if (obj instanceof DBNDatabaseItem) {
                        DBSObject dbsObject = ((DBNDatabaseItem) obj).getObject();
                        if (dbsObject instanceof DBSEntityAttribute) {
                            DBSEntityAttribute attribute = (DBSEntityAttribute) dbsObject;
                            DBEObjectManager<?> objectManager = attribute.getDataSource().getContainer().getPlatform().getEditorsRegistry().getObjectManager(attribute.getClass());
                            if (objectManager instanceof DBEObjectWithDependencies) {
                                try {
                                    List<? extends DBSObject> dependentObjectsList = ((DBEObjectWithDependencies) objectManager).getDependentObjectsList(monitor, attribute);
                                    changeDependentObjectsList(monitor, dependentObjectsList);
                                    if (!CommonUtils.isEmpty(dependentObjectsList)) {
                                        for (Object object : dependentObjectsList) {
                                            if (object instanceof DBSObject) {
                                                DBNDatabaseNode node = DBNUtils.getNodeByObject(monitor, (DBSObject) object, false);
                                                dependentObjectsListNodes.add(node);
                                            }
                                        }
                                    }
                                } catch (DBException e) {
                                    log.debug("Can't get object dependent list", e);
                                }
                            }
                        }
                    }
                }
            });
        } catch (InvocationTargetException e) {
            DBWorkbench.getPlatformUI().showError(
                    UINavigatorMessages.search_dependencies_error_title, UINavigatorMessages.search_dependencies_error_message, e.getTargetException());
        } catch (InterruptedException ignored) {
        }
        if (!CommonUtils.isEmpty(dependentObjectsListNodes)) {
            NavigatorObjectsDeleter dependentObjectsDeleter = NavigatorObjectsDeleter.of(dependentObjectsListNodes, window);
            String confirmMessage;
            if (dependentObjectsListNodes.size() == 1) {
                DBNDatabaseNode node = (DBNDatabaseNode) dependentObjectsListNodes.get(0);
                confirmMessage = NLS.bind(UINavigatorMessages.confirm_deleting_dependent_one_object, node.getNodeType(), node.getNodeName());
            } else {
                confirmMessage = NLS.bind(UINavigatorMessages.confirm_deleting_dependent_objects, dependentObjectsListNodes.size());
            }
            final ConfirmationDialog dialog = new ConfirmationDialog(
                    window.getShell(),
                    UINavigatorMessages.confirm_deleting_dependent_objects_title,
                    confirmMessage,
                    dependentObjectsListNodes,
                    dependentObjectsDeleter);
            final int result = dialog.open();
            if (result == IDialogConstants.YES_ID) {
                dependentObjectsDeleter.delete();
                return true;
            } else {
                return false;
            }
        } else {
            return true;
        }
    }

    private static void changeDependentObjectsList(@NotNull DBRProgressMonitor monitor, List<? extends DBSObject> dependentObjectsList) throws DBException {
        // Some indexes in some databases in fact duplicate existing keys, and therefore deleting keys will automatically delete indexes on the database side
        // Let's find this indexes and remove from dependent list
        if (!CommonUtils.isEmpty(dependentObjectsList)) {
            List<? extends DBSObject> indexList = dependentObjectsList.stream().filter(o -> o instanceof DBSTableIndex).collect(Collectors.toList());
            List<? extends DBSObject> constrList = dependentObjectsList.stream().filter(o -> o instanceof DBSTableConstraint).collect(Collectors.toList());
            for (DBSObject constraint : constrList) {
                for (DBSObject index : indexList) {
                    if (constraint instanceof DBSEntityReferrer && index instanceof DBSEntityReferrer) {
                        if (DBUtils.referrerMatches(monitor, (DBSEntityReferrer) constraint, DBUtils.getEntityAttributes(monitor, (DBSEntityReferrer) index))) {
                            dependentObjectsList.remove(index);
                        }
                    }
                }
            }
        }
    }

    private static class ConfirmationDialog extends MessageDialog {
        private final List<?> selectedObjects;

        private final NavigatorObjectsDeleter deleter;

        private ConfirmationDialog(final Shell shell, final String title, final String message,
                                   final List<?> selectedObjects, final NavigatorObjectsDeleter deleter) {
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
            this.deleter = deleter;
        }

        static ConfirmationDialog of(final Shell shell, final List<?> selectedObjects,
                                     final NavigatorObjectsDeleter deleter) {
            if (selectedObjects.size() > 1) {
                return new ConfirmationDialog(
                        shell,
                        UINavigatorMessages.confirm_deleting_multiple_objects_title,
                        NLS.bind(UINavigatorMessages.confirm_deleting_multiple_objects_message, selectedObjects.size()),
                        selectedObjects,
                        deleter);
            }
            final DBNNode node = (DBNNode) selectedObjects.get(0);
            final String title = NLS.bind(node instanceof DBNLocalFolder ? UINavigatorMessages.confirm_local_folder_delete_title : UINavigatorMessages.confirm_entity_delete_title, node.getNodeType(), node.getNodeName());
            final String message = NLS.bind(node instanceof DBNLocalFolder ? UINavigatorMessages.confirm_local_folder_delete_message : UINavigatorMessages.confirm_entity_delete_message, node.getNodeType(), node.getNodeName());
            return new ConfirmationDialog(shell, title, message, selectedObjects, deleter);
        }

        @Override
        protected Control createCustomArea(final Composite parent) {
            if (selectedObjects.size() > 1) {
                createObjectsTable(parent);
            }
            createDeleteContents(parent);
            createCascadeButton(parent);
            return super.createCustomArea(parent);
        }

        private void createObjectsTable(final Composite parent) {
            final Composite placeholder = UIUtils.createComposite(parent, 1);
            placeholder.setLayoutData(new GridData(GridData.FILL_BOTH));
            final Group tableGroup = UIUtils.createControlGroup(placeholder, UINavigatorMessages.confirm_deleting_multiple_objects_table_group_name, 1, GridData.FILL_BOTH, 0);
            tableGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
            final Table objectsTable = new Table(tableGroup, SWT.BORDER | SWT.FULL_SELECTION);
            objectsTable.setHeaderVisible(false);
            objectsTable.setLinesVisible(true);
            GridData gd = new GridData(GridData.FILL_BOTH);
            int fontHeight = UIUtils.getFontHeight(objectsTable);
            int rowCount = selectedObjects.size();
            gd.widthHint = fontHeight * 7;
            gd.heightHint = rowCount < 6 ? fontHeight * 2 * rowCount : fontHeight * 10;
            objectsTable.setLayoutData(gd);
            UIUtils.createTableColumn(objectsTable, SWT.LEFT, UINavigatorMessages.confirm_deleting_multiple_objects_column_name);
            UIUtils.createTableColumn(objectsTable, SWT.LEFT, UINavigatorMessages.confirm_deleting_multiple_objects_column_description);
            for (Object obj: selectedObjects) {
                if (!(obj instanceof DBNNode)) {
                    continue;
                }
                final DBNNode node = (DBNNode) obj;
                final TableItem item = new TableItem(objectsTable, SWT.NONE);
                item.setImage(DBeaverIcons.getImage(node.getNodeIcon()));
                if (node instanceof DBNResource && ((DBNResource) node).getResource() != null) {
                    item.setText(0, node.getName());
                    IPath resLocation = ((DBNResource) node).getResource().getLocation();
                    item.setText(1, resLocation == null ? "" : resLocation.toFile().getAbsolutePath());
                } else {
                    item.setText(0, node.getNodeFullName());
                    item.setText(1, CommonUtils.toString(node.getNodeDescription()));
                }
            }
            UIUtils.packColumns(objectsTable, true);
        }

        private void createDeleteContents(final Composite parent) {
            if (!deleter.isShowDeleteContents()) {
                return;
            }
            IProject project = deleter.getProjectToDelete();
            if (project == null) {
                return;
            }
            final Composite ph = UIUtils.createPlaceholder(parent, 2, 5);
            final Button keepContentsCheck =
                UIUtils.createCheckbox(
                    ph,
                    UINavigatorMessages.confirm_deleting_delete_contents_checkbox,
                    UINavigatorMessages.confirm_deleting_delete_contents_checkbox_tooltip,
                    false,
                    2
                );
            keepContentsCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    deleter.setDeleteContents(keepContentsCheck.getSelection());
                }
            });
            UIUtils.createLabelText(ph,
                UINavigatorMessages.confirm_deleting_project_location_label,
                project.getLocation().toFile().getAbsolutePath(),
                SWT.READ_ONLY);
        }

        private void createCascadeButton(final Composite parent) {
            if (!deleter.isShowCascade()) {
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
                    deleter.setDeleteCascade(cascadeCheckButton.getSelection());
                }
            });
        }

        @Override
        protected void createButtonsForButtonBar(final Composite parent) {
            createButton(parent, IDialogConstants.YES_ID, IDialogConstants.YES_LABEL, false);
            createButton(parent, IDialogConstants.NO_ID, IDialogConstants.NO_LABEL, true);
            if (deleter.isShowViewScript()) {
                createButton(parent, IDialogConstants.DETAILS_ID, UINavigatorMessages.actions_navigator_view_script_button, false);
            }
        }

        @Override
        protected boolean isResizable() {
            return true;
        }
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

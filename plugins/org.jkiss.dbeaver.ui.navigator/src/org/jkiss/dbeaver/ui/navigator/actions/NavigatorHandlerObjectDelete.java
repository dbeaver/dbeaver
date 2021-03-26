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
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.edit.DBEObjectManager;
import org.jkiss.dbeaver.model.edit.DBEObjectWithDependencies;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableConstraint;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.internal.UINavigatorMessages;
import org.jkiss.dbeaver.ui.navigator.dialogs.ConfirmNavigatorNodesDeleteDialog;
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
        final IStructuredSelection structuredSelection = (IStructuredSelection) selection;
        final IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
        tryDeleteObjects(window, structuredSelection.toList());
        return null;
    }

    public static boolean tryDeleteObjects(@NotNull IWorkbenchWindow window, @NotNull List<?> objects) {
        if (containsNodesFromDifferentDataSources(objects)) {
            DBWorkbench.getPlatformUI().showError(
                UINavigatorMessages.error_deleting_multiple_objects_from_different_datasources_title,
                UINavigatorMessages.error_deleting_multiple_objects_from_different_datasources_message
            );
            return false;
        }
        return tryDeleteObjects(window, objects, NavigatorObjectsDeleter.of(objects, window));
    }

    private static boolean containsNodesFromDifferentDataSources(@NotNull List<?> objects) {
        DBPDataSource dataSource = null;
        for (Object o: objects) {
            if (!(o instanceof DBNDatabaseNode)) {
                continue;
            }
            DBNDatabaseNode databaseNode = (DBNDatabaseNode) o;
            DBPDataSource currentDatasource;
            if (databaseNode instanceof  DBNDataSource) {
                currentDatasource = null;
            } else {
                currentDatasource = databaseNode.getDataSource();
            }
            if (dataSource == null) {
                dataSource = currentDatasource;
            } else if (!dataSource.equals(currentDatasource)) {
                return true;
            }
        }
        return false;
    }

    private static boolean tryDeleteObjects(final IWorkbenchWindow window, final List<?> selectedObjects, final NavigatorObjectsDeleter deleter) {
        final ConfirmNavigatorNodesDeleteDialog dialog = ConfirmNavigatorNodesDeleteDialog.of(
                window.getShell(),
                selectedObjects,
                deleter
        );
        final int result = dialog.open();
        if (result == IDialogConstants.YES_ID) {
            return deleteObjects(window, deleter, selectedObjects);
        } else if (result == IDialogConstants.DETAILS_ID) {
            final boolean persistCheck = deleter.showScriptWindow();
            if (persistCheck) {
                return deleteObjects(window, deleter, selectedObjects);
            } else {
                return tryDeleteObjects(window, selectedObjects, deleter);
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
            final ConfirmNavigatorNodesDeleteDialog dialog = ConfirmNavigatorNodesDeleteDialog.of(
                    window.getShell(),
                    UINavigatorMessages.confirm_deleting_dependent_objects_title,
                    confirmMessage,
                    dependentObjectsListNodes,
                    dependentObjectsDeleter
            );
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

/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.navigator.*;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.IRefreshablePart;
import org.jkiss.dbeaver.ui.UIConfirmation;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ConfirmationDialog;
import org.jkiss.dbeaver.ui.editors.DatabaseEditorInput;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditor;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.ui.navigator.INavigatorModelView;
import org.jkiss.dbeaver.ui.navigator.NavigatorPreferences;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.*;

public class NavigatorHandlerRefresh extends AbstractHandler {
    private static final Log log = Log.getLog(NavigatorHandlerRefresh.class);

    public NavigatorHandlerRefresh() {

    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        //final IWorkbenchWindow workbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
        final IWorkbenchPart workbenchPart = HandlerUtil.getActivePart(event);

        // If navigator refresh is possible then do not refresh active part directly
        // Because active part should be refresh in navigator event handler
        if (refreshInNavigator(event, workbenchPart, false)) {
            return null;
        }

        // Try to refresh as refreshable part
        if (workbenchPart instanceof IRefreshablePart refreshablePart) {
            if (workbenchPart instanceof IDatabaseEditor databaseEditor) {
                IEditorInput editorInput = databaseEditor.getEditorInput();
                if (editorInput instanceof IDatabaseEditorInput databaseEditorInput) {
                    DBSObject databaseObject = databaseEditorInput.getDatabaseObject();
                    if (databaseObject == null || !databaseObject.isPersisted()) {
                        // Do not refresh non-persistent objects
                        return null;
                    }
                }
            }
            if (refreshablePart.refreshPart(this, true) == IRefreshablePart.RefreshResult.CANCELED) {
                return null;
            }
            //return null;
        }

        return null;
    }

    static boolean refreshInNavigator(ExecutionEvent event, IWorkbenchPart workbenchPart, boolean refreshRoot) {
        // Try to get navigator view and refresh node
        INavigatorModelView navigatorView = GeneralUtils.adapt(workbenchPart, INavigatorModelView.class);
        if (navigatorView == null) {
            // Nothing to refresh
            return false;
        }
        final List<DBNNode> refreshObjects = new ArrayList<>();
        final ISelection selection = HandlerUtil.getCurrentSelection(event);

        DBNNode rootNode = navigatorView.getRootNode();
        if (rootNode == null) {
            if (workbenchPart instanceof IEditorPart editorPart) {
                if (editorPart.getEditorInput() instanceof IDatabaseEditorInput databaseEditorInput) {
                    rootNode = databaseEditorInput.getNavigatorNode();
                }
            }
        }
        if (rootNode != null && rootNode.getParentNode() instanceof DBNDatabaseNode || (refreshRoot && rootNode instanceof DBNProjectDatabases)) {
            refreshObjects.add(rootNode);
        } else if (selection instanceof IStructuredSelection structSelection) {

            for (Object object : structSelection) {
                if (object instanceof DBNNode) {
                    refreshObjects.add((DBNNode) object);
                }
            }
        }

        // Check for open editors with selected objects
        if (!refreshObjects.isEmpty()) {
            for (IEditorReference er : UIUtils.getActiveWorkbenchWindow().getActivePage().getEditorReferences()) {
                IEditorPart editorPart = er.getEditor(false);
                if (editorPart instanceof IRefreshablePart && editorPart.getEditorInput() instanceof DatabaseEditorInput && editorPart.isDirty()) {
                    DBNDatabaseNode editorNode = ((DatabaseEditorInput<?>) editorPart.getEditorInput()).getNavigatorNode();
                    for (Iterator<DBNNode> iter = refreshObjects.iterator(); iter.hasNext(); ) {
                        DBNNode nextNode = iter.next();
                        if (nextNode == editorNode || editorNode.isChildOf(nextNode) || nextNode.isChildOf(editorNode)) {
                            if (((IRefreshablePart) editorPart).refreshPart(navigatorView, true) == IRefreshablePart.RefreshResult.CANCELED) {
                                return true;
                            }
                            if (nextNode == editorNode) {
                                iter.remove();
                            }
                        }
                    }
                }
            }
        }

        // Refresh objects
        if (!refreshObjects.isEmpty()) {
            return refreshNavigator(refreshObjects);
        }
        return false;
    }

    public static boolean refreshNavigator(final Collection<? extends DBNNode> refreshObjects)
    {
        Job refreshJob = new AbstractJob("Refresh navigator object(s)") {
            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                monitor.beginTask("Refresh objects", refreshObjects.size());
                Set<DBNNode> refreshedSet = new HashSet<>();
                for (DBNNode node : refreshObjects) {
                    if (node.isDisposed() || node.isLocked()) {
                        // Skip locked nodes
                        continue;
                    }
                    if (monitor.isCanceled()) {
                        break;
                    }
                    // Check this node was already refreshed
                    if (!refreshedSet.isEmpty()) {
                        boolean skip = false;
                        for (DBNNode refreshed : refreshedSet) {
                            if (node == refreshed || node.isChildOf(refreshed)) {
                                skip = true;
                                break;
                            }
                        }
                        if (skip) {
                            continue;
                        }
                    }
                    // Check for dirty editor (some local changes) and ask for confirmation
                    if (node instanceof DBNDatabaseFolder && !(node.getParentNode() instanceof DBNDatabaseFolder) && node.getParentNode() instanceof DBNDatabaseNode) {
                        // USe parent if this node is a folder
                        node = node.getParentNode();
                    }

//                    if (!showConfirmation(node)) {
//                        continue;
//                    }
                    setName("Refresh '" + node.getNodeDisplayName() + "'...");
                    try {
                        DBNNode refreshed = node.refreshNode(monitor, DBNEvent.FORCE_REFRESH);
                        if (refreshed != null) {
                            refreshedSet.add(refreshed);
                            Throwable lastLoadError = refreshed.getLastLoadError();
                            if (lastLoadError != null) {
                                throw lastLoadError;
                            }
                        }
                    }
                    catch (Throwable ex) {
                        if (node instanceof DBNDataSource) {
                            try {
                                log.info("Unable to refresh datasource, disconnecting");
                                ((DBNDataSource) node).getDataSourceContainer().disconnect(monitor);
                            } catch (DBException e) {
                                log.warn("Unable to disconnect from datasource");
                            }
                        }
                        DBWorkbench.getPlatformUI().showError("Refresh", "Error refreshing node", ex);
                    }
                    monitor.worked(1);
                }
                monitor.done();
                return Status.OK_STATUS;
            }
        };
        refreshJob.setUser(true);
        refreshJob.schedule();

        return true;
    }

    private static boolean showConfirmation(DBNNode node) {
        return new UIConfirmation() {
            @Override
            protected Boolean runTask() {
                IEditorPart nodeEditor = NavigatorHandlerObjectOpen.findEntityEditor(UIUtils.getActiveWorkbenchWindow(), node);
                if (nodeEditor != null && nodeEditor.isDirty()) {
                    return ConfirmationDialog.confirmAction(
                        null,
                        NavigatorPreferences.CONFIRM_ENTITY_REVERT,
                        ConfirmationDialog.QUESTION,
                        nodeEditor.getTitle()) == IDialogConstants.YES_ID;
                } else {
                    return true;
                }
            }
        }.execute();
    }
}

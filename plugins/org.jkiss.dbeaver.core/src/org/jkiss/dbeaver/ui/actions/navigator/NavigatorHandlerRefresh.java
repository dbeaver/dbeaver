/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNEvent;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.ui.DBUserInterface;
import org.jkiss.dbeaver.ui.IRefreshablePart;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.ui.navigator.INavigatorModelView;

import java.util.*;

public class NavigatorHandlerRefresh extends AbstractHandler {

    private static final Log log = Log.getLog(NavigatorHandlerRefresh.class);

    public NavigatorHandlerRefresh() {

    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        //final IWorkbenchWindow workbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
        final IWorkbenchPart workbenchPart = HandlerUtil.getActivePart(event);
        INavigatorModelView navigatorView;
        if (workbenchPart instanceof INavigatorModelView) {
            navigatorView = (INavigatorModelView) workbenchPart;
        } else {
            navigatorView = workbenchPart.getAdapter(INavigatorModelView.class);
        }
        if (navigatorView == null) {
            // Try to refresh as refreshable part
            if (workbenchPart instanceof IRefreshablePart) {
                ((IRefreshablePart) workbenchPart).refreshPart(this, true);
            }
            return null;
        }
        final List<DBNNode> refreshObjects = new ArrayList<>();
        final ISelection selection = HandlerUtil.getCurrentSelection(event);

        DBNNode rootNode = navigatorView.getRootNode();
        if (rootNode == null) {
            if (workbenchPart instanceof IEditorPart) {
                if (((IEditorPart) workbenchPart).getEditorInput() instanceof IDatabaseEditorInput) {
                    rootNode = ((IDatabaseEditorInput) ((IEditorPart) workbenchPart).getEditorInput()).getNavigatorNode();
                }
            }
        }
        if (rootNode != null && rootNode.getParentNode() instanceof DBNDatabaseNode) {
            refreshObjects.add(rootNode);
        } else if (selection instanceof IStructuredSelection) {
            final IStructuredSelection structSelection = (IStructuredSelection)selection;

            for (Iterator<?> iter = structSelection.iterator(); iter.hasNext(); ){
                Object object = iter.next();
                if (object instanceof DBNNode) {
                    refreshObjects.add((DBNNode) object);
                }
            }
        }

        // Refresh objects
        if (!refreshObjects.isEmpty()) {
            refreshNavigator(refreshObjects);
        }

        return null;
    }

    public static void refreshNavigator(final Collection<? extends DBNNode> refreshObjects)
    {
        Job refreshJob = new AbstractJob("Refresh navigator object(s)") {
            public Throwable error;

            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                addJobChangeListener(new JobChangeAdapter() {
                    @Override
                    public void done(IJobChangeEvent event) {
                        if (error != null) {
                            DBUserInterface.getInstance().showError("Refresh", "Error refreshing node", error);
                        }
                    }
                });
                Set<DBNNode> refreshedSet = new HashSet<>();
                for (DBNNode node : refreshObjects) {
                    if (node.isDisposed() || node.isLocked()) {
                        // Skip locked nodes
                        continue;
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
                    setName("Refresh '" + node.getNodeName() + "'...");
                    try {
                        DBNNode refreshed = node.refreshNode(monitor, DBNEvent.FORCE_REFRESH);
                        if (refreshed != null) {
                            refreshedSet.add(refreshed);
                        }
                    }
                    catch (Throwable ex) {
                        error = ex;
                    }
                }
                return Status.OK_STATUS;
            }
        };
        refreshJob.setUser(true);
        refreshJob.schedule();
    }
}

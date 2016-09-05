/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.IRefreshablePart;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.IDatabaseEditorInput;
import org.jkiss.dbeaver.ui.navigator.INavigatorModelView;

import java.util.*;

public class NavigatorHandlerRefresh extends AbstractHandler {

    private static final Log log = Log.getLog(NavigatorHandlerRefresh.class);

    public static final Object FORCE_REFRESH = new Object();

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
                            UIUtils.showErrorDialog(null, "Refresh", "Error refreshing node", error);
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
                        DBNNode refreshed = node.refreshNode(monitor, FORCE_REFRESH);
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

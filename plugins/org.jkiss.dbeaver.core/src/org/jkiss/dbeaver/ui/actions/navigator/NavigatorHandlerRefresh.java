/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.ui.INavigatorModelView;
import org.jkiss.dbeaver.ext.ui.IRefreshablePart;
import org.jkiss.dbeaver.model.navigator.DBNDatabaseNode;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.AbstractJob;

import java.util.*;

public class NavigatorHandlerRefresh extends AbstractHandler {

    static final Log log = LogFactory.getLog(NavigatorHandlerRefresh.class);

    public NavigatorHandlerRefresh() {

    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
        //final IWorkbenchWindow workbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
        final IWorkbenchPart workbenchPart = HandlerUtil.getActivePart(event);
        if (!(workbenchPart instanceof INavigatorModelView)) {
            // Try to refresh as refreshable part
            if (workbenchPart instanceof IRefreshablePart) {
                ((IRefreshablePart) workbenchPart).refreshPart(this, true);
            }
            return null;
        }
        final INavigatorModelView navigatorView = (INavigatorModelView)workbenchPart;
        final List<DBNNode> refreshObjects = new ArrayList<DBNNode>();
        final ISelection selection = HandlerUtil.getCurrentSelection(event);

        DBNNode rootNode = navigatorView.getRootNode();
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
            @Override
            protected IStatus run(DBRProgressMonitor monitor) {

                Set<DBNNode> refreshedSet = new HashSet<DBNNode>();
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
                        DBNNode refreshed = node.refreshNode(monitor, this);
                        if (refreshed != null) {
                            refreshedSet.add(refreshed);
                        }
                    }
                    catch (DBException ex) {
                        log.error("Could not refresh navigator node", ex);
                    }
                }
                return Status.OK_STATUS;
            }
        };
        refreshJob.setUser(true);
        refreshJob.schedule();
    }
}

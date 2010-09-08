/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.handlers;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.ui.INavigatorModelView;
import org.jkiss.dbeaver.model.navigator.DBNNode;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.utils.DBeaverUtils;
import org.jkiss.dbeaver.utils.ViewUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class NavigatorRefreshHandler extends AbstractHandler {

    static final Log log = LogFactory.getLog(NavigatorRefreshHandler.class);

    public NavigatorRefreshHandler() {

    }

    public Object execute(ExecutionEvent event) throws ExecutionException {
        final IWorkbenchWindow workbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
        final IWorkbenchPart workbenchPart = HandlerUtil.getActivePart(event);
        if (!(workbenchPart instanceof INavigatorModelView)) {
            return null;
        }
        final INavigatorModelView navigatorView = (INavigatorModelView)workbenchPart;
        final List<DBNNode> refreshObjects = new ArrayList<DBNNode>();
        final ISelection selection = HandlerUtil.getCurrentSelection(event);

        DBNNode rootNode = navigatorView.getRootNode();
        if (rootNode != null && rootNode.getParentNode() != null) {
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
            try {
                DBeaverUtils.run(workbenchWindow, true, true, new DBRRunnableWithProgress() {
                    public void run(DBRProgressMonitor monitor)
                        throws InvocationTargetException, InterruptedException
                    {
                        for (DBNNode node : refreshObjects) {
                            if (node.isLocked()) {
                                // Skip locked nodes
                                continue;
                            }
                            try {
                                node.refreshNode(monitor);
                            }
                            catch (DBException ex) {
                                log.error("Could not refresh navigator node", ex);
                            }
                        }
                    }
                });
            } catch (InvocationTargetException e) {
                DBeaverUtils.showErrorDialog(
                    workbenchWindow.getShell(),
                    "Refresh", "Could not refresh navigator object(s)", e.getTargetException());
            } catch (InterruptedException e) {
                log.debug(e);
            }
        }

        return null;
    }
}
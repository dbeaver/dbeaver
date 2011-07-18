/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.internal.WorkbenchPage;
import org.jkiss.dbeaver.ui.actions.DataSourceHandler;
import org.jkiss.dbeaver.ui.dialogs.SearchObjectsDialog;

@SuppressWarnings("restriction")
public class NavigatorHandlerObjectFind extends DataSourceHandler {

    public Object execute(ExecutionEvent event) throws ExecutionException {
        WorkbenchPage activePage = (WorkbenchPage)HandlerUtil.getActiveWorkbenchWindow(event).getActivePage();
        if (activePage == null) {
            return null;
        }

        final Shell activeShell = HandlerUtil.getActiveShell(event);
        SearchObjectsDialog.open(
            activeShell,
            getDataSourceContainer(event, true, false));

        return null;
    }

}
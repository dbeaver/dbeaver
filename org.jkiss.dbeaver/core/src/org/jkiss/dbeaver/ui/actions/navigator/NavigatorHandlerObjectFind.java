/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ui.actions.DataSourceHandler;
import org.jkiss.dbeaver.ui.dialogs.search.FindObjectsDialog;

public class NavigatorHandlerObjectFind extends DataSourceHandler {

    public Object execute(ExecutionEvent event) throws ExecutionException {
        FindObjectsDialog dialog = new FindObjectsDialog(
            HandlerUtil.getActivePart(event),
            getDataSourceContainer(event, false, false));
        dialog.open();
        return null;
    }

}
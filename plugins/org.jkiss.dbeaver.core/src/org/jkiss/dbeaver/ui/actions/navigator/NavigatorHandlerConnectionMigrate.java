/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardDialog;
import org.jkiss.dbeaver.ui.dialogs.connection.MigrateConnectionWizard;

public class NavigatorHandlerConnectionMigrate extends AbstractHandler {

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {

        IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindow(event);
        final ISelection currentSelection = HandlerUtil.getCurrentSelection(event);
        ActiveWizardDialog dialog = new ActiveWizardDialog(
            window,
            new MigrateConnectionWizard(
                DBeaverCore.getInstance().getProjectRegistry().getActiveDataSourceRegistry(),
                currentSelection instanceof IStructuredSelection ? (IStructuredSelection) currentSelection : null));
        dialog.open();

        return null;
    }
}
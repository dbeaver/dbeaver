/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.actions.navigator;

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardDialog;
import org.jkiss.dbeaver.ui.export.project.ProjectCreateWizard;

public class NavigatorHandlerProjectCreate extends NavigatorHandlerObjectBase {

    public Object execute(ExecutionEvent event) throws ExecutionException {
        ProjectCreateWizard wizard = new ProjectCreateWizard();
        wizard.init(
            HandlerUtil.getActiveWorkbenchWindow(event).getWorkbench(),
            (IStructuredSelection) HandlerUtil.getCurrentSelection(event));
        ActiveWizardDialog dialog = new ActiveWizardDialog(
            HandlerUtil.getActiveShell(event),
            wizard);
        dialog.open();

        return null;
    }

}
/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardDialog;
import org.jkiss.dbeaver.ui.export.wizard.DataExportWizard;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DataExportHandler extends AbstractHandler {

    public Object execute(ExecutionEvent event) throws ExecutionException {
        final IWorkbenchWindow workbenchWindow = HandlerUtil.getActiveWorkbenchWindow(event);
        final ISelection selection = HandlerUtil.getCurrentSelection(event);
        if (!(selection instanceof IStructuredSelection)) {
            return null;
        }
        IStructuredSelection ss = (IStructuredSelection)selection;
        final List<DBSDataContainer> dataProviders = new ArrayList<DBSDataContainer>();
        for (Iterator<?> iter = ss.iterator(); iter.hasNext(); ) {
            Object object = iter.next();

            final DBSDataContainer adapted = (DBSDataContainer)Platform.getAdapterManager().getAdapter(object, DBSDataContainer.class);
            if (adapted != null) {
                dataProviders.add(adapted);
            }
        }

        // Refresh objects
        if (!dataProviders.isEmpty()) {
            DataExportWizard wizard = new DataExportWizard(dataProviders);
            wizard.init(workbenchWindow.getWorkbench(), (IStructuredSelection) selection);
            ActiveWizardDialog dialog = new ActiveWizardDialog(workbenchWindow.getShell(), wizard);
            dialog.open();
        }

        return null;
    }
}
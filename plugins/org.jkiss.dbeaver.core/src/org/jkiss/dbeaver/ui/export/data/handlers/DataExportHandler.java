/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.export.data.handlers;

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
import org.jkiss.dbeaver.ui.export.data.wizard.DataExportProvider;
import org.jkiss.dbeaver.ui.export.data.wizard.DataExportWizard;

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
        final List<DataExportProvider> dataProviders = new ArrayList<DataExportProvider>();
        for (Iterator<?> iter = ss.iterator(); iter.hasNext(); ) {
            Object object = iter.next();

            final DBSDataContainer adapted = (DBSDataContainer)Platform.getAdapterManager().getAdapter(object, DBSDataContainer.class);
            if (adapted != null) {
                dataProviders.add(new DataExportProvider(adapted));
            }
        }

        // Refresh objects
        if (!dataProviders.isEmpty()) {
            ActiveWizardDialog dialog = new ActiveWizardDialog(
                workbenchWindow,
                new DataExportWizard(dataProviders));
            dialog.open();
        }

        return null;
    }
}
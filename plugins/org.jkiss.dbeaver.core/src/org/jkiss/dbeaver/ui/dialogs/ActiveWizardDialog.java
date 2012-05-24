/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWizard;

/**
 * NewConnectionDialog
 */
public class ActiveWizardDialog extends WizardDialog
{
    public ActiveWizardDialog(IWorkbenchWindow window, IWizard wizard)
    {
        this(window, wizard, null);
    }

    public ActiveWizardDialog(IWorkbenchWindow window, IWizard wizard, IStructuredSelection selection)
    {
        super(window.getShell(), wizard);

        // Initialize wizard
        if (wizard instanceof IWorkbenchWizard) {
            if (selection == null) {
                if (window.getSelectionService().getSelection() instanceof IStructuredSelection) {
                    selection = (IStructuredSelection)window.getSelectionService().getSelection();
                }
            }
            ((IWorkbenchWizard)wizard).init(window.getWorkbench(), selection);
        }
        addPageChangingListener(new IPageChangingListener() {
            @Override
            public void handlePageChanging(PageChangingEvent event)
            {
                if (event.getCurrentPage() instanceof ActiveWizardPage) {
                    ((ActiveWizardPage) event.getCurrentPage()).deactivatePage();
                }
//                if (event.getTargetPage() instanceof ActiveWizardPage) {
//                    ((ActiveWizardPage) event.getTargetPage()).activatePage();
//                }
            }
        });


    }

}

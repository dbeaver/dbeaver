/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.IWorkbenchWizard;
import org.jkiss.dbeaver.ext.ui.IEmbeddedPart;

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

        this.addPageChangingListener(new IPageChangingListener()
        {
            public void handlePageChanging(PageChangingEvent event)
            {
                if (event.getCurrentPage() instanceof IEmbeddedPart) {
                    ((IEmbeddedPart)event.getCurrentPage()).deactivatePart();
                }
                if (event.getTargetPage() instanceof IEmbeddedPart) {
                    ((IEmbeddedPart)event.getTargetPage()).activatePart();
                }
            }
        });
    }

    @Override
    protected void cancelPressed()
    {
        IWizardPage curPage = getCurrentPage();
        if (curPage instanceof IEmbeddedPart) {
            ((IEmbeddedPart) curPage).deactivatePart();
        }
        super.cancelPressed();
    }

    @Override
    protected void finishPressed()
    {
        IWizardPage curPage = getCurrentPage();
        if (curPage instanceof IEmbeddedPart) {
            ((IEmbeddedPart) curPage).deactivatePart();
        }
        super.finishPressed();
    }
}

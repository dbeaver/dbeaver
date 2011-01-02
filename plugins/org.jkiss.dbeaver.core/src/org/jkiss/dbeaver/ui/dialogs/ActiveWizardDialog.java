/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.jface.dialogs.IPageChangingListener;
import org.eclipse.jface.dialogs.PageChangingEvent;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.ext.ui.IEmbeddedPart;

/**
 * NewConnectionDialog
 */
public class ActiveWizardDialog extends WizardDialog
{
    public ActiveWizardDialog(Shell shell, IWizard wizard)
    {
        super(shell, wizard);

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

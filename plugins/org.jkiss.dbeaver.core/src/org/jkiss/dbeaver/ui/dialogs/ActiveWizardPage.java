/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardPage;

/**
 * ActiveWizardPage
 */
public abstract class ActiveWizardPage<WIZARD extends IWizard> extends WizardPage
{
    protected ActiveWizardPage(String pageName) {
        super(pageName);
    }

    @Override
    public WIZARD getWizard() {
        return (WIZARD)super.getWizard();
    }

    /**
     * Determine if the page is complete and update the page appropriately.
     */
    protected void updatePageCompletion() {
        boolean pageComplete = determinePageCompletion();
        setPageComplete(pageComplete);
        if (pageComplete) {
            setErrorMessage(null);
        }
    }

    protected boolean determinePageCompletion() {
        return false;
    }

    @Override
    public void setVisible(boolean visible)
    {
        if (visible) {
            activatePage();
        } else {
            deactivatePage();
        }
        super.setVisible(visible);
    }

    public void activatePage() {

    }

    public void deactivatePage() {

    }
}

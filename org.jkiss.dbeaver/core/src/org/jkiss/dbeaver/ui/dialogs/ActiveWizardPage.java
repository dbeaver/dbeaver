/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.jface.wizard.WizardPage;
import org.jkiss.dbeaver.ext.ui.IEmbeddedPart;

/**
 * ActiveWizardPage
 */
public abstract class ActiveWizardPage<WIZARD extends IWizard> extends WizardPage implements IEmbeddedPart
{
    protected ActiveWizardPage(String pageName) {
        super(pageName);
    }

    protected ActiveWizardPage(String pageName, String title, ImageDescriptor titleImage) {
        super(pageName, title, titleImage);
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

    public void activatePart() {

    }

    public void deactivatePart() {

    }
}

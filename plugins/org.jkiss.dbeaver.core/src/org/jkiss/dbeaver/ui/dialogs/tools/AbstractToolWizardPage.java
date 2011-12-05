/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.tools;

import org.eclipse.jface.wizard.WizardPage;

public abstract class AbstractToolWizardPage<WIZARD extends AbstractToolWizard>extends WizardPage {

    protected final WIZARD wizard;

    protected AbstractToolWizardPage(WIZARD wizard, String pageName)
    {
        super(pageName);
        this.wizard = wizard;
    }

    @Override
    public boolean isPageComplete()
    {
        return wizard.getClientHome() != null;
    }

}

/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.tools;

import org.eclipse.jface.wizard.WizardPage;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;

abstract class AbstractToolWizardPage<T extends AbstractToolWizard> extends WizardPage {

    protected final T wizard;

    protected AbstractToolWizardPage(T wizard, String pageName)
    {
        super(pageName);
        this.wizard = wizard;
    }

    public MySQLCatalog getCatalog()
    {
        return wizard.getCatalog();
    }

    @Override
    public boolean isPageComplete()
    {
        return wizard.getServerHome() != null;
    }

}

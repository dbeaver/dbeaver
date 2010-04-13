/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbench;

/**
 * PrefPageMain
 */
public class PrefPageMain extends PreferencePage implements IWorkbenchPreferencePage
{
    public PrefPageMain()
    {
        super();
    }

    protected Control createContents(Composite parent)
    {
        super.noDefaultAndApplyButton();
        return parent;
    }

    public void init(IWorkbench workbench)
    {
    }
}

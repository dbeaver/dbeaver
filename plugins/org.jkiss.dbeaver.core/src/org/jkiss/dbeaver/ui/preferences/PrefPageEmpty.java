/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

/**
 * PrefPageEmpty
 */
public class PrefPageEmpty extends PreferencePage implements IWorkbenchPreferencePage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.ui.preferences.PrefPageEmpty";

    public PrefPageEmpty()
    {
        super();
    }

    @Override
    protected Control createContents(Composite parent)
    {
        super.noDefaultAndApplyButton();
        return parent;
    }

    @Override
    public void init(IWorkbench workbench)
    {
    }
}

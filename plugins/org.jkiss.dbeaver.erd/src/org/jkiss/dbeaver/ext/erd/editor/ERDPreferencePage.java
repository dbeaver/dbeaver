/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.editor;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.draw2d.GridData;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * ERDPreferencePage
 */
public class ERDPreferencePage extends PreferencePage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage {

    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.erd.general";
    private IAdaptable element;

    protected Control createContents(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        Group printGroup = UIUtils.createControlGroup(composite, "Print", 2, GridData.BEGINNING, 0);
        Combo modeCombo = UIUtils.createLabelCombo(printGroup, "Page Mode", SWT.READ_ONLY | SWT.DROP_DOWN);
        modeCombo.add("Tile");
        modeCombo.add("Fit Page");
        modeCombo.add("Fit Width");
        modeCombo.add("Fit Height");
        modeCombo.select(0);

        UIUtils.createLabelSpinner(printGroup, "Margin Top", 0, 0, Short.MAX_VALUE);
        UIUtils.createLabelSpinner(printGroup, "Margin Bottom", 0, 0, Short.MAX_VALUE);
        UIUtils.createLabelSpinner(printGroup, "Margin Left", 0, 0, Short.MAX_VALUE);
        UIUtils.createLabelSpinner(printGroup, "Margin Right", 0, 0, Short.MAX_VALUE);

        return composite;
    }


/* (non-Javadoc)
 * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
 */

    public void init(IWorkbench workbench)
    {
    }


    protected void performDefaults()
    {
        super.performDefaults();
    }

    public boolean performOk()
    {
        IPreferenceStore store = DBeaverCore.getInstance().getGlobalPreferenceStore();

        return true;
    }

    public IAdaptable getElement()
    {
        return element;
    }

    public void setElement(IAdaptable element)
    {
        this.element = element;
    }
}
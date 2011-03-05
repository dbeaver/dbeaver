/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.erd.editor;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.draw2d.GridData;
import org.eclipse.draw2d.PrintFigureOperation;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.erd.ERDConstants;
import org.jkiss.dbeaver.ui.UIUtils;

/**
 * ERDPreferencePage
 */
public class ERDPreferencePage extends PreferencePage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage {

    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.erd.general";
    private IAdaptable element;
    private Combo modeCombo;
    private Spinner spinnerMarginTop;
    private Spinner spinnerMarginBottom;
    private Spinner spinnerMarginLeft;
    private Spinner spinnerMarginRight;

    protected Control createContents(Composite parent)
    {
        IPreferenceStore store = DBeaverCore.getInstance().getGlobalPreferenceStore();

        Composite composite = UIUtils.createPlaceholder(parent, 1);

        Group printGroup = UIUtils.createControlGroup(composite, "Print", 2, GridData.BEGINNING, 0);
        modeCombo = UIUtils.createLabelCombo(printGroup, "Page Mode", SWT.READ_ONLY | SWT.DROP_DOWN);
        modeCombo.add("Tile");
        modeCombo.add("Fit Page");
        modeCombo.add("Fit Width");
        modeCombo.add("Fit Height");
        int modeIndex = 0;
        switch (store.getInt(ERDConstants.PREF_PRINT_PAGE_MODE)) {
            case PrintFigureOperation.FIT_PAGE: modeIndex = 1; break;
            case PrintFigureOperation.FIT_WIDTH: modeIndex = 2; break;
            case PrintFigureOperation.FIT_HEIGHT: modeIndex = 3; break;
        }
        modeCombo.select(modeIndex);

        spinnerMarginTop = UIUtils.createLabelSpinner(printGroup, "Margin Top", store.getInt(ERDConstants.PREF_PRINT_MARGIN_TOP), 0, Short.MAX_VALUE);
        spinnerMarginBottom = UIUtils.createLabelSpinner(printGroup, "Margin Bottom", store.getInt(ERDConstants.PREF_PRINT_MARGIN_BOTTOM), 0, Short.MAX_VALUE);
        spinnerMarginLeft = UIUtils.createLabelSpinner(printGroup, "Margin Left", store.getInt(ERDConstants.PREF_PRINT_MARGIN_LEFT), 0, Short.MAX_VALUE);
        spinnerMarginRight = UIUtils.createLabelSpinner(printGroup, "Margin Right", store.getInt(ERDConstants.PREF_PRINT_MARGIN_RIGHT), 0, Short.MAX_VALUE);

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

        int pageMode;
        switch (modeCombo.getSelectionIndex()) {
            case 1: pageMode = PrintFigureOperation.FIT_PAGE; break;
            case 2: pageMode = PrintFigureOperation.FIT_WIDTH; break;
            case 3: pageMode = PrintFigureOperation.FIT_HEIGHT; break;
            default: pageMode = PrintFigureOperation.TILE; break;
        }
        store.setValue(ERDConstants.PREF_PRINT_PAGE_MODE, pageMode);

        store.setValue(ERDConstants.PREF_PRINT_MARGIN_TOP, spinnerMarginTop.getSelection());
        store.setValue(ERDConstants.PREF_PRINT_MARGIN_BOTTOM, spinnerMarginBottom.getSelection());
        store.setValue(ERDConstants.PREF_PRINT_MARGIN_LEFT, spinnerMarginLeft.getSelection());
        store.setValue(ERDConstants.PREF_PRINT_MARGIN_RIGHT, spinnerMarginRight.getSelection());

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
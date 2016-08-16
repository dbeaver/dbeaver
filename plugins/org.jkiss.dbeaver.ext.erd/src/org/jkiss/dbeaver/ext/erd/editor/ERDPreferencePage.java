/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.erd.editor;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.draw2d.PrintFigureOperation;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.dbeaver.ext.erd.ERDActivator;
import org.jkiss.dbeaver.ext.erd.ERDConstants;
import org.jkiss.dbeaver.ext.erd.ERDMessages;
import org.jkiss.dbeaver.model.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.utils.ArrayUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * ERDPreferencePage
 */
public class ERDPreferencePage extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage {

    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.erd.general"; //$NON-NLS-1$
    private IAdaptable element;
    private Combo modeCombo;
    private Spinner spinnerMarginTop;
    private Spinner spinnerMarginBottom;
    private Spinner spinnerMarginLeft;
    private Spinner spinnerMarginRight;
    private Button gridCheck;
    private Button snapCheck;
    private Spinner spinnerGridWidth;
    private Spinner spinnerGridHeight;
    private List<Button> visibilityButtons = new ArrayList<>();
    private List<Button> styleButtons = new ArrayList<>();

    @Override
    protected Control createContents(Composite parent)
    {
        IPreferenceStore store = ERDActivator.getDefault().getPreferenceStore();

        Composite composite = UIUtils.createPlaceholder(parent, 2, 5);

        createGridGroup(store, composite);
        createPrintGroup(store, composite);
        createVisibilityGroup(store, composite);
        createStyleGroup(store, composite);

        return composite;
    }

    private void createGridGroup(IPreferenceStore store, Composite composite)
    {
        Group gridGroup = UIUtils.createControlGroup(composite, ERDMessages.pref_page_erd_group_grid, 2, GridData.VERTICAL_ALIGN_BEGINNING, 0);
        gridCheck = UIUtils.createLabelCheckbox(gridGroup, ERDMessages.pref_page_erd_checkbox_grid_enabled, store.getBoolean(ERDConstants.PREF_GRID_ENABLED));
        snapCheck = UIUtils.createLabelCheckbox(gridGroup, ERDMessages.pref_page_erd_checkbox_snap_to_grid, store.getBoolean(ERDConstants.PREF_GRID_SNAP_ENABLED));

        spinnerGridWidth = UIUtils.createLabelSpinner(gridGroup, ERDMessages.pref_page_erd_spinner_grid_width, store.getInt(ERDConstants.PREF_GRID_WIDTH), 5, Short.MAX_VALUE);
        spinnerGridHeight = UIUtils.createLabelSpinner(gridGroup, ERDMessages.pref_page_erd_spinner_grid_height, store.getInt(ERDConstants.PREF_GRID_HEIGHT), 5, Short.MAX_VALUE);
    }

    private void createPrintGroup(IPreferenceStore store, Composite composite)
    {
        Group printGroup = UIUtils.createControlGroup(composite, ERDMessages.pref_page_erd_group_print, 2, GridData.VERTICAL_ALIGN_BEGINNING, 0);
        modeCombo = UIUtils.createLabelCombo(printGroup, ERDMessages.pref_page_erd_combo_page_mode, SWT.READ_ONLY | SWT.DROP_DOWN);
        modeCombo.add(ERDMessages.pref_page_erd_item_tile);
        modeCombo.add(ERDMessages.pref_page_erd_item_fit_page);
        modeCombo.add(ERDMessages.pref_page_erd_item_fit_width);
        modeCombo.add(ERDMessages.pref_page_erd_item_fit_height);
        int modeIndex = 0;
        switch (store.getInt(ERDConstants.PREF_PRINT_PAGE_MODE)) {
            case PrintFigureOperation.FIT_PAGE: modeIndex = 1; break;
            case PrintFigureOperation.FIT_WIDTH: modeIndex = 2; break;
            case PrintFigureOperation.FIT_HEIGHT: modeIndex = 3; break;
        }
        modeCombo.select(modeIndex);

        spinnerMarginTop = UIUtils.createLabelSpinner(printGroup, ERDMessages.pref_page_erd_spinner_margin_top, store.getInt(ERDConstants.PREF_PRINT_MARGIN_TOP), 0, Short.MAX_VALUE);
        spinnerMarginBottom = UIUtils.createLabelSpinner(printGroup, ERDMessages.pref_page_erd_spinner_margin_bottom, store.getInt(ERDConstants.PREF_PRINT_MARGIN_BOTTOM), 0, Short.MAX_VALUE);
        spinnerMarginLeft = UIUtils.createLabelSpinner(printGroup, ERDMessages.pref_page_erd_spinner_margin_left, store.getInt(ERDConstants.PREF_PRINT_MARGIN_LEFT), 0, Short.MAX_VALUE);
        spinnerMarginRight = UIUtils.createLabelSpinner(printGroup, ERDMessages.pref_page_erd_spinner_margin_right, store.getInt(ERDConstants.PREF_PRINT_MARGIN_RIGHT), 0, Short.MAX_VALUE);
    }

    private void createVisibilityGroup(IPreferenceStore store, Composite composite)
    {
        ERDAttributeVisibility defaultVisibility = ERDAttributeVisibility.getDefaultVisibility(store);

        Group elemsGroup = UIUtils.createControlGroup(composite, "Attributes visibility", 1, GridData.VERTICAL_ALIGN_BEGINNING, 0);
        elemsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        for (ERDAttributeVisibility visibility : ERDAttributeVisibility.values()) {
            Button radio = new Button(elemsGroup, SWT.RADIO);
            radio.setData(visibility);
            radio.setText(visibility.getTitle());
            if (visibility == defaultVisibility) {
                radio.setSelection(true);
            }
            visibilityButtons.add(radio);
        }
    }

    private void createStyleGroup(IPreferenceStore store, Composite composite)
    {
        ERDAttributeStyle[] enabledStyles = ERDAttributeStyle.getDefaultStyles(store);

        Group elemsGroup = UIUtils.createControlGroup(composite, "Attribute styles", 1, GridData.VERTICAL_ALIGN_BEGINNING, 0);
        elemsGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
        for (ERDAttributeStyle style : ERDAttributeStyle.values()) {
            Button check = new Button(elemsGroup, SWT.CHECK);
            check.setData(style);
            check.setText(style.getTitle());
            if (ArrayUtils.contains(enabledStyles, style)) {
                check.setSelection(true);
            }
            styleButtons.add(check);
        }
    }

    @Override
    public void init(IWorkbench workbench)
    {
    }

    @Override
    protected void performDefaults()
    {
        super.performDefaults();
    }

    @Override
    public boolean performOk()
    {
        DBPPreferenceStore store = ERDActivator.getDefault().getPreferences();

        store.setValue(ERDConstants.PREF_GRID_ENABLED, gridCheck.getSelection());
        store.setValue(ERDConstants.PREF_GRID_SNAP_ENABLED, snapCheck.getSelection());
        store.setValue(ERDConstants.PREF_GRID_WIDTH, spinnerGridWidth.getSelection());
        store.setValue(ERDConstants.PREF_GRID_HEIGHT, spinnerGridHeight.getSelection());

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

        for (Button radio : visibilityButtons) {
            if (radio.getSelection()) {
                ERDAttributeVisibility.setDefaultVisibility(store, (ERDAttributeVisibility) radio.getData());
            }
        }
        List<ERDAttributeStyle> enabledStyles = new ArrayList<>();
        for (Button check : styleButtons) {
            if (check.getSelection()) {
                enabledStyles.add((ERDAttributeStyle) check.getData());
            }
        }
        ERDAttributeStyle.setDefaultStyles(store, enabledStyles.toArray(new ERDAttributeStyle[enabledStyles.size()]));

        PrefUtils.savePreferenceStore(store);

        return true;
    }

    @Override
    public IAdaptable getElement()
    {
        return element;
    }

    @Override
    public void setElement(IAdaptable element)
    {
        this.element = element;
    }

}
/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.erd.ui.editor;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.draw2dl.PrintFigureOperation;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.dbeaver.erd.model.ERDAttributeVisibility;
import org.jkiss.dbeaver.erd.ui.ERDUIConstants;
import org.jkiss.dbeaver.erd.ui.internal.ERDUIActivator;
import org.jkiss.dbeaver.erd.ui.internal.ERDUIMessages;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
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

    private Button contentsShowViews;
    private Button contentsShowPartitions;
    private Button changeBorderColors;
    private Button changeHeaderColors;

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
        DBPPreferenceStore store = ERDUIActivator.getDefault().getPreferences();

        Composite composite = UIUtils.createPlaceholder(parent, 2, 5);

        createContentsGroup(store, composite);
        createColorPrefGroup(store, composite);
        createVisibilityGroup(store, composite);
        createStyleGroup(store, composite);

        createGridGroup(store, composite);
        createPrintGroup(store, composite);

        return composite;
    }

    private void createContentsGroup(DBPPreferenceStore store, Composite composite)
    {
        Group contentsGroup = UIUtils.createControlGroup(composite, ERDUIMessages.erd_preference_page_title_diagram_contents, 1, GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL, 0);
        //((GridData)contentsGroup.getLayoutData()).horizontalSpan = 2;
        contentsShowViews = UIUtils.createCheckbox(contentsGroup, ERDUIMessages.erd_preference_page_title_shows_views, store.getBoolean(ERDUIConstants.PREF_DIAGRAM_SHOW_VIEWS));
        contentsShowPartitions = UIUtils.createCheckbox(contentsGroup, ERDUIMessages.erd_preference_page_title_shows_partitions, store.getBoolean(ERDUIConstants.PREF_DIAGRAM_SHOW_PARTITIONS));
    }

    private void createColorPrefGroup(DBPPreferenceStore store, Composite composite) {
        Group contentsGroup = UIUtils.createControlGroup(composite, ERDUIMessages.erd_preference_page_title_color_pref, 1, GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL, 0);
        //((GridData)contentsGroup.getLayoutData()).horizontalSpan = 2;
        changeBorderColors = UIUtils.createCheckbox(contentsGroup, ERDUIMessages.erd_preference_page_title_change_border_colors, store.getBoolean(ERDUIConstants.PREF_DIAGRAM_CHANGE_BORDER_COLORS));
        changeHeaderColors = UIUtils.createCheckbox(contentsGroup, ERDUIMessages.erd_preference_page_title_change_header_colors, store.getBoolean(ERDUIConstants.PREF_DIAGRAM_CHANGE_HEADER_COLORS));
    }

    private void createVisibilityGroup(DBPPreferenceStore store, Composite composite)
    {
        ERDAttributeVisibility defaultVisibility = ERDAttributeVisibility.getDefaultVisibility(store);

        Group elemsGroup = UIUtils.createControlGroup(composite, ERDUIMessages.erd_preference_page_title_attributes_visibility, 1, GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL, 0);
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

    private void createStyleGroup(DBPPreferenceStore store, Composite composite)
    {
        ERDViewStyle[] enabledStyles = ERDViewStyle.getDefaultStyles(store);

        Group elemsGroup = UIUtils.createControlGroup(composite, ERDUIMessages.erd_preference_page_title_attribute_style, 1, GridData.VERTICAL_ALIGN_BEGINNING | GridData.FILL_HORIZONTAL, 0);
        for (ERDViewStyle style : ERDViewStyle.values()) {
            Button check = new Button(elemsGroup, SWT.CHECK);
            check.setData(style);
            check.setText(style.getTitle());
            if (ArrayUtils.contains(enabledStyles, style)) {
                check.setSelection(true);
            }
            styleButtons.add(check);
        }
    }

    private void createGridGroup(DBPPreferenceStore store, Composite composite)
    {
        Group gridGroup = UIUtils.createControlGroup(composite, ERDUIMessages.pref_page_erd_group_grid, 2, GridData.VERTICAL_ALIGN_BEGINNING, 0);
        gridCheck = UIUtils.createCheckbox(gridGroup, ERDUIMessages.pref_page_erd_checkbox_grid_enabled, null, store.getBoolean(ERDUIConstants.PREF_GRID_ENABLED), 2);
        snapCheck = UIUtils.createCheckbox(gridGroup, ERDUIMessages.pref_page_erd_checkbox_snap_to_grid, null, store.getBoolean(ERDUIConstants.PREF_GRID_SNAP_ENABLED), 2);

        spinnerGridWidth = UIUtils.createLabelSpinner(gridGroup, ERDUIMessages.pref_page_erd_spinner_grid_width, store.getInt(ERDUIConstants.PREF_GRID_WIDTH), 5, Short.MAX_VALUE);
        spinnerGridHeight = UIUtils.createLabelSpinner(gridGroup, ERDUIMessages.pref_page_erd_spinner_grid_height, store.getInt(ERDUIConstants.PREF_GRID_HEIGHT), 5, Short.MAX_VALUE);
    }

    private void createPrintGroup(DBPPreferenceStore store, Composite composite)
    {
        Group printGroup = UIUtils.createControlGroup(composite, ERDUIMessages.pref_page_erd_group_print, 2, GridData.VERTICAL_ALIGN_BEGINNING, 0);
        modeCombo = UIUtils.createLabelCombo(printGroup, ERDUIMessages.pref_page_erd_combo_page_mode, SWT.READ_ONLY | SWT.DROP_DOWN);
        modeCombo.add(ERDUIMessages.pref_page_erd_item_tile);
        modeCombo.add(ERDUIMessages.pref_page_erd_item_fit_page);
        modeCombo.add(ERDUIMessages.pref_page_erd_item_fit_width);
        modeCombo.add(ERDUIMessages.pref_page_erd_item_fit_height);
        int modeIndex = 0;
        switch (store.getInt(ERDUIConstants.PREF_PRINT_PAGE_MODE)) {
            case PrintFigureOperation.FIT_PAGE: modeIndex = 1; break;
            case PrintFigureOperation.FIT_WIDTH: modeIndex = 2; break;
            case PrintFigureOperation.FIT_HEIGHT: modeIndex = 3; break;
        }
        modeCombo.select(modeIndex);

        spinnerMarginTop = UIUtils.createLabelSpinner(printGroup, ERDUIMessages.pref_page_erd_spinner_margin_top, store.getInt(ERDUIConstants.PREF_PRINT_MARGIN_TOP), 0, Short.MAX_VALUE);
        spinnerMarginBottom = UIUtils.createLabelSpinner(printGroup, ERDUIMessages.pref_page_erd_spinner_margin_bottom, store.getInt(ERDUIConstants.PREF_PRINT_MARGIN_BOTTOM), 0, Short.MAX_VALUE);
        spinnerMarginLeft = UIUtils.createLabelSpinner(printGroup, ERDUIMessages.pref_page_erd_spinner_margin_left, store.getInt(ERDUIConstants.PREF_PRINT_MARGIN_LEFT), 0, Short.MAX_VALUE);
        spinnerMarginRight = UIUtils.createLabelSpinner(printGroup, ERDUIMessages.pref_page_erd_spinner_margin_right, store.getInt(ERDUIConstants.PREF_PRINT_MARGIN_RIGHT), 0, Short.MAX_VALUE);
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
        DBPPreferenceStore store = ERDUIActivator.getDefault().getPreferences();

        store.setValue(ERDUIConstants.PREF_DIAGRAM_SHOW_VIEWS, contentsShowViews.getSelection());
        store.setValue(ERDUIConstants.PREF_DIAGRAM_SHOW_PARTITIONS, contentsShowPartitions.getSelection());
        store.setValue(ERDUIConstants.PREF_DIAGRAM_CHANGE_BORDER_COLORS, changeBorderColors.getSelection());
        store.setValue(ERDUIConstants.PREF_DIAGRAM_CHANGE_HEADER_COLORS, changeHeaderColors.getSelection());


        store.setValue(ERDUIConstants.PREF_GRID_ENABLED, gridCheck.getSelection());
        store.setValue(ERDUIConstants.PREF_GRID_SNAP_ENABLED, snapCheck.getSelection());
        store.setValue(ERDUIConstants.PREF_GRID_WIDTH, spinnerGridWidth.getSelection());
        store.setValue(ERDUIConstants.PREF_GRID_HEIGHT, spinnerGridHeight.getSelection());

        int pageMode;
        switch (modeCombo.getSelectionIndex()) {
            case 1: pageMode = PrintFigureOperation.FIT_PAGE; break;
            case 2: pageMode = PrintFigureOperation.FIT_WIDTH; break;
            case 3: pageMode = PrintFigureOperation.FIT_HEIGHT; break;
            default: pageMode = PrintFigureOperation.TILE; break;
        }
        store.setValue(ERDUIConstants.PREF_PRINT_PAGE_MODE, pageMode);

        store.setValue(ERDUIConstants.PREF_PRINT_MARGIN_TOP, spinnerMarginTop.getSelection());
        store.setValue(ERDUIConstants.PREF_PRINT_MARGIN_BOTTOM, spinnerMarginBottom.getSelection());
        store.setValue(ERDUIConstants.PREF_PRINT_MARGIN_LEFT, spinnerMarginLeft.getSelection());
        store.setValue(ERDUIConstants.PREF_PRINT_MARGIN_RIGHT, spinnerMarginRight.getSelection());

        for (Button radio : visibilityButtons) {
            if (radio.getSelection()) {
                ERDAttributeVisibility.setDefaultVisibility(store, (ERDAttributeVisibility) radio.getData());
            }
        }
        List<ERDViewStyle> enabledStyles = new ArrayList<>();
        for (Button check : styleButtons) {
            if (check.getSelection()) {
                enabledStyles.add((ERDViewStyle) check.getData());
            }
        }
        ERDViewStyle.setDefaultStyles(store, enabledStyles.toArray(new ERDViewStyle[enabledStyles.size()]));

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
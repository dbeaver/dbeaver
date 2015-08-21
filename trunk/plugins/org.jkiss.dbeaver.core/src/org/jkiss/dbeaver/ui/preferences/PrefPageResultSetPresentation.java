/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.model.DBPPreferenceStore;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.spreadsheet.Spreadsheet;
import org.jkiss.dbeaver.utils.PrefUtils;
import org.jkiss.utils.CommonUtils;

/**
 * PrefPageResultSetPresentation
 */
public class PrefPageResultSetPresentation extends TargetPrefPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.resultset.presentation"; //$NON-NLS-1$

    private Button gridShowOddRows;
    private Button gridShowCellIcons;
    private Combo gridDoubleClickBehavior;
    private Button autoSwitchMode;

    private Spinner textMaxColumnSize;

    public PrefPageResultSetPresentation()
    {
        super();
    }

    @Override
    protected boolean hasDataSourceSpecificOptions(DBSDataSourceContainer dataSourceDescriptor)
    {
        DBPPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return
            store.contains(DBeaverPreferences.RESULT_SET_SHOW_ODD_ROWS) ||
            store.contains(DBeaverPreferences.RESULT_SET_SHOW_CELL_ICONS) ||
            store.contains(DBeaverPreferences.RESULT_SET_DOUBLE_CLICK) ||
            store.contains(DBeaverPreferences.RESULT_SET_AUTO_SWITCH_MODE) ||

            store.contains(DBeaverPreferences.RESULT_TEXT_MAX_COLUMN_SIZE)
            ;
    }

    @Override
    protected boolean supportsDataSourceSpecificOptions()
    {
        return true;
    }

    @Override
    protected Control createPreferenceContent(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1, 5);

        {
            Group uiGroup = UIUtils.createControlGroup(composite, "Behavior", 2, SWT.NONE, 0);

            autoSwitchMode = UIUtils.createLabelCheckbox(uiGroup, "Switch to record/grid mode on single/multiple row(s)", false);
        }

        {
            Group uiGroup = UIUtils.createControlGroup(composite, "Grid", 2, SWT.NONE, 0);

            gridShowOddRows = UIUtils.createLabelCheckbox(uiGroup, "Mark odd/even rows", false);
            gridShowCellIcons = UIUtils.createLabelCheckbox(uiGroup, "Show cell icons", false);
            gridDoubleClickBehavior = UIUtils.createLabelCombo(uiGroup, "Double-click behavior", SWT.READ_ONLY);
            gridDoubleClickBehavior.add("None", Spreadsheet.DoubleClickBehavior.NONE.ordinal());
            gridDoubleClickBehavior.add("Editor", Spreadsheet.DoubleClickBehavior.EDITOR.ordinal());
            gridDoubleClickBehavior.add("Inline Editor", Spreadsheet.DoubleClickBehavior.INLINE_EDITOR.ordinal());
        }

        {
            Group uiGroup = UIUtils.createControlGroup(composite, "Plain text", 2, SWT.NONE, 0);

            textMaxColumnSize = UIUtils.createLabelSpinner(uiGroup, "Maximum column length", 0, 10, Integer.MAX_VALUE);
        }

        return composite;
    }

    @Override
    protected void loadPreferences(DBPPreferenceStore store)
    {
        try {
            gridShowOddRows.setSelection(store.getBoolean(DBeaverPreferences.RESULT_SET_SHOW_ODD_ROWS));
            gridShowCellIcons.setSelection(store.getBoolean(DBeaverPreferences.RESULT_SET_SHOW_CELL_ICONS));
            gridDoubleClickBehavior.select(
                Spreadsheet.DoubleClickBehavior.valueOf(store.getString(DBeaverPreferences.RESULT_SET_DOUBLE_CLICK)).ordinal());
            autoSwitchMode.setSelection(store.getBoolean(DBeaverPreferences.RESULT_SET_AUTO_SWITCH_MODE));

            textMaxColumnSize.setSelection(store.getInt(DBeaverPreferences.RESULT_TEXT_MAX_COLUMN_SIZE));
        } catch (Exception e) {
            log.warn(e);
        }
    }

    @Override
    protected void savePreferences(DBPPreferenceStore store)
    {
        try {
            store.setValue(DBeaverPreferences.RESULT_SET_SHOW_ODD_ROWS, gridShowOddRows.getSelection());
            store.setValue(DBeaverPreferences.RESULT_SET_SHOW_CELL_ICONS, gridShowCellIcons.getSelection());
            store.setValue(DBeaverPreferences.RESULT_SET_DOUBLE_CLICK, CommonUtils.fromOrdinal(Spreadsheet.DoubleClickBehavior.class, gridDoubleClickBehavior.getSelectionIndex()).name());
            store.setValue(DBeaverPreferences.RESULT_SET_AUTO_SWITCH_MODE, autoSwitchMode.getSelection());
            store.setValue(DBeaverPreferences.RESULT_TEXT_MAX_COLUMN_SIZE, textMaxColumnSize.getSelection());
        } catch (Exception e) {
            log.warn(e);
        }
        PrefUtils.savePreferenceStore(store);
    }

    @Override
    protected void clearPreferences(DBPPreferenceStore store)
    {
        store.setToDefault(DBeaverPreferences.RESULT_SET_SHOW_ODD_ROWS);
        store.setToDefault(DBeaverPreferences.RESULT_SET_SHOW_CELL_ICONS);
        store.setToDefault(DBeaverPreferences.RESULT_SET_DOUBLE_CLICK);
        store.setToDefault(DBeaverPreferences.RESULT_SET_AUTO_SWITCH_MODE);
        store.setToDefault(DBeaverPreferences.RESULT_TEXT_MAX_COLUMN_SIZE);
    }

    @Override
    public void applyData(Object data)
    {
        super.applyData(data);
    }

    @Override
    protected String getPropertyPageID()
    {
        return PAGE_ID;
    }

}
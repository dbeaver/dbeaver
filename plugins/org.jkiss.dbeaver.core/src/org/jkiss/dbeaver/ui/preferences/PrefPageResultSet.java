/*
 * Copyright (C) 2010-2015 Serge Rieder serge@jkiss.org
 * Copyright (C) 2011-2012 Eugene Fradkin eugene.fradkin@gmail.com
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDBinaryFormatter;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.spreadsheet.Spreadsheet;
import org.jkiss.dbeaver.utils.AbstractPreferenceStore;
import org.jkiss.utils.CommonUtils;

/**
 * PrefPageResultSet
 */
public class PrefPageResultSet extends TargetPrefPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.resultset"; //$NON-NLS-1$

    private Spinner resultSetSize;
    private Button resultSetUseSQLCheck;
    //private Button binaryShowStrings;
    private Combo binaryPresentationCombo;
    private Combo binaryEditorType;

    private Button keepStatementOpenCheck;
    private Button rollbackOnErrorCheck;
    private Spinner binaryStringMaxLength;
    private Spinner memoryContentSize;

    private Button serverSideOrderingCheck;

    private Button showOddRows;
    private Button showCellIcons;
    private Combo doubleClickBehavior;
    private Button autoSwitchMode;

    public PrefPageResultSet()
    {
        super();
//        setPreferenceStore(DBeaverCore.getGlobalPreferenceStore());
    }

    @Override
    protected boolean hasDataSourceSpecificOptions(DataSourceDescriptor dataSourceDescriptor)
    {
        AbstractPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return
            store.contains(DBeaverPreferences.RESULT_SET_MAX_ROWS) ||
            store.contains(DBeaverPreferences.RESULT_SET_MAX_ROWS_USE_SQL) ||
            store.contains(DBeaverPreferences.QUERY_ROLLBACK_ON_ERROR) ||
            store.contains(DBeaverPreferences.KEEP_STATEMENT_OPEN) ||
            store.contains(DBeaverPreferences.MEMORY_CONTENT_MAX_SIZE) ||
            store.contains(DBeaverPreferences.RESULT_SET_BINARY_SHOW_STRINGS) ||
            store.contains(DBeaverPreferences.RESULT_SET_BINARY_PRESENTATION) ||
            store.contains(DBeaverPreferences.RESULT_SET_BINARY_EDITOR_TYPE) ||
            store.contains(DBeaverPreferences.RESULT_SET_BINARY_STRING_MAX_LEN) ||
            store.contains(DBeaverPreferences.RESULT_SET_ORDER_SERVER_SIDE) ||
            store.contains(DBeaverPreferences.RESULT_SET_SHOW_ODD_ROWS) ||
            store.contains(DBeaverPreferences.RESULT_SET_SHOW_CELL_ICONS) ||
            store.contains(DBeaverPreferences.RESULT_SET_DOUBLE_CLICK) ||
            store.contains(DBeaverPreferences.RESULT_SET_AUTO_SWITCH_MODE)
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
        Composite composite = UIUtils.createPlaceholder(parent, 2, 5);

        {
            Group queriesGroup = UIUtils.createControlGroup(composite, CoreMessages.pref_page_database_general_group_queries, 2, SWT.NONE, 0);
            queriesGroup.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_BEGINNING));
            UIUtils.createControlLabel(queriesGroup, CoreMessages.pref_page_database_general_label_result_set_max_size);

            resultSetSize = new Spinner(queriesGroup, SWT.BORDER);
            resultSetSize.setSelection(0);
            resultSetSize.setDigits(0);
            resultSetSize.setIncrement(1);
            resultSetSize.setMinimum(1);
            resultSetSize.setMaximum(1024 * 1024);

            resultSetUseSQLCheck = UIUtils.createLabelCheckbox(queriesGroup, CoreMessages.pref_page_database_resultsets_label_use_sql, false);
            serverSideOrderingCheck = UIUtils.createLabelCheckbox(queriesGroup, CoreMessages.pref_page_database_resultsets_label_server_side_order, false);
        }

        // General settings
        {
            Group txnGroup = new Group(composite, SWT.NONE);
            txnGroup.setText(CoreMessages.pref_page_database_general_group_transactions);
            txnGroup.setLayout(new GridLayout(2, false));

            keepStatementOpenCheck = UIUtils.createLabelCheckbox(txnGroup, CoreMessages.pref_page_database_general_checkbox_keep_cursor, false);
            rollbackOnErrorCheck = UIUtils.createLabelCheckbox(txnGroup, CoreMessages.pref_page_database_general_checkbox_rollback_on_error, false);
        }

        {
            Group binaryGroup = UIUtils.createControlGroup(composite, CoreMessages.pref_page_database_resultsets_group_binary, 2, SWT.NONE, 0);
            GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
            gd.horizontalSpan = 2;
            binaryGroup.setLayoutData(gd);

            //binaryShowStrings = UIUtils.createLabelCheckbox(binaryGroup, CoreMessages.pref_page_database_resultsets_label_binary_use_strings, false);

            binaryPresentationCombo = UIUtils.createLabelCombo(binaryGroup, CoreMessages.pref_page_database_resultsets_label_binary_presentation, SWT.DROP_DOWN | SWT.READ_ONLY);
            for (DBDBinaryFormatter formatter : DBDBinaryFormatter.FORMATS) {
                binaryPresentationCombo.add(formatter.getTitle());
            }


            binaryEditorType = UIUtils.createLabelCombo(binaryGroup, CoreMessages.pref_page_database_resultsets_label_binary_editor_type, SWT.DROP_DOWN | SWT.READ_ONLY);
            binaryEditorType.add("Editor");
            binaryEditorType.add("Dialog");

            UIUtils.createControlLabel(binaryGroup, CoreMessages.pref_page_database_resultsets_label_binary_strings_max_length);
            binaryStringMaxLength = new Spinner(binaryGroup, SWT.BORDER);
            binaryStringMaxLength.setSelection(0);
            binaryStringMaxLength.setDigits(0);
            binaryStringMaxLength.setIncrement(1);
            binaryStringMaxLength.setMinimum(0);
            binaryStringMaxLength.setMaximum(10000);

            UIUtils.createControlLabel(binaryGroup, CoreMessages.pref_page_database_general_label_max_lob_length);
            memoryContentSize = new Spinner(binaryGroup, SWT.BORDER);
            memoryContentSize.setSelection(0);
            memoryContentSize.setDigits(0);
            memoryContentSize.setIncrement(1);
            memoryContentSize.setMinimum(0);
            memoryContentSize.setMaximum(1024 * 1024 * 1024);
        }

        {
            Group uiGroup = UIUtils.createControlGroup(composite, "UI", 2, SWT.NONE, 0);

            showOddRows = UIUtils.createLabelCheckbox(uiGroup, "Mark odd/even rows", false);
            showCellIcons = UIUtils.createLabelCheckbox(uiGroup, "Show cell icons", false);
            doubleClickBehavior = UIUtils.createLabelCombo(uiGroup, "Double-click behavior", SWT.READ_ONLY);
            doubleClickBehavior.add("None", Spreadsheet.DoubleClickBehavior.NONE.ordinal());
            doubleClickBehavior.add("Editor", Spreadsheet.DoubleClickBehavior.EDITOR.ordinal());
            doubleClickBehavior.add("Inline Editor", Spreadsheet.DoubleClickBehavior.INLINE_EDITOR.ordinal());
            autoSwitchMode = UIUtils.createLabelCheckbox(uiGroup, "Switch to record/grid mode\non single/multiple row(s)", false);
        }

        return composite;
    }

    @Override
    protected void loadPreferences(IPreferenceStore store)
    {
        try {
            resultSetSize.setSelection(store.getInt(DBeaverPreferences.RESULT_SET_MAX_ROWS));
            resultSetUseSQLCheck.setSelection(store.getBoolean(DBeaverPreferences.RESULT_SET_MAX_ROWS_USE_SQL));
            serverSideOrderingCheck.setSelection(store.getBoolean(DBeaverPreferences.RESULT_SET_ORDER_SERVER_SIDE));

            keepStatementOpenCheck.setSelection(store.getBoolean(DBeaverPreferences.KEEP_STATEMENT_OPEN));
            rollbackOnErrorCheck.setSelection(store.getBoolean(DBeaverPreferences.QUERY_ROLLBACK_ON_ERROR));
            memoryContentSize.setSelection(store.getInt(DBeaverPreferences.MEMORY_CONTENT_MAX_SIZE));
            binaryStringMaxLength.setSelection(store.getInt(DBeaverPreferences.RESULT_SET_BINARY_STRING_MAX_LEN));

            DBDBinaryFormatter formatter = DBUtils.getBinaryPresentation(store.getString(DBeaverPreferences.RESULT_SET_BINARY_PRESENTATION));
            for (int i = 0; i < binaryPresentationCombo.getItemCount(); i++) {
                if (binaryPresentationCombo.getItem(i).equals(formatter.getTitle())) {
                    binaryPresentationCombo.select(i);
                    break;
                }
            }

            DBDValueController.EditType editorType = DBDValueController.EditType.valueOf(store.getString(DBeaverPreferences.RESULT_SET_BINARY_EDITOR_TYPE));
            if (editorType == DBDValueController.EditType.EDITOR) {
                binaryEditorType.select(0);
            } else {
                binaryEditorType.select(1);
            }

            showOddRows.setSelection(store.getBoolean(DBeaverPreferences.RESULT_SET_SHOW_ODD_ROWS));
            showCellIcons.setSelection(store.getBoolean(DBeaverPreferences.RESULT_SET_SHOW_CELL_ICONS));
            doubleClickBehavior.select(
                Spreadsheet.DoubleClickBehavior.valueOf(store.getString(DBeaverPreferences.RESULT_SET_DOUBLE_CLICK)).ordinal());
            autoSwitchMode.setSelection(store.getBoolean(DBeaverPreferences.RESULT_SET_AUTO_SWITCH_MODE));
        } catch (Exception e) {
            log.warn(e);
        }
    }

    @Override
    protected void savePreferences(IPreferenceStore store)
    {
        try {
            store.setValue(DBeaverPreferences.RESULT_SET_MAX_ROWS, resultSetSize.getSelection());
            store.setValue(DBeaverPreferences.RESULT_SET_MAX_ROWS_USE_SQL, resultSetUseSQLCheck.getSelection());
            store.setValue(DBeaverPreferences.KEEP_STATEMENT_OPEN, keepStatementOpenCheck.getSelection());
            store.setValue(DBeaverPreferences.QUERY_ROLLBACK_ON_ERROR, rollbackOnErrorCheck.getSelection());
            store.setValue(DBeaverPreferences.MEMORY_CONTENT_MAX_SIZE, memoryContentSize.getSelection());

            String presentationTitle = binaryPresentationCombo.getItem(binaryPresentationCombo.getSelectionIndex());
            for (DBDBinaryFormatter formatter : DBDBinaryFormatter.FORMATS) {
                if (formatter.getTitle().equals(presentationTitle)) {
                    store.setValue(DBeaverPreferences.RESULT_SET_BINARY_PRESENTATION, formatter.getId());
                    break;
                }
            }
            store.setValue(DBeaverPreferences.RESULT_SET_BINARY_STRING_MAX_LEN, binaryStringMaxLength.getSelection());
            store.setValue(DBeaverPreferences.RESULT_SET_BINARY_EDITOR_TYPE,
                binaryEditorType.getSelectionIndex() == 0 ?
                    DBDValueController.EditType.EDITOR.name() :
                    DBDValueController.EditType.PANEL.name());

            store.setValue(DBeaverPreferences.RESULT_SET_ORDER_SERVER_SIDE, serverSideOrderingCheck.getSelection());

            store.setValue(DBeaverPreferences.RESULT_SET_SHOW_ODD_ROWS, showOddRows.getSelection());
            store.setValue(DBeaverPreferences.RESULT_SET_SHOW_CELL_ICONS, showCellIcons.getSelection());
            store.setValue(DBeaverPreferences.RESULT_SET_DOUBLE_CLICK, CommonUtils.fromOrdinal(Spreadsheet.DoubleClickBehavior.class, doubleClickBehavior.getSelectionIndex()).name());
            store.setValue(DBeaverPreferences.RESULT_SET_AUTO_SWITCH_MODE, autoSwitchMode.getSelection());
        } catch (Exception e) {
            log.warn(e);
        }
        RuntimeUtils.savePreferenceStore(store);
    }

    @Override
    protected void clearPreferences(IPreferenceStore store)
    {
        store.setToDefault(DBeaverPreferences.RESULT_SET_MAX_ROWS);
        store.setToDefault(DBeaverPreferences.RESULT_SET_MAX_ROWS_USE_SQL);
        store.setToDefault(DBeaverPreferences.KEEP_STATEMENT_OPEN);
        store.setToDefault(DBeaverPreferences.QUERY_ROLLBACK_ON_ERROR);
        store.setToDefault(DBeaverPreferences.MEMORY_CONTENT_MAX_SIZE);
        store.setToDefault(DBeaverPreferences.RESULT_SET_BINARY_SHOW_STRINGS);
        store.setToDefault(DBeaverPreferences.RESULT_SET_BINARY_PRESENTATION);
        store.setToDefault(DBeaverPreferences.RESULT_SET_BINARY_STRING_MAX_LEN);
        store.setToDefault(DBeaverPreferences.RESULT_SET_BINARY_EDITOR_TYPE);
        store.setToDefault(DBeaverPreferences.RESULT_SET_ORDER_SERVER_SIDE);
        store.setToDefault(DBeaverPreferences.RESULT_SET_SHOW_ODD_ROWS);
        store.setToDefault(DBeaverPreferences.RESULT_SET_SHOW_CELL_ICONS);
        store.setToDefault(DBeaverPreferences.RESULT_SET_DOUBLE_CLICK);
        store.setToDefault(DBeaverPreferences.RESULT_SET_AUTO_SWITCH_MODE);
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
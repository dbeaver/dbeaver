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
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBPPreferenceStore;
import org.jkiss.dbeaver.model.struct.DBSDataSourceContainer;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.PrefUtils;

/**
 * PrefPageDataEditor
 */
public class PrefPageDataEditor extends TargetPrefPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.dataeditor"; //$NON-NLS-1$

    private Button alwaysUseAllColumns;
    private Spinner maxTextContentSize;
    private Button editLongAsLobCheck;
    private Button commitOnEditApplyCheck;
    private Button commitOnContentApplyCheck;

    public PrefPageDataEditor()
    {
        super();
    }

    @Override
    protected boolean hasDataSourceSpecificOptions(DBSDataSourceContainer dataSourceDescriptor)
    {
        DBPPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return
                store.contains(DBeaverPreferences.RS_EDIT_USE_ALL_COLUMNS) ||
                store.contains(DBeaverPreferences.RS_EDIT_MAX_TEXT_SIZE) ||
                store.contains(DBeaverPreferences.RS_EDIT_LONG_AS_LOB) ||
                store.contains(DBeaverPreferences.RS_COMMIT_ON_EDIT_APPLY) ||
                store.contains(DBeaverPreferences.RS_COMMIT_ON_CONTENT_APPLY)
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
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        // Keys
        {
            Group contentGroup = new Group(composite, SWT.NONE);
            contentGroup.setText(CoreMessages.pref_page_content_editor_group_keys);
            contentGroup.setLayout(new GridLayout(2, false));

            alwaysUseAllColumns = UIUtils.createLabelCheckbox(contentGroup, CoreMessages.pref_page_content_editor_checkbox_keys_always_use_all_columns, false);
        }

        // Content
        {
            Group contentGroup = new Group(composite, SWT.NONE);
            contentGroup.setText(CoreMessages.pref_page_content_editor_group_content);
            contentGroup.setLayout(new GridLayout(2, false));

            UIUtils.createControlLabel(contentGroup, CoreMessages.pref_page_content_editor_label_max_text_length);

            maxTextContentSize = new Spinner(contentGroup, SWT.BORDER);
            maxTextContentSize.setSelection(0);
            maxTextContentSize.setDigits(0);
            maxTextContentSize.setIncrement(1000000);
            maxTextContentSize.setMinimum(0);
            maxTextContentSize.setMaximum(Integer.MAX_VALUE);

            editLongAsLobCheck = UIUtils.createLabelCheckbox(contentGroup, CoreMessages.pref_page_content_editor_checkbox_edit_long_as_lobs, false);
            commitOnEditApplyCheck = UIUtils.createLabelCheckbox(contentGroup, CoreMessages.pref_page_content_editor_checkbox_commit_on_value_apply, false);
            commitOnContentApplyCheck = UIUtils.createLabelCheckbox(contentGroup, CoreMessages.pref_page_content_editor_checkbox_commit_on_content_apply, false);
        }

        return composite;
    }

    @Override
    protected void loadPreferences(DBPPreferenceStore store)
    {
        try {
            alwaysUseAllColumns.setSelection(store.getBoolean(DBeaverPreferences.RS_EDIT_USE_ALL_COLUMNS));
            maxTextContentSize.setSelection(store.getInt(DBeaverPreferences.RS_EDIT_MAX_TEXT_SIZE));
            editLongAsLobCheck.setSelection(store.getBoolean(DBeaverPreferences.RS_EDIT_LONG_AS_LOB));
            commitOnEditApplyCheck.setSelection(store.getBoolean(DBeaverPreferences.RS_COMMIT_ON_EDIT_APPLY));
            commitOnContentApplyCheck.setSelection(store.getBoolean(DBeaverPreferences.RS_COMMIT_ON_CONTENT_APPLY));
        } catch (Exception e) {
            log.warn(e);
        }
    }

    @Override
    protected void savePreferences(DBPPreferenceStore store)
    {
        try {
            store.setValue(DBeaverPreferences.RS_EDIT_USE_ALL_COLUMNS, alwaysUseAllColumns.getSelection());
            store.setValue(DBeaverPreferences.RS_EDIT_MAX_TEXT_SIZE, maxTextContentSize.getSelection());
            store.setValue(DBeaverPreferences.RS_EDIT_LONG_AS_LOB, editLongAsLobCheck.getSelection());
            store.setValue(DBeaverPreferences.RS_COMMIT_ON_EDIT_APPLY, commitOnEditApplyCheck.getSelection());
            store.setValue(DBeaverPreferences.RS_COMMIT_ON_CONTENT_APPLY, commitOnContentApplyCheck.getSelection());
        } catch (Exception e) {
            log.warn(e);
        }
        PrefUtils.savePreferenceStore(store);
    }

    @Override
    protected void clearPreferences(DBPPreferenceStore store)
    {
        store.setToDefault(DBeaverPreferences.RS_EDIT_USE_ALL_COLUMNS);
        store.setToDefault(DBeaverPreferences.RS_EDIT_MAX_TEXT_SIZE);
        store.setToDefault(DBeaverPreferences.RS_EDIT_LONG_AS_LOB);
        store.setToDefault(DBeaverPreferences.RS_COMMIT_ON_EDIT_APPLY);
        store.setToDefault(DBeaverPreferences.RS_COMMIT_ON_CONTENT_APPLY);
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
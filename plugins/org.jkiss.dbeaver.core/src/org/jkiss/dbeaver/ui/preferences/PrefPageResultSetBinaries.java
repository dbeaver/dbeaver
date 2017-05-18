/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDBinaryFormatter;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.PrefUtils;

/**
 * PrefPageResultSetBinaries
 */
public class PrefPageResultSetBinaries extends TargetPrefPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.resultset.binaries"; //$NON-NLS-1$

    //private Button binaryShowStrings;
    private Combo binaryPresentationCombo;
    private Combo binaryEditorType;
    private Spinner binaryStringMaxLength;
    private Spinner memoryContentSize;
    private Combo encodingCombo;
    private Button contentCacheClob;
    private Button contentCacheBlob;
    private Spinner contentCacheMaxSize;

    private Spinner maxTextContentSize;
    private Button editLongAsLobCheck;
    private Button commitOnEditApplyCheck;
    private Button commitOnContentApplyCheck;


    public PrefPageResultSetBinaries()
    {
        super();
//        setPreferenceStore(DBeaverCore.getGlobalPreferenceStore());
    }

    @Override
    protected boolean hasDataSourceSpecificOptions(DBPDataSourceContainer dataSourceDescriptor)
    {
        DBPPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return
            store.contains(DBeaverPreferences.MEMORY_CONTENT_MAX_SIZE) ||
            store.contains(ModelPreferences.RESULT_SET_BINARY_PRESENTATION) ||
            store.contains(DBeaverPreferences.RESULT_SET_BINARY_EDITOR_TYPE) ||
            store.contains(ModelPreferences.RESULT_SET_BINARY_STRING_MAX_LEN) ||
            store.contains(ModelPreferences.CONTENT_HEX_ENCODING) ||
            store.contains(ModelPreferences.CONTENT_CACHE_CLOB) ||
            store.contains(ModelPreferences.CONTENT_CACHE_BLOB) ||
            store.contains(ModelPreferences.CONTENT_CACHE_MAX_SIZE) ||
            store.contains(DBeaverPreferences.RS_EDIT_LONG_AS_LOB) ||

            store.contains(DBeaverPreferences.RS_EDIT_MAX_TEXT_SIZE) ||
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
        Composite composite = UIUtils.createPlaceholder(parent, 2, 5);

        {
            Group binaryGroup = UIUtils.createControlGroup(composite, CoreMessages.pref_page_database_resultsets_group_binary, 2, SWT.NONE, 0);
            GridData gd = new GridData(GridData.VERTICAL_ALIGN_BEGINNING);
            gd.horizontalSpan = 2;
            binaryGroup.setLayoutData(gd);

            //binaryShowStrings = UIUtils.createLabelCheckbox(binaryGroup, CoreMessages.pref_page_database_resultsets_label_binary_use_strings, false);

            binaryPresentationCombo = UIUtils.createLabelCombo(binaryGroup, CoreMessages.pref_page_database_resultsets_label_binary_presentation, SWT.DROP_DOWN | SWT.READ_ONLY);
            for (DBDBinaryFormatter formatter : DBConstants.BINARY_FORMATS) {
                binaryPresentationCombo.add(formatter.getTitle());
            }


            binaryEditorType = UIUtils.createLabelCombo(binaryGroup, CoreMessages.pref_page_database_resultsets_label_binary_editor_type, SWT.DROP_DOWN | SWT.READ_ONLY);
            binaryEditorType.add("Editor");
            binaryEditorType.add("Dialog");

            binaryStringMaxLength = UIUtils.createLabelSpinner(binaryGroup, CoreMessages.pref_page_database_resultsets_label_binary_strings_max_length, 0, 0, 10000);
            binaryStringMaxLength.setDigits(0);
            binaryStringMaxLength.setIncrement(1);

            memoryContentSize = UIUtils.createLabelSpinner(binaryGroup, CoreMessages.pref_page_database_general_label_max_lob_length, 0, 0, 1024 * 1024 * 1024);
            memoryContentSize.setDigits(0);
            memoryContentSize.setIncrement(1);

            UIUtils.createControlLabel(binaryGroup, CoreMessages.pref_page_content_editor_hex_encoding);
            encodingCombo = UIUtils.createEncodingCombo(binaryGroup, GeneralUtils.getDefaultFileEncoding());

            contentCacheClob = UIUtils.createLabelCheckbox(binaryGroup, CoreMessages.pref_page_content_cache_clob, true);
            contentCacheBlob = UIUtils.createLabelCheckbox(binaryGroup, CoreMessages.pref_page_content_cache_blob, true);
            contentCacheMaxSize = UIUtils.createLabelSpinner(binaryGroup, CoreMessages.pref_page_database_general_label_cache_max_size, 0, 0, Integer.MAX_VALUE);
            contentCacheMaxSize.setDigits(0);
            contentCacheMaxSize.setIncrement(100000);
            editLongAsLobCheck = UIUtils.createLabelCheckbox(binaryGroup, CoreMessages.pref_page_content_editor_checkbox_edit_long_as_lobs, false);
        }

        // Content
        {
            Group contentGroup = new Group(composite, SWT.NONE);
            contentGroup.setText(CoreMessages.pref_page_content_editor_group_content);
            contentGroup.setLayout(new GridLayout(2, false));

            maxTextContentSize = UIUtils.createLabelSpinner(contentGroup, CoreMessages.pref_page_content_editor_label_max_text_length, 0, 0, Integer.MAX_VALUE);
            maxTextContentSize.setDigits(0);
            maxTextContentSize.setIncrement(1000000);

            commitOnEditApplyCheck = UIUtils.createLabelCheckbox(contentGroup, CoreMessages.pref_page_content_editor_checkbox_commit_on_value_apply, false);
            commitOnContentApplyCheck = UIUtils.createLabelCheckbox(contentGroup, CoreMessages.pref_page_content_editor_checkbox_commit_on_content_apply, false);
        }

        return composite;
    }

    @Override
    protected void loadPreferences(DBPPreferenceStore store)
    {
        try {
            memoryContentSize.setSelection(store.getInt(DBeaverPreferences.MEMORY_CONTENT_MAX_SIZE));
            binaryStringMaxLength.setSelection(store.getInt(ModelPreferences.RESULT_SET_BINARY_STRING_MAX_LEN));

            DBDBinaryFormatter formatter = DBValueFormatting.getBinaryPresentation(store.getString(ModelPreferences.RESULT_SET_BINARY_PRESENTATION));
            for (int i = 0; i < binaryPresentationCombo.getItemCount(); i++) {
                if (binaryPresentationCombo.getItem(i).equals(formatter.getTitle())) {
                    binaryPresentationCombo.select(i);
                    break;
                }
            }

            IValueController.EditType editorType = IValueController.EditType.valueOf(store.getString(DBeaverPreferences.RESULT_SET_BINARY_EDITOR_TYPE));
            if (editorType == IValueController.EditType.EDITOR) {
                binaryEditorType.select(0);
            } else {
                binaryEditorType.select(1);
            }
            UIUtils.setComboSelection(encodingCombo, store.getString(ModelPreferences.CONTENT_HEX_ENCODING));
            contentCacheClob.setSelection(store.getBoolean(ModelPreferences.CONTENT_CACHE_CLOB));
            contentCacheBlob.setSelection(store.getBoolean(ModelPreferences.CONTENT_CACHE_BLOB));
            contentCacheMaxSize.setSelection(store.getInt(ModelPreferences.CONTENT_CACHE_MAX_SIZE));
            editLongAsLobCheck.setSelection(store.getBoolean(DBeaverPreferences.RS_EDIT_LONG_AS_LOB));

            maxTextContentSize.setSelection(store.getInt(DBeaverPreferences.RS_EDIT_MAX_TEXT_SIZE));
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
            store.setValue(DBeaverPreferences.MEMORY_CONTENT_MAX_SIZE, memoryContentSize.getSelection());

            String presentationTitle = binaryPresentationCombo.getItem(binaryPresentationCombo.getSelectionIndex());
            for (DBDBinaryFormatter formatter : DBConstants.BINARY_FORMATS) {
                if (formatter.getTitle().equals(presentationTitle)) {
                    store.setValue(ModelPreferences.RESULT_SET_BINARY_PRESENTATION, formatter.getId());
                    break;
                }
            }
            store.setValue(ModelPreferences.RESULT_SET_BINARY_STRING_MAX_LEN, binaryStringMaxLength.getSelection());
            store.setValue(DBeaverPreferences.RESULT_SET_BINARY_EDITOR_TYPE,
                binaryEditorType.getSelectionIndex() == 0 ?
                    IValueController.EditType.EDITOR.name() :
                    IValueController.EditType.PANEL.name());
            store.setValue(ModelPreferences.CONTENT_HEX_ENCODING, UIUtils.getComboSelection(encodingCombo));
            store.setValue(ModelPreferences.CONTENT_CACHE_CLOB, contentCacheClob.getSelection());
            store.setValue(ModelPreferences.CONTENT_CACHE_BLOB, contentCacheBlob.getSelection());
            store.setValue(ModelPreferences.CONTENT_CACHE_MAX_SIZE, contentCacheMaxSize.getSelection());
            store.setValue(DBeaverPreferences.RS_EDIT_LONG_AS_LOB, editLongAsLobCheck.getSelection());

            store.setValue(DBeaverPreferences.RS_EDIT_MAX_TEXT_SIZE, maxTextContentSize.getSelection());
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
        store.setToDefault(ModelPreferences.MEMORY_CONTENT_MAX_SIZE);
        store.setToDefault(ModelPreferences.RESULT_SET_BINARY_PRESENTATION);
        store.setToDefault(ModelPreferences.RESULT_SET_BINARY_STRING_MAX_LEN);
        store.setToDefault(DBeaverPreferences.RESULT_SET_BINARY_EDITOR_TYPE);
        store.setToDefault(ModelPreferences.CONTENT_HEX_ENCODING);
        store.setToDefault(ModelPreferences.CONTENT_CACHE_CLOB);
        store.setToDefault(ModelPreferences.CONTENT_CACHE_BLOB);
        store.setToDefault(ModelPreferences.CONTENT_CACHE_MAX_SIZE);
        store.setToDefault(DBeaverPreferences.RS_EDIT_LONG_AS_LOB);

        store.setToDefault(DBeaverPreferences.RS_EDIT_MAX_TEXT_SIZE);
        store.setToDefault(DBeaverPreferences.RS_COMMIT_ON_EDIT_APPLY);
        store.setToDefault(DBeaverPreferences.RS_COMMIT_ON_CONTENT_APPLY);
    }

    @Override
    protected String getPropertyPageID()
    {
        return PAGE_ID;
    }

}
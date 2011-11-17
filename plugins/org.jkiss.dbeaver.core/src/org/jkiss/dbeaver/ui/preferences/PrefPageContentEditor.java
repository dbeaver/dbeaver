/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.AbstractPreferenceStore;

/**
 * PrefPageSQL
 */
public class PrefPageContentEditor extends TargetPrefPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.contenteditor"; //$NON-NLS-1$

    private Spinner maxTextContentSize;
    private Button editLongAsLobCheck;
    private Button commitOnEditApplyCheck;
    private Button commitOnContentApplyCheck;

    public PrefPageContentEditor()
    {
        super();
    }

    protected boolean hasDataSourceSpecificOptions(DataSourceDescriptor dataSourceDescriptor)
    {
        AbstractPreferenceStore store = dataSourceDescriptor.getPreferenceStore();
        return
                store.contains(PrefConstants.RS_EDIT_MAX_TEXT_SIZE) ||
                store.contains(PrefConstants.RS_EDIT_LONG_AS_LOB) ||
                store.contains(PrefConstants.RS_COMMIT_ON_EDIT_APPLY) ||
                store.contains(PrefConstants.RS_COMMIT_ON_CONTENT_APPLY)
            ;
    }

    protected boolean supportsDataSourceSpecificOptions()
    {
        return true;
    }

    protected Control createPreferenceContent(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1);

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

    protected void loadPreferences(IPreferenceStore store)
    {
        try {
            maxTextContentSize.setSelection(store.getInt(PrefConstants.RS_EDIT_MAX_TEXT_SIZE));
            editLongAsLobCheck.setSelection(store.getBoolean(PrefConstants.RS_EDIT_LONG_AS_LOB));
            commitOnEditApplyCheck.setSelection(store.getBoolean(PrefConstants.RS_COMMIT_ON_EDIT_APPLY));
            commitOnContentApplyCheck.setSelection(store.getBoolean(PrefConstants.RS_COMMIT_ON_CONTENT_APPLY));
        } catch (Exception e) {
            log.warn(e);
        }
    }

    protected void savePreferences(IPreferenceStore store)
    {
        try {
            store.setValue(PrefConstants.RS_EDIT_MAX_TEXT_SIZE, maxTextContentSize.getSelection());
            store.setValue(PrefConstants.RS_EDIT_LONG_AS_LOB, editLongAsLobCheck.getSelection());
            store.setValue(PrefConstants.RS_COMMIT_ON_EDIT_APPLY, commitOnEditApplyCheck.getSelection());
            store.setValue(PrefConstants.RS_COMMIT_ON_CONTENT_APPLY, commitOnContentApplyCheck.getSelection());
        } catch (Exception e) {
            log.warn(e);
        }
        RuntimeUtils.savePreferenceStore(store);
    }

    protected void clearPreferences(IPreferenceStore store)
    {
        store.setToDefault(PrefConstants.RS_EDIT_MAX_TEXT_SIZE);
        store.setToDefault(PrefConstants.RS_EDIT_LONG_AS_LOB);
        store.setToDefault(PrefConstants.RS_COMMIT_ON_EDIT_APPLY);
        store.setToDefault(PrefConstants.RS_COMMIT_ON_CONTENT_APPLY);
    }

    public void applyData(Object data)
    {
        super.applyData(data);
    }

    protected String getPropertyPageID()
    {
        return PAGE_ID;
    }

}
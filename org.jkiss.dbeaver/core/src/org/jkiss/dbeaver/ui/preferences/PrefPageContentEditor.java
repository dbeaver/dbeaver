/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.preferences;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.registry.DataSourceDescriptor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.AbstractPreferenceStore;
import org.jkiss.dbeaver.utils.DBeaverUtils;

/**
 * PrefPageSQL
 */
public class PrefPageContentEditor extends TargetPrefPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.contenteditor";

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
        Composite composite = new Composite(parent, SWT.NONE);
        composite.setLayout(new GridLayout(1, false));

        // Content
        {
            Group contentGroup = new Group(composite, SWT.NONE);
            contentGroup.setText("Content");
            contentGroup.setLayout(new GridLayout(2, false));

            UIUtils.createControlLabel(contentGroup, "Maximum text editor content length");

            maxTextContentSize = new Spinner(contentGroup, SWT.BORDER);
            maxTextContentSize.setSelection(0);
            maxTextContentSize.setDigits(0);
            maxTextContentSize.setIncrement(1000000);
            maxTextContentSize.setMinimum(0);
            maxTextContentSize.setMaximum(Integer.MAX_VALUE);

            editLongAsLobCheck = UIUtils.createLabelCheckbox(contentGroup, "Edit LONG columns as LOBs", false);
            commitOnEditApplyCheck = UIUtils.createLabelCheckbox(contentGroup, "Commit session on value edit apply", false);
            commitOnContentApplyCheck = UIUtils.createLabelCheckbox(contentGroup, "Commit session on content edit apply", false);
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
        DBeaverUtils.savePreferenceStore(store);
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
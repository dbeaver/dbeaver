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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.SQLPreferenceConstants;
import org.jkiss.dbeaver.ui.editors.sql.SQLScriptBindingType;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.PrefUtils;

/**
 * PrefPageSQLResources
 */
public class PrefPageSQLResources extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.sql.resources"; //$NON-NLS-1$

    private static final Log log = Log.getLog(PrefPageSQLResources.class);

    private Combo deleteEmptyCombo;
    private Button autoFoldersCheck;
    private Button connectionFoldersCheck;
    private Text scriptTitlePattern;
    private Button bindEmbeddedReadCheck;
    private Button bindEmbeddedWriteCheck;
    private Composite commentTypeComposite;
    private ControlEnableState commentTypeEnableBlock;
    private SQLScriptBindingType curScriptBindingType;

    public PrefPageSQLResources()
    {
        super();
    }

    @Override
    protected Control createContents(Composite parent) {
        Composite composite = UIUtils.createComposite(parent, 1);

        // Connection association
        {
            Composite connGroup = UIUtils.createControlGroup(composite, CoreMessages.pref_page_sql_editor_group_connection_association, 2, GridData.FILL_HORIZONTAL, 0);

            Label tipLabel = new Label(connGroup, SWT.WRAP);
            tipLabel.setText(CoreMessages.pref_page_sql_editor_checkbox_bind_connection_hint);
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            tipLabel.setLayoutData(gd);

            bindEmbeddedReadCheck = UIUtils.createCheckbox(connGroup, CoreMessages.pref_page_sql_editor_checkbox_bind_embedded_read, CoreMessages.pref_page_sql_editor_checkbox_bind_embedded_read_tip, false, 2);

            bindEmbeddedWriteCheck = UIUtils.createCheckbox(connGroup, CoreMessages.pref_page_sql_editor_checkbox_bind_embedded_write, CoreMessages.pref_page_sql_editor_checkbox_bind_embedded_write_tip, false, 2);
            bindEmbeddedWriteCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    enableCommentType();
                }
            });

            commentTypeComposite = UIUtils.createComposite(connGroup, 1);
            for (SQLScriptBindingType bt : SQLScriptBindingType.values()) {
                if (bt != SQLScriptBindingType.EXTERNAL) {
                    UIUtils.createRadioButton(commentTypeComposite, bt.getDescription(), bt, SelectionListener.widgetSelectedAdapter(selectionEvent -> {
                        curScriptBindingType = (SQLScriptBindingType)selectionEvent.widget.getData();
                    }));
                }
            }
        }

        // Resources
        {
            Composite scriptsGroup = UIUtils.createControlGroup(composite, CoreMessages.pref_page_sql_editor_group_resources, 2, GridData.FILL_HORIZONTAL, 0);

            deleteEmptyCombo = UIUtils.createLabelCombo(scriptsGroup, CoreMessages.pref_page_sql_editor_checkbox_delete_empty_scripts, SWT.DROP_DOWN | SWT.READ_ONLY);
            for (SQLPreferenceConstants.EmptyScriptCloseBehavior escb : SQLPreferenceConstants.EmptyScriptCloseBehavior.values()) {
                deleteEmptyCombo.add(escb.getTitle());
            }
            deleteEmptyCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
            deleteEmptyCombo.select(0);
            autoFoldersCheck = UIUtils.createCheckbox(scriptsGroup, CoreMessages.pref_page_sql_editor_checkbox_put_new_scripts, null, false, 2);
            connectionFoldersCheck = UIUtils.createCheckbox(scriptsGroup, CoreMessages.pref_page_sql_editor_checkbox_create_script_folders, null, false, 2);
            scriptTitlePattern = UIUtils.createLabelText(scriptsGroup, CoreMessages.pref_page_sql_editor_title_pattern, "");
            UIUtils.installContentProposal(
                    scriptTitlePattern,
                    new TextContentAdapter(),
                    new SimpleContentProposalProvider(new String[] {
                        GeneralUtils.variablePattern(SQLEditor.VAR_CONNECTION_NAME),
                        GeneralUtils.variablePattern(SQLEditor.VAR_DRIVER_NAME),
                        GeneralUtils.variablePattern(SQLEditor.VAR_FILE_NAME),
                        GeneralUtils.variablePattern(SQLEditor.VAR_FILE_EXT)}));
            UIUtils.setContentProposalToolTip(scriptTitlePattern, "Output file name patterns",
                    SQLEditor.VAR_CONNECTION_NAME, SQLEditor.VAR_DRIVER_NAME, SQLEditor.VAR_FILE_NAME, SQLEditor.VAR_FILE_EXT);
        }

        performDefaults();

        return composite;
    }

    @Override
    protected void performDefaults()
    {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        bindEmbeddedReadCheck.setSelection(store.getBoolean(SQLPreferenceConstants.SCRIPT_BIND_EMBEDDED_READ));
        bindEmbeddedWriteCheck.setSelection(store.getBoolean(SQLPreferenceConstants.SCRIPT_BIND_EMBEDDED_WRITE));
        try {
            SQLScriptBindingType bindingType = SQLScriptBindingType.valueOf(store.getString(SQLPreferenceConstants.SCRIPT_BIND_COMMENT_TYPE));
            for (Control ch : commentTypeComposite.getChildren()) {
                if (ch instanceof Button && ch.getData() == bindingType) {
                    ((Button) ch).setSelection(true);
                }
            }
        } catch (IllegalArgumentException e) {
            log.error(e);
        }
        enableCommentType();

        deleteEmptyCombo.setText(SQLPreferenceConstants.EmptyScriptCloseBehavior.getByName(
            store.getString(SQLPreferenceConstants.SCRIPT_DELETE_EMPTY)).getTitle());
        autoFoldersCheck.setSelection(store.getBoolean(SQLPreferenceConstants.SCRIPT_AUTO_FOLDERS));
        connectionFoldersCheck.setSelection(store.getBoolean(SQLPreferenceConstants.SCRIPT_CREATE_CONNECTION_FOLDERS));
        scriptTitlePattern.setText(store.getString(SQLPreferenceConstants.SCRIPT_TITLE_PATTERN));

        super.performDefaults();
    }

    private void enableCommentType() {
        if (bindEmbeddedWriteCheck.getSelection()) {
            if (commentTypeEnableBlock != null) {
                commentTypeEnableBlock.restore();
                commentTypeEnableBlock = null;
            }
        } else {
            if (commentTypeEnableBlock == null) {
                commentTypeEnableBlock = ControlEnableState.disable(commentTypeComposite);
            }
        }
    }

    @Override
    public boolean performOk()
    {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        store.setValue(SQLPreferenceConstants.SCRIPT_BIND_EMBEDDED_READ, bindEmbeddedReadCheck.getSelection());
        store.setValue(SQLPreferenceConstants.SCRIPT_BIND_EMBEDDED_WRITE, bindEmbeddedWriteCheck.getSelection());
        try {
            for (Control ch : commentTypeComposite.getChildren()) {
                if (ch instanceof Button && ((Button) ch).getSelection()) {
                    store.setValue(SQLPreferenceConstants.SCRIPT_BIND_COMMENT_TYPE, ch.getData().toString());
                }
            }
        } catch (IllegalArgumentException e) {
            log.error(e);
        }

        store.setValue(SQLPreferenceConstants.SCRIPT_DELETE_EMPTY,
            SQLPreferenceConstants.EmptyScriptCloseBehavior.getByTitle(deleteEmptyCombo.getText()).name());
        store.setValue(SQLPreferenceConstants.SCRIPT_AUTO_FOLDERS, autoFoldersCheck.getSelection());
        store.setValue(SQLPreferenceConstants.SCRIPT_CREATE_CONNECTION_FOLDERS, connectionFoldersCheck.getSelection());
        store.setValue(SQLPreferenceConstants.SCRIPT_TITLE_PATTERN, scriptTitlePattern.getText());

        PrefUtils.savePreferenceStore(store);

        return super.performOk();
    }

    @Override
    public void init(IWorkbench workbench) {

    }

    @Override
    public IAdaptable getElement() {
        return null;
    }

    @Override
    public void setElement(IAdaptable element) {

    }

}
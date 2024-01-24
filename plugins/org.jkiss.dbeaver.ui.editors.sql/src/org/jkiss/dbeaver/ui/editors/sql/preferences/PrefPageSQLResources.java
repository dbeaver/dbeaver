/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.preferences;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.contentassist.ContentAssistUtils;
import org.jkiss.dbeaver.ui.contentassist.SmartTextContentAdapter;
import org.jkiss.dbeaver.ui.contentassist.StringContentProposalProvider;
import org.jkiss.dbeaver.ui.controls.VariablesHintLabel;
import org.jkiss.dbeaver.ui.editors.StringEditorInput;
import org.jkiss.dbeaver.ui.editors.SubEditorSite;
import org.jkiss.dbeaver.ui.editors.sql.*;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;
import org.jkiss.dbeaver.ui.preferences.AbstractPrefPage;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.PrefUtils;

/**
 * PrefPageSQLResources
 */
public class PrefPageSQLResources extends AbstractPrefPage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage
{
    public static final String PAGE_ID = "org.jkiss.dbeaver.preferences.main.sql.resources"; //$NON-NLS-1$

    private static final Log log = Log.getLog(PrefPageSQLResources.class);
    
    private final boolean isStandaloneApp;

    private Combo deleteEmptyCombo;
    private Button autoFoldersCheck;
    private Button connectionFoldersCheck;
    private Text scriptTitlePattern;
    private Text scriptFileNamePattern;
    private Spinner bigScriptFileSizeBoundarySpinner;
    private Button bindEmbeddedReadCheck;
    private Button bindEmbeddedWriteCheck;
    private Composite commentTypeComposite;
    private ControlEnableState commentTypeEnableBlock;
    private SQLScriptBindingType curScriptBindingType;

    private Composite sqlTemplateViewerComposite;
    private Button sqlTemplateEnabledCheckbox;
    private SQLEditorBase sqlTemplateViewer;

    public PrefPageSQLResources() {
        super();
        isStandaloneApp = !DBWorkbench.isDistributed();
    }

    @NotNull
    @Override
    protected Control createPreferenceContent(@NotNull Composite parent) {
        Composite composite = UIUtils.createComposite(parent, 1);
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();

        // Resources
        {
            Composite scriptsGroup = UIUtils.createControlGroup(composite, SQLEditorMessages.pref_page_sql_editor_group_resources, 2, GridData.FILL_HORIZONTAL, 0);

            if (this.isStandaloneApp) {
                deleteEmptyCombo = UIUtils.createLabelCombo(scriptsGroup, SQLEditorMessages.pref_page_sql_editor_checkbox_delete_empty_scripts, SWT.DROP_DOWN | SWT.READ_ONLY);
                for (SQLPreferenceConstants.EmptyScriptCloseBehavior escb : SQLPreferenceConstants.EmptyScriptCloseBehavior.values()) {
                    deleteEmptyCombo.add(escb.getTitle());
                }
                deleteEmptyCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING));
                deleteEmptyCombo.select(0);
            }
            autoFoldersCheck = UIUtils.createCheckbox(
                scriptsGroup,
                SQLEditorMessages.pref_page_sql_editor_checkbox_put_new_scripts,
                SQLEditorMessages.pref_page_sql_editor_checkbox_put_new_scripts_tip,
                store.getBoolean(SQLPreferenceConstants.SCRIPT_AUTO_FOLDERS),
                2);
            connectionFoldersCheck = UIUtils.createCheckbox(
                scriptsGroup,
                SQLEditorMessages.pref_page_sql_editor_checkbox_create_script_folders,
                SQLEditorMessages.pref_page_sql_editor_checkbox_create_script_folders_tip,
                store.getBoolean(SQLPreferenceConstants.SCRIPT_CREATE_CONNECTION_FOLDERS),
                2);
            scriptTitlePattern = UIUtils.createLabelText(
                scriptsGroup,
                SQLEditorMessages.pref_page_sql_editor_title_pattern,
                store.getString(SQLPreferenceConstants.SCRIPT_TITLE_PATTERN));
            scriptFileNamePattern = UIUtils.createLabelText(
                scriptsGroup,
                SQLEditorMessages.pref_page_sql_editor_file_name_pattern,
                store.getString(SQLPreferenceConstants.SCRIPT_FILE_NAME_PATTERN));
            ContentAssistUtils.installContentProposal(
                scriptFileNamePattern,
                    new SmartTextContentAdapter(),
                    new StringContentProposalProvider(
                        GeneralUtils.variablePattern(SQLPreferenceConstants.VAR_CONNECTION_NAME),
                        GeneralUtils.variablePattern(SQLPreferenceConstants.VAR_DRIVER_NAME),
                        GeneralUtils.variablePattern(SQLPreferenceConstants.VAR_ACTIVE_DATABASE),
                        GeneralUtils.variablePattern(SQLPreferenceConstants.VAR_ACTIVE_SCHEMA),
                        GeneralUtils.variablePattern(SQLPreferenceConstants.VAR_ACTIVE_PROJECT)));
            UIUtils.setContentProposalToolTip(scriptFileNamePattern, SQLEditorMessages.pref_page_sql_editor_file_name_pattern_tip,
                SQLPreferenceConstants.VAR_CONNECTION_NAME, SQLPreferenceConstants.VAR_DRIVER_NAME,
                SQLPreferenceConstants.VAR_ACTIVE_DATABASE, SQLPreferenceConstants.VAR_ACTIVE_SCHEMA, SQLPreferenceConstants.VAR_ACTIVE_PROJECT);
            ContentAssistUtils.installContentProposal(
                scriptTitlePattern,
                new SmartTextContentAdapter(),
                new StringContentProposalProvider(
                    GeneralUtils.variablePattern(SQLPreferenceConstants.VAR_CONNECTION_NAME),
                    GeneralUtils.variablePattern(SQLPreferenceConstants.VAR_DRIVER_NAME),
                    GeneralUtils.variablePattern(SQLPreferenceConstants.VAR_FILE_NAME),
                    GeneralUtils.variablePattern(SQLPreferenceConstants.VAR_FILE_EXT),
                    GeneralUtils.variablePattern(SQLPreferenceConstants.VAR_ACTIVE_DATABASE),
                    GeneralUtils.variablePattern(SQLPreferenceConstants.VAR_ACTIVE_PROJECT),
                    GeneralUtils.variablePattern(SQLPreferenceConstants.VAR_ACTIVE_SCHEMA)));
            UIUtils.setContentProposalToolTip(scriptTitlePattern, SQLEditorMessages.pref_page_sql_editor_file_name_pattern_tip,
                SQLPreferenceConstants.VAR_CONNECTION_NAME, SQLPreferenceConstants.VAR_DRIVER_NAME, SQLPreferenceConstants.VAR_FILE_NAME, SQLPreferenceConstants.VAR_FILE_EXT,
                SQLPreferenceConstants.VAR_ACTIVE_DATABASE, SQLPreferenceConstants.VAR_ACTIVE_SCHEMA, SQLPreferenceConstants.VAR_ACTIVE_PROJECT);
            
            UIUtils.createControlLabel(
                scriptsGroup,
                SQLEditorMessages.sql_editor_prefs_script_disable_sql_syntax_parsing_for_scripts_bigger_than
            );
            bigScriptFileSizeBoundarySpinner = new Spinner(scriptsGroup, SWT.BORDER);
            bigScriptFileSizeBoundarySpinner.setDigits(0);
            bigScriptFileSizeBoundarySpinner.setIncrement(50);
            bigScriptFileSizeBoundarySpinner.setMinimum(0);
            bigScriptFileSizeBoundarySpinner.setMaximum(Integer.MAX_VALUE);
            long bigScriptSize = store.getLong(SQLPreferenceConstants.SCRIPT_BIG_FILE_LENGTH_BOUNDARY);
            bigScriptFileSizeBoundarySpinner.setSelection((int) (bigScriptSize / 1024));
        }

        // New Script template
        {
            Composite group = UIUtils.createControlGroup(composite, SQLEditorMessages.pref_page_sql_editor_new_script_template_group, 1, GridData.FILL_BOTH, 0);
            ((GridData) group.getLayoutData()).horizontalSpan = 2;

            sqlTemplateEnabledCheckbox = UIUtils.createCheckbox(
                group,
                SQLEditorMessages.pref_page_sql_editor_new_script_template_enable_checkbox,
                store.getBoolean(SQLPreferenceConstants.NEW_SCRIPT_TEMPLATE_ENABLED));

            {
                sqlTemplateViewerComposite = UIUtils.createPlaceholder(group, 1);
                sqlTemplateViewerComposite.setLayoutData(new GridData(GridData.FILL_BOTH));
                sqlTemplateViewerComposite.setLayout(new FillLayout());
                ((GridData) sqlTemplateViewerComposite.getLayoutData()).heightHint = 200;

                sqlTemplateViewer = new SQLEditorBase() {
                    @Override
                    public DBCExecutionContext getExecutionContext() {
                        return null;
                    }
                };

                setSQLTemplateText("", true);
                sqlTemplateViewer.createPartControl(sqlTemplateViewerComposite);
                sqlTemplateViewerComposite.addDisposeListener(e -> sqlTemplateViewer.dispose());

                sqlTemplateEnabledCheckbox.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        UIUtils.enableWithChildren(sqlTemplateViewerComposite, sqlTemplateEnabledCheckbox.getSelection());
                    }
                });
            }

            new VariablesHintLabel(
                group,
                SQLEditorMessages.pref_page_sql_editor_new_script_template_variables_tip,
                SQLEditorMessages.pref_page_sql_editor_new_script_template_variables,
                SQLNewScriptTemplateVariablesResolver.ALL_VARIABLES_INFO,
                false
            );
        }

        // Connection association
        {
            final ExpandableComposite expander = new ExpandableComposite(composite, SWT.NONE);
            expander.setLayoutData(new GridData(GridData.FILL, GridData.BEGINNING, false, false, 1, 1));
            expander.setText(SQLEditorMessages.sql_editor_prefs_script_advanced_settings);

            Composite connGroup = UIUtils.createControlGroup(
                expander,
                SQLEditorMessages.pref_page_sql_editor_group_connection_association,
                2,
                GridData.FILL_HORIZONTAL,
                0
            );
            expander.setClient(connGroup);
            Label tipLabel = new Label(connGroup, SWT.WRAP);
            tipLabel.setText(SQLEditorMessages.pref_page_sql_editor_checkbox_bind_connection_hint);
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 2;
            tipLabel.setLayoutData(gd);

            bindEmbeddedReadCheck = UIUtils.createCheckbox(
                connGroup,
                SQLEditorMessages.pref_page_sql_editor_checkbox_bind_embedded_read,
                SQLEditorMessages.pref_page_sql_editor_checkbox_bind_embedded_read_tip,
                store.getBoolean(SQLPreferenceConstants.SCRIPT_BIND_EMBEDDED_READ),
                2);

            bindEmbeddedWriteCheck = UIUtils.createCheckbox(
                connGroup,
                SQLEditorMessages.pref_page_sql_editor_checkbox_bind_embedded_write,
                SQLEditorMessages.pref_page_sql_editor_checkbox_bind_embedded_write_tip,
                store.getBoolean(SQLPreferenceConstants.SCRIPT_BIND_EMBEDDED_WRITE),
                2);
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

        setSettings(store);

        return composite;
    }

    private void setSettings(@NotNull DBPPreferenceStore store) {
        setScriptBindingTypes(SQLScriptBindingType.valueOf(store.getString(SQLPreferenceConstants.SCRIPT_BIND_COMMENT_TYPE)));
        enableCommentType();
        if (this.isStandaloneApp) {
            UIUtils.setComboSelection(deleteEmptyCombo, SQLPreferenceConstants.EmptyScriptCloseBehavior.getByName(
                    store.getString(SQLPreferenceConstants.SCRIPT_DELETE_EMPTY)).getTitle());
        }
        setSQLTemplateText(SQLEditorUtils.getNewScriptTemplate(store), false);
        UIUtils.enableWithChildren(sqlTemplateViewerComposite, sqlTemplateEnabledCheckbox.getSelection());
    }

    private void setScriptBindingTypes(SQLScriptBindingType bindingType) {
        try {
            for (Control ch : commentTypeComposite.getChildren()) {
                if (ch instanceof Button && ch.getData() == bindingType) {
                    ((Button) ch).setSelection(true);
                }
            }
        } catch (IllegalArgumentException e) {
            log.error(e);
        }
    }

    @Override
    protected void performDefaults() {
        DBPPreferenceStore store = DBWorkbench.getPlatform().getPreferenceStore();
        bindEmbeddedReadCheck.setSelection(store.getDefaultBoolean(SQLPreferenceConstants.SCRIPT_BIND_EMBEDDED_READ));
        bindEmbeddedWriteCheck.setSelection(store.getDefaultBoolean(SQLPreferenceConstants.SCRIPT_BIND_EMBEDDED_WRITE));
        setScriptBindingTypes(SQLScriptBindingType.NAME);
        enableCommentType();

        if (this.isStandaloneApp) {
            deleteEmptyCombo.setText(store.getDefaultString(SQLPreferenceConstants.SCRIPT_DELETE_EMPTY));
        }
        autoFoldersCheck.setSelection(store.getDefaultBoolean(SQLPreferenceConstants.SCRIPT_AUTO_FOLDERS));
        connectionFoldersCheck.setSelection(store.getDefaultBoolean(SQLPreferenceConstants.SCRIPT_CREATE_CONNECTION_FOLDERS));
        scriptTitlePattern.setText(store.getDefaultString(SQLPreferenceConstants.SCRIPT_TITLE_PATTERN));
        scriptFileNamePattern.setText(store.getDefaultString(SQLPreferenceConstants.SCRIPT_FILE_NAME_PATTERN));
        bigScriptFileSizeBoundarySpinner.setSelection(
            (int) (store.getDefaultLong(SQLPreferenceConstants.SCRIPT_BIG_FILE_LENGTH_BOUNDARY) / 1024));
        setSQLTemplateText(
            SQLUtils.generateCommentLine(null, SQLEditorMessages.pref_page_sql_editor_new_script_template_template), false);
        sqlTemplateEnabledCheckbox.setSelection(store.getDefaultBoolean(SQLPreferenceConstants.NEW_SCRIPT_TEMPLATE_ENABLED));
        UIUtils.enableWithChildren(sqlTemplateViewerComposite, sqlTemplateEnabledCheckbox.getSelection());

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

        if (this.isStandaloneApp) {
            store.setValue(SQLPreferenceConstants.SCRIPT_DELETE_EMPTY,
                SQLPreferenceConstants.EmptyScriptCloseBehavior.getByTitle(deleteEmptyCombo.getText()).name());
        }
        store.setValue(SQLPreferenceConstants.SCRIPT_AUTO_FOLDERS, autoFoldersCheck.getSelection());
        store.setValue(SQLPreferenceConstants.SCRIPT_CREATE_CONNECTION_FOLDERS, connectionFoldersCheck.getSelection());
        store.setValue(SQLPreferenceConstants.SCRIPT_TITLE_PATTERN, scriptTitlePattern.getText());
        store.setValue(SQLPreferenceConstants.SCRIPT_FILE_NAME_PATTERN, scriptFileNamePattern.getText());
        store.setValue(SQLPreferenceConstants.SCRIPT_BIG_FILE_LENGTH_BOUNDARY, bigScriptFileSizeBoundarySpinner.getSelection() * 1024L);

        store.setValue(SQLPreferenceConstants.NEW_SCRIPT_TEMPLATE_ENABLED, sqlTemplateEnabledCheckbox.getSelection());
        final IDocument document = sqlTemplateViewer.getDocument();
        if (document != null) {
            store.setValue(SQLPreferenceConstants.NEW_SCRIPT_TEMPLATE, document.get());
        }

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

    private void setSQLTemplateText(@NotNull String text, boolean readOnly) {
        try {
            final IEditorSite subSite = new SubEditorSite(UIUtils.getActiveWorkbenchWindow().getActivePage().getActivePart().getSite());
            final StringEditorInput sqlInput = new StringEditorInput("SQL preview", text, readOnly, GeneralUtils.getDefaultFileEncoding());
            sqlTemplateViewer.init(subSite, sqlInput);
            sqlTemplateViewer.reloadSyntaxRules();
        } catch (Exception e) {
            log.error(e);
        }
    }
}
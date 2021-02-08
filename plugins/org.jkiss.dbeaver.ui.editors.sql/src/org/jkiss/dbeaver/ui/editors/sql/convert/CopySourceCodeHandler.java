/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.sql.convert;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.commands.IElementUpdater;
import org.eclipse.ui.handlers.HandlerUtil;
import org.eclipse.ui.menus.UIElement;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.sql.SQLScriptElement;
import org.jkiss.dbeaver.runtime.properties.PropertySourceCustom;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.StyledTextUtils;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.dialogs.BaseSQLDialog;
import org.jkiss.dbeaver.ui.editors.sql.internal.SQLEditorMessages;
import org.jkiss.dbeaver.ui.editors.sql.registry.SQLConverterRegistry;
import org.jkiss.dbeaver.ui.editors.sql.registry.SQLTargetConverterDescriptor;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

public class CopySourceCodeHandler extends AbstractHandler implements IElementUpdater {

    private static final Log log = Log.getLog(CopySourceCodeHandler.class);

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        SQLEditor editor = RuntimeUtils.getObjectAdapter(HandlerUtil.getActiveEditor(event), SQLEditor.class);
        if (editor == null) {
            return null;
        }

        ISelection selection = editor.getSelectionProvider().getSelection();
        if (selection.isEmpty() || !(selection instanceof TextSelection)) {
            return null;
        }

        TextSelection textSelection = (TextSelection) selection;
        if (textSelection.getLength() < 2) {
            // Use active query
            SQLScriptElement activeQuery = editor.extractActiveQuery();
            if (activeQuery != null && activeQuery.getLength() > 1) {
                textSelection = new TextSelection(editor.getDocument(), activeQuery.getOffset(), activeQuery.getLength());
            }
        }
        TargetFormatDialog dialog = new TargetFormatDialog(editor, textSelection);
        if (dialog.open() != IDialogConstants.OK_ID) {
            return null;
        }

        UIUtils.setClipboardContents(Display.getCurrent(), TextTransfer.getInstance(), dialog.getConvertedText());

        return null;
    }

    @Override
    public void updateElement(UIElement element, Map parameters) {
        element.setText(SQLEditorMessages.editors_sql_actions_copy_as_source_code);
        element.setTooltip(SQLEditorMessages.editors_sql_actions_copy_as_source_code_tip);
    }

    private static class TargetFormatDialog extends BaseSQLDialog {
        private static final String DIALOG_ID = "DBeaver.SQLTargetFormatDialog";//$NON-NLS-1$

        private final SQLEditor editor;
        private final TextSelection selection;
        private PropertyTreeViewer propsViewer;
        private StyledText targetText;

        private SQLTargetConverterDescriptor curFormat;
        private Map<String, Object> options = new HashMap<>();
        private String result;
        private PropertySourceCustom propertySource;

        TargetFormatDialog(SQLEditor editor, TextSelection selection) {
            super(editor.getSite(), SQLEditorMessages.sql_editor_menu_choose_format, null);
            this.editor = editor;
            this.selection = selection;
        }

        @Override
        protected IDialogSettings getDialogBoundsSettings()
        {
            return UIUtils.getDialogSettings(DIALOG_ID);
        }

        @Override
        protected Composite createDialogArea(Composite parent) {
            String formatId = getDialogBoundsSettings().get("format");
            if (formatId != null) {
                curFormat = SQLConverterRegistry.getInstance().getTargetConverter(formatId);
                if (curFormat == null) {
                    log.warn("Can't find SQL text converter '" + formatId + "'");
                }
            }

            Composite composite = super.createDialogArea(parent);
            ((GridLayout)composite.getLayout()).numColumns = 2;

            {
                final SelectionAdapter formatChangeListener = new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        if (((Button)e.widget).getSelection()) {
                            if (curFormat == e.widget.getData()) {
                                return;
                            }
                            saveOptions();
                            curFormat = (SQLTargetConverterDescriptor) e.widget.getData();
                            loadOptions();
                            onFormatChange();
                        }
                    }
                };

                Composite formatPanel = UIUtils.createPlaceholder(composite, 1);
                GridData gd = new GridData(GridData.FILL_BOTH);
                gd.minimumWidth = 200;
                formatPanel.setLayoutData(gd);
                Group formatsGroup = UIUtils.createControlGroup(formatPanel, SQLEditorMessages.sql_editor_panel_format, 1, GridData.FILL_HORIZONTAL, 0);
                for (SQLTargetConverterDescriptor converter : SQLConverterRegistry.getInstance().getTargetConverters()) {
                    Button formatButton = new Button(formatsGroup, SWT.RADIO);
                    formatButton.setText(converter.getLabel());
                    formatButton.setToolTipText(converter.getDescription());
                    formatButton.setData(converter);
                    if (curFormat == converter) {
                        formatButton.setSelection(true);
                    }
                    formatButton.addSelectionListener(formatChangeListener);
                }

                Group settingsGroup = UIUtils.createControlGroup(formatPanel, SQLEditorMessages.pref_page_sql_format_label_settings, 1, GridData.FILL_HORIZONTAL, 0);
                settingsGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
                propsViewer = new PropertyTreeViewer(settingsGroup, SWT.BORDER);
                propsViewer.getTree().addListener(SWT.Modify, new Listener() {
                    @Override
                    public void handleEvent(Event event) {
                        saveOptions();
                        refreshResult();
                        targetText.setText(result);
                    }
                });
            }

            {
                Composite previewPanel = new Composite(composite, SWT.NONE);
                previewPanel.setLayoutData(new GridData(GridData.FILL_BOTH));
                previewPanel.setLayout(new GridLayout(1, false));
                SashForm sash = new SashForm(previewPanel, SWT.VERTICAL);
                sash.setLayoutData(new GridData(GridData.FILL_BOTH));
                createSQLPanel(sash);
                Composite targetGroup = UIUtils.createPlaceholder(sash, 1, 5);
                targetGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
                UIUtils.createControlLabel(targetGroup, SQLEditorMessages.controls_querylog_column_result_name);
                targetText = new StyledText(targetGroup, SWT.BORDER | SWT.READ_ONLY | SWT.V_SCROLL | SWT.WRAP);
                targetText.setLayoutData(new GridData(GridData.FILL_BOTH));
                targetText.setFont(UIUtils.getMonospaceFont());
                StyledTextUtils.fillDefaultStyledTextContextMenu(targetText);
            }

            loadOptions();
            onFormatChange();

            return composite;
        }

        private void loadOptions() {
            options.clear();
            if (curFormat != null) {
                IDialogSettings formatSettings = UIUtils.getSettingsSection(getDialogBoundsSettings(), curFormat.getId());
                
                for (DBPPropertyDescriptor prop : curFormat.getProperties()) {
                    Object propValue = formatSettings.get(CommonUtils.toString(prop.getId()));
                    if (propValue == null) {
                        propValue = prop.getDefaultValue();
                    }
                    if (propValue != null) {
                        options.put(CommonUtils.toString(prop.getId()), propValue);
                    }
                }
            }
        }

        private void saveOptions() {
            if (propertySource != null && curFormat != null) {
                IDialogSettings formatSettings = UIUtils.getSettingsSection(getDialogBoundsSettings(), curFormat.getId());
                for (Map.Entry<String, Object> entry : propertySource.getPropertiesWithDefaults().entrySet()) {
                    options.put(CommonUtils.toString(entry.getKey()), entry.getValue());
                    formatSettings.put(CommonUtils.toString(entry.getKey()), CommonUtils.toString(entry.getValue()));
                }
            }
        }

        private void onFormatChange() {
            if (curFormat != null) {
                propertySource = new PropertySourceCustom(curFormat.getProperties(), options);
                propsViewer.loadProperties(propertySource);

                refreshResult();
            } else {
                result = "Choose format";
            }
            targetText.setText(result);
        }

        private void refreshResult() {
            try {
                ISQLTextConverter converter = curFormat.createInstance(ISQLTextConverter.class);
                result = converter.convertText(
                        editor.getSQLDialect(),
                        editor.getSyntaxManager(),
                        editor.getRuleScanner(),
                        editor.getDocument(),
                        selection.getOffset(),
                        selection.getLength(),
                        options);
            } catch (DBException e) {
                log.error(e);
                result = CommonUtils.notEmpty(e.getMessage());
            }
        }

        @Override
        protected void createButtonsForButtonBar(Composite parent)
        {
            super.createButtonsForButtonBar(parent);
            Button okButton = getButton(IDialogConstants.OK_ID);
            if (okButton != null) {
                okButton.setText(SQLEditorMessages.dialog_view_sql_button_copy);
            }
        }

        String getConvertedText() {
            return result;
        }

        @Override
        protected DBCExecutionContext getExecutionContext() {
            return editor.getExecutionContext();
        }

        @Override
        protected String getSQLText() {
            return this.selection.getText();
        }

        @Override
        protected void okPressed() {
            if (curFormat != null) {
                saveOptions();
                IDialogSettings dialogSettings = getDialogBoundsSettings();
                dialogSettings.put("format", curFormat.getId());
            }
            super.okPressed();
        }
    }

}
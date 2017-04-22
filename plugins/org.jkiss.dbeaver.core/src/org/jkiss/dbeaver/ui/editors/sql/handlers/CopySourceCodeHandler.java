/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ui.editors.sql.handlers;

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
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.sql.BaseSQLDialog;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.convert.ISQLTextConverter;
import org.jkiss.dbeaver.ui.editors.sql.convert.SQLConverterRegistry;
import org.jkiss.dbeaver.ui.editors.sql.convert.SQLTargetConverterDescriptor;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.util.HashMap;
import java.util.Map;

public class CopySourceCodeHandler extends AbstractHandler {

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

        TargetFormatDialog dialog = new TargetFormatDialog(editor, (TextSelection)selection);
        if (dialog.open() != IDialogConstants.OK_ID) {
            return null;
        }

        UIUtils.setClipboardContents(Display.getCurrent(), TextTransfer.getInstance(), dialog.getConvertedText());

        return null;
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

        TargetFormatDialog(SQLEditor editor, TextSelection selection) {
            super(editor.getSite(), "Choose format", null);
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
                            curFormat = (SQLTargetConverterDescriptor) e.widget.getData();
                            options.clear();
                            updateResult();
                        }
                    }
                };

                Composite formatPanel = UIUtils.createPlaceholder(composite, 1);
                formatPanel.setLayoutData(new GridData(GridData.FILL_BOTH));
                Group formatsGroup = UIUtils.createControlGroup(formatPanel, "Format", 1, GridData.FILL_HORIZONTAL, 0);
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

                Group settingsGroup = UIUtils.createControlGroup(formatPanel, "Settings", 1, GridData.FILL_HORIZONTAL, 0);
                settingsGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
                propsViewer = new PropertyTreeViewer(settingsGroup, SWT.BORDER);
            }

            {
                Composite previewPanel = new Composite(composite, SWT.NONE);
                previewPanel.setLayoutData(new GridData(GridData.FILL_BOTH));
                previewPanel.setLayout(new GridLayout(1, false));
                SashForm sash = new SashForm(previewPanel, SWT.VERTICAL);
                sash.setLayoutData(new GridData(GridData.FILL_BOTH));
                createSQLPanel(sash);
                Composite targetGroup = UIUtils.createPlaceholder(sash, 1);
                targetGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
                UIUtils.createControlLabel(targetGroup, "Result");
                targetText = new StyledText(targetGroup, SWT.BORDER | SWT.READ_ONLY | SWT.V_SCROLL | SWT.WRAP);
                targetText.setLayoutData(new GridData(GridData.FILL_BOTH));
            }

            updateResult();

            return composite;
        }

        private void updateResult() {
            try {
                if (curFormat != null) {
                    ISQLTextConverter converter = curFormat.createInstance();
                    result = converter.convertText(
                            editor.getSQLDialect(),
                            editor.getSyntaxManager(),
                            editor.getRuleManager(),
                            editor.getDocument(),
                            selection.getOffset(),
                            selection.getLength(), options);
                } else {
                    result = "Choose format";
                }
                targetText.setText(result);
            } catch (DBException e) {
                log.error(e);
                targetText.setText(CommonUtils.notEmpty(e.getMessage()));
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
            getDialogBoundsSettings().put("format", curFormat.getId());
            super.okPressed();
        }
    }

}
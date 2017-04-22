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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.model.exec.DBCExecutionContext;
import org.jkiss.dbeaver.registry.sql.SQLCommandsRegistry;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.sql.BaseSQLDialog;
import org.jkiss.dbeaver.ui.editors.sql.SQLEditor;
import org.jkiss.dbeaver.ui.editors.sql.convert.SQLConverterRegistry;
import org.jkiss.dbeaver.ui.editors.sql.convert.SQLTargetConverterDescriptor;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;
import org.jkiss.dbeaver.utils.RuntimeUtils;

public class CopySourceCodeHandler extends AbstractHandler {

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
            Composite composite = super.createDialogArea(parent);
            ((GridLayout)composite.getLayout()).numColumns = 2;

            {
                Composite formatPanel = UIUtils.createPlaceholder(composite, 1);
                formatPanel.setLayoutData(new GridData(GridData.FILL_BOTH));
                Group formatsGroup = UIUtils.createControlGroup(formatPanel, "Format", 1, GridData.FILL_HORIZONTAL, 0);
                for (SQLTargetConverterDescriptor converter : SQLConverterRegistry.getInstance().getTargetConverters()) {
                    Button formatButton = new Button(formatsGroup, SWT.RADIO);
                    formatButton.setText(converter.getLabel());
                    formatButton.setToolTipText(converter.getDescription());
                }

                Group settingsGroup = UIUtils.createControlGroup(formatPanel, "Settings", 1, GridData.FILL_HORIZONTAL, 0);
                settingsGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
                PropertyTreeViewer propsViewer = new PropertyTreeViewer(settingsGroup, SWT.BORDER);
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
                StyledText targetText = new StyledText(targetGroup, SWT.BORDER | SWT.READ_ONLY);
                targetText.setLayoutData(new GridData(GridData.FILL_BOTH));
            }

            return composite;
        }

        String getConvertedText() {
            return selection.getText();
        }

        @Override
        protected DBCExecutionContext getExecutionContext() {
            return editor.getExecutionContext();
        }

        @Override
        protected String getSQLText() {
            return this.selection.getText();
        }
    }

}
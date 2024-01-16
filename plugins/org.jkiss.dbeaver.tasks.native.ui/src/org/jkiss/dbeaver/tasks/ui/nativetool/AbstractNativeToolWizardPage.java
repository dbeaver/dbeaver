/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

package org.jkiss.dbeaver.tasks.ui.nativetool;

import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.swt.widgets.Text;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.tasks.nativetool.AbstractImportExportSettings;
import org.jkiss.dbeaver.tasks.nativetool.AbstractNativeToolSettings;
import org.jkiss.dbeaver.tasks.nativetool.NativeToolUtils;
import org.jkiss.dbeaver.tasks.ui.nativetool.internal.TaskNativeUIMessages;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.contentassist.ContentAssistUtils;
import org.jkiss.dbeaver.ui.contentassist.SmartTextContentAdapter;
import org.jkiss.dbeaver.ui.contentassist.StringContentProposalProvider;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.util.Arrays;

public abstract class AbstractNativeToolWizardPage<WIZARD extends AbstractNativeToolWizard> extends ActiveWizardPage<WIZARD> {

    protected final WIZARD wizard;

    protected Text outputFolderText;
    protected Text outputFileText;
    protected Text extraCommandArgsText;

    protected AbstractNativeToolWizardPage(WIZARD wizard, String pageName)
    {
        super(pageName);
        setPageComplete(false);
        this.wizard = wizard;
    }

    @Override
    protected boolean determinePageCompletion() {
        AbstractNativeToolSettings settings = wizard.getSettings();
        if (settings.getClientHome() == null) {
            setErrorMessage(TaskNativeUIMessages.tools_wizard_message_no_client_home);
            return false;
        }
        if (settings instanceof AbstractImportExportSettings
            && ((AbstractImportExportSettings) settings).getOutputFolderPattern() == null)
        {
            setErrorMessage(TaskNativeUIMessages.tools_wizard_message_no_output_folder);
            return false;
        }
        return super.determinePageCompletion();
    }

    protected void createCheckButtons(Composite buttonsPanel, final Table table) {
        UIUtils.createDialogButton(buttonsPanel, TaskNativeUIMessages.tools_wizard_page_dialog_button_all, new CheckListener(table, true));
        UIUtils.createDialogButton(buttonsPanel, TaskNativeUIMessages.tools_wizard_page_dialog_button_none, new CheckListener(table, false));
    }

    protected void createOutputFolderInput(@NotNull Composite outputGroup, @NotNull AbstractImportExportSettings settings) {
        outputFolderText = DialogUtils.createOutputFolderChooser(
            outputGroup,
            TaskNativeUIMessages.tools_wizard_page_dialog_folder_pattern,
            null,
            settings.getOutputFolderPattern(),
            wizard.getProject(),
            true,
            e -> updateState());
        outputFileText = UIUtils.createLabelText(
            outputGroup,
            TaskNativeUIMessages.tools_wizard_page_dialog_file_pattern,
            settings.getOutputFilePattern());
        UIUtils.setContentProposalToolTip(
            outputFileText,
            TaskNativeUIMessages.tools_wizard_page_dialog_folder_pattern_tip,
            NativeToolUtils.ALL_VARIABLES);

        ContentAssistUtils.installContentProposal(
            outputFileText,
            new SmartTextContentAdapter(),
            new StringContentProposalProvider(Arrays.stream(NativeToolUtils.ALL_VARIABLES)
                .map(GeneralUtils::variablePattern)
                .toArray(String[]::new)));
        outputFileText.addModifyListener(e -> settings.setOutputFilePattern(outputFileText.getText()));

        UIUtils.setContentProposalToolTip(
            outputFolderText,
            TaskNativeUIMessages.tools_wizard_page_dialog_file_pattern_tip,
            NativeToolUtils.LIMITED_VARIABLES);
        ContentAssistUtils.installContentProposal(
            outputFolderText,
            new SmartTextContentAdapter(),
            new StringContentProposalProvider(Arrays.stream(NativeToolUtils.LIMITED_VARIABLES)
                .map(GeneralUtils::variablePattern)
                .toArray(String[]::new)));
    }

    protected void createExtraArgsInput(Composite outputGroup) {
        extraCommandArgsText = UIUtils.createLabelText(outputGroup, TaskNativeUIMessages.tools_wizard_page_dialog_label_extra_command_args, wizard.getSettings().getExtraCommandArgs());
        extraCommandArgsText.setToolTipText(TaskNativeUIMessages.tools_wizard_page_dialog_tooltip_extra_command_args);
        ContentAssistUtils.installContentProposal(
            extraCommandArgsText,
            new SmartTextContentAdapter(),
            new StringContentProposalProvider(new String[]{}));
        extraCommandArgsText.addModifyListener(e -> wizard.getSettings().setExtraCommandArgs(extraCommandArgsText.getText()));

    }

    protected void fixOutputFileExtension() {
        String text = outputFileText.getText();
        String name;
        String ext;
        int idxOfExtStart = text.lastIndexOf('.');
        if (idxOfExtStart > -1 && idxOfExtStart <= text.length()) {
            name = text.substring(0, idxOfExtStart);
            ext = text.substring(idxOfExtStart + 1);
        } else {
            name = text;
            ext = "";
        }
        String newExt = getExtension();
        boolean isDotWithEmptyExt = ext.isEmpty() && idxOfExtStart > -1; // {file_name}.
        if (!isDotWithEmptyExt && ext.equalsIgnoreCase(newExt)) {
            return;
        }
        if (!newExt.isEmpty()) {
            newExt = "." + newExt;
        }
        text = name + newExt;
        outputFileText.setText(text);
    }

    protected String getExtension() {
        return "sql";
    }

    public void saveState() {
        if (extraCommandArgsText != null) {
            wizard.getSettings().setExtraCommandArgs(extraCommandArgsText.getText());
        }
    }

    protected void updateState() {
        saveState();

        setPageComplete(true);
    }

    protected void updateTableCheckedStatus(@NotNull Table table, boolean check) {
        // Handling "All" and "None" buttons
    }

    private class CheckListener extends SelectionAdapter {
        private final Table table;
        private final boolean check;

        public CheckListener(Table table, boolean check) {
            this.table = table;
            this.check = check;
        }

        @Override
        public void widgetSelected(SelectionEvent e) {
            for (TableItem item : table.getItems()) {
                item.setChecked(check);
            }
            updateTableCheckedStatus(table, check);
            updateState();
        }
    }

}

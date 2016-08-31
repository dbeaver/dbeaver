/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.tools.transfer.stream;

import org.eclipse.jface.fieldassist.SimpleContentProposalProvider;
import org.eclipse.jface.fieldassist.TextContentAdapter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.tools.transfer.wizard.DataTransferWizard;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

public class StreamConsumerPageOutput extends ActiveWizardPage<DataTransferWizard> {

    private Combo encodingCombo;
    private Label encodingBOMLabel;
    private Button encodingBOMCheckbox;
    private Text directoryText;
    private Text fileNameText;
    private Button compressCheckbox;
    private Button showFolderCheckbox;
    private Button clipboardCheck;

    public StreamConsumerPageOutput() {
        super(CoreMessages.data_transfer_wizard_output_name);
        setTitle(CoreMessages.data_transfer_wizard_output_title);
        setDescription(CoreMessages.data_transfer_wizard_output_description);
        setPageComplete(false);
    }

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        Composite composite = new Composite(parent, SWT.NULL);
        GridLayout gl = new GridLayout();
        gl.marginHeight = 0;
        gl.marginWidth = 0;
        composite.setLayout(gl);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        final StreamConsumerSettings settings = getWizard().getPageSettings(this, StreamConsumerSettings.class);

        {
            Group generalSettings = UIUtils.createControlGroup(composite, CoreMessages.data_transfer_wizard_output_group_general, 5, GridData.FILL_HORIZONTAL, 0);
            clipboardCheck = UIUtils.createLabelCheckbox(generalSettings, "Copy to clipboard", false);
            clipboardCheck.setLayoutData(new GridData(GridData.BEGINNING, GridData.BEGINNING, false, false, 4, 1));
            clipboardCheck.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    settings.setOutputClipboard(clipboardCheck.getSelection());
                    toggleClipboardOutput();
                    updatePageCompletion();
                }
            });
            directoryText = DialogUtils.createOutputFolderChooser(generalSettings, null, new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e) {
                    settings.setOutputFolder(directoryText.getText());
                    updatePageCompletion();
                }
            });
            ((GridData)directoryText.getParent().getLayoutData()).horizontalSpan = 4;

            UIUtils.createControlLabel(generalSettings, CoreMessages.data_transfer_wizard_output_label_file_name_pattern);
            fileNameText = new Text(generalSettings, SWT.BORDER);
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 4;
            fileNameText.setToolTipText("Output file name pattern. Allowed variables: ${table} and ${timestamp}.");
            fileNameText.setLayoutData(gd);
            fileNameText.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e) {
                    settings.setOutputFilePattern(fileNameText.getText());
                    updatePageCompletion();
                }
            });
            UIUtils.installContentProposal(
                fileNameText,
                new TextContentAdapter(),
                new SimpleContentProposalProvider(new String[] { "${table}", "${timestamp}"} ));

            {
                UIUtils.createControlLabel(generalSettings, CoreMessages.data_transfer_wizard_output_label_encoding);
                encodingCombo = UIUtils.createEncodingCombo(generalSettings, settings.getOutputEncoding());
                //encodingCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, true, false, 1, 1));
                encodingCombo.addModifyListener(new ModifyListener() {
                    @Override
                    public void modifyText(ModifyEvent e) {
                        int index = encodingCombo.getSelectionIndex();
                        if (index >= 0) {
                            settings.setOutputEncoding(encodingCombo.getItem(index));
                        }
                        updatePageCompletion();
                    }
                });
                encodingBOMLabel = UIUtils.createControlLabel(generalSettings, CoreMessages.data_transfer_wizard_output_label_insert_bom);
                encodingBOMLabel.setToolTipText(CoreMessages.data_transfer_wizard_output_label_insert_bom_tooltip);
                encodingBOMCheckbox = new Button(generalSettings, SWT.CHECK);
                encodingBOMCheckbox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END, GridData.VERTICAL_ALIGN_BEGINNING, true, false, 1, 1));
                encodingBOMCheckbox.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        settings.setOutputEncodingBOM(encodingBOMCheckbox.getSelection());
                    }
                });
                new Label(generalSettings, SWT.NONE);
            }

            compressCheckbox = UIUtils.createLabelCheckbox(generalSettings, CoreMessages.data_transfer_wizard_output_checkbox_compress, false);
            compressCheckbox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, true, false, 4, 1));
            compressCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    settings.setCompressResults(compressCheckbox.getSelection());
                }
            });
        }

        {
            Group generalSettings = UIUtils.createControlGroup(composite, CoreMessages.data_transfer_wizard_output_group_progress, 4, GridData.FILL_HORIZONTAL, 0);

            showFolderCheckbox = UIUtils.createLabelCheckbox(generalSettings, CoreMessages.data_transfer_wizard_output_checkbox_open_folder, true);
            showFolderCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    settings.setOpenFolderOnFinish(showFolderCheckbox.getSelection());
                }
            });
            showFolderCheckbox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, false, false, 3, 1));
        }

        setControl(composite);

    }

    private void toggleClipboardOutput() {
        boolean clipboard = clipboardCheck.getSelection();
        directoryText.setEnabled(!clipboard);
        fileNameText.setEnabled(!clipboard);
        compressCheckbox.setEnabled(!clipboard);
        encodingCombo.setEnabled(!clipboard);
        encodingBOMLabel.setEnabled(!clipboard);
        encodingBOMCheckbox.setEnabled(!clipboard);
        showFolderCheckbox.setEnabled(!clipboard);
    }

    @Override
    public void activatePage()
    {
        final StreamConsumerSettings settings = getWizard().getPageSettings(this, StreamConsumerSettings.class);

        clipboardCheck.setSelection(settings.isOutputClipboard());
        directoryText.setText(settings.getOutputFolder());
        fileNameText.setText(settings.getOutputFilePattern());
        compressCheckbox.setSelection(settings.isCompressResults());
        encodingCombo.setText(settings.getOutputEncoding());
        encodingBOMCheckbox.setSelection(settings.isOutputEncodingBOM());
        showFolderCheckbox.setSelection(settings.isOpenFolderOnFinish());

        updatePageCompletion();
        toggleClipboardOutput();
    }

    @Override
    protected boolean determinePageCompletion()
    {
        final StreamConsumerSettings settings = getWizard().getPageSettings(this, StreamConsumerSettings.class);

        int selectionIndex = encodingCombo.getSelectionIndex();
        String encoding = null;
        if (selectionIndex >= 0) {
            encoding = encodingCombo.getItem(selectionIndex);
        }
        if (settings.isOutputClipboard() || encoding == null || GeneralUtils.getCharsetBOM(encoding) == null) {
            encodingBOMLabel.setEnabled(false);
            encodingBOMCheckbox.setEnabled(false);
        } else {
            encodingBOMLabel.setEnabled(true);
            encodingBOMCheckbox.setEnabled(true);
        }

        if (settings.isOutputClipboard()) {
            return true;
        }

        boolean valid = true;
        if (CommonUtils.isEmpty(settings.getOutputFolder())) {
            valid = false;
        }
        if (CommonUtils.isEmpty(settings.getOutputFilePattern())) {
            valid = false;
        }
        return valid;
    }

}
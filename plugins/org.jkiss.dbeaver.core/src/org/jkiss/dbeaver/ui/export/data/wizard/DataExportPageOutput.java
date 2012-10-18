/*
 * Copyright (C) 2010-2012 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ui.export.data.wizard;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.utils.CommonUtils;

class DataExportPageOutput extends ActiveWizardPage<DataExportWizard> {

    private static final int EXTRACT_TYPE_SINGLE_QUERY = 0;
    private static final int EXTRACT_TYPE_SEGMENTS = 1;

    private Combo encodingCombo;
    private Label encodingBOMLabel;
    private Button encodingBOMCheckbox;
    private Text directoryText;
    private Text fileNameText;
    private Button compressCheckbox;
    private Spinner threadsNumText;
    private Combo rowsExtractType;
    private Label segmentSizeLabel;
    private Text segmentSizeText;
    private Button newConnectionCheckbox;
    private Button rowCountCheckbox;
    private Button showFolderCheckbox;

    DataExportPageOutput() {
        super(CoreMessages.dialog_export_wizard_output_name);
        setTitle(CoreMessages.dialog_export_wizard_output_title);
        setDescription(CoreMessages.dialog_export_wizard_output_description);
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

        {
            Group generalSettings = UIUtils.createControlGroup(composite, CoreMessages.dialog_export_wizard_output_group_general, 5, GridData.FILL_HORIZONTAL, 0);
            directoryText = UIUtils.createOutputFolderChooser(generalSettings, new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e) {
                    getWizard().getSettings().setOutputFolder(directoryText.getText());
                    updatePageCompletion();
                }
            });
            ((GridData)directoryText.getLayoutData()).horizontalSpan = 3;

            UIUtils.createControlLabel(generalSettings, CoreMessages.dialog_export_wizard_output_label_file_name_pattern);
            fileNameText = new Text(generalSettings, SWT.BORDER);
            GridData gd = new GridData(GridData.FILL_HORIZONTAL);
            gd.horizontalSpan = 4;
            fileNameText.setLayoutData(gd);
            fileNameText.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e) {
                    getWizard().getSettings().setOutputFilePattern(fileNameText.getText());
                    updatePageCompletion();
                }
            });

            {
                UIUtils.createControlLabel(generalSettings, CoreMessages.dialog_export_wizard_output_label_encoding);
                encodingCombo = UIUtils.createEncodingCombo(generalSettings, getWizard().getSettings().getOutputEncoding());
                //encodingCombo.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, true, false, 1, 1));
                encodingCombo.addModifyListener(new ModifyListener() {
                    @Override
                    public void modifyText(ModifyEvent e) {
                        int index = encodingCombo.getSelectionIndex();
                        if (index >= 0) {
                            getWizard().getSettings().setOutputEncoding(encodingCombo.getItem(index));
                        }
                        updatePageCompletion();
                    }
                });
                encodingBOMLabel = UIUtils.createControlLabel(generalSettings, CoreMessages.dialog_export_wizard_output_label_insert_bom);
                encodingBOMLabel.setToolTipText(CoreMessages.dialog_export_wizard_output_label_insert_bom_tooltip);
                encodingBOMCheckbox = new Button(generalSettings, SWT.CHECK);
                encodingBOMCheckbox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END, GridData.VERTICAL_ALIGN_BEGINNING, true, false, 1, 1));
                encodingBOMCheckbox.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        getWizard().getSettings().setOutputEncodingBOM(encodingBOMCheckbox.getSelection());
                    }
                });
                new Label(generalSettings, SWT.NONE);
            }

            compressCheckbox = UIUtils.createLabelCheckbox(generalSettings, CoreMessages.dialog_export_wizard_output_checkbox_compress, false);
            compressCheckbox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, true, false, 4, 1));
            compressCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    getWizard().getSettings().setCompressResults(compressCheckbox.getSelection());
                }
            });
        }

        {
            Group generalSettings = UIUtils.createControlGroup(composite, CoreMessages.dialog_export_wizard_output_group_progress, 4, GridData.FILL_HORIZONTAL, 0);

            Label threadsNumLabel = UIUtils.createControlLabel(generalSettings, CoreMessages.dialog_export_wizard_output_label_max_threads);
            threadsNumText = new Spinner(generalSettings, SWT.BORDER);
            threadsNumText.setMinimum(1);
            threadsNumText.setMaximum(10);
            threadsNumText.addModifyListener(new ModifyListener() {
                @Override
                public void modifyText(ModifyEvent e) {
                    try {
                        getWizard().getSettings().setMaxJobCount(Integer.parseInt(threadsNumText.getText()));
                    } catch (NumberFormatException e1) {
                        // do nothing
                    }
                }
            });
            if (getWizard().getSettings().getDataProviders().size() < 2) {
                threadsNumLabel.setEnabled(false);
                threadsNumText.setEnabled(false);
            }
            threadsNumText.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, false, false, 3, 1));

            if (false) {
                UIUtils.createControlLabel(generalSettings, CoreMessages.dialog_export_wizard_output_label_extract_type);
                rowsExtractType = new Combo(generalSettings, SWT.DROP_DOWN | SWT.READ_ONLY);
                rowsExtractType.setItems(new String[] {
                    CoreMessages.dialog_export_wizard_output_combo_extract_type_item_single_query,
                    CoreMessages.dialog_export_wizard_output_combo_extract_type_item_by_segments });
                rowsExtractType.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        DataExportSettings exportSettings = getWizard().getSettings();
                        switch (rowsExtractType.getSelectionIndex()) {
                            case EXTRACT_TYPE_SEGMENTS: exportSettings.setExtractType(DataExportSettings.ExtractType.SEGMENTS); break;
                            case EXTRACT_TYPE_SINGLE_QUERY: exportSettings.setExtractType(DataExportSettings.ExtractType.SINGLE_QUERY); break;
                        }
                        updatePageCompletion();
                    }
                });

                segmentSizeLabel = UIUtils.createControlLabel(generalSettings, CoreMessages.dialog_export_wizard_output_label_segment_size);
                segmentSizeLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END, GridData.VERTICAL_ALIGN_BEGINNING, false, false, 1, 1));
                segmentSizeText = new Text(generalSettings, SWT.BORDER);
                segmentSizeText.addModifyListener(new ModifyListener() {
                    @Override
                    public void modifyText(ModifyEvent e)
                    {
                        try {
                            getWizard().getSettings().setSegmentSize(Integer.parseInt(segmentSizeText.getText()));
                        } catch (NumberFormatException e1) {
                            // just skip it
                        }
                    }
                });
                segmentSizeText.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END, GridData.VERTICAL_ALIGN_BEGINNING, false, false, 1, 1));
            }

            newConnectionCheckbox = UIUtils.createLabelCheckbox(generalSettings, CoreMessages.dialog_export_wizard_output_checkbox_new_connection, true);
            newConnectionCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    getWizard().getSettings().setOpenNewConnections(newConnectionCheckbox.getSelection());
                }
            });
            newConnectionCheckbox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, false, false, 3, 1));

            rowCountCheckbox = UIUtils.createLabelCheckbox(generalSettings, CoreMessages.dialog_export_wizard_output_checkbox_select_row_count, true);
            rowCountCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    getWizard().getSettings().setQueryRowCount(rowCountCheckbox.getSelection());
                }
            });
            rowCountCheckbox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, false, false, 3, 1));

            showFolderCheckbox = UIUtils.createLabelCheckbox(generalSettings, CoreMessages.dialog_export_wizard_output_checkbox_open_folder, true);
            showFolderCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    getWizard().getSettings().setOpenFolderOnFinish(showFolderCheckbox.getSelection());
                }
            });
            showFolderCheckbox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, false, false, 3, 1));
        }

        setControl(composite);

    }

    @Override
    public void activatePage()
    {
        DataExportSettings exportSettings = getWizard().getSettings();
        directoryText.setText(exportSettings.getOutputFolder());
        fileNameText.setText(exportSettings.getOutputFilePattern());
        threadsNumText.setSelection(exportSettings.getMaxJobCount());
        newConnectionCheckbox.setSelection(exportSettings.isOpenNewConnections());
        rowCountCheckbox.setSelection(exportSettings.isQueryRowCount());
        compressCheckbox.setSelection(exportSettings.isCompressResults());
        encodingCombo.setText(exportSettings.getOutputEncoding());
        encodingBOMCheckbox.setSelection(exportSettings.isOutputEncodingBOM());
        showFolderCheckbox.setSelection(exportSettings.isOpenFolderOnFinish());

        if (segmentSizeText != null) {
            segmentSizeText.setText(String.valueOf(exportSettings.getSegmentSize()));
            switch (exportSettings.getExtractType()) {
                case SINGLE_QUERY: rowsExtractType.select(EXTRACT_TYPE_SINGLE_QUERY); break;
                case SEGMENTS: rowsExtractType.select(EXTRACT_TYPE_SEGMENTS); break;
            }
        }

        updatePageCompletion();
    }

    @Override
    protected boolean determinePageCompletion()
    {
        if (rowsExtractType != null) {
            int selectionIndex = rowsExtractType.getSelectionIndex();
            if (selectionIndex == EXTRACT_TYPE_SEGMENTS) {
                segmentSizeLabel.setVisible(true);
                segmentSizeText.setVisible(true);
            } else {
                segmentSizeLabel.setVisible(false);
                segmentSizeText.setVisible(false);
            }
        }
        int selectionIndex = encodingCombo.getSelectionIndex();
        String encoding = null;
        if (selectionIndex >= 0) {
            encoding = encodingCombo.getItem(selectionIndex);
        }
        if (encoding == null || ContentUtils.getCharsetBOM(encoding) == null) {
            encodingBOMLabel.setEnabled(false);
            encodingBOMCheckbox.setEnabled(false);
        } else {
            encodingBOMLabel.setEnabled(true);
            encodingBOMCheckbox.setEnabled(true);
        }

        boolean valid = true;
        if (CommonUtils.isEmpty(getWizard().getSettings().getOutputFolder())) {
            valid = false;
        }
        if (CommonUtils.isEmpty(getWizard().getSettings().getOutputFilePattern())) {
            valid = false;
        }
        return valid;
    }

}
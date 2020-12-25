/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.tools.transfer.ui.pages.database;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.jkiss.dbeaver.model.data.DBDCellValue;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseProducerSettings;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.ui.internal.DTUIMessages;
import org.jkiss.dbeaver.tools.transfer.ui.wizard.DataTransferWizard;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;

import java.util.Locale;

public class DatabaseProducerPageExtractSettings extends ActiveWizardPage<DataTransferWizard> {

    private static final int EXTRACT_TYPE_SINGLE_QUERY = 0;
    private static final int EXTRACT_TYPE_SEGMENTS = 1;

    private Text threadsNumText;
    private Combo rowsExtractType;
    private Label segmentSizeLabel;
    private Text segmentSizeText;
    private Button newConnectionCheckbox;
    private Button rowCountCheckbox;
    private Button selectedColumnsOnlyCheckbox;
    private Button selectedRowsOnlyCheckbox;
    private Text fetchSizeText;

    public DatabaseProducerPageExtractSettings() {
        super(DTUIMessages.database_producer_page_extract_settings_name_and_title);
        setTitle(DTUIMessages.database_producer_page_extract_settings_name_and_title);
        setDescription(DTUIMessages.database_producer_page_extract_settings_description);
        setPageComplete(false);
    }

    @Override
    public void createControl(Composite parent) {
        initializeDialogUnits(parent);

        Composite composite = UIUtils.createComposite(parent, 1);

        final DatabaseProducerSettings settings = getWizard().getPageSettings(this, DatabaseProducerSettings.class);

        {
            Group generalSettings = UIUtils.createControlGroup(composite, DTMessages.data_transfer_wizard_output_group_progress, 4, GridData.FILL_HORIZONTAL, 0);

            Label threadsNumLabel = UIUtils.createControlLabel(generalSettings, DTMessages.data_transfer_wizard_output_label_max_threads);
            threadsNumText = new Text(generalSettings, SWT.BORDER);
            threadsNumText.setToolTipText(DTUIMessages.database_producer_page_extract_settings_threads_num_text_tooltip);
            threadsNumText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.ENGLISH));
            threadsNumText.addModifyListener(e -> {
                try {
                    getWizard().getSettings().setMaxJobCount(Integer.parseInt(threadsNumText.getText()));
                } catch (NumberFormatException e1) {
                    // do nothing
                }
            });
            if (getWizard().getSettings().getDataPipes().size() < 2) {
                threadsNumLabel.setEnabled(false);
                threadsNumText.setEnabled(false);
            }
            threadsNumText.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, false, false, 3, 1));

            {

                UIUtils.createControlLabel(generalSettings, DTMessages.data_transfer_wizard_output_label_extract_type);
                rowsExtractType = new Combo(generalSettings, SWT.DROP_DOWN | SWT.READ_ONLY);
                rowsExtractType.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, false, false, 3, 1));
                rowsExtractType.setItems(
                    DTMessages.data_transfer_wizard_output_combo_extract_type_item_single_query,
                    DTMessages.data_transfer_wizard_output_combo_extract_type_item_by_segments);
                rowsExtractType.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        switch (rowsExtractType.getSelectionIndex()) {
                            case EXTRACT_TYPE_SEGMENTS: settings.setExtractType(DatabaseProducerSettings.ExtractType.SEGMENTS); break;
                            case EXTRACT_TYPE_SINGLE_QUERY: settings.setExtractType(DatabaseProducerSettings.ExtractType.SINGLE_QUERY); break;
                        }
                        updatePageCompletion();
                    }
                });

                segmentSizeLabel = UIUtils.createControlLabel(generalSettings, DTMessages.data_transfer_wizard_output_label_segment_size);
                segmentSizeLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, false, false, 1, 1));
                segmentSizeText = new Text(generalSettings, SWT.BORDER);
                segmentSizeText.addModifyListener(e -> {
                    try {
                        settings.setSegmentSize(Integer.parseInt(segmentSizeText.getText()));
                    } catch (NumberFormatException e1) {
                        // just skip it
                    }
                });
                segmentSizeText.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, false, false, 1, 1));
            }

            newConnectionCheckbox = UIUtils.createCheckbox(generalSettings, DTMessages.data_transfer_wizard_output_checkbox_new_connection, DTUIMessages.database_producer_page_extract_settings_new_connection_checkbox_tooltip, true, 4);
            newConnectionCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    settings.setOpenNewConnections(newConnectionCheckbox.getSelection());
                }
            });

            rowCountCheckbox = UIUtils.createCheckbox(generalSettings, DTMessages.data_transfer_wizard_output_checkbox_select_row_count, DTUIMessages.database_producer_page_extract_settings_row_count_checkbox_tooltip, true, 4);
            rowCountCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    settings.setQueryRowCount(rowCountCheckbox.getSelection());
                }
            });

            fetchSizeText = UIUtils.createLabelText(generalSettings, DTUIMessages.database_producer_page_extract_settings_text_fetch_size_label, "", SWT.BORDER);
            fetchSizeText.setToolTipText(DTUIMessages.database_producer_page_extract_settings_text_fetch_size_tooltip);
            fetchSizeText.addVerifyListener(UIUtils.getIntegerVerifyListener(Locale.ENGLISH));
            fetchSizeText.addModifyListener(e -> {
                settings.setFetchSize(Integer.parseInt(fetchSizeText.getText()));
            });

            IStructuredSelection curSelection = getWizard().getCurrentSelection();
            boolean hasSelection = curSelection != null && !curSelection.isEmpty() && curSelection.getFirstElement() instanceof DBDCellValue;

            if (hasSelection) {
                selectedColumnsOnlyCheckbox = UIUtils.createCheckbox(generalSettings, DTMessages.data_transfer_wizard_output_checkbox_selected_columns_only, null, false, 4);
                selectedColumnsOnlyCheckbox.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        settings.setSelectedColumnsOnly(selectedColumnsOnlyCheckbox.getSelection());
                    }
                });

                selectedRowsOnlyCheckbox = UIUtils.createCheckbox(generalSettings, DTMessages.data_transfer_wizard_output_checkbox_selected_rows_only, null, false, 4);
                selectedRowsOnlyCheckbox.addSelectionListener(new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        settings.setSelectedRowsOnly(selectedRowsOnlyCheckbox.getSelection());
                    }
                });

                SelectionAdapter listener = new SelectionAdapter() {
                    @Override
                    public void widgetSelected(SelectionEvent e) {
                        boolean selection = selectedColumnsOnlyCheckbox.getSelection() || selectedRowsOnlyCheckbox.getSelection();
                        newConnectionCheckbox.setEnabled(!selection);
                    }
                };
                selectedColumnsOnlyCheckbox.addSelectionListener(listener);
                selectedRowsOnlyCheckbox.addSelectionListener(listener);
            }
        }

        setControl(composite);

    }

    @Override
    public void activatePage()
    {
        final DatabaseProducerSettings settings = getWizard().getPageSettings(this, DatabaseProducerSettings.class);

        threadsNumText.setText(String.valueOf(getWizard().getSettings().getMaxJobCount()));
        newConnectionCheckbox.setSelection(settings.isOpenNewConnections());
        rowCountCheckbox.setSelection(settings.isQueryRowCount());

        if (segmentSizeText != null) {
            segmentSizeText.setText(String.valueOf(settings.getSegmentSize()));
            switch (settings.getExtractType()) {
                case SINGLE_QUERY: rowsExtractType.select(EXTRACT_TYPE_SINGLE_QUERY); break;
                case SEGMENTS: rowsExtractType.select(EXTRACT_TYPE_SEGMENTS); break;
            }
        }
        fetchSizeText.setText(String.valueOf(settings.getFetchSize()));
        if (selectedColumnsOnlyCheckbox != null) {
            selectedColumnsOnlyCheckbox.setSelection(settings.isSelectedColumnsOnly());
        }
        if (selectedRowsOnlyCheckbox != null) {
            selectedRowsOnlyCheckbox.setSelection(settings.isSelectedRowsOnly());
        }

        updatePageCompletion();
    }

    @Override
    protected boolean determinePageCompletion()
    {
        if (rowsExtractType != null) {
            int selectionIndex = rowsExtractType.getSelectionIndex();
            if (selectionIndex == EXTRACT_TYPE_SEGMENTS) {
                segmentSizeLabel.setEnabled(true);
                segmentSizeText.setEnabled(true);
            } else {
                segmentSizeLabel.setEnabled(false);
                segmentSizeText.setEnabled(false);
            }
        }
        return true;
    }

}
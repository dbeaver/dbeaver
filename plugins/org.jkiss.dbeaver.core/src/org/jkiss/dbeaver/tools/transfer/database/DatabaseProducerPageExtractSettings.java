/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.tools.transfer.database;

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

public class DatabaseProducerPageExtractSettings extends ActiveWizardPage<DataTransferWizard> {

    private static final int EXTRACT_TYPE_SINGLE_QUERY = 0;
    private static final int EXTRACT_TYPE_SEGMENTS = 1;

    private Spinner threadsNumText;
    private Combo rowsExtractType;
    private Label segmentSizeLabel;
    private Text segmentSizeText;
    private Button newConnectionCheckbox;
    private Button rowCountCheckbox;

    public DatabaseProducerPageExtractSettings() {
        super("Extraction settings");
        setTitle("Extraction settings");
        setDescription("Database table(s) extraction settings");
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

        final DatabaseProducerSettings settings = getWizard().getPageSettings(this, DatabaseProducerSettings.class);

        {
            Group generalSettings = UIUtils.createControlGroup(composite, CoreMessages.data_transfer_wizard_output_group_progress, 4, GridData.FILL_HORIZONTAL, 0);

            Label threadsNumLabel = UIUtils.createControlLabel(generalSettings, CoreMessages.data_transfer_wizard_output_label_max_threads);
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
            if (getWizard().getSettings().getDataPipes().size() < 2) {
                threadsNumLabel.setEnabled(false);
                threadsNumText.setEnabled(false);
            }
            threadsNumText.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, false, false, 3, 1));

            {
                UIUtils.createControlLabel(generalSettings, CoreMessages.data_transfer_wizard_output_label_extract_type);
                rowsExtractType = new Combo(generalSettings, SWT.DROP_DOWN | SWT.READ_ONLY);
                rowsExtractType.setItems(new String[] {
                    CoreMessages.data_transfer_wizard_output_combo_extract_type_item_single_query,
                    CoreMessages.data_transfer_wizard_output_combo_extract_type_item_by_segments });
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

                segmentSizeLabel = UIUtils.createControlLabel(generalSettings, CoreMessages.data_transfer_wizard_output_label_segment_size);
                segmentSizeLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END, GridData.VERTICAL_ALIGN_BEGINNING, false, false, 1, 1));
                segmentSizeText = new Text(generalSettings, SWT.BORDER);
                segmentSizeText.addModifyListener(new ModifyListener() {
                    @Override
                    public void modifyText(ModifyEvent e)
                    {
                        try {
                            settings.setSegmentSize(Integer.parseInt(segmentSizeText.getText()));
                        } catch (NumberFormatException e1) {
                            // just skip it
                        }
                    }
                });
                segmentSizeText.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_END, GridData.VERTICAL_ALIGN_BEGINNING, false, false, 1, 1));
            }

            newConnectionCheckbox = UIUtils.createLabelCheckbox(generalSettings, CoreMessages.data_transfer_wizard_output_checkbox_new_connection, true);
            newConnectionCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    settings.setOpenNewConnections(newConnectionCheckbox.getSelection());
                }
            });
            newConnectionCheckbox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, false, false, 3, 1));

            rowCountCheckbox = UIUtils.createLabelCheckbox(generalSettings, CoreMessages.data_transfer_wizard_output_checkbox_select_row_count, true);
            rowCountCheckbox.addSelectionListener(new SelectionAdapter() {
                @Override
                public void widgetSelected(SelectionEvent e) {
                    settings.setQueryRowCount(rowCountCheckbox.getSelection());
                }
            });
            rowCountCheckbox.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_BEGINNING, GridData.VERTICAL_ALIGN_BEGINNING, false, false, 3, 1));
        }

        setControl(composite);

    }

    @Override
    public void activatePage()
    {
        final DatabaseProducerSettings settings = getWizard().getPageSettings(this, DatabaseProducerSettings.class);

        newConnectionCheckbox.setSelection(settings.isOpenNewConnections());
        rowCountCheckbox.setSelection(settings.isQueryRowCount());

        if (segmentSizeText != null) {
            segmentSizeText.setText(String.valueOf(settings.getSegmentSize()));
            switch (settings.getExtractType()) {
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
        return true;
    }

}
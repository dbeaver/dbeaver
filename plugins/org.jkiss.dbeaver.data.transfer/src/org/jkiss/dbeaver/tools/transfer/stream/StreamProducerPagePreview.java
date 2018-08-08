/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2018 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.tools.transfer.stream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferProcessorDescriptor;
import org.jkiss.dbeaver.tools.transfer.wizard.DataTransferPipe;
import org.jkiss.dbeaver.tools.transfer.wizard.DataTransferSettings;
import org.jkiss.dbeaver.tools.transfer.wizard.DataTransferWizard;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.ui.properties.PropertyTreeViewer;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class StreamProducerPagePreview extends ActiveWizardPage<DataTransferWizard> {

    private Table previewTable;

    public StreamProducerPagePreview() {
        super(DTMessages.data_transfer_wizard_page_preview_name);
        setTitle(DTMessages.data_transfer_wizard_page_preview_title);
        setDescription(DTMessages.data_transfer_wizard_page_preview_description);
        setPageComplete(false);
    }

    @Override
    public void createControl(Composite parent) {

        initializeDialogUnits(parent);

        Composite composite = new Composite(parent, SWT.NULL);
        composite.setLayout(new GridLayout());
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            Group previewGroup = new Group(composite, SWT.NONE);
            previewGroup.setText(DTMessages.data_transfer_wizard_settings_group_input_files);
            previewGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
            previewGroup.setLayout(new GridLayout(1, false));

            previewTable = new Table(previewGroup, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
            previewTable.setLayoutData(new GridData(GridData.FILL_BOTH));
            previewTable.setHeaderVisible(true);
            previewTable.setLinesVisible(true);

            UIUtils.createTableColumn(previewTable, SWT.LEFT, DTMessages.data_transfer_wizard_final_column_source);
            UIUtils.createTableColumn(previewTable, SWT.LEFT, DTMessages.data_transfer_wizard_final_column_target);

            UIUtils.asyncExec(() -> UIUtils.packColumns(previewTable, true));
        }

        setControl(composite);
    }

    @Override
    public void activatePage() {
        DataTransferProcessorDescriptor processor = getWizard().getSettings().getProcessor();


        updatePageCompletion();
    }

    @Override
    public void deactivatePage()
    {
        super.deactivatePage();
    }

    @Override
    protected boolean determinePageCompletion()
    {

        return true;
    }

}
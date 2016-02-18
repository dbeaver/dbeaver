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
package org.jkiss.dbeaver.tools.transfer.wizard;

import org.jkiss.dbeaver.Log;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProcessor;
import org.jkiss.dbeaver.tools.transfer.IDataTransferSettings;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;

import java.util.List;

class DataTransferPageFinal extends ActiveWizardPage<DataTransferWizard> {

    static final Log log = Log.getLog(DataTransferPageFinal.class);

    private Table resultTable;
    private boolean activated = false;

    DataTransferPageFinal() {
        super(CoreMessages.data_transfer_wizard_final_name);
        setTitle(CoreMessages.data_transfer_wizard_final_title);
        setDescription(CoreMessages.data_transfer_wizard_final_description);
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
            Group tablesGroup = UIUtils.createControlGroup(composite, CoreMessages.data_transfer_wizard_final_group_tables, 3, GridData.FILL_BOTH, 0);

            resultTable = new Table(tablesGroup, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
            resultTable.setLayoutData(new GridData(GridData.FILL_BOTH));
            resultTable.setHeaderVisible(true);
            resultTable.setLinesVisible(true);

            UIUtils.createTableColumn(resultTable, SWT.LEFT, CoreMessages.data_transfer_wizard_final_column_source);
            UIUtils.createTableColumn(resultTable, SWT.LEFT, CoreMessages.data_transfer_wizard_final_column_target);

            UIUtils.packColumns(resultTable);
        }

        setControl(composite);
    }

    @Override
    public void activatePage()
    {
        resultTable.removeAll();
        DataTransferSettings settings = getWizard().getSettings();
        List<DataTransferPipe> dataPipes = settings.getDataPipes();
        for (DataTransferPipe pipe : dataPipes) {
            IDataTransferSettings consumerSettings = settings.getNodeSettings(pipe.getConsumer());
            IDataTransferProcessor processor = null;
            if (settings.getProcessor() != null) {
                // Processor is optional
                try {
                    processor = settings.getProcessor().getInstance();
                } catch (Throwable e) {
                    log.error("Can't create processor", e);
                    continue;
                }
            }
            pipe.getConsumer().initTransfer(
                pipe.getProducer().getSourceObject(),
                consumerSettings,
                processor,
                processor == null ?
                    null :
                    settings.getProcessorProperties());
            TableItem item = new TableItem(resultTable, SWT.NONE);
            item.setText(0, DBUtils.getObjectFullName(pipe.getProducer().getSourceObject()));
            if (settings.getProducer() != null && settings.getProducer().getIcon() != null) {
                item.setImage(0, DBeaverIcons.getImage(settings.getProducer().getIcon()));
            }
            item.setText(1, pipe.getConsumer().getTargetName());
            if (settings.getProcessor() != null && settings.getProcessor().getIcon() != null) {
                item.setImage(1, DBeaverIcons.getImage(settings.getProcessor().getIcon()));
            } else if (settings.getConsumer() != null && settings.getConsumer().getIcon() != null) {
                item.setImage(1, DBeaverIcons.getImage(settings.getConsumer().getIcon()));
            }
        }
        activated = true;
        UIUtils.packColumns(resultTable, true);
        updatePageCompletion();
    }

    public boolean isActivated()
    {
        return activated;
    }

    @Override
    protected boolean determinePageCompletion()
    {
        return activated;
    }

}
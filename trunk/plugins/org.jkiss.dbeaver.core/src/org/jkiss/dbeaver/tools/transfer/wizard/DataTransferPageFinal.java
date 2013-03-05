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
package org.jkiss.dbeaver.tools.transfer.wizard;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableItem;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProcessor;
import org.jkiss.dbeaver.tools.transfer.IDataTransferSettings;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.ActiveWizardPage;

import java.util.List;

class DataTransferPageFinal extends ActiveWizardPage<DataTransferWizard> {

    static final Log log = LogFactory.getLog(DataTransferPageFinal.class);

    private Table resultTable;

    DataTransferPageFinal() {
        super(CoreMessages.dialog_export_wizard_final_name);
        setTitle(CoreMessages.dialog_export_wizard_final_title);
        setDescription(CoreMessages.dialog_export_wizard_final_description);
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
            Group tablesGroup = UIUtils.createControlGroup(composite, CoreMessages.dialog_export_wizard_final_group_tables, 3, GridData.FILL_BOTH, 0);

            resultTable = new Table(tablesGroup, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
            resultTable.setLayoutData(new GridData(GridData.FILL_BOTH));
            resultTable.setHeaderVisible(true);
            resultTable.setLinesVisible(true);
            resultTable.addControlListener(new ControlAdapter() {
                @Override
                public void controlResized(ControlEvent e) {
                    UIUtils.packColumns(resultTable);
                }
            });

            UIUtils.createTableColumn(resultTable, SWT.LEFT, CoreMessages.dialog_export_wizard_final_column_source);
            UIUtils.createTableColumn(resultTable, SWT.LEFT, CoreMessages.dialog_export_wizard_final_column_target);

            UIUtils.packColumns(resultTable);
        }

        setControl(composite);
    }

    @Override
    public void activatePage()
    {
        resultTable.removeAll();
        List<DataTransferPipe> dataPipes = getWizard().getSettings().getDataPipes();
        for (DataTransferPipe pipe : dataPipes) {
            IDataTransferSettings settings = getWizard().getSettings().getNodeSettings(pipe.getConsumer());
            IDataTransferProcessor processor;
            try {
                processor = getWizard().getSettings().getProcessor().createProcessor();
            } catch (DBException e) {
                log.error("Can't create processor", e);
                continue;
            }
            pipe.getConsumer().initTransfer(
                pipe.getProducer().getSourceObject(),
                settings,
                processor,
                getWizard().getSettings().getProcessorProperties());
            TableItem item = new TableItem(resultTable, SWT.NONE);
            item.setText(0, pipe.getProducer().getSourceObject().getName());
            item.setText(1, pipe.getConsumer().getTargetName());
        }
        UIUtils.packColumns(resultTable);
        updatePageCompletion();
    }

    @Override
    protected boolean determinePageCompletion()
    {
        return true;
    }

}
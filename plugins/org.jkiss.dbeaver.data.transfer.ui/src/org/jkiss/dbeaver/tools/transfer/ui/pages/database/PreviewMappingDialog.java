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

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.*;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.DBValueFormatting;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.ProxyProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tools.transfer.*;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseConsumerSettings;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseMappingContainer;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferNodeDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferRegistry;
import org.jkiss.dbeaver.tools.transfer.ui.internal.DTUIMessages;
import org.jkiss.dbeaver.ui.DBeaverIcons;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.BaseProgressDialog;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

class PreviewMappingDialog extends BaseProgressDialog {

    private static final Log log = Log.getLog(PreviewMappingDialog.class);

    private static final int previewRowCount = 100;
    private static final String DIALOG_ID = "DBeaver.DataTransfer.PreviewMappingDialog";

    private final DataTransferPipe pipe;
    private final DatabaseMappingContainer mappingContainer;
    private final DataTransferSettings dtSettings;
    private Table previewTable;

    PreviewMappingDialog(
        Shell parentShell,
        DataTransferPipe pipe,
        DatabaseMappingContainer mappingContainer,
        DataTransferSettings dtSettings) {
        super(parentShell, DTMessages.data_transfer_wizard_page_preview_name + " - " + mappingContainer.getTargetName(), null);

        this.pipe = pipe;
        this.mappingContainer = mappingContainer;
        this.dtSettings = dtSettings;
    }

    @Override
    protected IDialogSettings getDialogBoundsSettings() {
        return UIUtils.getDialogSettings(DIALOG_ID);
    }

    @Override
    protected void createButtonsForButtonBar(Composite parent) {
        createButton(parent, IDialogConstants.OK_ID, IDialogConstants.CLOSE_LABEL, true);
    }

    @Override
    protected Composite createDialogArea(Composite parent) {
        Composite main = super.createDialogArea(parent);
        main.setLayoutData(new GridData(GridData.FILL_BOTH));

        {
            Composite previewGroup = new Composite(main, SWT.NONE);
            previewGroup.setLayoutData(new GridData(GridData.FILL_BOTH));
            previewGroup.setLayout(new GridLayout(1, false));
            UIUtils.createControlLabel(previewGroup, DTMessages.data_transfer_wizard_settings_group_preview);

            previewTable = new Table(previewGroup, SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
            GridData gd = new GridData(GridData.FILL_BOTH);
            gd.widthHint = 600;
            gd.heightHint = 400;
            previewTable.setLayoutData(gd);
            previewTable.setHeaderVisible(true);
            previewTable.setLinesVisible(true);
        }

        UIUtils.asyncExec(this::loadTransferPreview);

        return main;
    }

    public void loadTransferPreview() {
        Throwable error = null;
        try {
            this.run(true, true, monitor -> {
                monitor.beginTask("Load preview", 1);
                try {
                    // Load preview
                    monitor.subTask("Process sample rows");
                    loadImportPreview(monitor);
                    monitor.worked(1);

                    monitor.done();

                } catch (Throwable e) {
                    throw new InvocationTargetException(e);
                }
            });
        } catch (InvocationTargetException e) {
            error = e.getTargetException();
        } catch (InterruptedException e) {
            // Ignore
        }

        Throwable finalError = error;
        if (finalError != null) {
            DBWorkbench.getPlatformUI().showError(DTUIMessages.stream_producer_page_preview_title_load_entity_meta,
                DTUIMessages.stream_producer_page_preview_message_entity_attributes, finalError);
        }
        UIUtils.asyncExec(() -> getShell().setFocus());
    }

    private void loadImportPreview(
        DBRProgressMonitor monitor) throws DBException {
        PreviewConsumer previewConsumer = new PreviewConsumer(monitor, mappingContainer);

        IDataTransferProducer producer = pipe.getProducer();
        IDataTransferSettings producerSettings = getNodeSettings(producer);

        IDataTransferSettings consumerSettings = getNodeSettings(pipe.getConsumer());

        try {

            IDataTransferConsumer realConsumer = pipe.getConsumer();
            try {
                pipe.setConsumer(previewConsumer);
                pipe.initPipe(dtSettings, 0, 1);

                producer.transferData(
                    previewConsumer.getCtlMonitor(),
                    previewConsumer,
                    dtSettings.getProcessor() == null ? null : dtSettings.getProcessor().getInstance(),
                    producerSettings,
                    null);
            } finally {
                pipe.setConsumer(realConsumer);
            }
        } finally {
            previewConsumer.close();
        }

        List<Object[]> rows = previewConsumer.getRows();
        List<String[]> strRows = new ArrayList<>(rows.size());
        DBSObject target = mappingContainer.getTarget();
        if (target == null) {
            if (consumerSettings instanceof DatabaseConsumerSettings) {
                target = ((DatabaseConsumerSettings) consumerSettings).getContainer();
            }
        }
        if (target == null) {
            throw new DBException("Can not determine target container");
        }
        try (DBCSession session = DBUtils.openUtilSession(monitor, target, "Generate preview values")) {
            DatabaseTransferConsumer.ColumnMapping[] columnMappings = previewConsumer.getColumnMappings();
            for (Object[] row : rows) {
                String[] strRow = new String[row.length];
                for (int i = 0; i < columnMappings.length; i++) {
                    DatabaseTransferConsumer.ColumnMapping attr = columnMappings[i];
                    if (attr == null) {
                        continue;
                    }
                    Object srcValue = row[attr.targetIndex];
                    Object value = attr.sourceValueHandler.getValueFromObject(session, attr.sourceAttr, srcValue, false, true);
                    String valueStr = attr.targetValueHandler.getValueDisplayString(attr.targetAttr.getTarget(), value, DBDDisplayFormat.UI);
                    strRow[attr.targetIndex] = valueStr;
                }
                strRows.add(strRow);
            }
        }

        UIUtils.asyncExec(() -> {
            previewTable.setRedraw(false);
            try {
                previewTable.removeAll();
                for (TableColumn column : previewTable.getColumns()) {
                    column.dispose();
                }
                for (DatabaseTransferConsumer.ColumnMapping columnMapping : previewConsumer.getColumnMappings()) {
                    if (columnMapping == null) {
                        continue;
                    }
                    TableColumn column = new TableColumn(previewTable, SWT.NONE);
                    column.setText(columnMapping.targetAttr.getTargetName());

                    DBSAttributeBase attr = columnMapping.targetAttr.getTarget();
                    if (attr == null) {
                        // We can use icon from source attribute
                        attr = columnMapping.sourceAttr;
                    }
                    column.setImage(DBeaverIcons.getImage(DBValueFormatting.getObjectImage(attr)));
                    column.setData(columnMapping);
                }

                for (String[] row : strRows) {
                    TableItem previewItem = new TableItem(previewTable, SWT.NONE);
                    for (int i = 0; i < row.length; i++) {
                        if (row[i] != null) {
                            previewItem.setText(i, row[i]);
                        }
                    }
                }
                UIUtils.packColumns(previewTable);
            } finally {
                previewTable.setRedraw(true);
            }
        });
    }

    @NotNull
    private IDataTransferSettings getNodeSettings(IDataTransferNode node) throws DBException {
        DataTransferNodeDescriptor producerNode = DataTransferRegistry.getInstance().getNodeByType(node.getClass());
        if (producerNode == null) {
            throw new DBException("Cannot find node descriptor for " + node.getClass().getName());
        }
        IDataTransferSettings producerSettings = dtSettings.getNodeSettings(producerNode);
        if (producerSettings == null) {
            throw new DBException("Cannot find node settings for " + producerNode.getName());
        }
        return producerSettings;
    }

    private class PreviewConsumer extends DatabaseTransferConsumer {

        private final DBRProgressMonitor ctlMonitor;
        private boolean fetchEnded;

        PreviewConsumer(DBRProgressMonitor monitor, DatabaseMappingContainer mappingContainer) {
            super(mappingContainer.getTarget());
            ctlMonitor = new ProxyProgressMonitor(monitor) {
                @Override
                public boolean isCanceled() {
                    return super.isCanceled() || fetchEnded;
                }
            };
            setPreview(true);
        }

        DBRProgressMonitor getCtlMonitor() {
            return ctlMonitor;
        }

        public List<Object[]> getRows() {
            return getPreviewRows();
        }

        @Override
        public void fetchRow(DBCSession session, DBCResultSet resultSet) throws DBCException {
            if (getPreviewRows().size() >= previewRowCount) {
                fetchEnded = true;
                return;
            }
            super.fetchRow(session, resultSet);
        }
    }

}

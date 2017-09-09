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
package org.jkiss.dbeaver.data.office.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.data.office.export.DataExporterXLSX;
import org.jkiss.dbeaver.data.office.export.StreamPOIConsumerSettings;
import org.jkiss.dbeaver.data.office.export.StreamPOITransferConsumer;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseProducerSettings;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferProducer;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetCommandHandler;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Map;

public class OpenSpreadsheetHandler extends AbstractHandler
{
    private static final Log log = Log.getLog(OpenSpreadsheetHandler.class);

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
        IResultSetController resultSet = ResultSetCommandHandler.getActiveResultSet(HandlerUtil.getActivePart(event));
        if (resultSet == null) {
            DBeaverUI.getInstance().showError("Open Excel", "No active results viewer");
            return null;
        }

        DBSDataContainer dataContainer = resultSet.getDataContainer();
        if (dataContainer == null || dataContainer.getDataSource() == null) {
            DBeaverUI.getInstance().showError("Open Excel", "Not connected to a database");
            return null;
        }


        AbstractJob exportJob = new AbstractJob("Open Excel") {

            {
                setUser(true);
                setSystem(false);
            }

            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                try {
                    File tempDir = DBeaverCore.getInstance().getTempFolder(monitor, "office-files");
                    File tempFile = new File(tempDir,
                        CommonUtils.escapeFileName(CommonUtils.truncateString(dataContainer.getName(), 32)) +
                            "." + new SimpleDateFormat("yyyyMMdd-HHmmss").format(System.currentTimeMillis()) + ".xlsx");

                    DataExporterXLSX exporter = new DataExporterXLSX();

                    StreamPOITransferConsumer consumer = new StreamPOITransferConsumer();
                    StreamPOIConsumerSettings settings = new StreamPOIConsumerSettings();

                    settings.setOutputEncodingBOM(false);
                    settings.setOpenFolderOnFinish(false);
                    settings.setOutputFolder(tempDir.getAbsolutePath());
                    settings.setOutputFilePattern(tempFile.getName());

                    Map<Object, Object> properties = DataExporterXLSX.getDefaultProperties();
                    consumer.initTransfer(dataContainer, settings, exporter, properties);

                    DatabaseTransferProducer producer = new DatabaseTransferProducer(dataContainer);
                    DatabaseProducerSettings producerSettings = new DatabaseProducerSettings();
                    producerSettings.setExtractType(DatabaseProducerSettings.ExtractType.SINGLE_QUERY);
                    producerSettings.setQueryRowCount(false);

                    producer.transferData(monitor, consumer, producerSettings);

                    consumer.finishTransfer(monitor, false);

                    DBeaverUI.asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            if (!UIUtils.launchProgram(tempFile.getAbsolutePath())) {
                                DBeaverUI.getInstance().showError("Open XLSX", "Can't open XLSX file '" + tempFile.getAbsolutePath() + "'");
                            }
                        }
                    });
                } catch (Exception e) {
                    DBeaverUI.getInstance().showError("Error opening in Excel", null, e);
                }
                return Status.OK_STATUS;
            }
        };
        exportJob.schedule();

        return null;
    }

}
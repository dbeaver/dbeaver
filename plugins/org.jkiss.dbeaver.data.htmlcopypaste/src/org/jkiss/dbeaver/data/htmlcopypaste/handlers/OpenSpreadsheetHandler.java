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
package org.jkiss.dbeaver.data.htmlcopypaste.handlers;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.ui.handlers.HandlerUtil;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.data.htmlcopypaste.export.DataExporterCopyHtml;
import org.jkiss.dbeaver.data.htmlcopypaste.transfer.stream.StreamTransfertConsumerHtmlCopy;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseProducerSettings;
import org.jkiss.dbeaver.tools.transfer.database.DatabaseTransferProducer;
import org.jkiss.dbeaver.tools.transfer.stream.StreamConsumerSettings;
import org.jkiss.dbeaver.tools.transfer.stream.StreamTransferConsumer;
import org.jkiss.dbeaver.ui.controls.resultset.IResultSetController;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetDataContainer;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetDataContainerOptions;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetHandlerMain;
import org.jkiss.dbeaver.ui.controls.resultset.ResultSetRow;
import org.jkiss.utils.CommonUtils;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpenSpreadsheetHandler extends AbstractHandler
{
    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException
    {
    	IResultSetController resultSet = ResultSetHandlerMain.getActiveResultSet(HandlerUtil.getActivePart(event));
        if (resultSet == null) {
            DBeaverUI.getInstance().showError("copy Html", "No active results viewer");
            return null;
        }

        List<Long> selectedRows = new ArrayList<>();
        for (ResultSetRow selectedRow : resultSet.getSelection().getSelectedRows()) {
            selectedRows.add(Long.valueOf(selectedRow.getRowNumber()));
        }
        List<String> selectedAttributes = new ArrayList<>();
        for (DBDAttributeBinding attributeBinding : resultSet.getSelection().getSelectedAttributes()) {
            selectedAttributes.add(attributeBinding.getName());
        }

        ResultSetDataContainerOptions options = new ResultSetDataContainerOptions();
        options.setSelectedRows(selectedRows);
        options.setSelectedColumns(selectedAttributes);
        ResultSetDataContainer dataContainer = new ResultSetDataContainer(resultSet.getDataContainer(), resultSet.getModel(), options);
        
        if (dataContainer == null || dataContainer.getDataSource() == null) {
            DBeaverUI.getInstance().showError("copy Html", "Not connected to a database");
            return null;
        }


        AbstractJob exportJob = new AbstractJob("copy Html") {

            {
                setUser(true);
                setSystem(false);
            }

            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                try {

              	
              	
                    DataExporterCopyHtml exporter = new DataExporterCopyHtml();
                    
                    StreamTransfertConsumerHtmlCopy consumer = new StreamTransfertConsumerHtmlCopy();
                    StreamConsumerSettings settings = new StreamConsumerSettings();
                  
                                 
                    Map<Object, Object> properties = new HashMap<>();
                  
                    settings.setOutputEncodingBOM(false);

                    settings.setOpenFolderOnFinish(false);
                    

                   settings.setOutputClipboard(true);


                    consumer.initTransfer(dataContainer, settings, false, exporter, properties);


                    DBDDataFilter dataFilter = resultSet.getModel().getDataFilter();
                   
                    DatabaseTransferProducer producer = new DatabaseTransferProducer(dataContainer, dataFilter);
                    DatabaseProducerSettings producerSettings = new DatabaseProducerSettings();
                    producerSettings.setExtractType(DatabaseProducerSettings.ExtractType.SINGLE_QUERY);

                    producerSettings.setQueryRowCount(true);
                    producerSettings.setSelectedRowsOnly(true);
                    producerSettings.setSelectedColumnsOnly(true);
                    
                    producer.transferData(monitor, consumer, null, producerSettings);

                    consumer.finishTransfer(monitor);

                    

                } catch (Exception e) {
                    DBeaverUI.getInstance().showError("Error copy Html", null, e);
                }
                return Status.OK_STATUS;
            }
        };
        exportJob.schedule();

        return null;
    }

}
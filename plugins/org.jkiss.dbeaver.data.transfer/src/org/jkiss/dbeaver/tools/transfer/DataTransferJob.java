/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.tools.transfer;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.exec.DBCStatistics;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

/**
 * Data transfer job
 */
public class DataTransferJob extends AbstractJob {

    private final DBCStatistics totalStatistics = new DBCStatistics();
    private final DataTransferSettings settings;
    private final DBTTask task;
    private long elapsedTime;
    private boolean hasErrors;

    private final Log log;

    public DataTransferJob(@NotNull DataTransferSettings settings, @NotNull DBTTask task, @NotNull Log log, int index) {
        super("Data transfer job [" + index + "]: " + settings.getConsumer().getName());
        this.settings = settings;
        this.task = task;
        this.log = log;
    }

    public DataTransferSettings getSettings() {
        return settings;
    }

    public long getElapsedTime() {
        return elapsedTime;
    }

    public boolean isHasErrors() {
        return hasErrors;
    }

    public DBCStatistics getTotalStatistics() {
        return totalStatistics;
    }

    @Override
    protected IStatus run(DBRProgressMonitor monitor) {
        monitor.beginTask("Perform data transfer", 1);
        hasErrors = false;
        long startTime = System.currentTimeMillis();
        for (; ;) {
            if (monitor.isCanceled()) {
                break;
            }
            DataTransferPipe transferPipe = settings.acquireDataPipe(monitor, task);
            if (transferPipe == null) {
                break;
            }
            try {
                if (!transferData(monitor, transferPipe)) {
                    hasErrors = true;
                }
            } catch (Exception e) {
                return GeneralUtils.makeExceptionStatus(e);
            }
        }
        monitor.done();
//        listener.subTaskFinished(task, null);
        elapsedTime = System.currentTimeMillis() - startTime;
        return Status.OK_STATUS;
    }

    private boolean transferData(DBRProgressMonitor monitor, DataTransferPipe transferPipe) throws Exception
    {
        IDataTransferProducer producer = transferPipe.getProducer();
        IDataTransferConsumer consumer = transferPipe.getConsumer();

        monitor.beginTask(
            NLS.bind(DTMessages.data_transfer_wizard_job_container_name,
                CommonUtils.truncateString(producer.getObjectName(), 200),
                CommonUtils.truncateString(consumer.getObjectName(), 200)), 1);

        IDataTransferSettings nodeSettings = settings.getNodeSettings(settings.getProducer());
        try {
            //consumer.initTransfer(producer.getDatabaseObject(), consumerSettings, );

            IDataTransferProcessor processor = settings.getProcessor() == null ? null : settings.getProcessor().getInstance();
            producer.transferData(monitor, consumer, processor, nodeSettings, task);

            totalStatistics.accumulate(producer.getStatistics());
            totalStatistics.accumulate(consumer.getStatistics());

            consumer.finishTransfer(monitor, false);
            return true;
        } catch (Exception e) {
            consumer.finishTransfer(monitor, e, task, false);
            log.error("Error transfering data from " + producer.getObjectName() + " to " + consumer.getObjectName(), e);
            throw e;
        } finally {
            monitor.done();
        }

    }

}

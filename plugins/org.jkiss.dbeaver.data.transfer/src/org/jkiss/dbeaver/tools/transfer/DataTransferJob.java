/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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

import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.model.task.DBTTaskExecutionListener;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.utils.CommonUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

/**
 * Data transfer job
 */
public class DataTransferJob implements DBRRunnableWithProgress {

    private DataTransferSettings settings;
    private DBTTask task;
    private long elapsedTime;
    private boolean hasErrors;

    private Locale locale;
    private Log log;
    private DBTTaskExecutionListener listener;

    public DataTransferJob(DataTransferSettings settings, DBTTask task, Locale locale, Log log, DBTTaskExecutionListener listener)
    {
        this.settings = settings;
        this.task = task;
        this.locale = locale;
        this.log = log;
        this.listener = listener;
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

    @Override
    public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException {
        monitor.beginTask("Perform data transfer", 1);
        hasErrors = false;
        long startTime = System.currentTimeMillis();
        for (; ;) {
            if (monitor.isCanceled()) {
                break;
            }
            DataTransferPipe transferPipe = settings.acquireDataPipe(monitor);
            if (transferPipe == null) {
                break;
            }
            try {
                if (!transferData(monitor, transferPipe)) {
                    hasErrors = true;
                }
            } catch (Exception e) {
                listener.subTaskFinished(e);
                throw new InvocationTargetException(e);
            }
        }
        monitor.done();
        listener.subTaskFinished(null);
        elapsedTime = System.currentTimeMillis() - startTime;
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
            try {
                producer.transferData(
                    monitor,
                    consumer,
                    processor,
                    nodeSettings,
                    task);
            } finally {
                consumer.finishTransfer(monitor, false);
            }
            return true;
        } catch (Exception e) {
            log.error("Error transfering data from " + producer.getObjectName() + " to " + consumer.getObjectName(), e);
            throw e;
        } finally {
            monitor.done();
        }

    }

}

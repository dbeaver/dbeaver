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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.utils.CommonUtils;

/**
 * Data transfer job
 */
public class DataTransferJob extends AbstractJob {

    private DataTransferSettings settings;
    private long elapsedTime;
    private boolean hasErrors;

    public DataTransferJob(DataTransferSettings settings)
    {
        super(DTMessages.data_transfer_wizard_job_name);
        this.settings = settings;

        setUser(true);
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
    public boolean belongsTo(Object family)
    {
        return family == settings;
    }

    @Override
    protected IStatus run(DBRProgressMonitor monitor)
    {
        hasErrors = false;
        long startTime = System.currentTimeMillis();
        for (; ;) {
            DataTransferPipe transferPipe = settings.acquireDataPipe(monitor);
            if (transferPipe == null) {
                break;
            }
            if (!transferData(monitor, transferPipe)) {
                hasErrors = true;
            }
        }
        elapsedTime = System.currentTimeMillis() - startTime;
        return Status.OK_STATUS;
    }

    private boolean transferData(DBRProgressMonitor monitor, DataTransferPipe transferPipe)
    {
        IDataTransferProducer producer = transferPipe.getProducer();
        IDataTransferConsumer consumer = transferPipe.getConsumer();

        IDataTransferSettings consumerSettings = settings.getNodeSettings(settings.getConsumer());

        setName(NLS.bind(DTMessages.data_transfer_wizard_job_container_name,
            CommonUtils.truncateString(producer.getObjectName(), 200),
            CommonUtils.truncateString(consumer.getObjectName(), 200)));

        IDataTransferSettings nodeSettings = settings.getNodeSettings(settings.getProducer());
        try {
            //consumer.initTransfer(producer.getDatabaseObject(), consumerSettings, );

            IDataTransferProcessor processor = settings.getProcessor() == null ? null : settings.getProcessor().getInstance();
            try {
                producer.transferData(
                    monitor,
                    consumer,
                    processor,
                    nodeSettings);
            } finally {
                consumer.finishTransfer(monitor, false);
            }
            return true;
        } catch (Exception e) {
            DBWorkbench.getPlatformUI().showError("Data export error", e.getMessage(), e);
            return false;
        }

    }

}

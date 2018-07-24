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
package org.jkiss.dbeaver.tools.transfer.wizard;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPart;
import org.jkiss.dbeaver.DBeaverPreferences;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProducer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferSettings;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

/**
 * Data transfer job
 */
public class DataTransferJob extends AbstractJob {

    private DataTransferSettings settings;

    public DataTransferJob(DataTransferSettings settings)
    {
        super(CoreMessages.data_transfer_wizard_job_name);
        this.settings = settings;

        setUser(true);
    }

    @Override
    public boolean belongsTo(Object family)
    {
        return family == settings;
    }

    @Override
    protected IStatus run(DBRProgressMonitor monitor)
    {
        boolean hasErrors = false;
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
        showResult(System.currentTimeMillis() - startTime, hasErrors);
        return Status.OK_STATUS;
    }

    private void showResult(final long time, final boolean hasErrors)
    {
        // Run async to avoid blocking progress monitor dialog
        UIUtils.asyncExec(() -> {
            // Make a sound
            Display.getCurrent().beep();
            // Notify agent
            if (time > DBeaverCore.getGlobalPreferenceStore().getLong(DBeaverPreferences.AGENT_LONG_OPERATION_TIMEOUT) * 1000) {
                DBeaverUI.notifyAgent(
                        "Data transfer completed", !hasErrors ? IStatus.INFO : IStatus.ERROR);
            }
            if (settings.isShowFinalMessage()) {
                // Show message box
                UIUtils.showMessageBox(
                    null,
                    "Data transfer",
                    "Data transfer completed " + (hasErrors ? "with errors " : "") + "(" + RuntimeUtils.formatExecutionTime(time) + ")",
                    hasErrors ? SWT.ICON_ERROR : SWT.ICON_INFORMATION);
            }
        });
    }

    private boolean transferData(DBRProgressMonitor monitor, DataTransferPipe transferPipe)
    {
        IDataTransferProducer producer = transferPipe.getProducer();
        IDataTransferConsumer consumer = transferPipe.getConsumer();

        IDataTransferSettings consumerSettings = settings.getNodeSettings(consumer);

        setName(NLS.bind(CoreMessages.data_transfer_wizard_job_container_name,
            CommonUtils.truncateString(producer.getSourceObject().getName(), 200)));

        IDataTransferSettings nodeSettings = settings.getNodeSettings(producer);
        try {
            //consumer.initTransfer(producer.getSourceObject(), consumerSettings, );

            producer.transferData(
                monitor,
                consumer,
                nodeSettings);
            consumer.finishTransfer(monitor, false);
            return true;
        } catch (Exception e) {
            new DataTransferErrorJob(e).schedule();
            return false;
        }

    }

}

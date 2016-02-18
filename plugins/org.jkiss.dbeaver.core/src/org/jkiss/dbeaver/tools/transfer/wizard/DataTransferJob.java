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

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.dbeaver.tools.transfer.IDataTransferConsumer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferProducer;
import org.jkiss.dbeaver.tools.transfer.IDataTransferSettings;
import org.jkiss.dbeaver.ui.UIUtils;

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
        final Shell shell = DBeaverUI.getActiveWorkbenchShell();
        shell.getDisplay().asyncExec(new Runnable() {
            @Override
            public void run()
            {
                UIUtils.showMessageBox(
                    shell,
                    "Data transfer",
                    "Data transfer completed " +  (hasErrors ? "with errors " : "") + "(" + RuntimeUtils.formatExecutionTime(time) + ")",
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
            producer.getSourceObject().getName()));

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

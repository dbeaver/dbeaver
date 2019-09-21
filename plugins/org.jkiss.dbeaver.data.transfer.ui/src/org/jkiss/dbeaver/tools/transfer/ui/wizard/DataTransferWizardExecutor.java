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
package org.jkiss.dbeaver.tools.transfer.ui.wizard;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.task.DBTTaskExecutionListener;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.runtime.ui.DBPPlatformUI;
import org.jkiss.dbeaver.tools.transfer.DataTransferSettings;
import org.jkiss.dbeaver.tools.transfer.internal.DTMessages;
import org.jkiss.dbeaver.tools.transfer.task.DTTaskHandlerTransfer;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Locale;

class DataTransferWizardExecutor implements DBRRunnableContext, DBTTaskExecutionListener {
    private static final Log log = Log.getLog(DataTransferWizard.class);

    private DBRRunnableContext staticContext;
    private DataTransferSettings settings;
    private long startTime;
    private boolean started;

    public DataTransferWizardExecutor(DBRRunnableContext staticContext, DataTransferSettings settings) {
        this.staticContext = staticContext;
        this.settings = settings;
    }

    void executeTask() throws DBException {
        startTime = System.currentTimeMillis();

        DTTaskHandlerTransfer handlerTransfer = new DTTaskHandlerTransfer();
        handlerTransfer.executeWithSettings(this, Locale.getDefault(), log, this, settings);
    }

    @Override
    public void taskStarted(@NotNull Object task) {
        this.started = true;
    }

    @Override
    public void taskFinished(@NotNull Object task, @Nullable Throwable error) {
        this.started = false;

        UIUtils.asyncExec(() -> {
            // Make a sound
            Display.getCurrent().beep();
            // Notify agent
            long time = System.currentTimeMillis() - startTime;
            boolean hasErrors = error != null;
            DBPPlatformUI platformUI = DBWorkbench.getPlatformUI();
            if (time > platformUI.getLongOperationTimeout() * 1000) {
                platformUI.notifyAgent(
                    "Data transfer completed", !hasErrors ? IStatus.INFO : IStatus.ERROR);
            }
            if (settings.isShowFinalMessage() && !hasErrors) {
                // Show message box
                UIUtils.showMessageBox(
                    null,
                    DTMessages.data_transfer_wizard_name,
                    "Data transfer completed (" + RuntimeUtils.formatExecutionTime(time) + ")",
                    SWT.ICON_INFORMATION);
            }
        });

    }

    @Override
    public void subTaskFinished(@Nullable Throwable error) {

    }

    @Override
    public void run(boolean fork, boolean cancelable, DBRRunnableWithProgress runnable) throws InvocationTargetException, InterruptedException {
        if (!started) {
            staticContext.run(false, true, runnable);
        } else {
            new AbstractJob(DTMessages.data_transfer_wizard_job_name) {
                @Override
                protected IStatus run(DBRProgressMonitor monitor) {
                    try {
                        runnable.run(monitor);
                    } catch (InvocationTargetException e) {
                        return GeneralUtils.makeErrorStatus("Error running data transfer", e.getTargetException());
                    } catch (InterruptedException e) {
                        return Status.CANCEL_STATUS;
                    }
                    return Status.OK_STATUS;
                }
            }.schedule();
        }
    }

}
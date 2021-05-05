/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.views.process;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProcessController;
import org.jkiss.dbeaver.model.runtime.DBRProcessDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.IHelpContextIds;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ShellProcessView extends ViewPart implements DBRProcessController
{
    public static final String VIEW_ID = "org.jkiss.dbeaver.core.shellProcess";

    private StyledText processLogText;
    private static int viewId = 0;
    private DBRProcessDescriptor processDescriptor;

    @Override
    public void createPartControl(Composite parent)
    {
        Composite group = UIUtils.createPlaceholder(parent, 1);
        group.setLayout(new FillLayout());

        processLogText = new StyledText(group, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
        UIUtils.setHelp(group, IHelpContextIds.CTX_QUERY_MANAGER);
    }

    @Override
    public DBRProcessDescriptor getProcessDescriptor()
    {
        return processDescriptor;
    }

    @Override
    public void terminateProcess()
    {
        if (processDescriptor != null) {
            if (processDescriptor.isRunning()) {
                processDescriptor.terminate();
                UIUtils.asyncExec(() ->
                    setPartName(processDescriptor.getName() + " (destroyed: " + processDescriptor.getExitValue() + ")"));

            }
        }
    }

    @Override
    public void dispose()
    {
        terminateProcess();
        super.dispose();
    }

    @Override
    public void setFocus()
    {
        if (processLogText != null && !processLogText.isDisposed()) {
            processLogText.setFocus();
        }
    }

    public synchronized static String getNextId()
    {
        viewId++;
        return String.valueOf(viewId);
    }

    public void initProcess(DBRProcessDescriptor processDescriptor)
    {
        this.processDescriptor = processDescriptor;
        setPartName(processDescriptor.getName());

        new ProcessLogger().schedule();
    }

    private class ProcessLogger extends AbstractJob {

        ProcessLogger()
        {
            super(processDescriptor.getName());
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor)
        {
            try {
                Process process = processDescriptor.getProcess();
                if (process == null) {
                    return Status.OK_STATUS;
                }
                try {
                    final InputStream execOut = process.getInputStream();
                    final BufferedReader reader = new BufferedReader(
                        new InputStreamReader(execOut, GeneralUtils.getDefaultConsoleEncoding())
                    );

                    for (;;) {
                        final String line = reader.readLine();
                        if (line == null) {
                            break;
                        }
                        writeProcessLog(line);
                    }
                } finally {
                    processDescriptor.terminate();
                }

            } catch (Exception e) {
                return GeneralUtils.makeExceptionStatus(e);
            }
            return Status.OK_STATUS;
        }
    }

    private void writeProcessLog(final String line)
    {
        if (line.isEmpty()) {
            return;
        }
        final String logLine = line + GeneralUtils.getDefaultLineSeparator();
        UIUtils.asyncExec(() -> {
            if (processLogText == null || processLogText.isDisposed()) {
                return;
            }
            processLogText.append(logLine);
        });
    }

}

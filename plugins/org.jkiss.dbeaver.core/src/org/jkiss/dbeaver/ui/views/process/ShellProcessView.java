/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.views.process;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.part.ViewPart;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRShellCommand;
import org.jkiss.dbeaver.runtime.AbstractJob;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.help.IHelpContextIds;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ShellProcessView extends ViewPart
{
    public static final String VIEW_ID = "org.jkiss.dbeaver.core.shellProcess";

    private StringBuilder processLog = new StringBuilder();
    private Text processLogText;
    private static int viewId = 0;
    private DBRShellCommand command;
    private ProcessBuilder processBuilder;
    private Process process;

    public void createPartControl(Composite parent)
    {
        Composite group = UIUtils.createPlaceholder(parent, 1);
        group.setLayout(new FillLayout());

        processLogText = new Text(group, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.READ_ONLY);
        UIUtils.setHelp(group, IHelpContextIds.CTX_QUERY_MANAGER);
    }

    @Override
    public void dispose()
    {
        terminateProcess();
        super.dispose();
    }

    private void terminateProcess()
    {
        if (process != null) {
            process.destroy();
        }
    }

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

    public void initProcess(DBRShellCommand command, ProcessBuilder process)
    {
        this.command = command;
        this.processBuilder = process;
        setPartName(process.command().get(0));

        new ProcessLogger().schedule();
    }

    private class ProcessLogger extends AbstractJob {

        protected ProcessLogger()
        {
            super(processBuilder.command().get(0));
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor)
        {
            try {
                process = processBuilder.start();
                try {
                    final InputStream execOut = process.getInputStream();
                    final BufferedReader reader = new BufferedReader(
                        new InputStreamReader(execOut)
                    );

                    for (;;) {
                        final String line = reader.readLine();
                        if (line == null) {
                            break;
                        }
                        writeProcessLog(line);
                    }
                } finally {
                    process = null;
                }

            } catch (IOException e) {
                return RuntimeUtils.makeExceptionStatus(e);
            }
            return Status.OK_STATUS;
        }
    }

    private void writeProcessLog(final String line)
    {
        if (line.isEmpty()) {
            return;
        }
        processLog.append(line).append(ContentUtils.getDefaultLineSeparator());
        final Shell shell = DBeaverCore.getActiveWorkbenchShell();
        if (shell == null) {
            return;
        }
        shell.getDisplay().asyncExec(new Runnable() {
            public void run()
            {
                if (processLogText == null || processLogText.isDisposed()) {
                    return;
                }
                processLogText.setText(processLog.toString());
            }
        });
    }

}

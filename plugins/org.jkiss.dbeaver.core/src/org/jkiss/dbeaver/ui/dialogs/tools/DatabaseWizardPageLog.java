/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ui.dialogs.tools;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Date;
import java.util.List;


public class DatabaseWizardPageLog extends WizardPage {

    private StyledText dumpLogText;
    private String task;

    public DatabaseWizardPageLog(String task)
    {
        super(NLS.bind(CoreMessages.tools_wizard_page_log_task_progress, task));
        this.task = task;
        setTitle(NLS.bind(CoreMessages.tools_wizard_page_log_task_progress, task));
        setDescription(NLS.bind(CoreMessages.tools_wizard_page_log_task_progress_log, task));
    }

    @Override
    public boolean isPageComplete()
    {
        return true;
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite composite = UIUtils.createPlaceholder(parent, 1);

        dumpLogText = new StyledText(composite, SWT.MULTI | SWT.READ_ONLY | SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
        GridData gd = new GridData(GridData.FILL_BOTH);
        dumpLogText.setLayoutData(gd);

        setControl(composite);
    }

    public void appendLog(final String line)
    {
        if (getShell().isDisposed()) {
            return;
        }
        UIUtils.runInUI(getShell(), new Runnable() {
            @Override
            public void run()
            {
                synchronized (DatabaseWizardPageLog.this) {
                    if (!dumpLogText.isDisposed()) {
                        dumpLogText.append(line);
                        //dumpLogText.append(ContentUtils.getDefaultLineSeparator());
                        dumpLogText.setCaretOffset(dumpLogText.getCharCount());
                        dumpLogText.showSelection();
                    }
                }
            }
        });
    }

    public void clearLog()
    {
        if (getShell().isDisposed()) {
            return;
        }
        UIUtils.runInUI(getShell(), new Runnable() {
            @Override
            public void run()
            {
                synchronized (DatabaseWizardPageLog.this) {
                    if (!dumpLogText.isDisposed()) {
                        dumpLogText.setText(""); //$NON-NLS-1$
                    }
                }
            }
        });
    }

    public void startLogReader(ProcessBuilder processBuilder, InputStream stream)
    {
        new LogReaderJob(processBuilder, stream).start();
    }

    public void startNullReader(InputStream stream)
    {
        new NullReaderJob(stream).start();
    }

    private class LogReaderJob extends Thread {
        private ProcessBuilder processBuilder;
        private InputStream input;
        //private BufferedReader in;
        protected LogReaderJob(ProcessBuilder processBuilder, InputStream stream)
        {
            super(NLS.bind(CoreMessages.tools_wizard_page_log_task_log_reader, task));
            //in = new BufferedReader(new InputStreamReader(stream), 100);
            this.processBuilder = processBuilder;
            this.input = stream;
        }

        @Override
        public void run()
        {
            clearLog();
            String lf = GeneralUtils.getDefaultLineSeparator();
            List<String> command = processBuilder.command();
            StringBuilder cmdString = new StringBuilder();
            cmdString.append(command.get(0));
//            for (String cmd : command) {
//                if (cmd.startsWith("--password")) continue;
//                if (cmdString.length() > 0) cmdString.append(' ');
//                cmdString.append(cmd);
//            }
            cmdString.append(lf);
            appendLog(cmdString.toString());
            appendLog(NLS.bind(CoreMessages.tools_wizard_page_log_task_started_at, task, new Date()) + lf);

            try {
                InputStream in = input;
//                if (in instanceof FilterInputStream) {
//                    try {
//                        final Field inField = FilterInputStream.class.getDeclaredField("in");
//                        inField.setAccessible(true);
//                        in = (InputStream) inField.get(in);
//                    } catch (Exception e) {
//                        appendLog(e.getMessage() + lf);
//                    }
//                }
                try (Reader reader = new InputStreamReader(in)) {
                    StringBuilder buf = new StringBuilder();
                    for (; ; ) {
                        int b = reader.read();
                        if (b == -1) {
                            break;
                        }
                        buf.append((char) b);
                        if (b == '\n') {
                            appendLog(buf.toString());
                            buf.setLength(0);
                        }
                        //int avail = input.available();
                    }
                }

            } catch (IOException e) {
                // just skip
                appendLog(e.getMessage() + lf);
            } finally {
                appendLog(NLS.bind(CoreMessages.tools_wizard_page_log_task_finished, task, new Date()) + lf);
            }
        }
    }

    private class NullReaderJob extends Thread {
        private InputStream input;
        protected NullReaderJob(InputStream stream)
        {
            super(NLS.bind(CoreMessages.tools_wizard_page_log_task_log_reader, task));
            this.input = stream;
        }

        @Override
        public void run()
        {
            try {
                byte[] buffer = new byte[1000];
                for (;;) {
                    int count = input.read(buffer);
                    if (count <= 0) {
                        break;
                    }
                }
            } catch (IOException e) {
                // just skip
            }
        }
    }

}

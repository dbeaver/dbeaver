/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2011-2012 Eugene Fradkin (eugene.fradkin@gmail.com)
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
package org.jkiss.dbeaver.ui.dialogs.tools;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.core.DBeaverUI;
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
        appendLog(line, false);
    }

    public void appendLog(final String line, final boolean error)
    {
        if (getShell().isDisposed()) {
            return;
        }
        DBeaverUI.syncExec(new Runnable() {
            @Override
            public void run() {
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
        DBeaverUI.syncExec(new Runnable() {
            @Override
            public void run() {
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
        protected LogReaderJob(ProcessBuilder processBuilder, InputStream stream)
        {
            super(NLS.bind(CoreMessages.tools_wizard_page_log_task_log_reader, task));

            this.processBuilder = processBuilder;
            this.input = stream;
        }

        @Override
        public void run()
        {
            AbstractToolWizard wizard = (AbstractToolWizard) getWizard();

            String lf = GeneralUtils.getDefaultLineSeparator();
            List<String> command = processBuilder.command();

            // Dump command line
            StringBuilder cmdString = new StringBuilder();
            for (String cmd : command) {
                if (wizard.isSecureString(cmd)) {
                    cmd = "******";
                }
                if (cmdString.length() > 0) cmdString.append(' ');
                cmdString.append(cmd);
            }
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
                try (Reader reader = new InputStreamReader(in, GeneralUtils.getDefaultConsoleEncoding())) {
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

/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.tasks.ui.nativetool;

import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.console.MessageConsole;
import org.eclipse.ui.console.TextConsoleViewer;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.tasks.nativetool.NativeToolUtils;
import org.jkiss.dbeaver.tasks.ui.nativetool.internal.TaskNativeUIMessages;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;


public class NativeToolWizardPageLog extends WizardPage {

    private static final Log log = Log.getLog(NativeToolWizardPageLog.class);

    private TextConsoleViewer consoleViewer;
    private String task;
    private PrintStream writer;
    private MessageConsole console;

    public NativeToolWizardPageLog(String task)
    {
        super(NLS.bind(TaskNativeUIMessages.tools_wizard_page_log_task_progress, task));
        this.task = task;
        setTitle(NLS.bind(TaskNativeUIMessages.tools_wizard_page_log_task_progress, task));
        setDescription(NLS.bind(TaskNativeUIMessages.tools_wizard_page_log_task_progress_log, task));
    }

    @Override
    public boolean isPageComplete()
    {
        return true;
    }

    @Override
    public void createControl(Composite parent)
    {
        Composite composite = new Composite(parent, SWT.BORDER);
        composite.setLayoutData(new GridData(GridData.FILL_BOTH));
        composite.setLayout(new FillLayout());

        console = new MessageConsole("tool-log-console", null);
        consoleViewer = new LogConsoleViewer(composite);
        console.setWaterMarks(1024*1024*3, 1024*1024*4);

        try {
            writer = new PrintStream(console.newMessageStream(), true, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            writer = new PrintStream(console.newMessageStream(), true);
        }

        setControl(composite);
    }

    public PrintStream getLogWriter() {
        return writer;
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
        writer.print(line);
    }

    public void clearLog()
    {
        if (getShell().isDisposed()) {
            return;
        }
        UIUtils.syncExec(() -> {
            synchronized (NativeToolWizardPageLog.this) {
                console.clearConsole();
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
        LogReaderJob(ProcessBuilder processBuilder, InputStream stream)
        {
            super(NLS.bind(TaskNativeUIMessages.tools_wizard_page_log_task_log_reader, task));

            this.processBuilder = processBuilder;
            this.input = stream;
        }

        @Override
        public void run()
        {
            AbstractNativeToolWizard wizard = (AbstractNativeToolWizard) getWizard();

            String lf = GeneralUtils.getDefaultLineSeparator();
            List<String> command = processBuilder.command();

            // Dump command line
            StringBuilder cmdString = new StringBuilder();
            for (String cmd : command) {
                if (NativeToolUtils.isSecureString(wizard.getSettings(), cmd)) {
                    cmd = "******";
                }
                if (cmdString.length() > 0) cmdString.append(' ');
                cmdString.append(cmd);
            }
            cmdString.append(lf);
            appendLog(cmdString.toString());

            appendLog(NLS.bind(TaskNativeUIMessages.tools_wizard_page_log_task_started_at, task, new Date()) + lf);

            try {
                InputStream in = input;
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
                appendLog(NLS.bind(TaskNativeUIMessages.tools_wizard_page_log_task_finished, task, new Date()) + lf);
            }
        }
    }

    private class NullReaderJob extends Thread {
        private InputStream input;
        protected NullReaderJob(InputStream stream)
        {
            super(NLS.bind(TaskNativeUIMessages.tools_wizard_page_log_task_log_reader, task));
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

    private class LogConsoleViewer extends TextConsoleViewer implements IDocumentListener {
        LogConsoleViewer(Composite composite) {
            super(composite, NativeToolWizardPageLog.this.console);
        }

        @Override
        public void setDocument(IDocument document) {
            IDocument oldDocument= getDocument();
            super.setDocument(document);
            if (oldDocument != null) {
                oldDocument.removeDocumentListener(this);
            }
            if (document != null) {
                document.addDocumentListener(this);
            }
        }

        @Override
        public void documentAboutToBeChanged(DocumentEvent event) {
        }

        @Override
        public void documentChanged(DocumentEvent event) {
            revealEndOfDocument();
        }
    }
}

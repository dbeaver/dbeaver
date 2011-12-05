/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ui.dialogs.tools;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.ContentUtils;

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
        super(task + " progress");
        this.task = task;
        setTitle(task + " progress");
        setDescription(task + " progress log");
    }

    @Override
    public boolean isPageComplete()
    {
        return true;
    }

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
            super(task + " log reader");
            //in = new BufferedReader(new InputStreamReader(stream), 100);
            this.processBuilder = processBuilder;
            this.input = stream;
        }

        public void run()
        {
            String lf = ContentUtils.getDefaultLineSeparator();
            List<String> command = processBuilder.command();
            StringBuilder cmdString = new StringBuilder();
            for (String cmd : command) {
                if (cmd.startsWith("--password")) continue;
                if (cmdString.length() > 0) cmdString.append(' ');
                cmdString.append(cmd);
            }
            cmdString.append(lf);
            appendLog(cmdString.toString());
            appendLog(task + " started at " + new Date() + lf);

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
                Reader reader = new InputStreamReader(in);
                try {
                    StringBuilder buf = new StringBuilder();
                    for (;;) {
                        int b = reader.read();
                        if (b == -1) {
                            break;
                        }
                        buf.append((char)b);
                        if (b == '\n') {
                            appendLog(buf.toString());
                            buf.setLength(0);
                        }
                        //int avail = input.available();
                    }
                } finally {
                    reader.close();
                }

            } catch (IOException e) {
                // just skip
                appendLog(e.getMessage() + lf);
            } finally {
                appendLog(task + " finished " + new Date() + lf);
            }
        }
    }

    private class NullReaderJob extends Thread {
        private InputStream input;
        protected NullReaderJob(InputStream stream)
        {
            super(task + " log reader");
            this.input = stream;
        }

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

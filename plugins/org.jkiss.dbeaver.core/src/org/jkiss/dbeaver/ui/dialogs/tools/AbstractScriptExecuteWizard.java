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

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.core.CoreMessages;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.IOUtils;

import java.io.*;
import java.util.Collection;

public abstract class AbstractScriptExecuteWizard<BASE_OBJECT extends DBSObject, PROCESS_ARG>
        extends AbstractToolWizard<BASE_OBJECT, PROCESS_ARG> implements IImportWizard
{
    protected File inputFile;

    public AbstractScriptExecuteWizard(Collection<BASE_OBJECT> dbObject, String task) {
        super(dbObject, task);
        this.inputFile = null;
	}

    public File getInputFile()
    {
        return inputFile;
    }

    public void setInputFile(File inputFile)
    {
        this.inputFile = inputFile;
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle(task);
        setNeedsProgressMonitor(true);
    }

    @Override
    public void addPages() {
        super.addPages();
        addPage(logPage);
    }

	@Override
	public void onSuccess() {
        UIUtils.showMessageBox(getShell(),
                task,
                NLS.bind(CoreMessages.tools_script_execute_wizard_task_completed, task, getObjectsName()) , //$NON-NLS-1$
                        SWT.ICON_INFORMATION);
	}

    @Override
    protected void startProcessHandler(DBRProgressMonitor monitor, PROCESS_ARG arg, ProcessBuilder processBuilder, Process process)
    {
        logPage.startLogReader(
            processBuilder,
            process.getInputStream());
        new ScriptTransformerJob(monitor, process.getOutputStream()).start();
        //logPage.startLogReader(processBuilder, process.getInputStream());
    }

    @Override
    protected boolean isMergeProcessStreams()
    {
        return true;
    }

    class ScriptTransformerJob extends Thread {
        private DBRProgressMonitor monitor;
        private OutputStream output;

        protected ScriptTransformerJob(DBRProgressMonitor monitor, OutputStream stream)
        {
            super(task);
            this.monitor = monitor;
            this.output = stream;
        }

        @Override
        public void run()
        {
            try {
                try (InputStream scriptStream = new ProgressStreamReader(
                    monitor,
                    new FileInputStream(inputFile),
                    inputFile.length()))
                {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(scriptStream, getInputCharset()));
                    PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, getOutputCharset()));
                    while (!monitor.isCanceled()) {
//                        int count = scriptStream.read(buffer);
//                        if (count <= 0) {
//                            break;
//                        }
//                        totalBytes += count;
//                        output.write(buffer, 0, count);
//                        output.flush();
//                        monitor.subTask(numberFormat.format(totalBytes) + " bytes");
//                        monitor.worked(count / BUFFER_SIZE);
                        String line = reader.readLine();
                        if (line == null) {
                            break;
                        }
                        writer.println(line);
                        writer.flush();
                        //output.flush();
                    }
                    output.flush();
                } finally {
                    IOUtils.close(output);
                }
            } catch (IOException e) {
                logPage.appendLog(e.getMessage());
            }
            finally {
                monitor.done();
            }
        }
    }

    protected String getInputCharset() {
        return "UTF-8";
    }

    protected String getOutputCharset() {
        return "UTF-8";
    }

    private class ProgressStreamReader extends InputStream {

        static final int BUFFER_SIZE = 10000;

        private final DBRProgressMonitor monitor;
        private final InputStream original;
        private final long streamLength;
        private long totalRead;

        private ProgressStreamReader(DBRProgressMonitor monitor, InputStream original, long streamLength)
        {
            this.monitor = monitor;
            this.original = original;
            this.streamLength = streamLength;
            this.totalRead = 0;

            monitor.beginTask(task, (int)streamLength);
        }

        @Override
        public int read() throws IOException
        {
            int res = original.read();
            showProgress(res);
            return res;
        }

        @Override
        public int read(byte[] b) throws IOException
        {
            int res = original.read(b);
            showProgress(res);
            return res;
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException
        {
            int res = original.read(b, off, len);
            showProgress(res);
            return res;
        }

        @Override
        public long skip(long n) throws IOException
        {
            long res = original.skip(n);
            showProgress(res);
            return res;
        }

        @Override
        public int available() throws IOException
        {
            return original.available();
        }

        @Override
        public void close() throws IOException
        {
            monitor.done();
            original.close();
        }

        private void showProgress(long length)
        {
            totalRead += length;
            monitor.worked((int)length);
        }
    }

}

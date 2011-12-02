/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.tools;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.IOUtils;

import java.io.*;
import java.util.List;

class MySQLDatabaseImportWizard extends MySQLAbstractToolWizard implements IImportWizard {

    File inputFile;
    boolean isImport;
    private MySQLDatabaseImportWizardPageSettings mainPage;

    public MySQLDatabaseImportWizard(MySQLCatalog catalog, boolean isImport) {
        super(catalog, isImport ? "Import" : "Script");
        this.inputFile = null;
        this.isImport = isImport;
	}

    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle("Database Import");
        setNeedsProgressMonitor(true);
        mainPage = new MySQLDatabaseImportWizardPageSettings(this);
    }

    public void addPages() {
        super.addPages();
        addPage(mainPage);
        addPage(logPage);
    }

	@Override
	public void onSuccess() {
        UIUtils.showMessageBox(getShell(),
            isImport ? "Database import" : "Script execute",
            "Database '" + getCatalog().getName() + "' " + (isImport ? "import" : "script") + " completed",
            SWT.ICON_INFORMATION);
	}

    @Override
    protected void fillProcessParameters(List<String> cmd)
    {
        String dumpPath = new File(getServerHome().getHomePath(), "bin/mysql").getAbsolutePath();
        cmd.add(dumpPath);
    }

    @Override
    protected void startProcessHandler(DBRProgressMonitor monitor, ProcessBuilder processBuilder, Process process)
    {
        new ScriptTransformerJob(monitor, process.getOutputStream()).start();
        logPage.startNullReader(process.getInputStream());
    }

    protected boolean isMergeProcessStreams()
    {
        return true;
    }

    class ScriptTransformerJob extends Thread {
        private DBRProgressMonitor monitor;
        private OutputStream output;

        protected ScriptTransformerJob(DBRProgressMonitor monitor, OutputStream stream)
        {
            super("Script execute");
            this.monitor = monitor;
            this.output = stream;
        }

        public void run()
        {
            try {
                InputStream scriptStream = new ProgressStreamReader(
                    monitor,
                    new FileInputStream(inputFile),
                    inputFile.length());
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(scriptStream));
                    PrintWriter writer = new PrintWriter(new OutputStreamWriter(output));
                    for (;;) {
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
                    }
                    output.flush();
                } finally {
                    IOUtils.close(scriptStream);
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

            monitor.beginTask(isImport ? "Import database" : "Execute script", (int)streamLength);
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

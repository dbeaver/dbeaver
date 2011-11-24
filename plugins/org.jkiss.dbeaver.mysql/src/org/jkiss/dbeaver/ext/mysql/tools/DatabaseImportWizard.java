/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.tools;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.program.Program;
import org.eclipse.ui.IImportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.dbeaver.ext.mysql.model.MySQLCatalog;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.IOUtils;

import java.io.*;
import java.text.NumberFormat;
import java.util.List;

class DatabaseImportWizard extends AbstractToolWizard implements IImportWizard {

    File inputFile;

    private DatabaseImportWizardPageSettings mainPage;

    public DatabaseImportWizard(MySQLCatalog catalog) {
        super(catalog, "Import");
        this.inputFile = null;
	}

    public void init(IWorkbench workbench, IStructuredSelection selection) {
        setWindowTitle("Database Import");
        setNeedsProgressMonitor(true);
        mainPage = new DatabaseImportWizardPageSettings(this);
    }

    public void addPages() {
        super.addPages();
        addPage(mainPage);
        addPage(logPage);
    }

	@Override
	public void onSuccess() {
        UIUtils.showMessageBox(getShell(), "Database import", "Database '" + getCatalog().getName() + "' import completed", SWT.ICON_INFORMATION);
        Program.launch(inputFile.getAbsoluteFile().getParentFile().getAbsolutePath());
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
        new ScriptTransformerJob(monitor, process.getInputStream());
    }

    class ScriptTransformerJob extends Thread {
        private DBRProgressMonitor monitor;
        private InputStream input;

        protected ScriptTransformerJob(DBRProgressMonitor monitor, InputStream stream)
        {
            super("Script dumper");
            this.monitor = monitor;
            this.input = stream;
        }

        public void run()
        {
            monitor.beginTask("Import database", 100);
            long totalBytesDumped = 0;
            long prevStatusUpdateTime = 0;
            byte[] buffer = new byte[10000];
            try {
                NumberFormat numberFormat = NumberFormat.getInstance();
                OutputStream output = new BufferedOutputStream(new FileOutputStream(inputFile), 10000);
                try {
                    for (;;) {
                        int count = input.read(buffer);
                        if (count <= 0) {
                            break;
                        }
                        totalBytesDumped += count;
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - prevStatusUpdateTime > 300) {
                            monitor.subTask(numberFormat.format(totalBytesDumped) + " bytes");
                            prevStatusUpdateTime = currentTime;
                        }
                        output.write(buffer, 0, count);
                    }
                    //logPage.setMessage("Done (" + String.valueOf(totalBytesDumped) + " bytes)");
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

}

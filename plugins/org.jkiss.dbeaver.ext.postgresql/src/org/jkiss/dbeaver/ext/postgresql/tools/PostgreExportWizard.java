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
package org.jkiss.dbeaver.ext.postgresql.tools;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreDataSourceProvider;
import org.jkiss.dbeaver.ext.postgresql.PostgreServerHome;
import org.jkiss.dbeaver.ext.postgresql.model.PostgreTableBase;
import org.jkiss.dbeaver.model.DBPPreferenceStore;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.ui.dialogs.DialogUtils;
import org.jkiss.dbeaver.ui.dialogs.tools.AbstractExportWizard;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.*;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class PostgreExportWizard extends AbstractExportWizard<PostgreDatabaseExportInfo> implements IExportWizard {

    public enum DumpMethod {
        ONLINE,
        LOCK_ALL_TABLES,
        NORMAL
    }

    DumpMethod method;
    boolean noCreateStatements;
    boolean addDropStatements = true;
    boolean disableKeys = true;
    boolean extendedInserts = true;
    boolean dumpEvents;
    boolean comments;
    boolean removeDefiner;
    boolean binariesInHex;
    boolean showViews;
    public List<PostgreDatabaseExportInfo> objects = new ArrayList<>();

    private PostgreExportWizardPageObjects objectsPage;
    private PostgreExportWizardPageSettings settingsPage;

    public PostgreExportWizard(Collection<DBSObject> objects) {
        super(objects, "Database export");
        this.method = DumpMethod.NORMAL;
        this.outputFolder = new File(DialogUtils.getCurDialogFolder()); //$NON-NLS-1$ //$NON-NLS-2$

        final DBPPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();
        this.outputFilePattern = store.getString("Postgre.export.outputFilePattern");
        if (CommonUtils.isEmpty(this.outputFilePattern)) {
            this.outputFilePattern = "dump-${database}-${timestamp}.sql";
        }
        noCreateStatements = CommonUtils.getBoolean(store.getString("Postgre.export.noCreateStatements"), false);
        addDropStatements = CommonUtils.getBoolean(store.getString("Postgre.export.addDropStatements"), true);
        disableKeys = CommonUtils.getBoolean(store.getString("Postgre.export.disableKeys"), true);
        extendedInserts = CommonUtils.getBoolean(store.getString("Postgre.export.extendedInserts"), true);
        dumpEvents = CommonUtils.getBoolean(store.getString("Postgre.export.dumpEvents"), false);
        comments = CommonUtils.getBoolean(store.getString("Postgre.export.comments"), false);
        removeDefiner = CommonUtils.getBoolean(store.getString("Postgre.export.removeDefiner"), false);
        binariesInHex = CommonUtils.getBoolean(store.getString("Postgre.export.binariesInHex"), false);
        showViews = CommonUtils.getBoolean(store.getString("Postgre.export.showViews"), false);
    }

    @Override
    public void init(IWorkbench workbench, IStructuredSelection selection) {
        super.init(workbench, selection);
        objectsPage = new PostgreExportWizardPageObjects(this);
        settingsPage = new PostgreExportWizardPageSettings(this);
    }

    @Override
    public void addPages() {
        addPage(objectsPage);
        addPage(settingsPage);
        super.addPages();
    }

    @Override
    public IWizardPage getNextPage(IWizardPage page) {
        if (page == settingsPage) {
            return null;
        }
        return super.getNextPage(page);
    }

    @Override
    public IWizardPage getPreviousPage(IWizardPage page) {
        if (page == logPage) {
            return settingsPage;
        }
        return super.getPreviousPage(page);
    }

    @Override
	public void onSuccess() {
        UIUtils.showMessageBox(
            getShell(),
            "Database export",
            "Export '" + getObjectsName() + "'",
            SWT.ICON_INFORMATION);
        UIUtils.launchProgram(outputFolder.getAbsolutePath());
	}

    @Override
    public void fillProcessParameters(List<String> cmd, PostgreDatabaseExportInfo arg) throws IOException
    {
        File dumpBinary = DBUtils.getHomeBinary(getClientHome(), PostgreConstants.BIN_FOLDER, "pg_dump"); //$NON-NLS-1$
        String dumpPath = dumpBinary.getAbsolutePath();
        cmd.add(dumpPath);
        switch (method) {
            case LOCK_ALL_TABLES:
                cmd.add("--lock-all-tables"); //$NON-NLS-1$
                break;
            case ONLINE:
                cmd.add("--single-transaction"); //$NON-NLS-1$
                break;
        }

        if (noCreateStatements) {
            cmd.add("--no-create-info"); //$NON-NLS-1$
        } else {
            if (CommonUtils.isEmpty(arg.getTables())) {
                cmd.add("--routines"); //$NON-NLS-1$
            }
        }
        if (addDropStatements) cmd.add("--add-drop-table"); //$NON-NLS-1$
        if (disableKeys) cmd.add("--disable-keys"); //$NON-NLS-1$
        if (extendedInserts) {
            cmd.add("--extended-insert"); //$NON-NLS-1$
        } else {
            cmd.add("--skip-extended-insert"); //$NON-NLS-1$
        }
        if (binariesInHex) {
            cmd.add("--hex-blob"); //$NON-NLS-1$
        }
        if (dumpEvents) cmd.add("--events"); //$NON-NLS-1$
        if (comments) cmd.add("--comments"); //$NON-NLS-1$
    }

    @Override
    public boolean performFinish() {
        objectsPage.saveState();

        final DBPPreferenceStore store = DBeaverCore.getGlobalPreferenceStore();
        store.setValue("Postgre.export.outputFilePattern", this.outputFilePattern);
        store.setValue("Postgre.export.noCreateStatements", noCreateStatements);
        store.setValue("Postgre.export.addDropStatements", addDropStatements);
        store.setValue("Postgre.export.disableKeys", disableKeys);
        store.setValue("Postgre.export.extendedInserts", extendedInserts);
        store.setValue("Postgre.export.dumpEvents", dumpEvents);
        store.setValue("Postgre.export.comments", comments);
        store.setValue("Postgre.export.removeDefiner", removeDefiner);
        store.setValue("Postgre.export.binariesInHex", binariesInHex);
        store.setValue("Postgre.export.showViews", showViews);

        return super.performFinish();
    }

    @Override
    public PostgreServerHome findServerHome(String clientHomeId)
    {
        return PostgreDataSourceProvider.getServerHome(clientHomeId);
    }

    @Override
    public Collection<PostgreDatabaseExportInfo> getRunInfo() {
        return objects;
    }

    @Override
    protected List<String> getCommandLine(PostgreDatabaseExportInfo arg) throws IOException
    {
        List<String> cmd = PostgreToolScript.getPostgreToolCommandLine(this, arg);
        if (objects.isEmpty()) {
            // no dump
        } else if (!CommonUtils.isEmpty(arg.getTables())) {
            cmd.add(arg.getSchema().getName());
            for (PostgreTableBase table : arg.getTables()) {
                cmd.add(table.getName());
            }
        } else {
            cmd.add(arg.getSchema().getName());
        }

        return cmd;
    }

    @Override
    public boolean isVerbose()
    {
        return true;
    }

    @Override
    protected void startProcessHandler(DBRProgressMonitor monitor, final PostgreDatabaseExportInfo arg, ProcessBuilder processBuilder, Process process)
    {
        super.startProcessHandler(monitor, arg, processBuilder, process);

        String outFileName = GeneralUtils.replaceVariables(outputFilePattern, new GeneralUtils.IVariableResolver() {
            @Override
            public String get(String name) {
                switch (name) {
                    case VARIABLE_DATABASE:
                        return arg.getSchema().getName();
                    case VARIABLE_HOST:
                        return arg.getSchema().getDataSource().getContainer().getConnectionConfiguration().getHostName();
                    case VARIABLE_TABLE:
                        final Iterator<PostgreTableBase> iterator = arg.getTables() == null ? null : arg.getTables().iterator();
                        if (iterator != null && iterator.hasNext()) {
                            return iterator.next().getName();
                        } else {
                            return "null";
                        }
                    case VARIABLE_TIMESTAMP:
                        return RuntimeUtils.getCurrentTimeStamp();
                    default:
                        System.getProperty(name);
                }
                return null;
            }
        });

        File outFile = new File(outputFolder, outFileName);
        boolean isFiltering = removeDefiner;
        Thread job = isFiltering ?
            new DumpFilterJob(monitor, process.getInputStream(), outFile) :
            new DumpCopierJob(monitor, "Export database", process.getInputStream(), outFile);
        job.start();
    }

    private static Pattern DEFINER_PATTER = Pattern.compile("DEFINER\\s*=\\s*`[^*]*`@`[0-9a-z\\-_\\.%]*`", Pattern.CASE_INSENSITIVE);

    class DumpFilterJob extends DumpJob {
        protected DumpFilterJob(DBRProgressMonitor monitor, InputStream stream, File outFile)
        {
            super("Export database", monitor, stream, outFile);
        }

        @Override
        public void runDump() throws IOException {
            monitor.beginTask("Export database", 100);
            long prevStatusUpdateTime = 0;
            try {
                NumberFormat numberFormat = NumberFormat.getInstance();

                LineNumberReader reader = new LineNumberReader(new InputStreamReader(input, GeneralUtils.DEFAULT_ENCODING));
                try (OutputStream output = new FileOutputStream(outFile)) {
                    BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(output, GeneralUtils.DEFAULT_ENCODING));
                    for (;;) {
                        String line = reader.readLine();
                        if (line == null) {
                            break;
                        }
                        final Matcher matcher = DEFINER_PATTER.matcher(line);
                        if (matcher.find()) {
                            line = matcher.replaceFirst("");
                        }
                        long currentTime = System.currentTimeMillis();
                        if (currentTime - prevStatusUpdateTime > 300) {
                            monitor.subTask("Saved " + numberFormat.format(reader.getLineNumber()) + " lines");
                            prevStatusUpdateTime = currentTime;
                        }
                        line = filterLine(line);
                        writer.write(line);
                        writer.newLine();
                    }
                    writer.flush();
                }
            }
            finally {
                monitor.done();
            }
        }

        @NotNull
        private String filterLine(@NotNull String line) {
            return line;
        }
    }

}

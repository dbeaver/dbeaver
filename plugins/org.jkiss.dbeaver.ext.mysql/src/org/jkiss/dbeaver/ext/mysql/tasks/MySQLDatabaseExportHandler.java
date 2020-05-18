package org.jkiss.dbeaver.ext.mysql.tasks;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableBase;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.registry.task.TaskPreferenceStore;
import org.jkiss.dbeaver.tasks.nativetool.NativeToolUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.*;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MySQLDatabaseExportHandler extends MySQLNativeToolHandler<MySQLExportSettings, DBSObject, MySQLDatabaseExportInfo> {

    @Override
    public Collection<MySQLDatabaseExportInfo> getRunInfo(MySQLExportSettings settings) {
        return settings.getExportObjects();
    }

    @Override
    protected MySQLExportSettings createTaskSettings(DBRRunnableContext context, DBTTask task) throws DBException {
        MySQLExportSettings settings = new MySQLExportSettings();
        settings.loadSettings(context, new TaskPreferenceStore(task));

        return settings;
    }

    @Override
    protected boolean validateTaskParameters(DBTTask task, MySQLExportSettings settings, Log log) {
        if (task.getType().getId().equals(MySQLTasks.TASK_DATABASE_BACKUP)) {
            final File dir = settings.getOutputFolder();
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    log.error("Can't create directory '" + dir.getAbsolutePath() + "'");
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected List<String> getCommandLine(MySQLExportSettings settings, MySQLDatabaseExportInfo arg) throws IOException {
        List<String> cmd = getMySQLToolCommandLine(this, settings, arg);
        if (!CommonUtils.isEmpty(arg.getTables())) {
            cmd.add(arg.getDatabase().getName());
            for (MySQLTableBase table : arg.getTables()) {
                cmd.add(table.getName());
            }
        } else {
            cmd.add(arg.getDatabase().getName());
        }

        return cmd;
    }

    @Override
    public void fillProcessParameters(MySQLExportSettings settings, MySQLDatabaseExportInfo arg, List<String> cmd) throws IOException {
        File dumpBinary = RuntimeUtils.getNativeClientBinary(settings.getClientHome(), MySQLConstants.BIN_FOLDER, "mysqldump"); //$NON-NLS-1$
        String dumpPath = dumpBinary.getAbsolutePath();
        cmd.add(dumpPath);
        switch (settings.getMethod()) {
            case LOCK_ALL_TABLES:
                cmd.add("--lock-all-tables"); //$NON-NLS-1$
                break;
            case ONLINE:
                cmd.add("--single-transaction"); //$NON-NLS-1$
                break;
        }

        if (settings.isNoCreateStatements()) {
            cmd.add("--no-create-info"); //$NON-NLS-1$
        } else {
            if (CommonUtils.isEmpty(arg.getTables())) {
                cmd.add("--routines"); //$NON-NLS-1$
            }
        }
        if (settings.isAddDropStatements()) {
            cmd.add("--add-drop-table"); //$NON-NLS-1$
        } else {
            cmd.add("--skip-add-drop-table"); //$NON-NLS-1$
        }
        if (settings.isDisableKeys()) cmd.add("--disable-keys"); //$NON-NLS-1$
        if (settings.isExtendedInserts()) {
            cmd.add("--extended-insert"); //$NON-NLS-1$
        } else {
            cmd.add("--skip-extended-insert"); //$NON-NLS-1$
        }
        if (settings.isBinariesInHex()) {
            cmd.add("--hex-blob"); //$NON-NLS-1$
        }
        if (settings.isNoData()) {
            cmd.add("--no-data"); //$NON-NLS-1$
        }
        if (settings.isDumpEvents()) cmd.add("--events"); //$NON-NLS-1$
        if (settings.isComments()) cmd.add("--comments"); //$NON-NLS-1$

        settings.addExtraCommandArgs(cmd);
    }

    @Override
    protected boolean needsModelRefresh() {
        return false;
    }

    @Override
    protected boolean isLogInputStream() {
        return false;
    }

    @Override
    protected void startProcessHandler(DBRProgressMonitor monitor, DBTTask task, MySQLExportSettings settings, final MySQLDatabaseExportInfo arg, ProcessBuilder processBuilder, Process process, Log log) throws IOException {
        super.startProcessHandler(monitor, task, settings, arg, processBuilder, process, log);
        String outFileName = GeneralUtils.replaceVariables(settings.getOutputFilePattern(), name -> {
            switch (name) {
                case NativeToolUtils.VARIABLE_DATABASE:
                    return arg.getDatabase().getName();
                case NativeToolUtils.VARIABLE_HOST:
                    return arg.getDatabase().getDataSource().getContainer().getConnectionConfiguration().getHostName();
                case NativeToolUtils.VARIABLE_TABLE:
                    final Iterator<MySQLTableBase> iterator = arg.getTables() == null ? null : arg.getTables().iterator();
                    if (iterator != null && iterator.hasNext()) {
                        return iterator.next().getName();
                    } else {
                        return "null";
                    }
                case NativeToolUtils.VARIABLE_TIMESTAMP:
                    return RuntimeUtils.getCurrentTimeStamp();
                case NativeToolUtils.VARIABLE_DATE:
                    return RuntimeUtils.getCurrentDate();
                default:
                    System.getProperty(name);
            }
            return null;
        });

        File outFile = new File(settings.getOutputFolder(), outFileName);

        boolean isFiltering = settings.isRemoveDefiner();
        Thread job = isFiltering ?
            new DumpFilterJob(monitor, process.getInputStream(), outFile, log) :
            new DumpCopierJob(monitor, "Dump database", process.getInputStream(), outFile, log);
        job.start();
    }


    static class DumpFilterJob extends DumpJob {
        private Pattern DEFINER_PATTER = Pattern.compile("DEFINER\\s*=\\s*`[^*]*`@`[0-9a-z\\-_\\.%]*`", Pattern.CASE_INSENSITIVE);

        DumpFilterJob(DBRProgressMonitor monitor, InputStream stream, File outFile, Log log) {
            super("MySQL databasse dump filter", monitor, stream, outFile, log);
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
                    for (; ; ) {
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
            } finally {
                monitor.done();
            }
        }

        @NotNull
        private String filterLine(@NotNull String line) {
            return line;
        }
    }

}

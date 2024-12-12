/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mysql.tasks;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.mysql.MySQLConstants;
import org.jkiss.dbeaver.ext.mysql.model.MySQLTableBase;
import org.jkiss.dbeaver.model.connection.DBPNativeClientLocation;
import org.jkiss.dbeaver.model.fs.DBFUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableContext;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.task.DBTTask;
import org.jkiss.dbeaver.registry.task.TaskPreferenceStore;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
import org.jkiss.utils.CommonUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MySQLDatabaseExportHandler extends MySQLNativeToolHandler<MySQLExportSettings, DBSObject, MySQLDatabaseExportInfo> {
    private static final String DISTRIB = "Distrib ";
    private static final String VER = "Ver ";

    @Override
    public Collection<MySQLDatabaseExportInfo> getRunInfo(MySQLExportSettings settings) {
        return settings.getExportObjects();
    }

    @Override
    protected MySQLExportSettings createTaskSettings(DBRRunnableContext context, DBTTask task) throws DBException {
        MySQLExportSettings settings = new MySQLExportSettings(task.getProject());
        settings.loadSettings(context, new TaskPreferenceStore(task));

        return settings;
    }

    @Override
    protected boolean validateTaskParameters(DBTTask task, MySQLExportSettings settings, Log log) {
        if (task.getType().getId().equals(MySQLTasks.TASK_DATABASE_BACKUP)) {
            for (MySQLDatabaseExportInfo exportObject : settings.getExportObjects()) {
                final String dir = settings.getOutputFolder(exportObject);
                try {
                    Path outFile = DBFUtils.resolvePathFromString(new VoidProgressMonitor(), task.getProject(), dir);
                    if (!Files.exists(outFile)) {
                        Files.createDirectories(outFile);
                    }
                } catch (Exception e) {
                    log.error("Can't create directory '" + dir + "'");
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    protected List<String> getCommandLine(MySQLExportSettings settings, MySQLDatabaseExportInfo arg) throws IOException {
        List<String> cmd = super.getCommandLine(settings, arg);
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
        DBPNativeClientLocation nativeClientLocation = settings.getClientHome();
        if (nativeClientLocation == null) {
            throw new IllegalArgumentException("Client home can not be null!");
        }
        File dumpBinary = RuntimeUtils.getNativeClientBinary(settings.getClientHome(), MySQLConstants.BIN_FOLDER, "mysqldump"); //$NON-NLS-1$
        String dumpPath = dumpBinary.getAbsolutePath();
        cmd.add(dumpPath);
        cmd.add(settings.getMethod().getCliOption());
        if (supportsColumnStatistics(dumpPath) && !arg.getDatabase().getDataSource().supportsColumnStatistics()) {
            cmd.add("--column-statistics=0");
        }
        if (settings.isNoCreateStatements()) {
            cmd.add("--no-create-info"); //$NON-NLS-1$
        } else {
            if (CommonUtils.isEmpty(arg.getTables()) && !settings.isNoRoutines()) {
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
    protected void startProcessHandler(DBRProgressMonitor monitor, DBTTask task, MySQLExportSettings settings, final MySQLDatabaseExportInfo arg, ProcessBuilder processBuilder, Process process, Log log) throws IOException, DBException {
        super.startProcessHandler(monitor, task, settings, arg, processBuilder, process, log);
        String outFileStr = settings.getOutputFile(arg);
        Path outFile = DBFUtils.resolvePathFromString(monitor, task.getProject(), outFileStr);
        if (Files.exists(outFile)) {
            // Unlike pg_dump, mysqldump happily overrides files which can easily lead to a lost dump.
            // We prevent that with our manual check
            // https://github.com/dbeaver/dbeaver/issues/11532
            throw new IOException("Output file already exists");
        }
        boolean isFiltering = settings.isRemoveDefiner();
        Thread job = isFiltering ?
            new DumpFilterJob(monitor, process.getInputStream(), outFile, log) :
            new DumpCopierJob(monitor, "Dump database", process.getInputStream(), outFile, log);
        job.start();
    }


    static class DumpFilterJob extends DumpJob {
        private final Pattern DEFINER_PATTER = Pattern.compile("DEFINER\\s*=\\s*`[^*]*`@`[0-9a-z\\-_\\.%]*`", Pattern.CASE_INSENSITIVE);

        DumpFilterJob(DBRProgressMonitor monitor, InputStream stream, Path outFile, Log log) {
            super("MySQL databasse dump filter", monitor, stream, outFile, log);
        }

        @Override
        public void runDump() throws IOException {
            monitor.beginTask("Export database", 100);
            long prevStatusUpdateTime = 0;
            try {
                NumberFormat numberFormat = NumberFormat.getInstance();

                LineNumberReader reader = new LineNumberReader(new InputStreamReader(input, GeneralUtils.DEFAULT_ENCODING));
                try (OutputStream output = Files.newOutputStream(outFile)) {
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

    private static boolean supportsColumnStatistics(@NotNull String mysqldumpPath) {
        String fullVersion;
        try {
            fullVersion = RuntimeUtils.executeProcess(mysqldumpPath, MySQLConstants.FLAG_VERSION);
        } catch (DBException e) {
            return false;
        }
        if (fullVersion == null || fullVersion.contains("MariaDB")) {
            return false;
        }
        int fromIdx = fullVersion.indexOf(DISTRIB);
        if (fromIdx == -1) {
            fromIdx = fullVersion.indexOf(VER);
            if (fromIdx == -1) {
                return false;
            }
            fromIdx += VER.length();
        } else {
            fromIdx += DISTRIB.length();
        }
        int toIdx = fullVersion.indexOf(".", fromIdx);
        int majorVersion = CommonUtils.toInt(fullVersion.substring(fromIdx, toIdx));
        return majorVersion >= 8;
    }
}

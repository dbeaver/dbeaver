/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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

package org.jkiss.dbeaver.model.sql.format.external;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.format.SQLFormatter;
import org.jkiss.dbeaver.model.sql.format.SQLFormatterConfiguration;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.ArgumentTokenizer;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.IOUtils;

import java.io.*;
import java.util.List;

/**
 * External SQL formatter
 */
public class SQLExternalFormatter implements SQLFormatter {

    public static final String FORMATTER_ID = "EXTERNAL";

    private static final Log log = Log.getLog(SQLExternalFormatter.class);

    @Override
    public String format(String source, SQLFormatterConfiguration configuration) {
        final DBPPreferenceStore store = configuration.getSyntaxManager().getPreferenceStore();
        final String command = store.getString(ModelPreferences.SQL_FORMAT_EXTERNAL_CMD);
        int timeout = store.getInt(ModelPreferences.SQL_FORMAT_EXTERNAL_TIMEOUT);
        boolean useFile = store.getBoolean(ModelPreferences.SQL_FORMAT_EXTERNAL_FILE);
        try {
            final FormatJob formatJob = new FormatJob(configuration, command, source, useFile);
            formatJob.schedule();
            for (int i = 0; i < 10; i++) {
                Thread.sleep(timeout / 10);
                if (formatJob.finished) {
                    return formatJob.result;
                }
            }
            log.warn("Formatter process hangs. Terminating.");
            formatJob.stop();
        }
        catch (Exception ex) {
            log.warn("Error executing external formatter [" + command + "]", ex);
        }

        return source;
    }

    private static class FormatJob extends AbstractJob {
        SQLFormatterConfiguration configuration;
        String command;
        Process process;
        String source;
        boolean useFile;
        String result = "";
        public boolean finished;

        public FormatJob(SQLFormatterConfiguration configuration, String command, String source, boolean useFile) {
            super("External format: " + command);
            this.command = command;
            this.configuration = configuration;
            this.source = source;
            this.useFile = useFile;
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            final String sourceEncoding = configuration.getSourceEncoding();
            File tmpFile = null;
            try {
                if (CommonUtils.isEmpty(command)) {
                    throw new IOException("No command specified for external formatter");
                }
                if (useFile) {
                    tmpFile = File.createTempFile("dbeaver-sql-format", "sql");
                    try (final OutputStream os = new FileOutputStream(tmpFile)) {
                        try (final Writer out = new OutputStreamWriter(os, sourceEncoding)) {
                            IOUtils.copyText(new StringReader(source), out);
                        }
                    }
                    command = command.replace("${file}", tmpFile.getAbsolutePath());
                }
                List<String> commandList = ArgumentTokenizer.tokenize(command, false);
                ProcessBuilder pb = new ProcessBuilder(commandList);
                pb.redirectErrorStream(true);
                process = pb.start();
                try {
                    if (tmpFile == null) {
                        try (final OutputStream stdout = process.getOutputStream()) {
                            stdout.write(source.getBytes(sourceEncoding));
                        }
                    }
                    int rc = process.waitFor();
                    StringWriter buf = new StringWriter();
                    try (Reader input = new InputStreamReader(process.getInputStream(), sourceEncoding)) {
                        IOUtils.copyText(input, buf);
                        if (rc != 0) {
                            log.debug("Formatter result code: " + rc);
                        }
                    }
                    result = buf.toString();
                } finally {
                    process.destroy();
                }
            } catch (Exception e) {
                result = source;
                finished = true;
                return GeneralUtils.makeExceptionStatus(e);
//                log.error(e);
            } finally {
                if (tmpFile != null && tmpFile.exists()) {
                    if (!tmpFile.delete()) {
                        log.debug("Can't delete temp file '" + tmpFile.getAbsolutePath() + "'");
                    }
                }
            }
            finished = true;
            return Status.OK_STATUS;
        }

        void stop() {
            if (process != null) {
                process.destroy();
            }
        }

    }
}
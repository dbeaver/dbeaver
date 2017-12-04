/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
public class SQLFormatterExternal implements SQLFormatter {

    public static final String FORMATTER_ID = "EXTERNAL";

    private static final Log log = Log.getLog(SQLFormatterExternal.class);
    public static final String VAR_FILE = "file";

    @Override
    public String format(String source, SQLFormatterConfiguration configuration) {
        final DBPPreferenceStore store = configuration.getSyntaxManager().getPreferenceStore();
        final String command = store.getString(ModelPreferences.SQL_FORMAT_EXTERNAL_CMD);
        int timeout = store.getInt(ModelPreferences.SQL_FORMAT_EXTERNAL_TIMEOUT);
        boolean useFile = store.getBoolean(ModelPreferences.SQL_FORMAT_EXTERNAL_FILE);
        if (CommonUtils.isEmpty(command)) {
            // Nothing to format
            return source;
        }

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
            super("External format - " + command);
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
                    command = command.replace(GeneralUtils.variablePattern(VAR_FILE), tmpFile.getAbsolutePath());
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
                    StringWriter buf = new StringWriter();
                    try (Reader input = new InputStreamReader(process.getInputStream(), sourceEncoding)) {
                        IOUtils.copyText(input, buf);
                    }
                    int rc = process.waitFor();
                    if (rc != 0) {
                        log.debug("Formatter result code: " + rc);
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
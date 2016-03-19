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
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.format.SQLFormatter;
import org.jkiss.dbeaver.model.sql.format.SQLFormatterConfiguration;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;

/**
 * External SQL formatter
 */
public class SQLExternalFormatter implements SQLFormatter {

    public static final String FORMATTER_ID = "External";

    static final Log log = Log.getLog(SQLExternalFormatter.class);

    @Override
    public String format(String source, SQLFormatterConfiguration configuration) {
        final String command = configuration.getSyntaxManager().getPreferenceStore().getString(ModelPreferences.SQL_FORMAT_EXTERNAL_CMD);
        if (CommonUtils.isEmpty(command)) {
            log.error("No command specified for external formatter");
            return source;
        }
        try {
            final FormatJob formatJob = new FormatJob(configuration, command, source);
            formatJob.schedule();
            for (int i = 0; i < 10; i++) {
                Thread.sleep(50);
                if (formatJob.finished) {
                    return formatJob.result.toString();
                }
            }
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
        StringBuilder result = new StringBuilder();
        public boolean finished;

        public FormatJob(SQLFormatterConfiguration configuration, String command, String source) {
            super("External format: " + command);
            this.command = command;
            this.configuration = configuration;
            this.source = source;
        }

        @Override
        protected IStatus run(DBRProgressMonitor monitor) {
            try {
                process = Runtime.getRuntime().exec(command);
                try {
                    final String sourceEncoding = configuration.getSourceEncoding();
                    try (final OutputStream stdout = process.getOutputStream()) {
                        stdout.write(source.getBytes(sourceEncoding));
                    }
                    try (BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream(), sourceEncoding))) {
                        String line;
                        while ((line = input.readLine()) != null) {
                            if (result.length() > 0) result.append('\n');
                            result.append(line);
                        }
                    }
                } finally {
                    process.destroy();
                }
            } catch (Exception e) {
                result = new StringBuilder(source);
                finished = true;
                return GeneralUtils.makeExceptionStatus(e);
//                log.error(e);
            }
            finished = true;
            return Status.OK_STATUS;
        }

        void stop() {
            process.destroy();
        }

    }
}
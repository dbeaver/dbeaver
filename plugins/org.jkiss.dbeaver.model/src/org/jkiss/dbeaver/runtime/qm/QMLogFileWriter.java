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
package org.jkiss.dbeaver.runtime.qm;

import org.eclipse.core.runtime.IStatus;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceListener;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.qm.*;
import org.jkiss.dbeaver.model.qm.meta.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.runtime.DBWorkbench;
import org.jkiss.dbeaver.utils.ContentUtils;
import org.jkiss.dbeaver.utils.GeneralUtils;
import org.jkiss.utils.CommonUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

/**
 * Query manager log writer
 */
public class QMLogFileWriter implements QMMetaListener, DBPPreferenceListener {

    private static final Log log = Log.getLog(QMLogFileWriter.class);
    private static final DateTimeFormatter LOG_FILENAME_FORMATTER = DateTimeFormatter
        .ofPattern("'dbeaver_sql_'" + GeneralUtils.DEFAULT_DATE_PATTERN + "'.log'", Locale.ENGLISH);

    private File logFile;
    private boolean enabled;

    private Writer logWriter;
    private QMEventFilter eventFilter;
    private final String lineSeparator;

    public QMLogFileWriter()
    {
        lineSeparator = GeneralUtils.getDefaultLineSeparator();
        DBWorkbench.getPlatform().getPreferenceStore().addPropertyChangeListener(this);
        initLogFile();
    }

    public void dispose()
    {
        DBWorkbench.getPlatform().getPreferenceStore().removePropertyChangeListener(this);
    }

    private synchronized void initLogFile()
    {
        final DBPPreferenceStore preferences = DBWorkbench.getPlatform().getPreferenceStore();
        enabled = preferences.getBoolean(QMConstants.PROP_STORE_LOG_FILE);
        if (enabled) {
            final int daysToKeepLogs = preferences.getInt(QMConstants.PROP_HISTORY_DAYS);
            String logFolderPath = preferences.getString(QMConstants.PROP_LOG_DIRECTORY);
            if (CommonUtils.isEmpty(logFolderPath)) {
                logFolderPath = GeneralUtils.getMetadataFolder().toAbsolutePath().toString();
            }
            try {
                purgeOldLogs(Path.of(logFolderPath), daysToKeepLogs);
            } catch (IOException e) {
                log.debug("Error purging old logs: " + e.getMessage());
            }

            File logFolder = new File(logFolderPath);
            if (!logFolder.exists()) {
                if (!logFolder.mkdirs()) {
                    log.error("Can't create log folder '" + logFolderPath + "'");
                }
            }
            logFile = new File(logFolder, LOG_FILENAME_FORMATTER.format(LocalDate.now()));
            try {
                logWriter = new FileWriter(logFile, true);
            } catch (IOException e) {
                log.error("Can't open log writer", e);
            }
        } else {
            if (logWriter != null) {
                ContentUtils.close(logWriter);
                logWriter = null;
            }
        }
        eventFilter = new DefaultEventFilter();
    }

    private static void purgeOldLogs(@NotNull Path logDirectory, int daysToKeep) throws IOException {
        final LocalDate today = LocalDate.now();
        final LocalDate judgementDay = today.minusDays(daysToKeep);

        try (Stream<Path> list = Files.list(logDirectory)) {
            list
                .filter(file -> {
                    try {
                        final LocalDate date = LOG_FILENAME_FORMATTER.parse(file.getFileName().toString(), LocalDate::from);
                        return judgementDay.isAfter(date);
                    } catch (DateTimeParseException e) {
                        return false;
                    }
                })
                .forEach(file -> {
                    try {
                        Files.delete(file);
                    } catch (IOException e) {
                        log.debug("Unable to purge the old log file '" + file + "': " + e.getMessage());
                    }
                });
        }
    }

    @Override
    public synchronized void metaInfoChanged(@NotNull DBRProgressMonitor monitor, @NotNull List<QMMetaEvent> events)
    {
        if (!enabled || logWriter == null) {
            return;
        }

        StringBuilder logBuffer = new StringBuilder(4000);
        for (QMMetaEvent event : events) {
            if (eventFilter.accept(event)) {
                writeEvent(logBuffer, event);
            }
        }

        try {
            logWriter.write(logBuffer.toString());
            logWriter.flush();
        } catch (IOException e) {
            log.warn("IO error writing QM log. Disable log file writer", e);
            ContentUtils.close(logWriter);
            logWriter = null;
        }
    }

    @Override
    public void preferenceChange(PreferenceChangeEvent event)
    {
        if (event.getProperty().startsWith(QMConstants.PROP_PREFIX)) {
            initLogFile();
        }
    }

    private void writeEvent(StringBuilder buffer, QMMetaEvent event)
    {
        QMMObject object = event.getObject();
        QMEventAction action = event.getAction();
        // Filter
        if (object instanceof QMMStatementInfo || object instanceof QMMTransactionSavepointInfo ||
            (object instanceof QMMStatementExecuteInfo && action != QMEventAction.END)) {
            return;
        }

        // Entry
        int severity = object instanceof QMMStatementExecuteInfo ? IStatus.INFO : IStatus.OK;
        buffer.append("!ENTRY ");
        appendEntryInfo(buffer, severity, object.getObjectId(), object.getOpenTime());

        // Message
        buffer.append("!MESSAGE ");
        if (object instanceof QMMStatementExecuteInfo) {
            QMMStatementExecuteInfo executeInfo = (QMMStatementExecuteInfo)object;
            buffer.append(executeInfo.getQueryString());
            buffer.append(lineSeparator);
            buffer.append("!SUBENTRY 1 ");
            int subSeverity = executeInfo.hasError() ? IStatus.ERROR : severity;
            appendEntryInfo(buffer, subSeverity, executeInfo.getErrorCode(), object.getCloseTime());
            buffer.append("!MESSAGE ");
            if (executeInfo.hasError()) {
                buffer.append(executeInfo.getErrorMessage());
            } else {
                buffer.append("SUCCESS [").append(executeInfo.getUpdateRowCount()).append("]");
            }

        } else if (object instanceof QMMTransactionInfo) {
            QMMTransactionInfo transactionInfo = (QMMTransactionInfo) object;
            if (transactionInfo.isCommitted()) {
                buffer.append("COMMIT");
            } else {
                buffer.append("ROLLBACK");
            }
        } else if (object instanceof QMMConnectionInfo) {
            QMMConnectionInfo sessionInfo = (QMMConnectionInfo) object;
            buffer.append(action).append(" SESSION [").append(sessionInfo.getContainerName()).append("]");
        }
        buffer.append(lineSeparator);

        buffer.append(lineSeparator);
    }

    private void appendEntryInfo(StringBuilder buffer, int severity, long code, long time)
    {
        buffer.append(DBConstants.MODEL_BUNDLE_ID).append(" ").append(severity).append(" ").append(code).append(" ");
        appendDate(buffer, time);
        buffer.append(lineSeparator);
    }

    private void appendDate(StringBuilder sb, long timestamp) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(timestamp);
        appendPaddedInt(c.get(Calendar.YEAR), 4, sb).append('-');
        appendPaddedInt(c.get(Calendar.MONTH) + 1, 2, sb).append('-');
        appendPaddedInt(c.get(Calendar.DAY_OF_MONTH), 2, sb).append(' ');
        appendPaddedInt(c.get(Calendar.HOUR_OF_DAY), 2, sb).append(':');
        appendPaddedInt(c.get(Calendar.MINUTE), 2, sb).append(':');
        appendPaddedInt(c.get(Calendar.SECOND), 2, sb).append('.');
        appendPaddedInt(c.get(Calendar.MILLISECOND), 3, sb);
    }

    private StringBuilder appendPaddedInt(int value, int pad, StringBuilder buffer) {
        pad = pad - 1;
        if (pad == 0)
            return buffer.append(Integer.toString(value));
        int padding = (int) Math.pow(10, pad);
        if (value >= padding)
            return buffer.append(Integer.toString(value));
        while (padding > value && padding > 1) {
            buffer.append('0');
            padding = padding / 10;
        }
        buffer.append(value);
        return buffer;
    }

}

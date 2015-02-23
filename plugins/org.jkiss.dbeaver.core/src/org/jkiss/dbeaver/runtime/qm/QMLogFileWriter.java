/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.runtime.qm;

import org.jkiss.dbeaver.core.Log;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.jkiss.dbeaver.DBeaverConstants;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.runtime.qm.meta.*;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Calendar;
import java.util.List;

/**
 * Query manager log writer
 */
public class QMLogFileWriter implements QMMetaListener, IPropertyChangeListener {

    static final Log log = Log.getLog(QMLogFileWriter.class);

    private File logFile;
    private boolean enabled;

    private Writer logWriter;
    private QMEventFilter eventFilter;
    private final String lineSeparator;

    public QMLogFileWriter()
    {
        lineSeparator = ContentUtils.getDefaultLineSeparator();
        DBeaverCore.getGlobalPreferenceStore().addPropertyChangeListener(this);
        initLogFile();
    }

    public void dispose()
    {
        DBeaverCore.getGlobalPreferenceStore().removePropertyChangeListener(this);
    }

    private synchronized void initLogFile()
    {
        enabled = DBeaverCore.getGlobalPreferenceStore().getBoolean(QMConstants.PROP_STORE_LOG_FILE);
        if (enabled) {
            String logFolder = DBeaverCore.getGlobalPreferenceStore().getString(QMConstants.PROP_LOG_DIRECTORY);
            String logFileName = "dbeaver_sql_" + RuntimeUtils.getCurrentDate() + ".log";
            logFile = new File(logFolder, logFileName);
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

    @Override
    public synchronized void metaInfoChanged(List<QMMetaEvent> events)
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
    public void propertyChange(PropertyChangeEvent event)
    {
        if (event.getProperty().startsWith(QMConstants.PROP_PREFIX)) {
            initLogFile();
        }
    }

    private void writeEvent(StringBuilder buffer, QMMetaEvent event)
    {
        QMMObject object = event.getObject();
        QMMetaEvent.Action action = event.getAction();
        // Filter
        if (object instanceof QMMStatementInfo || object instanceof QMMTransactionSavepointInfo ||
            (object instanceof QMMStatementExecuteInfo && action != QMMetaEvent.Action.END)) {
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
                buffer.append("SUCCESS [").append(executeInfo.getRowCount()).append("]");
            }

        } else if (object instanceof QMMTransactionInfo) {
            QMMTransactionInfo transactionInfo = (QMMTransactionInfo)object;
            if (transactionInfo.isCommited()) {
                buffer.append("COMMIT");
            } else {
                buffer.append("ROLLBACK");
            }
        } else if (object instanceof QMMSessionInfo) {
            QMMSessionInfo sessionInfo = (QMMSessionInfo)object;
            buffer.append(action).append(" SESSION [").append(sessionInfo.getContainer().getName()).append("]");
        }
        buffer.append(lineSeparator);

        buffer.append(lineSeparator);
    }

    private void appendEntryInfo(StringBuilder buffer, int severity, long code, long time)
    {
        buffer.append(DBeaverConstants.PLUGIN_ID).append(" ").append(severity).append(" ").append(code).append(" ");
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

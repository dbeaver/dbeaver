package org.jkiss.dbeaver.runtime.qm;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.jkiss.dbeaver.core.DBeaverCore;
import org.jkiss.dbeaver.runtime.RuntimeUtils;
import org.jkiss.dbeaver.runtime.qm.meta.QMMObject;
import org.jkiss.dbeaver.runtime.qm.meta.QMMStatementExecuteInfo;
import org.jkiss.dbeaver.utils.ContentUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.List;

/**
 * Query manager log writer
 */
public class QMLogFileWriter implements QMMetaListener, IPropertyChangeListener {

    static final Log log = LogFactory.getLog(QMLogFileWriter.class);

    private File logFile;
    private boolean enabled;

    private Writer logWriter;

    public QMLogFileWriter()
    {
        DBeaverCore.getInstance().getGlobalPreferenceStore().addPropertyChangeListener(this);
        initLogFile();
    }

    public void dispose()
    {
        DBeaverCore.getInstance().getGlobalPreferenceStore().removePropertyChangeListener(this);
    }

    private synchronized void initLogFile()
    {
        enabled = DBeaverCore.getInstance().getGlobalPreferenceStore().getBoolean(QMConstants.PROP_STORE_LOG_FILE);
        if (enabled) {
            String logFolder = DBeaverCore.getInstance().getGlobalPreferenceStore().getString(QMConstants.PROP_LOG_DIRECTORY);
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
    }

    @Override
    public synchronized void metaInfoChanged(List<QMMetaEvent> events)
    {
        if (!enabled || logWriter == null) {
            return;
        }

        StringBuilder logBuffer = new StringBuilder(4000);
        for (int i = 0; i < events.size(); i++) {
            QMMetaEvent event = events.get(i - 1);
            QMMObject object = event.getObject();
            if (object instanceof QMMStatementExecuteInfo) {

            }
        }

        try {
            logWriter.write(logBuffer.toString());
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

}

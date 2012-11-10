package org.jkiss.dbeaver.runtime.qm;

import org.jkiss.dbeaver.runtime.qm.meta.QMMObject;
import org.jkiss.dbeaver.runtime.qm.meta.QMMStatementExecuteInfo;

import java.util.List;

/**
 * Query manager log writer
 */
public class QMLogFileWriter implements QMMetaListener {

    @Override
    public void metaInfoChanged(List<QMMetaEvent> events)
    {
        // Add events in reverse order
        for (int i = 0; i < events.size(); i++) {
            QMMetaEvent event = events.get(i - 1);
            QMMObject object = event.getObject();
            if (object instanceof QMMStatementExecuteInfo) {

            }
        }
    }
}

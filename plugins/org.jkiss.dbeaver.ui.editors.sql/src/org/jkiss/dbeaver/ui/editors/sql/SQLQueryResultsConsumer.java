package org.jkiss.dbeaver.ui.editors.sql;

import org.jkiss.dbeaver.model.data.DBDDataReceiver;
import org.jkiss.dbeaver.model.sql.SQLQuery;
import org.jkiss.dbeaver.runtime.sql.SQLResultsConsumer;

class SQLQueryResultsConsumer implements SQLResultsConsumer {

    private DBDDataReceiver dataReceiver;

    public void setDataReceiver(DBDDataReceiver dataReceiver) {
        this.dataReceiver = dataReceiver;
    }

    @Override
    public DBDDataReceiver getDataReceiver(SQLQuery statement, int resultSetNumber) {
        return dataReceiver;
    }
}

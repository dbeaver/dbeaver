package org.jkiss.dbeaver.ui.controls.resultset;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.dbc.DBCSession;
import org.jkiss.dbeaver.DBException;

/**
 * ResultSetProvider
 */
public interface ResultSetProvider {

    DBCSession getSession()
        throws DBException;

    DBPDataSource getDataSource();

    boolean isConnected();

    void extractResultSetData(int offset);

}

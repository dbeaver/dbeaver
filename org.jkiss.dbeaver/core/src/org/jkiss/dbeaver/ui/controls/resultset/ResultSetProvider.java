package org.jkiss.dbeaver.ui.controls.resultset;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.dbc.DBCSession;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.ui.IDataSourceUser;

/**
 * ResultSetProvider
 */
public interface ResultSetProvider extends IDataSourceUser {

    DBCSession getSession() throws DBException;

    boolean isConnected();

    void extractResultSetData(int offset);

}

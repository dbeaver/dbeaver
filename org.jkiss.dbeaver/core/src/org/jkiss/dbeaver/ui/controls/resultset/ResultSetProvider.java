package org.jkiss.dbeaver.ui.controls.resultset;

import org.jkiss.dbeaver.model.DBPDataSource;

/**
 * ResultSetProvider
 */
public interface ResultSetProvider {

    DBPDataSource getDataSource();

    boolean isConnected();

    void extractResultSetData(int offset);

}

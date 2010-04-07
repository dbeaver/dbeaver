package org.jkiss.dbeaver.ui.controls.resultset;

import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.data.DBDValueLocator;

/**
 * ResultSet column info
 */
class ResultSetColumn {
    final DBCColumnMetaData metaData;
    final DBDValueHandler valueHandler;
    boolean editable;
    DBDValueLocator valueLocator;

    ResultSetColumn(DBCColumnMetaData metaData, DBDValueHandler valueHandler)
    {
        this.metaData = metaData;
        this.valueHandler = valueHandler;
    }
}

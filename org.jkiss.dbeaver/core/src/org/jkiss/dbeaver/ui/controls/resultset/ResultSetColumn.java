package org.jkiss.dbeaver.ui.controls.resultset;

import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;
import org.jkiss.dbeaver.model.data.DBDValueHandler;

/**
 * ResultSet column info
 */
class ResultSetColumn {
    final DBCColumnMetaData metaData;
    final DBDValueHandler valueHandler;

    ResultSetColumn(DBCColumnMetaData metaData, DBDValueHandler valueHandler)
    {
        this.metaData = metaData;
        this.valueHandler = valueHandler;
    }
}

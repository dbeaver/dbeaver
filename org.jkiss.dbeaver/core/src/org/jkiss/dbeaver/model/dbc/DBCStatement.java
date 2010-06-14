/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.dbc;

import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.runtime.DBRBlockingObject;
import org.jkiss.dbeaver.model.DBPDataSource;

/**
 * DBCStatement
 */
public interface DBCStatement extends DBRBlockingObject
{

    DBPDataSource getDataSource();

    boolean executeStatement() throws DBCException;

    DBCResultSet openResultSet() throws DBCException;

    int getUpdateRowCount() throws DBCException;

    void close();

    void setLimit(int offset, int limit) throws DBCException;

    /**
     * Statement data container.
     * Usually it is null, but in some cases (like table data editor) it's set to certain DBS object.
     * Can be used by result set metadata objects to perform values manipulation.
     * @return data container or null
     */
    DBSObject getDataContainer();

    void setDataContainer(DBSObject container);

}

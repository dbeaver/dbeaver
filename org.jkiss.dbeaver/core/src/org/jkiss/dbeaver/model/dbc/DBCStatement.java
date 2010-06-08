/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.dbc;

import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * DBCStatement
 */
public interface DBCStatement
{
    Object getNestedStatement();

    DBCSession getSession();

    void execute() throws DBCException;

    boolean hasResultSet() throws DBCException;

    DBCResultSet getResultSet() throws DBCException;

    void closeResultSet();

    int getUpdateCount() throws DBCException;

    void cancel() throws DBCException;

    void close();

    void setFirstResult(int offset) throws DBCException;

    void setMaxResults(int limit) throws DBCException;

    /**
     * Statement data container.
     * Usually it is null, but in some cases (like table data editor) it's set to certain DBS object.
     * Can be used by result set metadata objects to perform values manipulation.
     * @return data container or null
     */
    DBSObject getDataContainer();

    void setDataContainer(DBSObject container);

}

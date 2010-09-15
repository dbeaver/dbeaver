/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext;

import org.jkiss.dbeaver.DBException;

/**
 * IResultSetExporter
 */
public interface IResultSetExporter {

    void exportResultSet(IResultSetProvider resultSetProvider)
        throws DBException;

}

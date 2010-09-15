/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext;

import org.jkiss.dbeaver.DBException;

import java.util.Map;

/**
 * IResultSetExporter
 */
public interface IResultSetExporter {

    void init(Map<String, Object> properties)
        throws DBException;

    void exportResultSet(IResultSetProvider resultSetProvider)
        throws DBException;

    void dispose();

}

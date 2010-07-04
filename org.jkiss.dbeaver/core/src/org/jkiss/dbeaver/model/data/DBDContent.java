/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.model.dbc.DBCException;

/**
 * DBC LOB
 *
 * @author Serge Rider
 */
public interface DBDContent extends DBDValue {

    long getContentLength() throws DBCException;

    String getContentType() throws DBCException;

    void release();

}

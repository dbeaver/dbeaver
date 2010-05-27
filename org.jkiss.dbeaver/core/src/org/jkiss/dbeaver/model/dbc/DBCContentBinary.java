/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.dbc;

import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.io.InputStream;

/**
 * DBC BLOB
 *
 * @author Serge Rider
 */
public interface DBCContentBinary extends DBCContent {

    InputStream getContents() throws DBCException;

    void updateContents(InputStream stream, long contentLength, DBRProgressMonitor monitor) throws DBCException;

}

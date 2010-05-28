/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.dbc.DBCException;

import java.io.InputStream;

/**
 * DBC BLOB
 *
 * @author Serge Rider
 */
public interface DBDContentBinary extends DBDContent {

    InputStream getContents() throws DBCException;

    void updateContents(
        DBDValueController valueController,
        InputStream stream,
        long contentLength,
        DBRProgressMonitor monitor) throws DBCException;

}

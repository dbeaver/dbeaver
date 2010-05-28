/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.data.DBDValueController;
import org.jkiss.dbeaver.model.dbc.DBCException;

import java.io.Reader;

/**
 * DBC CLOB
 *
 * @author Serge Rider
 */
public interface DBDContentCharacter extends DBDContent {

    String getCharset();

    Reader getContents() throws DBCException;

    void updateContents(
        DBDValueController valueController,
        Reader stream,
        long contentLength,
        DBRProgressMonitor monitor) throws DBCException;

}

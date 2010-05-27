/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.dbc;

import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.io.Reader;

/**
 * DBC CLOB
 *
 * @author Serge Rider
 */
public interface DBCContentCharacter extends DBCContent {

    String getCharset();

    Reader getContents() throws DBCException;

    void updateContents(Reader stream, long contentLength, DBRProgressMonitor monitor) throws DBCException;

}

/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.model.dbc.DBCException;
import org.jkiss.dbeaver.model.dbc.DBCColumnMetaData;

import java.io.IOException;

/**
 * Extension of DBDValueHandler.
 * Implementors of this interface support read/write of values from/in external streams.
 * Used by LOB and BINARY editors.
 */
public interface DBDStreamHandler {

    /**
     * Content kind
     * @return
     */
    DBDStreamKind getContentKind(DBCColumnMetaData columnMetaData);

    /**
     * Gets stream representation of value
     * @param value value object
     * @return InputStream or Reader depending on content kind
     * @throws DBCException
     * @throws IOException
     */
    Object getContents(Object value)
        throws DBCException, IOException;

    long getContentLength(Object value)
        throws DBCException, IOException;

    String getContentType(Object value)
        throws DBCException, IOException;
    
    String getContentEncoding(Object value)
        throws DBCException, IOException;

    /**
     * Updates value from specified content
     * @param value
     * @param content InputStream or Reader depending on content kind
     * @param contentSize
     * @return
     * @throws DBCException
     * @throws IOException
     */
    Object updateContents(Object value, Object content, long contentSize)
        throws DBCException, IOException;

}

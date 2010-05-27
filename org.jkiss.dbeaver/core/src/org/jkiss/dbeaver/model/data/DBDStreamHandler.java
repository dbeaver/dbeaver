/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.model.dbc.DBCException;

import java.io.IOException;
import java.io.InputStream;

/**
 * Extension of DBDValueHandler.
 * Implementors of this interface support read/write of values from/in external streams.
 * Used by LOB and BINARY editors.
 */
public interface DBDStreamHandler {

    /**
     * Gets stream representation of value
     * @param value value object
     * @return stream
     * @throws DBCException
     * @throws IOException
     */
    InputStream getContentStream(Object value)
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
     * @param content
     * @param contentSize
     * @return
     * @throws DBCException
     * @throws IOException
     */
    Object updateContent(Object value, InputStream content, long contentSize)
        throws DBCException, IOException;

}

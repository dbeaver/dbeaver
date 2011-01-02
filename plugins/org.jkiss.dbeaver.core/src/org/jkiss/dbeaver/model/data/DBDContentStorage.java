/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;

/**
 * Content storage
 *
 * @author Serge Rider
 */
public interface DBDContentStorage {

    InputStream getContentStream() throws IOException;

    Reader getContentReader() throws IOException;

    long getContentLength();

    String getCharset();

    DBDContentStorage cloneStorage(DBRProgressMonitor monitor) throws IOException;

    void release();

}
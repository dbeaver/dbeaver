/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

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

    void release();

}
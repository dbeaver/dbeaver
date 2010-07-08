/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import org.jkiss.dbeaver.model.dbc.DBCException;

import java.io.Reader;
import java.io.IOException;
import java.io.InputStream;

/**
 * Content storage
 *
 * @author Serge Rider
 */
public interface DBDContentStorage {

    Reader getContentReader() throws IOException;

    InputStream getContentStream() throws IOException;

    void release();

}
/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

import java.io.IOException;
import java.io.InputStream;

/**
 * Content storage
 *
 * @author Serge Rider
 */
public interface DBDContentStorage {

    InputStream getContentStream() throws IOException;

    String getCharset();

    void release();

}
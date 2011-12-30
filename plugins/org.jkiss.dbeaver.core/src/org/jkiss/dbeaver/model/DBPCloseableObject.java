/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model;

import java.io.Closeable;

/**
 * Closable object
 */
public interface DBPCloseableObject extends Closeable
{

    /**
     * Closes object
     */
    void close();

}

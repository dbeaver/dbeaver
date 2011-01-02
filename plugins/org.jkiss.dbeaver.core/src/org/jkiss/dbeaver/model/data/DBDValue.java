/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.data;

/**
 * DBDvalue
 */
public interface DBDValue extends DBDObject {

    boolean isNull();

    DBDValue makeNull();

    /**
     * Checks transactional state (transactional values valid only during transaction at which thy were retrieved
     * @return
     */
    //boolean isTransactional();

    /**
     * Releases allocated resources. Resets to original value
     */
    void release();
}

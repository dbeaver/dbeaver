/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.runtime;

import org.jkiss.dbeaver.DBException;

/**
 * Object which can block execution flow.
 * Such as socket, statement or connection, etc.
 */
public interface DBRBlockingObject {

    /**
     * Cancels block.
     * In actual implementation this object may not block process at the moment of invocation
     * of this methid. Implementor should check object's state and cancel blocking on demand.
     * @throws DBException on error
     */
    void cancelBlock() throws DBException;

}

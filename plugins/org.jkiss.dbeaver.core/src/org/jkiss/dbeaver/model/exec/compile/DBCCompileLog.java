/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.exec.compile;

import org.apache.commons.logging.Log;

import java.util.Collection;

/**
 * Oracle compile log
 */
public interface DBCCompileLog extends Log {

    Throwable getError();

    Collection<DBCCompileError> getErrorStack();

    void clearLog();

}

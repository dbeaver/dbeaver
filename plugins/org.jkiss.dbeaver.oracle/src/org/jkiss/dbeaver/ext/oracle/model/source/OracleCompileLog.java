package org.jkiss.dbeaver.ext.oracle.model.source;

import org.apache.commons.logging.Log;

import java.util.Collection;

/**
 * Oracle compile log
 */
public interface OracleCompileLog extends Log {

    Throwable getError();

    Collection<OracleCompileError> getErrorStack();

    void clearLog();

}

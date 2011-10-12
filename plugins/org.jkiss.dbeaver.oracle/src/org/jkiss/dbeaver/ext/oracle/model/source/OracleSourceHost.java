/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model.source;

import sun.rmi.runtime.Log;

/**
 * Compile host
 */
public interface OracleSourceHost {

    OracleSourceObject getObject();

    Log getCompileLog();

    void setCompileInfo(String message, boolean error);

    void positionSource(int line, int position);

}

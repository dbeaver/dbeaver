/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model.source;

/**
 * Compile host
 */
public interface OracleSourceHost {

    OracleSourceObject getObject();

    OracleCompileLog getCompileLog();

    void setCompileInfo(String message, boolean error);

    void positionSource(int line, int position);

    void showCompileLog();

}

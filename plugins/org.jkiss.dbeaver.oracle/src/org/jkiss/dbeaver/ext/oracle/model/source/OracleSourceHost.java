/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model.source;

import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectStateful;

/**
 * Compile host
 */
public interface OracleSourceHost {

    DBSObjectStateful getSourceObject();

    OracleCompileLog getCompileLog();

    void setCompileInfo(String message, boolean error);

    void positionSource(int line, int position);

    void showCompileLog();

}

/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.exec.compile;

import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Compile host
 */
public interface DBCSourceHost {

    DBSObject getSourceObject();

    DBCCompileLog getCompileLog();

    void setCompileInfo(String message, boolean error);

    void positionSource(int line, int position);

    void showCompileLog();

}

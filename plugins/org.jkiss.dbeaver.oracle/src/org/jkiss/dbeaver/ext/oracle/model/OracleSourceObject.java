/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Stored code interface
 */
public interface OracleSourceObject extends DBSObject {

    void setName(String name);

    OracleSchema getSchema();

    OracleSourceType getSourceType();

    String getSourceDeclaration(DBRProgressMonitor monitor)
        throws DBException;

    void setSourceDeclaration(String source);

}

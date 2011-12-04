/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.mysql.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * MySQLSourceObject
 */
public interface MySQLSourceObject extends DBSObject {

    String getSourceText(DBRProgressMonitor monitor)
        throws DBException;

    void setSourceText(String sourceText)
        throws DBException;

}

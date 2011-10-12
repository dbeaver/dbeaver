/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model.source;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

/**
 * Stored code interface
 */
public interface OracleSourceObjectEx extends OracleSourceObject {

    String getSourceDefinition(DBRProgressMonitor monitor)
        throws DBException;

    void setSourceDefinition(String source);

}

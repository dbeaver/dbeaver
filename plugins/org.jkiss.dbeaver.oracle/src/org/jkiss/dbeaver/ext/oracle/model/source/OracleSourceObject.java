/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model.source;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.IDatabasePersistAction;
import org.jkiss.dbeaver.ext.oracle.model.OracleDataSource;
import org.jkiss.dbeaver.ext.oracle.model.OracleSchema;
import org.jkiss.dbeaver.ext.oracle.model.OracleSourceType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectStateful;

/**
 * Stored code interface
 */
public interface OracleSourceObject extends DBSObjectStateful {

    void setName(String name);

    OracleDataSource getDataSource();

    OracleSchema getSchema();

    OracleSourceType getSourceType();

    String getSourceDeclaration(DBRProgressMonitor monitor)
        throws DBException;

    void setSourceDeclaration(String source);

    IDatabasePersistAction[] getCompileActions();

}

/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Collection;

/**
 * DBSTableIndex
 */
public interface DBSProcedure extends DBSObject, DBPQualifiedObject
{
    DBSObjectContainer getContainer();

    DBSProcedureType getProcedureType();

    Collection<? extends DBSProcedureColumn> getColumns(DBRProgressMonitor monitor) throws DBException;

}
/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Collection;

/**
 * DBSIndex
 */
public interface DBSProcedure extends DBSEntityQualified
{
    DBSEntityContainer getContainer();

    DBSProcedureType getProcedureType();

    Collection<? extends DBSProcedureColumn> getColumns(DBRProgressMonitor monitor) throws DBException;

}
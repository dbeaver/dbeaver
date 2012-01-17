/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * DBSIndex
 */
public interface DBSProcedureColumn extends DBSColumnBase, DBSObject
{

    DBSProcedure getProcedure();

    DBSProcedureColumnType getColumnType();

}
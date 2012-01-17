/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * DBSIndex
 */
public interface DBSProcedureColumn extends DBSColumnBase, DBSEntityAttribute
{

    DBSProcedure getProcedure();

    DBSProcedureColumnType getColumnType();

}
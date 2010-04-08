package org.jkiss.dbeaver.model.struct;

/**
 * DBSIndex
 */
public interface DBSProcedureColumn extends DBSColumnDefinition
{

    DBSProcedure getProcedure();

    DBSProcedureColumnType getColumnType();
}
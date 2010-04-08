package org.jkiss.dbeaver.model.struct;

/**
 * DBSTableColumn
 */
public interface DBSTableColumn extends DBSColumnDefinition
{
    DBSTable getTable();

    int getOrdinalPosition();

    String getDefaultValue();
}

/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * DBSTableColumn
 */
public interface DBSTableColumn extends DBSColumnDefinition, DBSEntityAttribute
{
    DBSTable getTable();

    int getOrdinalPosition();

    String getDefaultValue();
    
    boolean isAutoIncrement();

}

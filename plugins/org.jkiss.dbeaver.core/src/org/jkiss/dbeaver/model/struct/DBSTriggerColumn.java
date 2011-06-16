/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * DBSTriggerColumn
 */
public interface DBSTriggerColumn extends DBSEntityAttribute
{
    DBSTrigger getTrigger();

    int getOrdinalPosition();

    DBSTableColumn getTableColumn();

}
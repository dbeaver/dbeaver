/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * DBSTrigger
 */
public interface DBSTrigger extends DBSObject
{
    DBSTable getTable();

    DBSActionTiming getActionTiming();

    DBSManipulationType getManipulationType();

    int getOrdinalPosition();

}
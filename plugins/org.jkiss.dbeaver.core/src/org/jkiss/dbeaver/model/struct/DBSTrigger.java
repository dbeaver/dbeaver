/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * DBSTrigger
 */
public interface DBSTrigger extends DBSEntity
{
    DBSTable getTable();

//    DBSActionTiming getActionTiming();
//
//    DBSManipulationType getManipulationType();
//
//    int getOrdinalPosition();

}
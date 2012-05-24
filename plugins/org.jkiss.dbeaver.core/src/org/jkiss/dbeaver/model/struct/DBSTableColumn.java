/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * DBSTableColumn
 */
public interface DBSTableColumn extends DBSEntityAttribute
{
    @Override
    DBSTable getParentObject();

    int getOrdinalPosition();


}

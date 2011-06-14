/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * DBSDataType
 */
public interface DBSDataType extends DBSObject
{
    int getValueType();

    DBSDataKind getDataKind();

}

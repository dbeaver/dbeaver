/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * DBSColumnDefinition
 */
public interface DBSColumnDefinition extends DBSObject, DBSColumnBase
{
    long getMaxLength();

    int getRadix();

}
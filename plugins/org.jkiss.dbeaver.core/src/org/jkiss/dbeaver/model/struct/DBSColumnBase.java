/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * DBSColumnBase
 */
public interface DBSColumnBase extends DBSTypedObject
{
    String getName();

    boolean isNullable();

}
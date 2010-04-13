/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * DBSColumnDefinition
 */
public interface DBSColumnDefinition extends DBSStructureObject, DBSTypedObject
{
    int getMaxLength();

    int getScale();

    int getPrecision();

    int getRadix();

    boolean isNullable();
}
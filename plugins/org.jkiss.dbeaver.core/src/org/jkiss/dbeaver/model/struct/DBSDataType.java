/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

/**
 * DBSDataType
 */
public interface DBSDataType
{
    int getTypeNumber();

    String getName();

    String getDescription();

    DBSDataKind getDataKind();

    boolean isUnsigned();

    boolean isSearchable();

    int getPrecision();

    int getMinScale();

    int getMaxScale();
}

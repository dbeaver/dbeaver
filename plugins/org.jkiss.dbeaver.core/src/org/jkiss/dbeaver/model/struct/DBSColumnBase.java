/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.model.DBPNamedObject;

/**
 * DBSColumnBase
 */
public interface DBSColumnBase extends DBSTypedObject, DBPNamedObject
{
    boolean isNotNull();

    long getMaxLength();
}
/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.struct;

import org.jkiss.dbeaver.model.struct.DBSSchema;

/**
 * AbstractSchema
 */
public abstract class AbstractSchema implements DBSSchema
{
    @Override
    public String toString()
    {
        return getName() + " [" + getDataSource().getContainer().getName() + "]";
    }

    public boolean isPersisted()
    {
        return true;
    }
}

/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.struct;

import org.jkiss.dbeaver.model.struct.DBSIndexColumn;

/**
 * AbstractIndexColumn
 */
public abstract class AbstractIndexColumn implements DBSIndexColumn
{

    public boolean isPersisted()
    {
        return true;
    }
}

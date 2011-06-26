/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Oracle abstract partition
 */
public abstract class OraclePartitionBase<PARENT extends DBSObject> extends OracleObject<PARENT>
{
    protected OraclePartitionBase(PARENT parent, String name, boolean persisted)
    {
        super(parent, name, persisted);
    }
}

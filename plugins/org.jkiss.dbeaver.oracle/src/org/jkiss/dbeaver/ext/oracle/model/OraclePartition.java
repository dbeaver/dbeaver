/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

/**
 * Oracle partition
 */
public abstract class OraclePartition<PARENT extends OracleObject> extends OracleObject<PARENT>
{
    protected OraclePartition(PARENT parent, String name, boolean persisted)
    {
        super(parent, name, persisted);
    }
}

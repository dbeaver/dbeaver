/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.runtime;

import org.jkiss.dbeaver.ext.IDatabaseObjectCommand;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Abstract object command
 */
public abstract class AbstractDatabaseObjectCommand<OBJECT_TYPE extends DBSObject> implements IDatabaseObjectCommand<OBJECT_TYPE> {
    public String getDescription()
    {
        return "";
    }

    public MergeResult merge(IDatabaseObjectCommand<OBJECT_TYPE> prevCommand)
    {
        return MergeResult.NONE;
    }

}

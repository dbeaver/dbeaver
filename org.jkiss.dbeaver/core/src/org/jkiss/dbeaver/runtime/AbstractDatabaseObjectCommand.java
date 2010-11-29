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
    private final String title;

    protected AbstractDatabaseObjectCommand(String title)
    {
        this.title = title;
    }

    public String getTitle()
    {
        return title;
    }

    public long getFlags()
    {
        return FLAG_NONE;
    }

    public MergeResult doMerge(IDatabaseObjectCommand<OBJECT_TYPE> prevCommand)
    {
        return MergeResult.NONE;
    }

    public void undoMerge(IDatabaseObjectCommand<OBJECT_TYPE> prevCommand)
    {
    }

}

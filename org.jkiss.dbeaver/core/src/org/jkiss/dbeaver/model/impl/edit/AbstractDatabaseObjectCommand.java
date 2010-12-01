/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.edit;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.ext.IDatabaseObjectCommand;
import org.jkiss.dbeaver.model.struct.DBSObject;

/**
 * Abstract object command
 */
public abstract class AbstractDatabaseObjectCommand<OBJECT_TYPE extends DBSObject> implements IDatabaseObjectCommand<OBJECT_TYPE> {
    private final String title;
    private final Image icon;

    protected AbstractDatabaseObjectCommand(String title, Image icon)
    {
        this.title = title;
        this.icon = icon;
    }

    protected AbstractDatabaseObjectCommand(String title)
    {
        this(title, null);
    }

    public String getTitle()
    {
        return title;
    }

    public Image getIcon()
    {
        return icon;
    }

    public long getFlags()
    {
        return FLAG_NONE;
    }

    public MergeResult merge(IDatabaseObjectCommand<OBJECT_TYPE> prevCommand)
    {
        return MergeResult.NONE;
    }

}

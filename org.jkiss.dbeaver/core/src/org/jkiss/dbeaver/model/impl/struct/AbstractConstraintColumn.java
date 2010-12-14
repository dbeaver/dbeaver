/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.struct;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.ext.ui.IObjectImageProvider;
import org.jkiss.dbeaver.model.struct.DBSConstraintColumn;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;

/**
 * Abstract constraint column
 */
public abstract class AbstractConstraintColumn implements DBSConstraintColumn, IObjectImageProvider
{
    public Image getObjectImage()
    {
        DBSTableColumn tableColumn = getTableColumn();
        if (tableColumn instanceof IObjectImageProvider) {
            return ((IObjectImageProvider)tableColumn).getObjectImage();
        }
        return null;
    }

    public String getObjectId() {
        return getConstraint().getObjectId() + "." + getName();
    }

    public boolean isPersisted()
    {
        return true;
    }
}
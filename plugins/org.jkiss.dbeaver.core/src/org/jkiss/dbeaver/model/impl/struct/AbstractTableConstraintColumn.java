/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.struct;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.ext.ui.IObjectImageProvider;
import org.jkiss.dbeaver.model.struct.DBSTableConstraintColumn;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;

/**
 * Abstract constraint column
 */
public abstract class AbstractTableConstraintColumn implements DBSTableConstraintColumn, IObjectImageProvider
{
    @Override
    public Image getObjectImage()
    {
        DBSTableColumn tableColumn = getAttribute();
        if (tableColumn instanceof IObjectImageProvider) {
            return ((IObjectImageProvider)tableColumn).getObjectImage();
        }
        return null;
    }

    @Override
    public boolean isPersisted()
    {
        return getParentObject().isPersisted();
    }
}
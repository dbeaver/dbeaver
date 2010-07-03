/*
 * Copyright (c) 2010, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.meta;

import org.jkiss.dbeaver.model.anno.Property;
import org.jkiss.dbeaver.model.struct.DBSColumnDefinition;
import org.jkiss.dbeaver.model.struct.DBSConstraintColumn;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;
import org.jkiss.dbeaver.ext.IObjectImageProvider;
import org.eclipse.swt.graphics.Image;

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
}
/*
 * Copyright (c) 2012, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.model.impl.struct;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.ext.ui.IObjectImageProvider;
import org.jkiss.dbeaver.model.struct.DBSTableColumn;
import org.jkiss.dbeaver.model.struct.DBSTriggerColumn;

/**
 * AbstractTriggerColumn
 */
public abstract class AbstractTriggerColumn implements DBSTriggerColumn, IObjectImageProvider
{

    @Override
    public boolean isPersisted()
    {
        return true;
    }

    @Override
    public Image getObjectImage()
    {
        DBSTableColumn tableColumn = getTableColumn();
        if (tableColumn instanceof IObjectImageProvider) {
            return ((IObjectImageProvider)tableColumn).getObjectImage();
        }
        return null;
    }

}

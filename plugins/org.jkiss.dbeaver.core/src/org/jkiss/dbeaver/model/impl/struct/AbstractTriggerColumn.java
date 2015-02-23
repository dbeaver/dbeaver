/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.model.impl.struct;

import org.eclipse.swt.graphics.Image;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.ext.ui.IObjectImageProvider;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableColumn;
import org.jkiss.dbeaver.model.struct.rdb.DBSTriggerColumn;

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

    @Nullable
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

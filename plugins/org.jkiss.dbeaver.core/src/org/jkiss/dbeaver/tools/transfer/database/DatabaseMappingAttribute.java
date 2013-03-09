/*
 * Copyright (C) 2010-2012 Serge Rieder
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
package org.jkiss.dbeaver.tools.transfer.database;

import org.eclipse.swt.graphics.Image;
import org.jkiss.dbeaver.ext.ui.IObjectImageProvider;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.ui.DBIcon;

/**
* DatabaseMappingAttribute
*/
class DatabaseMappingAttribute implements DatabaseMappingObject {
    DBSAttributeBase source;
    DBSEntityAttribute target;
    String targetName;
    DBSDataType targetType;
    DatabaseMappingType mappingType;

    DatabaseMappingAttribute(DBSAttributeBase source)
    {
        this.source = source;
        this.mappingType = DatabaseMappingType.unspecified;
    }

    @Override
    public Image getIcon()
    {
        if (source instanceof IObjectImageProvider) {
            return ((IObjectImageProvider) source).getObjectImage();
        }
        return DBIcon.TREE_COLUMN.getImage();
    }

    @Override
    public String getSourceName()
    {
        return DBUtils.getObjectFullName(source);
    }

    public String getTargetName()
    {
        switch (mappingType) {
            case existing: return DBUtils.getObjectFullName(target);
            case create: return targetName;
            case skip: return "[skip]";
            default: return "?";
        }
    }

    @Override
    public DatabaseMappingType getMappingType()
    {
        return mappingType;
    }

    public void setMappingType(DatabaseMappingType mappingType)
    {
        this.mappingType = mappingType;
    }
}

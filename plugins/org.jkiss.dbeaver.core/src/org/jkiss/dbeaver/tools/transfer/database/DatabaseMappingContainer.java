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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.core.DBeaverUI;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.DBRRunnableWithProgress;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.ui.DBIcon;
import org.jkiss.dbeaver.ui.UIUtils;

import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

/**
* DatabaseMappingContainer
*/
class DatabaseMappingContainer implements DatabaseMappingObject {
    private DBSDataContainer source;
    private DBSDataManipulator target;
    private String targetName;
    private DatabaseMappingType mappingType;
    private Map<DBSAttributeBase, DatabaseMappingAttribute> attributeMappings = new LinkedHashMap<DBSAttributeBase, DatabaseMappingAttribute>();

    public DatabaseMappingContainer(DBSDataContainer source)
    {
        this.source = source;
        this.mappingType = DatabaseMappingType.unspecified;
    }

    public DBSDataContainer getSource()
    {
        return source;
    }

    public DBSDataManipulator getTarget()
    {
        return target;
    }

    public void setTarget(DBSDataManipulator target)
    {
        this.target = target;
    }

    public DatabaseMappingType getMappingType()
    {
        return mappingType;
    }

    public void setMappingType(DatabaseMappingType mappingType)
    {
        this.mappingType = mappingType;
    }

    public boolean isCompleted()
    {
        for (Map.Entry<DBSAttributeBase, DatabaseMappingAttribute> attr : attributeMappings.entrySet()) {
            if (attr.getValue().mappingType == DatabaseMappingType.unspecified) {
                return false;
            }
        }
        return true;
    }

    @Override
    public Image getIcon()
    {
        return DBIcon.TREE_TABLE.getImage();
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

    public void setTargetName(String targetName)
    {
        this.targetName = targetName;
    }

    public Collection<DatabaseMappingAttribute> getAttributeMappings()
    {
        if (attributeMappings.isEmpty()) {
            try {
                readAttributes();
            } catch (InvocationTargetException e) {
                UIUtils.showErrorDialog(null, "Attributes read failed", "Can't get attributes from " + DBUtils.getObjectFullName(source), e.getTargetException());
            } catch (InterruptedException e) {
                // Skip it
            }
        }
        return attributeMappings.values();
    }

    private void readAttributes() throws InvocationTargetException, InterruptedException
    {
        DBeaverUI.runInProgressService(new DBRRunnableWithProgress() {
            @Override
            public void run(DBRProgressMonitor monitor) throws InvocationTargetException, InterruptedException
            {
                try {
                    if (source instanceof DBSEntity) {
                        for (DBSEntityAttribute attr : ((DBSEntity) source).getAttributes(monitor)) {
                            attributeMappings.put(attr, new DatabaseMappingAttribute(attr));
                        }
                    } else {
                        // Seems to be a dynamic query. Execute it to get metadata
                    }
                } catch (DBException e) {
                    throw new InvocationTargetException(e);
                }
            }
        });
    }
}

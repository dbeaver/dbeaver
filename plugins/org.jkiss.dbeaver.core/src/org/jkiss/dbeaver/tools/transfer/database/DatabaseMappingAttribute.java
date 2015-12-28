/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.tools.transfer.database;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
* DatabaseMappingAttribute
*/
class DatabaseMappingAttribute implements DatabaseMappingObject {

    public static final String TARGET_NAME_SKIP = "[skip]";
    final DatabaseMappingContainer parent;
    DBSAttributeBase source;
    DBSEntityAttribute target;
    String targetName;
    String targetType;
    DatabaseMappingType mappingType;

    DatabaseMappingAttribute(DatabaseMappingContainer parent, DBSAttributeBase source)
    {
        this.parent = parent;
        this.source = source;
        this.mappingType = DatabaseMappingType.unspecified;
    }

    public DatabaseMappingContainer getParent()
    {
        return parent;
    }

    @Override
    public DBPImage getIcon()
    {
        if (source instanceof DBPImageProvider) {
            return ((DBPImageProvider) source).getObjectImage();
        }
        return DBIcon.TREE_COLUMN;
    }

    @Override
    public DBSAttributeBase getSource()
    {
        return source;
    }

    public String getSourceType()
    {
        String typeName = source.getTypeName();
        if (source.getDataKind() == DBPDataKind.STRING) {
            typeName += "(" + source.getMaxLength() + ")";
        }
        return typeName;
    }

    @Override
    public String getTargetName()
    {
        switch (mappingType) {
            case existing: return DBUtils.getObjectFullName(target);
            case create: return targetName;
            case skip: return TARGET_NAME_SKIP;
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
        switch (mappingType) {
            case create:
                targetName = getSource().getName();
                break;
        }
    }

    void updateMappingType(DBRProgressMonitor monitor) throws DBException
    {
        switch (parent.getMappingType()) {
            case existing:
            {
                mappingType = DatabaseMappingType.unspecified;
                if (parent.getTarget() instanceof DBSEntity) {
                    if (CommonUtils.isEmpty(targetName)) {
                        targetName = source.getName();
                    }
                    target = DBUtils.findObject(
                        ((DBSEntity) parent.getTarget()).getAttributes(monitor), targetName);
                    if (target != null) {
                        mappingType = DatabaseMappingType.existing;
                    } else {
                        mappingType = DatabaseMappingType.create;
                    }
                }
                break;
            }
            case create:
                mappingType = DatabaseMappingType.create;
                if (CommonUtils.isEmpty(targetName)) {
                    targetName = source.getName();
                }
                break;
            case skip:
                mappingType = DatabaseMappingType.skip;
                break;
            default:
                mappingType = DatabaseMappingType.unspecified;
                break;
        }
    }

    @Override
    public DBSEntityAttribute getTarget()
    {
        return target;
    }

    public void setTarget(DBSEntityAttribute target)
    {
        this.target = target;
    }

    public void setTargetName(String targetName)
    {
        this.targetName = targetName;
    }

    public String getTargetType(DBPDataSource targetDataSource)
    {
        if (!CommonUtils.isEmpty(targetType)) {
            return targetType;
        }
        // TODO: make some smart data type matcher
        // Current solution looks like hack
        String typeName = source.getTypeName();
        DBPDataKind dataKind = source.getDataKind();
        if (targetDataSource instanceof DBPDataTypeProvider) {
            DBPDataTypeProvider dataTypeProvider = (DBPDataTypeProvider) targetDataSource;
            DBSDataType dataType = dataTypeProvider.getLocalDataType(typeName);
            if (dataType == null && typeName.equals("DOUBLE")) {
                dataType = dataTypeProvider.getLocalDataType("DOUBLE PRECISION");
                if (dataType != null) {
                    typeName = dataType.getTypeName();
                }
            }
            if (dataType != null && dataType.getDataKind() != dataKind) {
                // Type mismatch
                dataType = null;
            }
            if (dataType == null) {
                // Type not supported by target database
                // Let's try to find something similar
                List<DBSDataType> possibleTypes = new ArrayList<>();
                for (DBSDataType type : dataTypeProvider.getLocalDataTypes()) {
                    if (type.getDataKind() == dataKind) {
                        possibleTypes.add(type);
                    }
                }
                typeName = DBUtils.getDefaultDataTypeName(targetDataSource, dataKind);
                if (!possibleTypes.isEmpty()) {
                    DBSDataType targetType = null;
                    for (DBSDataType type : possibleTypes) {
                        if (type.getName().equalsIgnoreCase(typeName)) {
                            targetType = type;
                            break;
                        }
                    }
                    if (targetType == null) {
                        targetType = possibleTypes.get(0);
                    }
                    typeName = targetType.getTypeName();
                }
            }
            if (dataType != null) {
                dataKind = dataType.getDataKind();
            }
        }

        String modifiers = SQLUtils.getColumnTypeModifiers(source, typeName, dataKind);
        if (modifiers != null) {
            typeName += modifiers;
        }
        return typeName;
    }

    public void setTargetType(String targetType)
    {
        this.targetType = targetType;
    }
}

/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jkiss.dbeaver.tools.transfer.database;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLDataSource;
import org.jkiss.dbeaver.model.sql.SQLDialect;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.List;

/**
* DatabaseMappingAttribute
*/
public class DatabaseMappingAttribute implements DatabaseMappingObject {

    private static final Log log = Log.getLog(DatabaseMappingAttribute.class);

    public static final String TARGET_NAME_SKIP = "[skip]";

    private final DatabaseMappingContainer parent;
    private DBSAttributeBase source;
    private DBSEntityAttribute target;
    private String targetName;
    private String targetType;
    private DatabaseMappingType mappingType;

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
        return DBValueFormatting.getObjectImage(source);
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
            case existing:
                if (target != null) {
                    return DBUtils.getObjectFullName(target, DBPEvaluationContext.UI);
                } else {
                    return targetName;
                }
            case create:
                return targetName;
            case skip:
                return TARGET_NAME_SKIP;
            default:
                return "?";
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

    public void updateMappingType(DBRProgressMonitor monitor) throws DBException
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

        if (mappingType == DatabaseMappingType.create && !CommonUtils.isEmpty(targetName)) {
            // Convert target name case (#1516)
            DBSObjectContainer container = parent.getSettings().getContainer();
            if (container != null) {
                targetName = DBObjectNameCaseTransformer.transformName(container.getDataSource(), targetName);
            }
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
            if (dataType != null && !DBPDataKind.canConsume(dataKind, dataType.getDataKind())) {
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
                DBSDataType targetType = null;
                if (!possibleTypes.isEmpty()) {
                    // Try to get any partial match
                    for (DBSDataType type : possibleTypes) {
                        if (type.getName().contains(typeName) || typeName.contains(type.getName())) {
                            targetType = type;
                            break;
                        }
                    }
                }
                if (targetType == null) {
                    typeName = DBUtils.getDefaultDataTypeName(targetDataSource, dataKind);
                    if (!possibleTypes.isEmpty()) {
                        for (DBSDataType type : possibleTypes) {
                            if (type.getName().equalsIgnoreCase(typeName)) {
                                targetType = type;
                                break;
                            }
                        }
                    }
                }
                if (targetType == null && !possibleTypes.isEmpty()) {
                    targetType = possibleTypes.get(0);
                }
                if (targetType != null) {
                    typeName = targetType.getTypeName();
                }
            }
            if (dataType != null) {
                dataKind = dataType.getDataKind();
            }
        }

        // Get type modifiers from target datasource
        if (source != null && targetDataSource instanceof SQLDataSource) {
            SQLDialect dialect = ((SQLDataSource) targetDataSource).getSQLDialect();
            String modifiers = dialect.getColumnTypeModifiers(targetDataSource, source, typeName, dataKind);
            if (modifiers != null) {
                typeName += modifiers;
            }
        }

        return typeName;
    }

    public void setTargetType(String targetType)
    {
        this.targetType = targetType;
    }

    void saveSettings(IDialogSettings settings) {
        if (targetName != null) {
            settings.put("targetName", targetName);
        }
        if (targetType != null) {
            settings.put("targetType", targetType);
        }
        if (mappingType != null) {
            settings.put("mappingType", mappingType.name());
        }
    }

    public void loadSettings(IDialogSettings settings) {
        targetName = settings.get("targetName");
        targetType = settings.get("targetType");
        if (settings.get("mappingType") != null) {
            try {
                DatabaseMappingType newMappingType = DatabaseMappingType.valueOf(settings.get("mappingType"));

                if (!CommonUtils.isEmpty(targetName)) {
                    DBSDataManipulator targetEntity = parent.getTarget();
                    if (targetEntity instanceof DBSEntity) {
                        this.target = ((DBSEntity) targetEntity).getAttribute(new VoidProgressMonitor(), targetName);
                    }
                }

                if (target != null && newMappingType == DatabaseMappingType.create) {
                    // Change create to existing.
                    newMappingType = DatabaseMappingType.existing;
                } else if (target == null && newMappingType == DatabaseMappingType.existing) {
                    newMappingType = DatabaseMappingType.create;
                }

                setMappingType(newMappingType);
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

    @Override
    public String toString() {
        return DBUtils.getObjectFullName(source, DBPEvaluationContext.UI);
    }
}

/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.utils.CommonUtils;

import java.util.Map;

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
                targetName = getSourceLabelOrName(getSource());
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
                    DBSEntity targetEntity = (DBSEntity) parent.getTarget();
                    this.target = DBUtils.findObject(
                        targetEntity.getAttributes(monitor), DBUtils.getUnQuotedIdentifier(targetEntity.getDataSource(), targetName));
                    if (this.target != null) {
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
                    targetName = getSourceLabelOrName(source);
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

    private String getSourceLabelOrName(DBSAttributeBase source) {
        String name = null;
        if (source instanceof DBDAttributeBinding) {
            name = ((DBDAttributeBinding) source).getLabel();
        }
        if (CommonUtils.isEmpty(name)) {
            name = source.getName();
        }
        DBSObjectContainer container = parent.getSettings().getContainer();

        if (container != null && !DBUtils.isQuotedIdentifier(container.getDataSource(), name)) {
            name = DBObjectNameCaseTransformer.transformName(container.getDataSource(), name);
        }

        return container == null ? name : DBUtils.getQuotedIdentifier(container.getDataSource(), name);
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

        return DBStructUtils.mapTargetDataType(targetDataSource, source);
    }

    public void setTargetType(String targetType)
    {
        this.targetType = targetType;
    }

    void saveSettings(Map<String, Object> settings) {
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

    public void loadSettings(Map<String, Object> settings) {
        targetName = CommonUtils.toString(settings.get("targetName"));
        targetType = CommonUtils.toString(settings.get("targetType"));
        if (settings.get("mappingType") != null) {
            try {
                DatabaseMappingType newMappingType = DatabaseMappingType.valueOf((String) settings.get("mappingType"));

                if (!CommonUtils.isEmpty(targetName)) {
                    DBSDataManipulator targetEntity = parent.getTarget();
                    if (targetEntity instanceof DBSEntity) {
                        this.target = ((DBSEntity) targetEntity).getAttribute(new VoidProgressMonitor(),
                            DBUtils.getUnQuotedIdentifier(((DBSEntity)targetEntity).getDataSource(), targetName));
                    }
                }

                if (target != null && newMappingType == DatabaseMappingType.create) {
                    // Change create to existing.
                    newMappingType = DatabaseMappingType.existing;
                } else if (target == null && newMappingType == DatabaseMappingType.existing) {
                    newMappingType = DatabaseMappingType.create;
                }

                mappingType = newMappingType;
            } catch (Exception e) {
                log.error(e);
            }
        }
    }

    @Override
    public String toString() {
        return source.getName();
    }
}

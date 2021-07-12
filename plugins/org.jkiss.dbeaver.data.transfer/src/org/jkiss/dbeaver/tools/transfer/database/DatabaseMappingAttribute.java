/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLConstants;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferAttributeTransformerDescriptor;
import org.jkiss.dbeaver.tools.transfer.registry.DataTransferRegistry;
import org.jkiss.dbeaver.tools.transfer.stream.StreamDataImporterColumnInfo;
import org.jkiss.utils.CommonUtils;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
* DatabaseMappingAttribute
*/
public class DatabaseMappingAttribute implements DatabaseMappingObject {

    private static final Log log = Log.getLog(DatabaseMappingAttribute.class);

    public static final String TARGET_NAME_SKIP = "[skip]";

    private final DatabaseMappingContainer parent;
    private final DBSAttributeBase source;
    @Nullable
    private DBSEntityAttribute target;
    private String targetName;
    private String targetType;
    private DatabaseMappingType mappingType;
    private DataTransferAttributeTransformerDescriptor transformer;
    private final Map<String, Object> transformerProperties = new LinkedHashMap<>();

    DatabaseMappingAttribute(DatabaseMappingContainer parent, DBSAttributeBase source)
    {
        this.parent = parent;
        this.source = source;
        this.mappingType = DatabaseMappingType.unspecified;
    }

    DatabaseMappingAttribute(DatabaseMappingAttribute attribute, DatabaseMappingContainer parent) {
        this.parent = parent;
        this.source = attribute.source;
        this.target = attribute.target;
        this.targetName = attribute.targetName;
        this.targetType = attribute.targetType;
        this.mappingType = attribute.mappingType;
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
                    List<? extends DBSEntityAttribute> targetAttributes = targetEntity.getAttributes(monitor);
                    if (targetAttributes != null) {
                        target = CommonUtils.findBestCaseAwareMatch(targetAttributes, targetName, DBSEntityAttribute::getName);
                    } else {
                        target = null;
                    }

                    if (source instanceof StreamDataImporterColumnInfo && targetAttributes != null) {
                        StreamDataImporterColumnInfo source = (StreamDataImporterColumnInfo) this.source;

                        if (!source.isMappingMetadataPresent()) {
                            List<DBSEntityAttribute> suitableTargetAttributes = targetAttributes
                                .stream()
                                .filter(attr -> !DBUtils.isPseudoAttribute(attr) && !DBUtils.isHiddenObject(attr))
                                .sorted(Comparator.comparing(DBSEntityAttribute::getOrdinalPosition))
                                .collect(Collectors.toList());

                            if (source.getOrdinalPosition() < suitableTargetAttributes.size()) {
                                DBSEntityAttribute targetAttribute = suitableTargetAttributes.get(source.getOrdinalPosition());
                                targetName = targetAttribute.getName();
                                target = CommonUtils.findBestCaseAwareMatch(targetAttributes, targetName, DBSEntityAttribute::getName);
                            }
                        }

                        if (target != null) {
                            source.setTypeName(target.getTypeName());
                            source.setMaxLength(target.getMaxLength());
                            source.setDataKind(target.getDataKind());
                        }
                    }
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

    public String getTargetType(DBPDataSource targetDataSource, boolean addModifiers)
    {
        if (!CommonUtils.isEmpty(targetType)) {
            return targetType;
        }
        String dataType = DBStructUtils.mapTargetDataType(targetDataSource, source, addModifiers);
        if (!SQLConstants.KEYWORD_NULL.equals(dataType)) {
            return dataType;
        }
        // This is a MySQL-specific workaround. In case of a statement like
        // SELECT NULL 'column_name',
        // column_name's type is NULL (at least that's what JDBC driver returns).
        // Here we substitute this 'data type' to varchar.
        // See https://github.com/dbeaver/dbeaver/issues/11946.
        if (addModifiers) {
            return "varchar(100)";
        }
        return "varchar";
    }

    public void setTargetType(String targetType)
    {
        this.targetType = targetType;
    }

    public DataTransferAttributeTransformerDescriptor getTransformer() {
        return transformer;
    }

    public void setTransformer(DataTransferAttributeTransformerDescriptor transformer) {
        this.transformer = transformer;
    }

    public Map<String, Object> getTransformerProperties() {
        synchronized (transformerProperties) {
            return new LinkedHashMap<>(transformerProperties);
        }
    }

    public void setTransformerProperties(Map<String, Object> properties) {
        synchronized (transformerProperties) {
            transformerProperties.clear();
            transformerProperties.putAll(properties);
        }
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

            if (transformer != null) {
                settings.put("transformer", transformer.getId());
                settings.put("transformerProperties", new LinkedHashMap<>(transformerProperties));
            }
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
        Object transformerId = settings.get("transformer");
        if (transformerId != null) {
            transformer = DataTransferRegistry.getInstance().getAttributeTransformer(transformerId.toString());
            if (transformer == null) {
                log.error("Can't find attribute transformer " + transformerId);
            } else {
                Map<String, Object> tp = (Map<String, Object>) settings.get("transformerProperties");
                transformerProperties.clear();
                if (tp != null) {
                    transformerProperties.putAll(tp);
                }
            }
        }
    }

    @Override
    public String toString() {
        return source.getName();
    }
}

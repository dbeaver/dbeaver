/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.tools.transfer.DTConstants;
import org.jkiss.dbeaver.tools.transfer.internal.DTActivator;
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
    @Nullable
    private final DBSAttributeBase source;
    @Nullable
    private DBSEntityAttribute target;
    private String targetName;
    private String targetType;
    private DatabaseMappingType mappingType;
    private DataTransferAttributeTransformerDescriptor transformer;
    private final Map<String, Object> transformerProperties = new LinkedHashMap<>();

    DatabaseMappingAttribute(DatabaseMappingContainer parent, @NotNull DBSAttributeBase source) {
        this.parent = parent;
        this.source = source;
        this.mappingType = DatabaseMappingType.unspecified;
    }

    DatabaseMappingAttribute(
        @NotNull DatabaseMappingContainer parent,
        @Nullable DBSAttributeBase source,
        @Nullable DBSEntityAttribute target,
        @NotNull DatabaseMappingType mappingType)
    {
        this.parent = parent;
        this.source = source;
        this.target = target;
        this.mappingType = mappingType;
    }

    DatabaseMappingAttribute(@NotNull DatabaseMappingAttribute attribute, @NotNull DatabaseMappingContainer parent) {
        this.parent = parent;
        this.source = attribute.source;
        this.target = attribute.target;
        this.targetName = attribute.targetName;
        this.targetType = attribute.targetType;
        this.mappingType = attribute.mappingType;
    }

    public DatabaseMappingContainer getParent() {
        return parent;
    }

    @Override
    public DBPImage getIcon() {
        return DBValueFormatting.getObjectImage(source);
    }

    @Nullable
    @Override
    public DBSAttributeBase getSource() {
        return source;
    }

    public String getSourceType() {
        if (source == null) {
            return null;
        }
        String typeName = source.getTypeName();
        DBSDataContainer container = parent.getSource();
        if (container != null && container.getDataSource() != null) {
            String typeModifiers = container.getDataSource().getSQLDialect().getColumnTypeModifiers(
                container.getDataSource(),
                source,
                typeName,
                source.getDataKind());
            if (typeModifiers != null) {
                typeName += typeModifiers;
            }
        }
        return typeName;
    }

    @Override
    public String getTargetName() {
        switch (mappingType) {
            case existing:
            case recreate:
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
    public DatabaseMappingType getMappingType() {
        return mappingType;
    }

    public void setMappingType(DatabaseMappingType mappingType) {
        this.mappingType = mappingType;
        switch (mappingType) {
            case create:
                targetName = getSourceLabelOrName(getSource(), true, false);
                break;
        }
    }

    public void updateMappingType(DBRProgressMonitor monitor, boolean forceRefresh, boolean updateAttributesNames) throws DBException {
        if (mappingType == DatabaseMappingType.skip) {
            // We already have mapping for the attribute with the skip type
            return;
        }
        if (parent.getMappingType() == DatabaseMappingType.skip) {
            mappingType = DatabaseMappingType.skip;
            return;
        }

        mappingType = DatabaseMappingType.unspecified;
        if (parent.getTarget() instanceof DBSEntity) {
            if (forceRefresh || CommonUtils.isEmpty(targetName)) {
                targetName = getSourceLabelOrName(source, false, updateAttributesNames);
            }
            DBSEntity targetEntity = (DBSEntity) parent.getTarget();
            List<? extends DBSEntityAttribute> targetAttributes = targetEntity.getAttributes(monitor);
            if (CommonUtils.isEmpty(targetAttributes) && targetEntity instanceof DBPRefreshableObject) {
                // Reload table attributes cache. It can be empty after table deleting
                ((DBPRefreshableObject) targetEntity).refreshObject(monitor);
                targetAttributes = targetEntity.getAttributes(monitor);
            }
            if (targetAttributes != null) {
                target = CommonUtils.findBestCaseAwareMatch(
                    targetAttributes,
                    DBUtils.getUnQuotedIdentifier(targetEntity.getDataSource(), targetName),
                    DBSEntityAttribute::getName
                );
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
                        target = CommonUtils.findBestCaseAwareMatch(
                            targetAttributes,
                            DBUtils.getUnQuotedIdentifier(targetEntity.getDataSource(), targetName),
                            DBSEntityAttribute::getName
                        );
                        if (target != null && !targetAttribute.getName().equalsIgnoreCase(target.getName())) {
                            // In case of violated order (some columns are missing in the source, for example), if it turned out to find a suitable column by name
                            targetName = target.getName();
                        } else {
                            targetName = targetAttribute.getName();
                        }
                    }
                }
               // Looks like now it useless code and autodetection types happens early. Origin ticket dbeaver#10034
               // if (target != null) {
               //     source.setTypeName(target.getTypeName());
               //     source.setMaxLength(target.getMaxLength());
               //     source.setDataKind(target.getDataKind());
               // }
            }
            if (this.target != null) {
                if (parent.getMappingType() == DatabaseMappingType.recreate) {
                    mappingType = DatabaseMappingType.create;
                } else {
                    mappingType = DatabaseMappingType.existing;
                }
            } else {
                mappingType = DatabaseMappingType.create;
            }
        } else {
            // Case recreate container mapping in the new table or just create
            mappingType = DatabaseMappingType.create;
            if (forceRefresh || CommonUtils.isEmpty(targetName)) {
                if (!updateAttributesNames && CommonUtils.isNotEmpty(targetName)) {
                    // We want to keep targetName in this case. It can be the targetName from a task as example
                    targetName = getSourceLabelOrName(targetName, false, false);
                } else {
                    targetName = getSourceLabelOrName(source, true, updateAttributesNames);
                }
            }
        }

        if (mappingType == DatabaseMappingType.create && !CommonUtils.isEmpty(targetName)) {
            // Convert target name case (#1516)
            DBSObjectContainer container = parent.getSettings().getContainer();
            if (container != null && container.getDataSource() != null) {
                DBPDataSource targetDataSource = container.getDataSource();
                targetName = DatabaseTransferUtils.getTransformedName(
                    targetDataSource,
                    targetName,
                    DBUtils.isQuotedIdentifier(targetDataSource, targetName) || isSkipNameTransformation());
            }
        } else if (mappingType == DatabaseMappingType.unspecified && source != null && targetName != null) {
            String sourceLabelOrName = getSourceLabelOrName(source, false, updateAttributesNames);
            if (sourceLabelOrName != null && sourceLabelOrName.equalsIgnoreCase(targetName) && !sourceLabelOrName.equals(targetName)) {
                // Here we change the target name if we switched from target container with identifier case X to container with identifier case Y
                // See https://github.com/dbeaver/dbeaver/issues/13236
                targetName = sourceLabelOrName;
            }
        }
    }

    String getSourceLabelOrName(DBSAttributeBase source, boolean addSpecialTransformation, boolean updateAttributesNames) {
        return getSourceLabelOrName(getSourceAttributeName(source), addSpecialTransformation, updateAttributesNames);
    }

    private String getSourceLabelOrName(String name, boolean addSpecialTransformation, boolean updateAttributesNames) {
        DBSObjectContainer container = parent.getSettings().getContainer();

        if (container != null && container.getDataSource() != null) {
            if (addSpecialTransformation) {
                name = DatabaseTransferUtils.getTransformedName(
                    container.getDataSource(),
                    name,
                    DBUtils.isQuotedIdentifier(container.getDataSource(), name) || isSkipNameTransformation() || !updateAttributesNames);
            } else if (!DBUtils.isQuotedIdentifier(container.getDataSource(), name) && !isSkipNameTransformation()) {
                name = DBObjectNameCaseTransformer.transformName(container.getDataSource(), name);
            }
        }

        return name;
    }

    @NotNull
    private String getSourceAttributeName(@NotNull DBSAttributeBase source) {
        String name = null;
        if (source instanceof DBDAttributeBinding) {
            name = ((DBDAttributeBinding) source).getLabel();
        }
        if (CommonUtils.isEmpty(name)) {
            name = source.getName();
        }
        return name;
    }

    private boolean isSkipNameTransformation() {
        boolean isSkipNameTransformation = false;
        if (source instanceof DBSObject) {
            DBPDataSource sourceDataSource = ((DBSObject) source).getDataSource();
            String sourceAttributeName = getSourceAttributeName(source);
            if (sourceDataSource != null && sourceDataSource.getSQLDialect() != null
                && CommonUtils.isNotEmpty(sourceAttributeName)) {
                isSkipNameTransformation = sourceDataSource.getSQLDialect().mustBeQuoted(sourceAttributeName, true);
            }
        }
        return isSkipNameTransformation;
    }

    @Nullable
    @Override
    public DBSEntityAttribute getTarget() {
        return target;
    }

    public void setTarget(@Nullable DBSEntityAttribute target) {
        this.target = target;
    }

    public void setTargetName(String targetName) {
        this.targetName = targetName;
    }

    public String getTargetType(DBPDataSource targetDataSource, boolean addModifiers) {
        if (!CommonUtils.isEmpty(targetType)) {
            return targetType;
        }

        changeDataTypeLength(targetDataSource);
        return DBStructUtils.mapTargetDataType(targetDataSource, source, addModifiers);
    }

    private void changeDataTypeLength(@NotNull DBPDataSource targetDataSource) {
        if (source instanceof DBSTypedObjectExt2) {
            DBPPreferenceStore preferenceStore = targetDataSource.getContainer().getPreferenceStore();
            DBPPreferenceStore store = DTActivator.getDefault().getPreferences();
            if (preferenceStore.contains(DTConstants.PREF_MAX_TYPE_LENGTH) || store.contains(DTConstants.PREF_MAX_TYPE_LENGTH)) {
                int maxDataTypeLength = preferenceStore.contains(DTConstants.PREF_MAX_TYPE_LENGTH) ?
                    preferenceStore.getInt(DTConstants.PREF_MAX_TYPE_LENGTH) : store.getInt(DTConstants.PREF_MAX_TYPE_LENGTH);
                DBSTypedObjectExt2 sourceExt = (DBSTypedObjectExt2) source;
                if (source.getDataKind() == DBPDataKind.NUMERIC &&
                    source.getPrecision() != null && source.getPrecision() > maxDataTypeLength
                ) {
                    sourceExt.setPrecision(maxDataTypeLength);
                } else if (source.getMaxLength() > maxDataTypeLength) {
                    sourceExt.setMaxLength(maxDataTypeLength);
                }
            }
        }
    }

    public void setTargetType(String targetType) {
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
                settings.put("transformerPropertiesNames", String.join(",", transformerProperties.keySet()));
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
                        DBSEntity dbsEntity = (DBSEntity) targetEntity;
                        if (dbsEntity.getDataSource() != null) {
                            this.target = CommonUtils.findBestCaseAwareMatch(
                                CommonUtils.safeCollection(dbsEntity.getAttributes(new VoidProgressMonitor())),
                                DBUtils.getUnQuotedIdentifier(dbsEntity.getDataSource(), targetName),
                                DBSEntityAttribute::getName);
                        }
                    }
                }

                if (target != null && newMappingType == DatabaseMappingType.create && parent.getMappingType() != DatabaseMappingType.recreate) {
                    // Change create to existing. Do not change mapping type for the recreate type
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
                String[] tpNames = CommonUtils.toString(settings.get("transformerPropertiesNames"), "").split(",");
                transformerProperties.clear();
                if (tp != null) {
                    for (String name : tpNames) {
                        if (!CommonUtils.isEmpty(name)) {
                            transformerProperties.put(name, tp.get(name));
                        }
                    }
                }
            }
        }
    }

    @Override
    public String toString() {
        return source != null ? source.getName() : getTargetName();
    }
}

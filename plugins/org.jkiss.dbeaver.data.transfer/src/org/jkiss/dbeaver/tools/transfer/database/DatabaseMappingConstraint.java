/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.DBIcon;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPImage;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * Table constraint mapping for metadata migration.
 */
public class DatabaseMappingConstraint implements DatabaseMappingObject {
    @NotNull
    private final DatabaseMappingContainer parent;
    @NotNull
    private final DBSEntityConstraint source;
    @Nullable
    private DBSEntityConstraint target;
    @Nullable
    private DBSEntityConstraint referencedConstraint;
    @NotNull
    private final DBSEntityConstraintType constraintType;
    @NotNull
    private final List<DatabaseMappingConstraintAttribute> attributeMappings = new ArrayList<>();
    @NotNull
    private DatabaseMappingType mappingType;
    @Nullable
    private String targetName;

    DatabaseMappingConstraint(
        @NotNull DatabaseMappingContainer parent,
        @NotNull DBSEntityConstraint source
    ) {
        this.parent = parent;
        this.source = source;
        this.constraintType = source.getConstraintType();
        this.targetName = source.getName();
        this.mappingType = DatabaseMappingType.create;
    }

    DatabaseMappingConstraint(
        @NotNull DatabaseMappingConstraint constraint,
        @NotNull DatabaseMappingContainer parent
    ) {
        this.parent = parent;
        this.source = constraint.source;
        this.target = constraint.target;
        this.referencedConstraint = constraint.referencedConstraint;
        this.constraintType = constraint.constraintType;
        this.mappingType = constraint.mappingType;
        this.targetName = constraint.targetName;
        for (DatabaseMappingConstraintAttribute attribute : constraint.attributeMappings) {
            if (attribute.getTargetAttributeMapping().getSource() != null) {
                DatabaseMappingAttribute targetAttributeMapping = parent.getAttributeMapping(attribute.getTargetAttributeMapping()
                    .getSource());
                if (targetAttributeMapping != null) {
                    this.attributeMappings.add(new DatabaseMappingConstraintAttribute(
                        attribute.getSourceReference(),
                        targetAttributeMapping
                    ));
                }
            }
        }
    }

    @NotNull
    public DatabaseMappingContainer getParent() {
        return parent;
    }

    @NotNull
    @Override
    public DBPImage getIcon() {
        if (constraintType == DBSEntityConstraintType.PRIMARY_KEY || constraintType == DBSEntityConstraintType.UNIQUE_KEY) {
            return DBIcon.TREE_UNIQUE_KEY;
        }
        if (constraintType == DBSEntityConstraintType.FOREIGN_KEY) {
            return DBIcon.TREE_FOREIGN_KEY;
        }
        return DBIcon.TREE_CONSTRAINT;
    }

    @NotNull
    @Override
    public DBSEntityConstraint getSource() {
        return source;
    }

    @Nullable
    @Override
    public DBSObject getTarget() {
        return target;
    }

    public void setTarget(@Nullable DBSEntityConstraint target) {
        this.target = target;
    }

    @Nullable
    public DBSEntityConstraint getReferencedConstraint() {
        return referencedConstraint;
    }

    void setReferencedConstraint(@Nullable DBSEntityConstraint referencedConstraint) {
        this.referencedConstraint = referencedConstraint;
    }

    @NotNull
    public DBSEntityConstraintType getConstraintType() {
        return constraintType;
    }

    @NotNull
    @Override
    public DatabaseMappingType getMappingType() {
        return mappingType;
    }

    public void setMappingType(@NotNull DatabaseMappingType mappingType) {
        this.mappingType = mappingType;
        if (mappingType == DatabaseMappingType.create) {
            target = null;
        }
    }

    @NotNull
    @Override
    public String getTargetName() {
        return switch (mappingType) {
            case existing -> target == null ? CommonUtils.notEmpty(targetName) : DBUtils.getObjectFullName(target, DBPEvaluationContext.UI);
            case skip -> DatabaseMappingAttribute.TARGET_NAME_SKIP;
            default -> targetName == null ? source.getName() : targetName;
        };
    }

    public void setTargetName(@Nullable String targetName) {
        this.targetName = targetName;
    }

    void applySettings(@NotNull DatabaseMappingConstraint constraint) {
        if (constraint.mappingType == DatabaseMappingType.skip ||
            mappingType != DatabaseMappingType.existing ||
            constraint.mappingType == DatabaseMappingType.existing) {
            this.mappingType = constraint.mappingType;
            this.target = constraint.target;
        }
        this.targetName = constraint.targetName;
        this.referencedConstraint = constraint.referencedConstraint;
    }

    void saveSettings(@NotNull Map<String, Object> settings) {
        settings.put("mappingType", mappingType.name());
        if (CommonUtils.isNotEmpty(targetName)) {
            settings.put("targetName", targetName);
        } else if (target != null) {
            settings.put("targetName", target.getName());
        }
    }

    void loadSettings(@NotNull Map<String, Object> settings) {
        targetName = CommonUtils.toString(settings.get("targetName"), targetName);
        if (settings.get("mappingType") instanceof String mappingTypeName) {
            try {
                mappingType = DatabaseMappingType.valueOf(mappingTypeName);
            } catch (IllegalArgumentException ignored) {
            }
        }
    }

    @NotNull
    public List<DatabaseMappingConstraintAttribute> getAttributeMappings() {
        return Collections.unmodifiableList(attributeMappings);
    }

    void addAttributeMapping(@NotNull DatabaseMappingConstraintAttribute attributeMapping) {
        attributeMappings.add(attributeMapping);
    }

    // Constraint mapping resolution logic (moved from DatabaseMappingConstraintResolver)

    static void refreshConstraintMappings(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DatabaseMappingContainer containerMapping,
        @NotNull List<DatabaseMappingConstraint> constraintMappings
    ) throws DBException {
        Map<String, DatabaseMappingConstraint> previousConstraintMappings = new HashMap<>();
        for (DatabaseMappingConstraint previousConstraintMapping : constraintMappings) {
            previousConstraintMappings.put(previousConstraintMapping.getSource().getName(), previousConstraintMapping);
        }
        constraintMappings.clear();

        if (containerMapping.getMappingType() == DatabaseMappingType.skip ||
            !(containerMapping.getSource() instanceof DBSEntity sourceEntity)) {
            return;
        }

        containerMapping.getAttributeMappings(monitor);
        Set<String> mappedConstraintNames = new HashSet<>();

        for (DBSEntityConstraint sourceConstraint : CommonUtils.safeCollection(sourceEntity.getConstraints(monitor))) {
            DatabaseMappingConstraint newConstraintMapping = createConstraintMapping(monitor, containerMapping, sourceConstraint);
            DatabaseMappingConstraint previousConstraintMapping = previousConstraintMappings.get(sourceConstraint.getName());
            if (previousConstraintMapping != null &&
                (newConstraintMapping.getMappingType() != DatabaseMappingType.skip ||
                    previousConstraintMapping.getMappingType() == DatabaseMappingType.skip)) {
                newConstraintMapping.applySettings(previousConstraintMapping);
            }
            constraintMappings.add(newConstraintMapping);
            mappedConstraintNames.add(sourceConstraint.getName());
        }

        for (DBSEntityAssociation sourceAssociation : CommonUtils.safeCollection(sourceEntity.getAssociations(monitor))) {
            if (!mappedConstraintNames.add(sourceAssociation.getName())) {
                continue;
            }
            boolean associationInTransferScope = DatabaseTransferUtils.canResolveForeignKeyReferencedPrimaryKey(
                monitor,
                containerMapping.getSettings(),
                containerMapping,
                sourceAssociation
            );
            DatabaseMappingConstraint newConstraintMapping = createConstraintMapping(monitor, containerMapping, sourceAssociation);
            if (!associationInTransferScope) {
                newConstraintMapping.setMappingType(DatabaseMappingType.skip);
            }
            DatabaseMappingConstraint previousConstraintMapping = previousConstraintMappings.get(sourceAssociation.getName());
            if (previousConstraintMapping != null &&
                (newConstraintMapping.getMappingType() != DatabaseMappingType.skip ||
                    previousConstraintMapping.getMappingType() == DatabaseMappingType.skip)) {
                newConstraintMapping.applySettings(previousConstraintMapping);
            }
            constraintMappings.add(newConstraintMapping);
        }
    }

    @NotNull
    private static DatabaseMappingConstraint createConstraintMapping(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DatabaseMappingContainer containerMapping,
        @NotNull DBSEntityConstraint sourceConstraint
    ) throws DBException {
        DatabaseMappingConstraint constraintMapping = new DatabaseMappingConstraint(containerMapping, sourceConstraint);
        if (sourceConstraint instanceof DBSEntityAssociation association) {
            constraintMapping.setReferencedConstraint(association.getReferencedConstraint());
        }
        if (sourceConstraint instanceof DBSEntityReferrer referrer) {
            for (DBSEntityAttributeRef attributeRef : CommonUtils.safeCollection(referrer.getAttributeReferences(monitor))) {
                DBSEntityAttribute sourceAttribute = attributeRef.getAttribute();
                if (sourceAttribute == null) {
                    constraintMapping.setMappingType(DatabaseMappingType.skip);
                    return constraintMapping;
                }
                DatabaseMappingAttribute attributeMapping = containerMapping.getAttributeMapping(sourceAttribute);
                if (attributeMapping == null || attributeMapping.getMappingType() == DatabaseMappingType.skip) {
                    constraintMapping.setMappingType(DatabaseMappingType.skip);
                    return constraintMapping;
                }
                constraintMapping.addAttributeMapping(new DatabaseMappingConstraintAttribute(attributeRef, attributeMapping));
            }
        }
        constraintMapping.updateTargetConstraintMappingType(monitor);
        return constraintMapping;
    }

    private void updateTargetConstraintMappingType(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (mappingType == DatabaseMappingType.skip ||
            parent.getMappingType() != DatabaseMappingType.existing ||
            !(parent.getTarget() instanceof DBSEntity targetEntity) ||
            !(source instanceof DBSEntityReferrer)) {
            return;
        }

        List<DBSEntityAttribute> targetAttributes = new ArrayList<>(attributeMappings.size());
        for (DatabaseMappingConstraintAttribute attributeMapping : attributeMappings) {
            DBSEntityAttribute targetAttribute = attributeMapping.getTargetAttributeMapping().getTarget();
            if (targetAttribute == null) {
                return;
            }
            targetAttributes.add(targetAttribute);
        }

        if (constraintType == DBSEntityConstraintType.FOREIGN_KEY) {
            if (!(source instanceof DBSEntityAssociation sourceAssociation) ||
                !DatabaseTransferUtils.canResolveForeignKeyReferencedPrimaryKey(
                    monitor,
                    parent.getSettings(),
                    parent,
                    sourceAssociation)) {
                setMappingType(DatabaseMappingType.skip);
                return;
            }
            updateExistingForeignKeyMappingType(monitor, targetAttributes, targetEntity);
            return;
        }

        for (DBSEntityConstraint targetConstraint : CommonUtils.safeCollection(targetEntity.getConstraints(monitor))) {
            if (targetConstraint.getConstraintType() == constraintType &&
                targetConstraint instanceof DBSEntityReferrer targetReferrer &&
                isSameConstraintAttributes(monitor, targetReferrer, targetAttributes)) {
                setTargetName(targetConstraint.getName());
                setTarget(targetConstraint);
                setMappingType(DatabaseMappingType.existing);
                return;
            }
        }
    }

    @Nullable
    private DBSEntity getReferencedTargetEntity(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSEntity referencedSourceEntity
    ) throws DBException {
        if (CommonUtils.equalObjects(referencedSourceEntity, parent.getSource())) {
            return parent.getTarget() instanceof DBSEntity targetEntity &&
                parent.getMappingType().isValid() ? targetEntity : null;
        }
        for (DatabaseMappingContainer mapping : parent.getSettings().getDataMappings().values()) {
            if (CommonUtils.equalObjects(mapping.getSource(), referencedSourceEntity) &&
                mapping.getMappingType().isValid() &&
                mapping.getMappingType() != DatabaseMappingType.skip &&
                mapping.getTarget() instanceof DBSEntity targetEntity) {
                return targetEntity;
            }
        }
        return DatabaseTransferUtils.findTargetEntityBySourceName(monitor, parent.getSettings(), referencedSourceEntity);
    }

    private void updateExistingForeignKeyMappingType(
        @NotNull DBRProgressMonitor monitor,
        @NotNull List<DBSEntityAttribute> targetAttributes,
        @NotNull DBSEntity targetEntity
    ) throws DBException {
        if (!(source instanceof DBSEntityAssociation sourceAssociation)) {
            return;
        }

        DBSEntityConstraint sourceReferencedConstraint = sourceAssociation.getReferencedConstraint();
        DBSEntity sourceReferencedEntity = sourceReferencedConstraint == null
            ? sourceAssociation.getAssociatedEntity()
            : sourceReferencedConstraint.getParentObject();
        if (sourceReferencedEntity == null) {
            return;
        }

        DBSEntity referencedTargetEntity = getReferencedTargetEntity(monitor, sourceReferencedEntity);
        if (referencedTargetEntity == null) {
            return;
        }

        List<DBSEntityAttribute> referencedTargetAttributes = getReferencedTargetAttributes(
            monitor,
            sourceReferencedConstraint,
            sourceReferencedEntity,
            referencedTargetEntity
        );
        for (DBSEntityAssociation targetAssociation : CommonUtils.safeCollection(targetEntity.getAssociations(monitor))) {
            DBSEntityConstraint referencedConstraint1 = targetAssociation.getReferencedConstraint();
            DBSEntity targetReferencedEntity = referencedConstraint1 == null ? targetAssociation.getAssociatedEntity()
                : referencedConstraint1.getParentObject();
            if (targetAssociation.getConstraintType() != DBSEntityConstraintType.FOREIGN_KEY ||
                !(targetAssociation instanceof DBSEntityReferrer targetReferrer) ||
                !isSameConstraintAttributes(monitor, targetReferrer, targetAttributes) ||
                !CommonUtils.equalObjects(targetReferencedEntity, referencedTargetEntity)) {
                continue;
            }
            if (!referencedTargetAttributes.isEmpty()) {
                DBSEntityConstraint referencedConstraint = targetAssociation.getReferencedConstraint();
                if (!(referencedConstraint instanceof DBSEntityReferrer referrer &&
                    isSameConstraintAttributes(monitor, referrer, referencedTargetAttributes))) {
                    continue;
                }
            }
            setTargetName(targetAssociation.getName());
            setTarget(targetAssociation);
            setMappingType(DatabaseMappingType.existing);
            return;
        }
    }

    @NotNull
    private List<DBSEntityAttribute> getReferencedTargetAttributes(
        @NotNull DBRProgressMonitor monitor,
        @Nullable DBSEntityConstraint sourceReferencedConstraint,
        @NotNull DBSEntity sourceReferencedEntity,
        @NotNull DBSEntity referencedTargetEntity
    ) throws DBException {
        if (!(sourceReferencedConstraint instanceof DBSEntityReferrer sourceReferencedReferrer)) {
            return Collections.emptyList();
        }

        List<DBSEntityAttribute> result = new ArrayList<>();
        DatabaseMappingContainer referencedMapping = CommonUtils.equalObjects(sourceReferencedEntity, parent.getSource())
            ? parent
            : null;
        if (referencedMapping == null) {
            for (DatabaseMappingContainer mapping : parent.getSettings().getDataMappings().values()) {
                if (CommonUtils.equalObjects(mapping.getSource(), sourceReferencedEntity)) {
                    referencedMapping = mapping;
                    break;
                }
            }
        }

        for (DBSEntityAttribute sourceAttribute : DBUtils.getEntityAttributes(monitor, sourceReferencedReferrer)) {
            DBSEntityAttribute targetAttribute;
            if (referencedMapping != null && referencedMapping.getTarget() instanceof DBSEntity) {
                DatabaseMappingAttribute attributeMapping = referencedMapping.getAttributeMapping(sourceAttribute);
                targetAttribute = attributeMapping == null ? null : attributeMapping.getTarget();
            } else {
                targetAttribute = DatabaseTransferUtils.findTargetAttributeBySourceName(
                    monitor,
                    referencedTargetEntity,
                    sourceAttribute
                );
            }
            if (targetAttribute == null) {
                return Collections.emptyList();
            }
            result.add(targetAttribute);
        }
        return result;
    }

    static boolean isSameConstraintAttributes(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSEntityReferrer constraint,
        @NotNull List<DBSEntityAttribute> expectedAttributes
    ) throws DBException {
        List<? extends DBSEntityAttributeRef> actualReferences = CommonUtils.safeList(constraint.getAttributeReferences(monitor));
        if (actualReferences.size() != expectedAttributes.size()) {
            return false;
        }
        for (int i = 0; i < actualReferences.size(); i++) {
            DBSEntityAttribute actualAttribute = actualReferences.get(i).getAttribute();
            DBSEntityAttribute expectedAttribute = expectedAttributes.get(i);
            if (actualAttribute == null || !actualAttribute.getName().equalsIgnoreCase(expectedAttribute.getName())) {
                return false;
            }
        }
        return true;
    }
}

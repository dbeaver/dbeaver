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
package org.jkiss.dbeaver.model.virtual;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.*;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDAttributeValue;
import org.jkiss.dbeaver.model.data.DBDLabelValuePair;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.preferences.DBPPreferenceStore;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * Virtual entity descriptor
 */
public class DBVEntity extends DBVObject implements DBSEntity, DBPQualifiedObject, DBSDictionary, DBPAdaptable {

    public static final String[] DEFAULT_DESCRIPTION_COLUMN_PATTERNS = {
        "title",
        "name",
        "label",
        "display",
        "displayname",
        "description",
        "comment",
        "remark",
        "information",
        "email",
    };
    private static final int MIN_DESC_COLUMN_LENGTH = 4;
    private static final int MAX_DESC_COLUMN_LENGTH = 1000;

    @NotNull
    private final DBVContainer container;
    @NotNull
    private String name;
    private String description;
    private String descriptionColumnNames;

    private List<DBVEntityConstraint> entityConstraints;
    private List<DBVEntityForeignKey> entityForeignKeys;
    private List<DBVEntityAttribute> entityAttributes;
    private List<DBVColorOverride> colorOverrides;

    public DBVEntity(@NotNull DBVContainer container, @NotNull String name, String descriptionColumnNames) {
        this.container = container;
        this.name = name;
        this.descriptionColumnNames = descriptionColumnNames;
    }

    // Copy constructor
    public DBVEntity(@NotNull DBVContainer container, @NotNull DBVEntity copy, @NotNull DBVModel targetModel) {
        this.container = container;
        copyFrom(copy, targetModel);
    }

    public synchronized void dispose() {
        if (entityForeignKeys != null) {
            for (DBVEntityForeignKey  fk : entityForeignKeys) {
                fk.dispose();
            }
            entityForeignKeys.clear();
        }
    }

    public void copyFrom(@NotNull DBVEntity src, @NotNull DBVModel targetModel) {
        this.name = src.name;
        this.descriptionColumnNames = src.descriptionColumnNames;

        if (!CommonUtils.isEmpty(src.entityConstraints)) {
            this.entityConstraints = new ArrayList<>(src.entityConstraints.size());
            for (DBVEntityConstraint c : src.entityConstraints) {
                this.entityConstraints.add(new DBVEntityConstraint(this, c));
            }
        } else {
            this.entityConstraints = null;
        }
        if (this.entityForeignKeys != null) {
            for (DBVEntityForeignKey fk : this.entityForeignKeys) {
                fk.dispose();
            }
        }
        this.entityForeignKeys = null;
        if (!CommonUtils.isEmpty(src.entityForeignKeys)) {
            this.entityForeignKeys = new ArrayList<>(src.entityForeignKeys.size());
            for (DBVEntityForeignKey fk : src.entityForeignKeys) {
                DBVEntityForeignKey fkCopy = new DBVEntityForeignKey(this, fk, targetModel);
                if (fkCopy.getRefEntityId() == null) {
                    fkCopy.dispose();
                    log.debug("Can't copy virtual foreign key '" + fk.getName() + "' - target entity cannot be resolved");
                } else {
                    this.entityForeignKeys.add(fkCopy);
                }
            }
        }
        if (!CommonUtils.isEmpty(src.entityAttributes)) {
            this.entityAttributes = new ArrayList<>(src.entityAttributes.size());
            for (DBVEntityAttribute attribute : src.entityAttributes) {
                this.entityAttributes.add(new DBVEntityAttribute(this, null, attribute));
            }
        } else {
            this.entityAttributes = null;
        }
        if (!CommonUtils.isEmpty(src.colorOverrides)) {
            this.colorOverrides = new ArrayList<>(src.colorOverrides.size());
            for (DBVColorOverride co : src.colorOverrides) {
                this.colorOverrides.add(new DBVColorOverride(co));
            }
        } else {
            this.colorOverrides = null;
        }
        super.copyFrom(src);
    }

    DBVEntity(@NotNull DBVContainer container, @NotNull String name, @NotNull Map<String, Object> map) {
        this.container = container;
        this.name = name;
        this.descriptionColumnNames = (String) map.get("description");
        // Attributes
        for (Map.Entry<String, Map<String, Object>> attrObject : JSONUtils.getNestedObjects(map, "attributes")) {
            String attrName = attrObject.getKey();
            Map<String, Object> attrMap = attrObject.getValue();
            DBVEntityAttribute attr = new DBVEntityAttribute(this, null, attrName, attrMap);
            if (entityAttributes == null) entityAttributes = new ArrayList<>();
            entityAttributes.add(attr);
        }
        // Constraints
        for (Map.Entry<String, Map<String, Object>> consObject : JSONUtils.getNestedObjects(map, "constraints")) {
            String consName = consObject.getKey();
            Map<String, Object> consMap = consObject.getValue();
            String consType = JSONUtils.getString(consMap, "type");
            DBVEntityConstraint constraint = new DBVEntityConstraint(this, DBSEntityConstraintType.VIRTUAL_KEY, consName);
            boolean useAllColumns = JSONUtils.getBoolean(consMap, "useAllColumns");
            constraint.setUseAllColumns(useAllColumns);
            if (!useAllColumns) {
                for (String attrName : JSONUtils.deserializeStringList(consMap, "attributes")) {
                    constraint.addAttribute(attrName);
                }
            }
            if (entityConstraints == null) entityConstraints = new ArrayList<>();
            entityConstraints.add(constraint);
        }
        // Foreign keys
        for (Map<String, Object> fkObject : JSONUtils.getObjectList(map, "foreign-keys")) {
            String entityId = JSONUtils.getString(fkObject, "entity");
            if (CommonUtils.isEmpty(entityId)) {
                continue;
            }
            String refConsId = JSONUtils.getString(fkObject, "constraint");

            DBVEntityForeignKey fk = new DBVEntityForeignKey(this);
            fk.setReferencedConstraint(entityId, refConsId);

            Map<String, Object> attrMap = JSONUtils.getObject(fkObject, "attributes");
            List<DBVEntityForeignKeyColumn> attrs = new ArrayList<>();
            for (Map.Entry<String, Object> attr : attrMap.entrySet()) {
                attrs.add(new DBVEntityForeignKeyColumn(fk, attr.getKey(), (String) attr.getValue()));
            }
            fk.setAttributes(attrs);

            if (entityForeignKeys == null) {
                entityForeignKeys = new ArrayList<>();
            }
            entityForeignKeys.add(fk);
        }

        // Color mappings
        for (Map<String, Object> colorObj : JSONUtils.getObjectList(map, "colors")) {
            DBVColorOverride curColor = new DBVColorOverride(
                JSONUtils.getString(colorObj, "name"),
                DBCLogicalOperator.valueOf(JSONUtils.getString(colorObj, "operator")),
                null,
                JSONUtils.getString(colorObj, "foreground"),
                JSONUtils.getString(colorObj, "background")
            );
            curColor.setRange(JSONUtils.getBoolean(colorObj, "range"));
            curColor.setSingleColumn(JSONUtils.getBoolean(colorObj, "single-column"));
            curColor.setColorForeground2(JSONUtils.getString(colorObj, "foreground2"));
            curColor.setColorBackground2(JSONUtils.getString(colorObj, "background2"));
            for (String strValue : JSONUtils.deserializeStringList(colorObj, "values")) {
                curColor.addAttributeValue(strValue);
            }
            addColorOverride(curColor);
        }
        loadPropertiesFrom(map, "properties");
    }

    @NotNull
    public DBVContainer getContainer() {
        return container;
    }

    @Nullable
    public DBSEntity getRealEntity(@NotNull DBRProgressMonitor monitor) throws DBException {
        DBSObjectContainer realContainer = container.getRealContainer(monitor);
        if (realContainer == null) {
            return null;
        }
        DBSObject realObject = realContainer.getChild(monitor, name);
        if (realObject instanceof DBSEntity) {
            return (DBSEntity) realObject;
        }
        log.warn("Entity '" + name + "' not found in '" + realContainer.getName() + "'");
        return null;
    }

    @NotNull
    @Override
    public String getName() {
        return name;
    }

    @Nullable
    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public DBVContainer getParentObject() {
        return container;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource() {
        return container.getDataSource();
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @NotNull
    @Override
    public DBSEntityType getEntityType() {
        return DBSEntityType.VIRTUAL_ENTITY;
    }

    public List<DBVEntityAttribute> getEntityAttributes() {
        return entityAttributes;
    }

    public List<DBVEntityAttribute> getCustomAttributes() {
        if (!CommonUtils.isEmpty(entityAttributes)) {
            List<DBVEntityAttribute> result = null;
            for (DBVEntityAttribute attr : entityAttributes) {
                if (attr.isCustom()) {
                    if (result == null) result = new ArrayList<>();
                    result.add(attr);
                }
            }
            if (result != null) {
                return result;
            }
        }
        return Collections.emptyList();
    }

    public DBVEntityAttribute getVirtualAttribute(String name) {
        if (!CommonUtils.isEmpty(entityAttributes)) {
            for (DBVEntityAttribute attr : entityAttributes) {
                if (CommonUtils.equalObjects(name, attr.getName())) {
                    return attr;
                }
            }
        }
        return null;
    }

    @NotNull
    @Override
    public List<? extends DBSEntityAttribute> getAttributes(@NotNull DBRProgressMonitor monitor) throws DBException {
        DBSEntity realEntity = getRealEntity(monitor);
        if (realEntity != null) {
            final List<? extends DBSEntityAttribute> realAttributes = realEntity.getAttributes(monitor);
            if (!CommonUtils.isEmpty(realAttributes)) {
                List<DBVEntityAttribute> customAttributes = getCustomAttributes();
                if (!CommonUtils.isEmpty(customAttributes)) {
                    List<DBSEntityAttribute> allAttrs = new ArrayList<>();
                    allAttrs.addAll(realAttributes);
                    allAttrs.addAll(customAttributes);
                    return allAttrs;
                }
                return realAttributes;
            }
        }
        return getCustomAttributes();
    }

    @Nullable
    @Override
    public DBSEntityAttribute getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName) {
        try {
            return DBUtils.findObject(getAttributes(monitor), attributeName);
        } catch (DBException e) {
            log.error("Can't obtain real entity's attributes", e);
            return null;
        }
    }

    @Nullable
    public DBVEntityAttribute getVirtualAttribute(DBDAttributeBinding binding, boolean create) {
        if (entityAttributes != null || create) {
            if (entityAttributes == null) {
                entityAttributes = new ArrayList<>();
            }
            DBSObject[] path = DBUtils.getObjectPath(binding, true);
            DBVEntityAttribute topAttribute = DBUtils.findObject(entityAttributes, path[0].getName());
            if (topAttribute == null && create) {
                topAttribute = new DBVEntityAttribute(this, null, path[0].getName());
                entityAttributes.add(topAttribute);
            }
            if (topAttribute != null) {
                for (int i = 1; i < path.length; i++) {
                    DBVEntityAttribute nextAttribute = topAttribute.getChild(path[i].getName());
                    if (nextAttribute == null) {
                        if (create) {
                            nextAttribute = new DBVEntityAttribute(this, topAttribute, path[i].getName());
                            topAttribute.addChild(nextAttribute);
                        } else {
                            log.debug("Can't find nested attribute '" + binding + "' in '" + topAttribute.getName());
                            return null;
                        }
                    }
                    topAttribute = nextAttribute;
                }
            }

            return topAttribute;
        }
        return null;
    }

    public void addVirtualAttribute(DBVEntityAttribute attribute) {
        addVirtualAttribute(attribute, true);
    }

    void addVirtualAttribute(DBVEntityAttribute attribute, boolean reflect) {
        if (entityAttributes == null) {
            entityAttributes = new ArrayList<>();
        }
        entityAttributes.add(attribute);
        if (reflect) {
            DBUtils.fireObjectUpdate(this);
        }
    }

    public void removeVirtualAttribute(DBVEntityAttribute attribute) {
        entityAttributes.remove(attribute);
        DBUtils.fireObjectUpdate(this, attribute);
    }

    @Nullable
    @Override
    public Collection<? extends DBVEntityConstraint> getConstraints(@NotNull DBRProgressMonitor monitor) throws DBException {
        return entityConstraints;
    }

    @NotNull
    public List<DBVEntityConstraint> getConstraints() {
        return entityConstraints == null ? Collections.emptyList() : entityConstraints;
    }

    public DBVEntityConstraint getBestIdentifier() {
        if (entityConstraints == null) {
            entityConstraints = new ArrayList<>();
        }
        if (entityConstraints.isEmpty()) {
            entityConstraints.add(new DBVEntityConstraint(
                this,
                DBSEntityConstraintType.VIRTUAL_KEY,
                "VIRTUAL_PK"));
        }
        for (DBVEntityConstraint constraint : entityConstraints) {
            if (constraint.getConstraintType().isUnique() && !CommonUtils.isEmpty(constraint.getAttributes())) {
                return constraint;
            }
        }
        return entityConstraints.get(0);
    }

    public void addConstraint(DBVEntityConstraint constraint) {
        addConstraint(constraint, true);
    }

    public void addConstraint(DBVEntityConstraint constraint, boolean reflect) {
        if (entityConstraints == null) {
            entityConstraints = new ArrayList<>();
        }
        entityConstraints.add(constraint);

        if (reflect) {
            DBUtils.fireObjectUpdate(this, constraint);
        }
    }

    public void removeConstraint(DBVEntityConstraint constraint) {
        if (entityConstraints != null) {
            entityConstraints.remove(constraint);
            DBUtils.fireObjectUpdate(this, constraint);
        }
    }

    @Nullable
    @Override
    public synchronized List<DBVEntityForeignKey> getAssociations(@NotNull DBRProgressMonitor monitor) throws DBException {
        // Bind logical foreign keys
        if (entityForeignKeys != null) {
            if (monitor != null) {
                for (DBVEntityForeignKey fk : entityForeignKeys) {
                    fk.getRealReferenceConstraint(monitor);
                }
            }
        }
        return entityForeignKeys;
    }

    @NotNull
    public synchronized List<DBVEntityForeignKey> getForeignKeys() {
        return entityForeignKeys != null ? entityForeignKeys : Collections.emptyList();
    }

    public synchronized void addForeignKey(@NotNull DBVEntityForeignKey foreignKey) {
        if (entityForeignKeys == null) {
            entityForeignKeys = new ArrayList<>();
        }
        entityForeignKeys.add(foreignKey);
        DBUtils.fireObjectUpdate(this, foreignKey);
    }

    public synchronized void removeForeignKey(@NotNull DBVEntityForeignKey foreignKey) {
        if (entityForeignKeys != null) {
            entityForeignKeys.remove(foreignKey);
            DBUtils.fireObjectUpdate(this, foreignKey);
            foreignKey.dispose();
        }
    }

    @Nullable
    @Override
    public Collection<? extends DBSEntityAssociation> getReferences(@NotNull DBRProgressMonitor monitor) throws DBException {
        return null;
    }

    public String getDescriptionColumnNames() {
        return descriptionColumnNames;
    }

    public void setDescriptionColumnNames(String descriptionColumnNames) {
        this.descriptionColumnNames = descriptionColumnNames;
    }

    public Collection<DBSEntityAttribute> getDescriptionColumns(DBRProgressMonitor monitor, DBSEntity entity)
        throws DBException {
        return getDescriptionColumns(monitor, entity, descriptionColumnNames);
    }

    @NotNull
    public <T extends DBSAttributeBase> Collection<T> getDescriptionColumns(@NotNull Collection<? extends T> attributes) {
        return getDescriptionColumns(attributes, descriptionColumnNames);
    }

    public static Collection<DBSEntityAttribute> getDescriptionColumns(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSEntity entity,
        @NotNull String descColumns
    ) throws DBException {
        return getDescriptionColumns(entity.getAttributes(monitor), descColumns);
    }

    @NotNull
    public static <T extends DBSAttributeBase> Collection<T> getDescriptionColumns(
        @Nullable Collection<? extends T> attributes,
        @NotNull String descColumns
    ) {
        if (CommonUtils.isEmpty(descColumns) || CommonUtils.isEmpty(attributes)) {
            return Collections.emptyList();
        }
        List<T> result = new ArrayList<>();
        StringTokenizer st = new StringTokenizer(descColumns, ",");
        while (st.hasMoreTokens()) {
            String colName = st.nextToken();
            for (T attr : attributes) {
                if (matchesName(attr, colName)) {
                    result.add(attr);
                    break;
                }
            }
        }
        return result;
    }

    private static boolean matchesName(@NotNull DBSAttributeBase attribute, @NotNull String name) {
        if (attribute instanceof DBSObject) {
            final DBPDataSource dataSource = ((DBSObject) attribute).getDataSource();
            if (dataSource != null) {
                name = DBUtils.getUnQuotedIdentifier(dataSource, name);
            }
        }

        return attribute.getName().equalsIgnoreCase(name);
    }

    public static String getDefaultDescriptionColumn(DBRProgressMonitor monitor, DBSEntityAttribute keyColumn) throws DBException {

        Collection<? extends DBSEntityAttribute> allColumns = keyColumn.getParentObject().getAttributes(monitor);
        if (allColumns == null || allColumns.isEmpty()) {
            return null;
        }
        if (allColumns.size() == 1) {
            return DBUtils.getQuotedIdentifier(keyColumn);
        }
        // Find all string columns
        Map<String, DBSEntityAttribute> stringColumns = new TreeMap<>();
        for (DBSEntityAttribute column : allColumns) {
            if (column != keyColumn &&
                column.getDataKind() == DBPDataKind.STRING &&
                (column.getMaxLength() <= 0 ||
                    (column.getMaxLength() < MAX_DESC_COLUMN_LENGTH &&
                    column.getMaxLength() >= MIN_DESC_COLUMN_LENGTH))) {
                stringColumns.put(column.getName(), column);
            }
        }
        if (stringColumns.isEmpty()) {
            return DBUtils.getQuotedIdentifier(keyColumn);
        }
        if (stringColumns.size() > 1) {
            // Make some tests
            for (String pattern : getDescriptionColumnPatterns(keyColumn.getDataSource().getContainer().getPreferenceStore())) {
                for (String columnName : stringColumns.keySet()) {
                    if (columnName.toLowerCase(Locale.ENGLISH).contains(pattern)) {
                        return DBUtils.getQuotedIdentifier(stringColumns.get(columnName));
                    }
                }
            }
        }
        // No columns match pattern
        return DBUtils.getQuotedIdentifier(stringColumns.values().iterator().next());
    }

    @NotNull
    public static List<String> getDescriptionColumnPatterns(@NotNull DBPPreferenceStore store) {
        return CommonUtils.splitString(store.getString(ModelPreferences.RESULT_REFERENCE_DESCRIPTION_COLUMN_PATTERNS), '|');
    }

    @NotNull
    public List<DBVColorOverride> getColorOverrides() {
        return colorOverrides == null ? Collections.emptyList() : colorOverrides;
    }

    public List<DBVColorOverride> getColorOverrides(String attrName) {
        List<DBVColorOverride> result = new ArrayList<>();
        if (colorOverrides != null) {
            for (DBVColorOverride co : colorOverrides) {
                if (CommonUtils.equalObjects(attrName, co.getAttributeName())) {
                    result.add(co);
                }
            }
        }
        return result;
    }

    public void setColorOverrides(List<DBVColorOverride> colorOverrides) {
        this.colorOverrides = colorOverrides;
    }

    public void setColorOverride(DBDAttributeBinding attribute, Object value, String foreground, String background) {
        final String attrName = attribute.getName();
        final DBVColorOverride co = new DBVColorOverride(
            attrName,
            DBCLogicalOperator.EQUALS,
            new Object[]{value},
            foreground,
            background);

        if (colorOverrides == null) {
            colorOverrides = new ArrayList<>();
        } else {
            colorOverrides.removeIf(c -> c.matches(attrName, DBCLogicalOperator.EQUALS, co.getAttributeValues()));
        }
        colorOverrides.add(co);
    }

    public void addColorOverride(DBVColorOverride color) {
        if (colorOverrides == null) {
            colorOverrides = new ArrayList<>();
        }
        colorOverrides.add(color);
    }

    public void removeColorOverride(DBDAttributeBinding attribute) {
        if (colorOverrides == null) {
            return;
        }
        final String attrName = attribute.getName();
        colorOverrides.removeIf(c -> c.getAttributeName().equals(attrName));
    }

    public void removeColorOverride(DBVColorOverride co) {
        if (colorOverrides == null) {
            return;
        }
        colorOverrides.remove(co);
    }

    public void removeAllColorOverride() {
        if (colorOverrides == null) {
            return;
        }
        colorOverrides.clear();
    }

    @Override
    public boolean hasValuableData() {
        if (!CommonUtils.isEmpty(descriptionColumnNames) ||
            !CommonUtils.isEmpty(getProperties()) ||
            !CommonUtils.isEmpty(entityForeignKeys) ||
            !CommonUtils.isEmpty(colorOverrides))
        {
            return true;
        }
        if (!CommonUtils.isEmpty(entityConstraints)) {
            for (DBVEntityConstraint c : entityConstraints) {
                if (c.hasAttributes()) {
                    return true;
                }
            }
        }
        if (!CommonUtils.isEmpty(entityAttributes)) {
            for (DBVEntityAttribute attr : entityAttributes) {
                if (attr.hasValuableData()) {
                    return true;
                }
            }
        }
        return false;
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context) {
        return DBUtils.getFullQualifiedName(getDataSource(),
            container instanceof DBVModel ? null : container,
            this);
    }

    @Override
    public String toString() {
        return name;
    }

    public void bindEntity(DBRProgressMonitor monitor) throws DBException {
        if (!CommonUtils.isEmpty(entityForeignKeys)) {
            for (DBVEntityForeignKey fk : entityForeignKeys) {
                fk.getRealReferenceConstraint(monitor);
            }
        }
    }

    @Override
    public <T> T getAdapter(Class<T> adapter) {
        return null;
    }

    @Override
    public boolean supportsDictionaryEnumeration() {
        return true;
    }

    @NotNull
    @Override
    public DBSDictionaryAccessor getDictionaryAccessor(
        @NotNull DBRProgressMonitor monitor,
        List<DBDAttributeValue> precedingKeys,
        @NotNull DBSEntityAttribute keyColumn,
        boolean sortAsc,
        boolean sortByDesc
    ) throws DBException {
        final DBSEntity realEntity = getRealEntity(monitor);
        if (realEntity instanceof DBSDictionary) {
            return ((DBSDictionary) realEntity).getDictionaryAccessor(
                monitor,
                    precedingKeys,
                keyColumn,
                sortAsc,
                sortByDesc
            );
        } else {
            return emptyDictionaryAccessor;
        }
    }

    private static final DBSDictionaryAccessor emptyDictionaryAccessor = new DBSDictionaryAccessor() {

        @NotNull
        @Override
        public DBRProgressMonitor getProgressMonitor() {
            return new VoidProgressMonitor();
        }

        @Override
        public boolean isKeyComparable() {
            return false;
        }
        
        @NotNull
        @Override
        public List<DBDLabelValuePair> getValueEntry(@NotNull Object keyValue) throws DBException {
            return Collections.emptyList();
        }
        
        @NotNull
        public List<DBDLabelValuePair> getValues(long offset, long maxResults) {
            return Collections.emptyList();
        }

        @NotNull
        public List<DBDLabelValuePair> getSimilarValues(
            @NotNull Object pattern,
            boolean caseInsensitive,
            boolean byDesc,
            long offset,
            long maxResults
        ) {
            return Collections.emptyList();
        }

        @NotNull
        @Override
        public List<DBDLabelValuePair> getValuesNear(
            @NotNull Object value,
            boolean isPreceeding,
            long offset,
            long maxResults
        ) throws DBException {
            return Collections.emptyList();
        }
        
        @NotNull
        @Override
        public List<DBDLabelValuePair> getSimilarValuesNear(
            @NotNull Object pattern, boolean caseInsensitive, boolean byDesc, 
            Object value, boolean isPreceeding, 
            long offset, long maxResults
        ) throws DBException {
            return Collections.emptyList();
        }

        @Override
        public void close() throws Exception {
            // do nothing
        }

        @NotNull
        @Override
        public List<DBDLabelValuePair> getValues(long offset, int pageSize) throws DBException {
            return Collections.emptyList();
        }
    };

    @NotNull
    @Override
    public List<DBDLabelValuePair> getDictionaryEnumeration(
        @NotNull DBRProgressMonitor monitor,
        @NotNull DBSEntityAttribute keyColumn,
        @Nullable Object keyPattern,
        @Nullable String searchText, @Nullable List<DBDAttributeValue> preceedingKeys,
        boolean caseInsensitiveSearch,
        boolean sortAsc,
        boolean sortByValue,
        int offset,
        int maxResults
    ) throws DBException {
        final DBSEntity realEntity = getRealEntity(monitor);
        if (realEntity instanceof DBSDictionary) {
            return ((DBSDictionary) realEntity).getDictionaryEnumeration(
                monitor,
                keyColumn,
                keyPattern,
                searchText,
                preceedingKeys,
                caseInsensitiveSearch,
                sortAsc,
                sortByValue,
                offset,
                maxResults
            );
        } else {
            return Collections.emptyList();
        }
    }

    @NotNull
    @Override
    public List<DBDLabelValuePair> getDictionaryValues(@NotNull DBRProgressMonitor monitor, @NotNull List<DBSEntityAttribute> keyColumns, @NotNull List<Object[]> keyValues, @Nullable List<DBDAttributeValue[]> preceedingKeys, boolean sortByValue, boolean sortAsc, boolean omitNonDescriptive) throws DBException {
        DBSEntity realEntity = getRealEntity(monitor);
        return realEntity instanceof DBSDictionary dictionary ?
            dictionary.getDictionaryValues(monitor, keyColumns, keyValues, preceedingKeys, sortByValue, sortAsc, false) :
            Collections.emptyList();
    }

    public DBVModel getModel() {
        for (DBVContainer container = getContainer(); container != null; container = container.getParentObject()) {
            if (container instanceof DBVModel) {
                return (DBVModel) container;
            }
        }
        throw new IllegalStateException("Root container must be model");
    }

    void handleRename(String oldName, String newName) {
        this.name = newName;
        this.container.renameEntity(this, oldName, newName);
    }
}

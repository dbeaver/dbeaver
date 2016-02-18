/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.virtual;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * Dictionary descriptor
 */
public class DBVEntity extends DBVObject implements DBSEntity, DBPQualifiedObject {

    private static final String[] DESC_COLUMN_PATTERNS = {
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

    private final DBVContainer container;
    private String name;
    private String description;
    private String descriptionColumnNames;
    List<DBVEntityConstraint> entityConstraints;
    List<DBVEntityAttribute> entityAttributes;
    Map<String, String> properties;
    List<DBVColorOverride> colorOverrides;

    public DBVEntity(DBVContainer container, String name, String descriptionColumnNames) {
        this.container = container;
        this.name = name;
        this.descriptionColumnNames = descriptionColumnNames;
    }

    public DBVEntity(DBVContainer container, DBVEntity copy) {
        this.container = container;
        this.name = copy.name;
        this.descriptionColumnNames = copy.descriptionColumnNames;

        if (!CommonUtils.isEmpty(copy.entityConstraints)) {
            this.entityConstraints = new ArrayList<>(copy.entityConstraints.size());
            for (DBVEntityConstraint c : copy.entityConstraints) {
                this.entityConstraints.add(new DBVEntityConstraint(this, c));
            }
        }
        if (!CommonUtils.isEmpty(copy.entityAttributes)) {
            this.entityAttributes = new ArrayList<>(copy.entityAttributes.size());
            for (DBVEntityAttribute attribute : copy.entityAttributes) {
                this.entityAttributes.add(new DBVEntityAttribute(this, null, attribute));
            }
        }
        if (!CommonUtils.isEmpty(copy.properties)) {
            this.properties = new LinkedHashMap<>(copy.properties);
        }
    }

    @Nullable
    public DBSEntity getRealEntity(DBRProgressMonitor monitor) throws DBException
    {
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
    public String getDescription()
    {
        return description;
    }

    @Override
    public DBVContainer getParentObject()
    {
        return container;
    }

    @NotNull
    @Override
    public DBPDataSource getDataSource()
    {
        return container.getDataSource();
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    @Override
    public boolean isPersisted()
    {
        return true;
    }

    @NotNull
    @Override
    public DBSEntityType getEntityType()
    {
        return DBSEntityType.VIRTUAL_ENTITY;
    }

    @Nullable
    public String getProperty(String name)
    {
        return CommonUtils.isEmpty(properties) ? null : properties.get(name);
    }

    public void setProperty(String name, @Nullable String value)
    {
        if (properties == null) {
            properties = new LinkedHashMap<>();
        }
        if (value == null) {
            properties.remove(name);
        } else {
            properties.put(name, value);
        }
    }

    @NotNull
    @Override
    public Collection<? extends DBSEntityAttribute> getAttributes(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        DBSEntity realEntity = getRealEntity(monitor);
        if (realEntity != null) {
            final Collection<? extends DBSEntityAttribute> realAttributes = realEntity.getAttributes(monitor);
            if (!CommonUtils.isEmpty(realAttributes)) {
                return realAttributes;
            }
        }
        return Collections.emptyList();
/*
        // Merge with virtual attributes
        for (DBVEntityAttribute va : entityAttributes) {
            boolean found = false;
            for (int i = 0; i < attributes.size(); i++) {
                DBSEntityAttribute attr = attributes.get(i);
                if (va.getName().equals(attr.getName())) {
                    attributes.set(i, va);
                    found = true;
                    break;
                }
            }
            if (!found) {
                attributes.add(va);
            }
        }
        return attributes;
*/
    }

    @Nullable
    @Override
    public DBSEntityAttribute getAttribute(@NotNull DBRProgressMonitor monitor, @NotNull String attributeName)
    {
        try {
            return DBUtils.findObject(getAttributes(monitor), attributeName);
        } catch (DBException e) {
            log.error("Can't obtain real entity's attributes", e);
            return null;
        }
    }

    @Nullable
    public DBVEntityAttribute getVirtualAttribute(DBDAttributeBinding binding, boolean create)
    {
        if (entityAttributes != null || create) {
            if (entityAttributes == null) {
                entityAttributes = new ArrayList<>();
            }
            DBSObject[] path = DBUtils.getObjectPath(binding, true);
            DBVEntityAttribute topAttribute = DBUtils.findObject(entityAttributes, path[0].getName());
            if (topAttribute != null) {
                for (int i = 1; i < path.length; i++) {
                    topAttribute = topAttribute.getChild(path[i].getName());
                    if (topAttribute == null) {
                        log.debug("Can't find hierarchical attribute '" + binding + "'");
                        return null;
                    }
                }
            }
            if (create) {

            }

            return topAttribute;
        }
        return null;
    }

    void addVirtualAttribute(DBVEntityAttribute attribute) {
        if (entityAttributes == null) {
            entityAttributes = new ArrayList<>();
        }
        entityAttributes.add(attribute);
    }

    void resetVirtualAttribute(DBVEntityAttribute attribute) {
        entityAttributes.remove(attribute);
    }

    @Nullable
    @Override
    public Collection<? extends DBVEntityConstraint> getConstraints(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        return entityConstraints;
    }

    public DBVEntityConstraint getBestIdentifier()
    {
        if (entityConstraints == null) {
            entityConstraints = new ArrayList<>();
        }
        if (entityConstraints.isEmpty()) {
            entityConstraints.add(new DBVEntityConstraint(this, DBSEntityConstraintType.VIRTUAL_KEY, "PRIMARY"));
        }
        for (DBVEntityConstraint constraint : entityConstraints) {
            if (constraint.getConstraintType().isUnique()) {
                return constraint;
            }
        }
        return entityConstraints.get(0);
    }

    void addConstraint(DBVEntityConstraint constraint)
    {
        if (entityConstraints == null) {
            entityConstraints = new ArrayList<>();
        }
        entityConstraints.add(constraint);
    }

    @Nullable
    @Override
    public Collection<? extends DBSEntityAssociation> getAssociations(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    @Nullable
    @Override
    public Collection<? extends DBSEntityAssociation> getReferences(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    public String getDescriptionColumnNames() {
        return descriptionColumnNames;
    }

    public void setDescriptionColumnNames(String descriptionColumnNames)
    {
        this.descriptionColumnNames = descriptionColumnNames;
    }

    public Collection<DBSEntityAttribute> getDescriptionColumns(DBRProgressMonitor monitor, DBSEntity entity)
        throws DBException
    {
        if (CommonUtils.isEmpty(descriptionColumnNames)) {
            return Collections.emptyList();
        }
        List<DBSEntityAttribute> result = new ArrayList<>();
        Collection<? extends DBSEntityAttribute> attributes = entity.getAttributes(monitor);
        if (!CommonUtils.isEmpty(attributes)) {
            StringTokenizer st = new StringTokenizer(descriptionColumnNames, ",");
            while (st.hasMoreTokens()) {
                String colName = st.nextToken();
                for (DBSEntityAttribute attr : attributes) {
                    if (colName.equalsIgnoreCase(attr.getName())) {
                        result.add(attr);
                    }
                }
            }
        }
        return result;
    }

    public static String getDefaultDescriptionColumn(DBRProgressMonitor monitor, DBSEntityAttribute keyColumn)
        throws DBException
    {
        assert keyColumn.getParentObject() != null;

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
                column.getMaxLength() < MAX_DESC_COLUMN_LENGTH &&
                column.getMaxLength() >= MIN_DESC_COLUMN_LENGTH)
            {
                stringColumns.put(column.getName(), column);
            }
        }
        if (stringColumns.isEmpty()) {
            return DBUtils.getQuotedIdentifier(keyColumn);
        }
        if (stringColumns.size() > 1) {
            // Make some tests
            for (String pattern : DESC_COLUMN_PATTERNS) {
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

    public List<DBVColorOverride> getColorOverrides() {
        return colorOverrides;
    }

    public void setColorOverrides(List<DBVColorOverride> colorOverrides) {
        this.colorOverrides = colorOverrides;
    }

    public void setColorOverride(DBDAttributeBinding attribute, Object value, String foreground, String background) {
        final String attrName = attribute.getName();
        final DBVColorOverride co = new DBVColorOverride(
            attrName,
            DBCLogicalOperator.EQUALS,
            new Object[] { value },
            foreground,
            background);

        if (colorOverrides == null) {
            colorOverrides = new ArrayList<>();
        } else {
            for (Iterator<DBVColorOverride> iterator = colorOverrides.iterator(); iterator.hasNext(); ) {
                DBVColorOverride c = iterator.next();
                if (c.matches(attrName, DBCLogicalOperator.EQUALS, co.getAttributeValues())) {
                    iterator.remove();
                }
            }
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
        for (Iterator<DBVColorOverride> iterator = colorOverrides.iterator(); iterator.hasNext(); ) {
            DBVColorOverride c = iterator.next();
            if (c.getAttributeName().equals(attrName)) {
                iterator.remove();
            }
        }
    }

    @Override
    public boolean hasValuableData() {
        if (!CommonUtils.isEmpty(descriptionColumnNames) || !CommonUtils.isEmpty(properties)) {
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
            return true;
        }
        if (!CommonUtils.isEmpty(colorOverrides)) {
            return true;
        }
        return false;
    }

    @NotNull
    @Override
    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            container,
            this);
    }

    @Override
    public String toString() {
        return name;
    }

}

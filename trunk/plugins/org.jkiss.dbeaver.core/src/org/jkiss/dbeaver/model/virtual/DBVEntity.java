/*
 * Copyright (C) 2010-2015 Serge Rieder
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
package org.jkiss.dbeaver.model.virtual;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPQualifiedObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLBuilder;

import java.io.IOException;
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
        "description",
        "comment",
        "remark",
    };
    private static final int MIN_DESC_COLUMN_LENGTH = 4;
    private static final int MAX_DESC_COLUMN_LENGTH = 1000;

    private final DBVContainer container;
    private String name;
    private String description;
    private String descriptionColumnNames;
    private List<DBVEntityConstraint> entityConstraints;
    private Map<String, String> properties;

    public DBVEntity(DBVContainer container, String name, String descriptionColumnNames) {
        this.container = container;
        this.name = name;
        this.descriptionColumnNames = descriptionColumnNames;
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
            properties = new LinkedHashMap<String, String>();
        }
        if (value == null) {
            properties.remove(name);
        } else {
            properties.put(name, value);
        }
    }

    @Override
    public Collection<? extends DBSEntityAttribute> getAttributes(DBRProgressMonitor monitor) throws DBException
    {
        DBSEntity realEntity = getRealEntity(monitor);
        if (realEntity == null) {
            return Collections.emptyList();
        }
        return realEntity.getAttributes(monitor);
    }

    @Nullable
    @Override
    public DBSEntityAttribute getAttribute(DBRProgressMonitor monitor, String attributeName)
    {
        try {
            return DBUtils.findObject(getAttributes(monitor), attributeName);
        } catch (DBException e) {
            log.error("Can't obtain real entity's attributes", e);
            return null;
        }
    }

    @Nullable
    @Override
    public Collection<? extends DBVEntityConstraint> getConstraints(DBRProgressMonitor monitor) throws DBException
    {
        return entityConstraints;
    }

    public DBVEntityConstraint getBestIdentifier()
    {
        if (entityConstraints == null) {
            entityConstraints = new ArrayList<DBVEntityConstraint>();
        }
        if (entityConstraints.isEmpty()) {
            entityConstraints.add(new DBVEntityConstraint(this, DBSEntityConstraintType.VIRTUAL_KEY, "PRIMARY"));
        }
        return entityConstraints.get(0);
    }

    public void addConstraint(DBVEntityConstraint constraint)
    {
        if (entityConstraints == null) {
            entityConstraints = new ArrayList<DBVEntityConstraint>();
        }
        entityConstraints.add(constraint);
    }

    @Nullable
    @Override
    public Collection<? extends DBSEntityAssociation> getAssociations(DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

    @Nullable
    @Override
    public Collection<? extends DBSEntityAssociation> getReferences(DBRProgressMonitor monitor) throws DBException
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
        java.util.List<DBSEntityAttribute> result = new ArrayList<DBSEntityAttribute>();
        Collection<? extends DBSEntityAttribute> attributes = entity.getAttributes(monitor);
        StringTokenizer st = new StringTokenizer(descriptionColumnNames, ",");
        while (st.hasMoreTokens()) {
            String colName = st.nextToken();
            for (DBSEntityAttribute attr : attributes) {
                if (colName.equalsIgnoreCase(attr.getName())) {
                    result.add(attr);
                }
            }
        }
        return result;
    }

    public static String getDefaultDescriptionColumn(DBRProgressMonitor monitor, DBSEntityAttribute keyColumn)
        throws DBException
    {
        Collection<? extends DBSEntityAttribute> allColumns = keyColumn.getParentObject().getAttributes(monitor);
        if (allColumns.size() == 1) {
            return DBUtils.getQuotedIdentifier(keyColumn);
        }
        // Find all string columns
        Map<String, DBSEntityAttribute> stringColumns = new TreeMap<String, DBSEntityAttribute>();
        for (DBSEntityAttribute column : allColumns) {
            if (column != keyColumn &&
                column.getDataKind() == DBPDataKind.STRING &&
                column.getMaxLength() < MAX_DESC_COLUMN_LENGTH &&
                column.getMaxLength() >= MIN_DESC_COLUMN_LENGTH)
            {
                stringColumns.put(column.getName().toLowerCase(), column);
            }
        }
        if (stringColumns.isEmpty()) {
            return DBUtils.getQuotedIdentifier(keyColumn);
        }
        if (stringColumns.size() > 1) {
            // Make some tests
            for (String pattern : DESC_COLUMN_PATTERNS) {
                for (String columnName : stringColumns.keySet()) {
                    if (columnName.contains(pattern)) {
                        return DBUtils.getQuotedIdentifier(stringColumns.get(columnName));
                    }
                }
            }
        }
        // No columns match pattern
        return DBUtils.getQuotedIdentifier(stringColumns.values().iterator().next());
    }

    @Override
    public void persist(XMLBuilder xml) throws IOException
    {
        xml.startElement(RegistryConstants.TAG_ENTITY);
        xml.addAttribute(RegistryConstants.ATTR_NAME, getName());
        if (!CommonUtils.isEmpty(getDescriptionColumnNames())) {
            xml.addAttribute(RegistryConstants.ATTR_DESCRIPTION, getDescriptionColumnNames());
        }
        if (!CommonUtils.isEmpty(properties)) {
            for (Map.Entry<String, String> prop : properties.entrySet()) {
                xml.startElement(RegistryConstants.TAG_PROPERTY);
                xml.addAttribute(RegistryConstants.ATTR_NAME, prop.getKey());
                xml.addAttribute(RegistryConstants.ATTR_VALUE, prop.getValue());
                xml.endElement();
            }
        }
        for (DBVEntityConstraint c : CommonUtils.safeCollection(entityConstraints)) {
            if (c.hasAttributes()) {
                xml.startElement(RegistryConstants.TAG_CONSTRAINT);
                xml.addAttribute(RegistryConstants.ATTR_NAME, c.getName());
                xml.addAttribute(RegistryConstants.ATTR_TYPE, c.getConstraintType().getName());
                for (DBVEntityConstraintColumn cc : CommonUtils.safeCollection(c.getAttributeReferences(null))) {
                    xml.startElement(RegistryConstants.TAG_ATTRIBUTE);
                    xml.addAttribute(RegistryConstants.ATTR_NAME, cc.getAttributeName());
                    xml.endElement();
                }
                xml.endElement();
            }
        }
        xml.endElement();
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
        return false;
    }

    public void copyFrom(DBVEntity copy)
    {
        if (!CommonUtils.isEmpty(copy.entityConstraints)) {
            this.entityConstraints = new ArrayList<DBVEntityConstraint>(copy.entityConstraints.size());
            for (DBVEntityConstraint c : copy.entityConstraints) {
                DBVEntityConstraint constraint = new DBVEntityConstraint(this, c.getConstraintType(), c.getName());
                constraint.copyFrom(c);
                this.entityConstraints.add(constraint);
            }
        }
        if (!CommonUtils.isEmpty(copy.properties)) {
            this.properties = new LinkedHashMap<String, String>(copy.properties);
        }
    }

    @Override
    public String getFullQualifiedName()
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            container,
            this);
    }
}

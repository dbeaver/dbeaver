package org.jkiss.dbeaver.model.virtual;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.registry.RegistryConstants;
import org.jkiss.dbeaver.runtime.VoidProgressMonitor;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLBuilder;

import java.io.IOException;
import java.util.*;

/**
 * Dictionary descriptor
 */
public class DBVEntity extends DBVObject implements DBSEntity {

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

    public DBVEntity(DBVContainer container, String name, String descriptionColumnNames) {
        this.container = container;
        this.name = name;
        this.descriptionColumnNames = descriptionColumnNames;
    }

    public DBVEntity(DBVEntity copy)
    {
        this.container = copy.container;
        this.name = copy.name;
        this.descriptionColumnNames = copy.descriptionColumnNames;
    }

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

    public String getName() {
        return name;
    }

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

    @Override
    public Collection<? extends DBSEntityAttribute> getAttributes(DBRProgressMonitor monitor) throws DBException
    {
        DBSEntity realEntity = getRealEntity(monitor);
        if (realEntity == null) {
            return Collections.emptyList();
        }
        return realEntity.getAttributes(monitor);
    }

    public DBSEntityAttribute getAttribute(String attributeName)
    {
        // Here we use void monitor.
        // In real life entity columns SHOULD be already read so it doesn't matter
        // But I'm afraid that in some very special cases it does. Thant's too bad.
        try {
            return DBUtils.findObject(getAttributes(VoidProgressMonitor.INSTANCE), attributeName);
        } catch (DBException e) {
            log.error("Can't obtain real entity's attributes", e);
            return null;
        }
    }

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
            entityConstraints.add(new DBVEntityConstraint(this));
        }
        return entityConstraints.get(0);
    }

    @Override
    public Collection<? extends DBSEntityAssociation> getAssociations(DBRProgressMonitor monitor) throws DBException
    {
        return null;
    }

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
                JDBCUtils.getDataKind(column) == DBSDataKind.STRING &&
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

    public void persist(XMLBuilder xml) throws IOException
    {
        xml.startElement(RegistryConstants.TAG_ENTITY);
        xml.addAttribute(RegistryConstants.ATTR_NAME, getName());
        if (!CommonUtils.isEmpty(getDescriptionColumnNames())) {
            xml.addAttribute(RegistryConstants.ATTR_DESCRIPTION, getDescriptionColumnNames());
        }
        for (DBVEntityConstraint c : CommonUtils.safeCollection(entityConstraints)) {
            if (c.hasAttributes()) {
                xml.startElement(RegistryConstants.TAG_CONSTRAINT);
                for (DBVEntityConstraintColumn cc : c.getAttributeReferences(null)) {
                    xml.startElement(RegistryConstants.TAG_ATTRIBUTE);
                    xml.addAttribute(RegistryConstants.ATTR_NAME, cc.getAttributeName());
                    xml.endElement();
                }
                xml.endElement();
            }
        }
        xml.endElement();
    }

    public boolean hasValuableData() {
        if (!CommonUtils.isEmpty(descriptionColumnNames)) {
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

    void copyFrom(DBVEntity entity) {

    }

}

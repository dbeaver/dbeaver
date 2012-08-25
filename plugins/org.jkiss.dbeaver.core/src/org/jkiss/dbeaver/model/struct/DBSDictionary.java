package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * Dictionary descriptor
 */
public class DBSDictionary {

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

    private String entityReference;
    private String name;
    private String uniqueColumns;
    private String descriptionColumnNames;

    public DBSDictionary(String entityReference, String name, String descriptionColumnNames) {
        this.entityReference = entityReference;
        this.name = name;
        this.descriptionColumnNames = descriptionColumnNames;
    }

    public DBSDictionary(DBSDictionary dictionary)
    {
        this.entityReference = dictionary.entityReference;
        this.name = dictionary.name;
        this.descriptionColumnNames = dictionary.descriptionColumnNames;
    }

    public DBSDictionary(DBRProgressMonitor monitor, DBSEntityAttribute keyColumn) throws DBException
    {
        this.entityReference = DBUtils.getObjectUniqueName(keyColumn.getParentObject());
        this.name = keyColumn.getParentObject().getName();
        this.descriptionColumnNames = getDefaultDescriptionColumn(monitor, keyColumn);
    }

    public String getEntityReference()
    {
        return entityReference;
    }

    public String getName() {
        return name;
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

}

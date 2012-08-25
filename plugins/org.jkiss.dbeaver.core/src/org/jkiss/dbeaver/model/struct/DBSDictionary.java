package org.jkiss.dbeaver.model.struct;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

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
    private String descriptionColumns;

    public DBSDictionary(String entityReference, String name, String descriptionColumns) {
        this.entityReference = entityReference;
        this.name = name;
        this.descriptionColumns = descriptionColumns;
    }

    public DBSDictionary(DBSDictionary dictionary)
    {
        this.entityReference = dictionary.entityReference;
        this.name = dictionary.name;
        this.descriptionColumns = dictionary.descriptionColumns;
    }

    public DBSDictionary(DBRProgressMonitor monitor, DBSEntityAttribute keyColumn) throws DBException
    {
        this.entityReference = DBUtils.getObjectUniqueName(keyColumn.getParentObject());
        this.name = keyColumn.getParentObject().getName();
        this.descriptionColumns = getDefaultDescriptionColumn(monitor, keyColumn);
    }

    public String getEntityReference()
    {
        return entityReference;
    }

    public String getName() {
        return name;
    }

    public String getDescriptionColumns() {
        return descriptionColumns;
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

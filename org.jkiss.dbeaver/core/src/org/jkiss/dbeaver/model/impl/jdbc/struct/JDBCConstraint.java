package org.jkiss.dbeaver.model.impl.jdbc.struct;

import org.jkiss.dbeaver.model.impl.meta.AbstractConstraint;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDColumnValue;
import org.jkiss.dbeaver.model.data.DBDLabelValuePair;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.dbc.DBCStatement;
import org.jkiss.dbeaver.model.dbc.DBCExecutionContext;
import org.jkiss.dbeaver.model.dbc.DBCResultSet;
import org.jkiss.dbeaver.DBException;

import java.util.List;
import java.util.Collection;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;

/**
 * JDBC abstract constraint
 */
public abstract class JDBCConstraint<DATASOURCE extends DBPDataSource, TABLE extends DBSTable>
    extends AbstractConstraint<DATASOURCE, TABLE>
    implements DBSConstraintEnumerable
{
    private static final int MAX_DESC_COLUMN_LENGTH = 1000;

    protected JDBCConstraint(TABLE table, String name, String description, DBSConstraintType constraintType) {
        super(table, name, description, constraintType);
    }

    /**
     * Enumerations supported only for unique constraints
     * @return true for unique constraint else otherwise
     */
    public boolean supportsEnumeration() {
        return getConstraintType().isUnique();
    }

    /**
     * Returns prepared statements for enumeration fetch
     * @param monitor progress monitor
     * @param keyColumn enumeration column.
     * @param keyPattern pattern for enumeration values. If null or empty then returns full enumration set
     * @param preceedingKeys other constrain key values. May be null.
     * @param maxResults maximum enumeration values in result set
     * @return
     * @throws DBException
     */
    public Collection<DBDLabelValuePair> getKeyEnumeration(
        DBRProgressMonitor monitor,
        DBSTableColumn keyColumn,
        Object keyPattern,
        List<DBDColumnValue> preceedingKeys,
        int maxResults)
        throws DBException
    {
        if (keyColumn.getTable() != this.getTable()) {
            throw new IllegalArgumentException("Bad key column argument");
        }
        DBPDataSource dataSource = keyColumn.getDataSource();
        DBCExecutionContext context = getDataSource().openContext(monitor, "Select '" + keyColumn.getName() + "' enumeration values");
        try {
            DBDValueHandler keyValueHandler = DBUtils.getColumnValueHandler(getDataSource(), keyColumn);
            StringBuilder query = new StringBuilder();
            query.append("SELECT ").append(DBUtils.getQuotedIdentifier(dataSource, keyColumn.getName()));
            DBSTableColumn descColumn = getKeyDescriptionColumn(monitor, keyColumn);
            if (descColumn != null) {
                query.append(", ").append(DBUtils.getQuotedIdentifier(dataSource, descColumn.getName()));
            }
            query.append(" FROM ").append(keyColumn.getTable().getFullQualifiedName());
            List<String> conditions = new ArrayList<String>();
            if (keyPattern != null) {
                if (keyPattern instanceof CharSequence) {
                    if (((CharSequence)keyPattern).length() > 0) {
                        conditions.add(DBUtils.getQuotedIdentifier(dataSource, keyColumn.getName()) + " LIKE ?");
                    } else {
                        keyPattern = null;
                    }
                } else if (keyPattern instanceof Number) {
                    conditions.add(DBUtils.getQuotedIdentifier(dataSource, keyColumn.getName()) + " >= ?");
                } else {
                    // not supported
                }
            }
            if (preceedingKeys != null && !preceedingKeys.isEmpty()) {
                for (DBDColumnValue precColumn : preceedingKeys) {
                    conditions.add(DBUtils.getQuotedIdentifier(dataSource, precColumn.getColumn().getName()) + " = ?");
                }
            }
            if (!conditions.isEmpty()) {
                query.append(" WHERE");
                for (int i = 0; i < conditions.size(); i++) {
                    if (i > 0) {
                        query.append(" AND");
                    }
                    query.append(" ").append(conditions.get(i));
                }
            }
            DBCStatement dbStat = context.prepareStatement(query.toString(), false, false, false);
            try {
                int paramPos = 0;
                if (keyPattern instanceof CharSequence) {
                    // Add % for LIKE operand
                    keyPattern = keyPattern.toString() + "%";
                }
                if (keyPattern != null) {
                    keyValueHandler.bindValueObject(monitor, dbStat, keyColumn, paramPos++, keyPattern);
                }

                if (preceedingKeys != null && !preceedingKeys.isEmpty()) {
                    for (DBDColumnValue precColumn : preceedingKeys) {
                        DBDValueHandler precValueHandler = DBUtils.getColumnValueHandler(dataSource, precColumn.getColumn());
                        precValueHandler.bindValueObject(monitor, dbStat, precColumn.getColumn(), paramPos++, precColumn.getValue());
                    }
                }
                dbStat.setLimit(0, 100);
                if (dbStat.executeStatement()) {
                    DBCResultSet dbResult = dbStat.openResultSet();
                    try {
                        List<DBDLabelValuePair> values = new ArrayList<DBDLabelValuePair>();
                        DBDValueHandler descValueHandler = null;
                        if (descColumn != null) {
                            descValueHandler = DBUtils.getColumnValueHandler(dataSource, descColumn);
                        }
                        // Extract enumeration values and (optionally) their descriptions
                        while (dbResult.nextRow()) {
                            // Check monitor
                            if (monitor.isCanceled()) {
                                break;
                            }
                            // Get value and description
                            Object keyValue = keyValueHandler.getValueObject(monitor, dbResult, keyColumn, 0);
                            if (keyValue == null) {
                                continue;
                            }
                            String keyLabel = keyValueHandler.getValueDisplayString(keyColumn, keyValue);
                            if (descValueHandler != null) {
                                Object descValue = descValueHandler.getValueObject(monitor, dbResult, descColumn, 1);
                                keyLabel = descValueHandler.getValueDisplayString(descColumn, descValue);
                            }
                            values.add(new DBDLabelValuePair(keyLabel, keyValue));
                        }
                        return values;
                    }
                    finally {
                        dbResult.close();
                    }
                } else {
                    return null;
                }
            }
            finally {
                dbStat.close();
            }
        }
        finally {
            context.close();
        }
    }

    private static final String[] DESC_COLUMN_PATTERNS = {
        "title",
        "name",
        "label",
        "display",
        "description",
        "comment",
        "remark",
    };

    private DBSTableColumn getKeyDescriptionColumn(DBRProgressMonitor monitor, DBSTableColumn keyColumn)
        throws DBException
    {
        Collection<? extends DBSTableColumn> allColumns = keyColumn.getTable().getColumns(monitor);
        if (allColumns.size() == 1) {
            return null;
        }
        // Find all string columns
        Map<String, DBSTableColumn> stringColumns = new TreeMap<String, DBSTableColumn>();
        for (DBSTableColumn column : allColumns) {
            if (column != keyColumn && JDBCUtils.getDataKind(column) == DBSDataKind.STRING && column.getMaxLength() < MAX_DESC_COLUMN_LENGTH) {
                stringColumns.put(column.getName().toLowerCase(), column);
            }
        }
        if (stringColumns.isEmpty()) {
            return null;
        }
        if (stringColumns.size() > 1) {
            // Make some tests
            for (String pattern : DESC_COLUMN_PATTERNS) {
                for (String columnName : stringColumns.keySet()) {
                    if (columnName.contains(pattern)) {
                        return stringColumns.get(columnName);
                    }
                }
            }
        }
        // No columns match pattern
        return stringColumns.values().iterator().next();
    }


}

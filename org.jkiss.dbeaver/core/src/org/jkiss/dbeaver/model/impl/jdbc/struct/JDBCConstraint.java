package org.jkiss.dbeaver.model.impl.jdbc.struct;

import org.jkiss.dbeaver.model.impl.meta.AbstractConstraint;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.DBPDataSource;
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

import net.sf.jkiss.utils.CommonUtils;

/**
 * JDBC abstract constraint
 */
public abstract class JDBCConstraint<DATASOURCE extends DBPDataSource, TABLE extends DBSTable>
    extends AbstractConstraint<DATASOURCE, TABLE>
    implements DBSConstraintEnumerable
{

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
        DBCExecutionContext context = getDataSource().openContext(monitor, "Select '" + keyColumn.getName() + "' enumeration values");
        try {
            DBDValueHandler valueHandler = DBSUtils.getColumnValueHandler(getDataSource(), keyColumn);
            String query = "SELECT " + keyColumn.getName() + " FROM " + keyColumn.getTable().getFullQualifiedName();
            if (keyPattern != null) {
                if (keyPattern instanceof CharSequence) {
                    String autoComplete = keyPattern.toString();
                    if (!CommonUtils.isEmpty(autoComplete)) {
                        autoComplete = autoComplete.replace('\'', '"');
                        query += " WHERE " + keyColumn.getName() + " LIKE '" + autoComplete + "%'";
                    }
                } else if (keyPattern instanceof Number) {
                    query += " WHERE " + keyColumn.getName() + " >= " + keyPattern;
                } else {
                    // not supported
                }
            }
            DBCStatement dbStat = context.prepareStatement(query, false, false);
            try {
                dbStat.setLimit(0, 100);
                if (dbStat.executeStatement()) {
                    DBCResultSet dbResult = dbStat.openResultSet();
                    try {
                        List<DBDLabelValuePair> values = new ArrayList<DBDLabelValuePair>();
                        while (dbResult.nextRow()) {
                            Object keyValue = valueHandler.getValueObject(dbResult, keyColumn, 0);
                            String keyLabel = valueHandler.getValueDisplayString(keyColumn, keyValue);
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

}

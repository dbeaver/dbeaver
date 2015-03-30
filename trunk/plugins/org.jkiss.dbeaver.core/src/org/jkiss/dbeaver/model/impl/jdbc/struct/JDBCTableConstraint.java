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
package org.jkiss.dbeaver.model.impl.jdbc.struct;

import org.jkiss.dbeaver.core.Log;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeValue;
import org.jkiss.dbeaver.model.data.DBDDisplayFormat;
import org.jkiss.dbeaver.model.data.DBDLabelValuePair;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableConstraint;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSConstraintEnumerable;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * JDBC abstract constraint
 */
public abstract class JDBCTableConstraint<TABLE extends JDBCTable>
    extends AbstractTableConstraint<TABLE>
    implements DBSConstraintEnumerable, DBPSaveableObject
{
    static final Log log = Log.getLog(JDBCTableConstraint.class);

    private boolean persisted;

    protected JDBCTableConstraint(TABLE table, String name, @Nullable String description, DBSEntityConstraintType constraintType, boolean persisted) {
        super(table, name, description, constraintType);
        this.persisted = persisted;
    }

    @Property(viewable = true, editable = true, valueTransformer = DBObjectNameCaseTransformer.class, order = 1)
    @Override
    public String getName()
    {
        return super.getName();
    }

    @Override
    public boolean isPersisted()
    {
        return persisted;
    }

    @Override
    public void setPersisted(boolean persisted)
    {
        this.persisted = persisted;
    }

    /**
     * Enumerations supported only for unique constraints
     * @return true for unique constraint else otherwise
     */
    @Override
    public boolean supportsEnumeration() {
        return getConstraintType().isUnique();
    }

    /**
     * Returns prepared statements for enumeration fetch
     * @param session execution context
     * @param keyColumn enumeration column.
     * @param keyPattern pattern for enumeration values. If null or empty then returns full enumration set
     * @param preceedingKeys other constrain key values. May be null.
     * @param maxResults maximum enumeration values in result set     @return
     * @throws DBException
     */
    @Override
    public Collection<DBDLabelValuePair> getKeyEnumeration(
        DBCSession session,
        DBSEntityAttribute keyColumn,
        Object keyPattern,
        List<DBDAttributeValue> preceedingKeys,
        int maxResults)
        throws DBException
    {
        if (keyColumn.getParentObject() != this.getTable()) {
            throw new IllegalArgumentException("Bad key column argument");
        }
        DBPDataSource dataSource = session.getDataSource();
        DBVEntity dictionary = dataSource.getContainer().getVirtualModel().findEntity(getTable(), false);
        if (dictionary != null && !CommonUtils.isEmpty(dictionary.getDescriptionColumnNames())) {
            // Try to use dictionary description
            try {
                return readKeyEnumeration(session, keyColumn, keyPattern, preceedingKeys, dictionary, maxResults);
            } catch (DBException e) {
                log.warn("Can't query with redefined dictionary columns (" + dictionary.getDescriptionColumnNames() + ")", e);
            }
        }
        // Use default one
        return readKeyEnumeration(
            session,
            keyColumn,
            keyPattern,
            preceedingKeys,
            null,
            maxResults);
    }

    private Collection<DBDLabelValuePair> readKeyEnumeration(
        DBCSession session,
        DBSEntityAttribute keyColumn,
        Object keyPattern,
        List<DBDAttributeValue> preceedingKeys,
        DBVEntity dictionary,
        int maxResults)
        throws DBException
    {
        DBDValueHandler keyValueHandler = DBUtils.findValueHandler(session, keyColumn);
        StringBuilder query = new StringBuilder();
        query.append("SELECT ").append(DBUtils.getQuotedIdentifier(keyColumn));
        String descColumns;
        if (dictionary != null) {
            descColumns = dictionary.getDescriptionColumnNames();
        } else {
            descColumns = DBVEntity.getDefaultDescriptionColumn(session.getProgressMonitor(), keyColumn);
        }
        if (descColumns != null) {
            query.append(", ").append(descColumns);
        }
        query.append(" FROM ").append(DBUtils.getObjectFullName(keyColumn.getParentObject()));
        List<String> conditions = new ArrayList<String>();
        if (keyPattern != null) {
            if (keyPattern instanceof CharSequence) {
                if (((CharSequence)keyPattern).length() > 0) {
                    conditions.add(DBUtils.getQuotedIdentifier(keyColumn) + " LIKE ?");
                } else {
                    keyPattern = null;
                }
            } else if (keyPattern instanceof Number) {
                conditions.add(DBUtils.getQuotedIdentifier(keyColumn) + " >= ?");
            } else {
                // not supported
            }
        }
        if (preceedingKeys != null && !preceedingKeys.isEmpty()) {
            for (DBDAttributeValue precAttribute : preceedingKeys) {
                conditions.add(DBUtils.getQuotedIdentifier(getDataSource(), precAttribute.getAttribute().getName()) + " = ?");
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
        DBCStatement dbStat = session.prepareStatement(DBCStatementType.QUERY, query.toString(), false, false, false);
        try {
            int paramPos = 0;
            if (keyPattern instanceof CharSequence) {
                // Add % for LIKE operand
                keyPattern = keyPattern.toString() + "%";
            }
            if (keyPattern != null) {
                keyValueHandler.bindValueObject(session, dbStat, keyColumn, paramPos++, keyPattern);
            }

            if (preceedingKeys != null && !preceedingKeys.isEmpty()) {
                for (DBDAttributeValue precAttribute : preceedingKeys) {
                    DBDValueHandler precValueHandler = DBUtils.findValueHandler(session, precAttribute.getAttribute());
                    precValueHandler.bindValueObject(session, dbStat, precAttribute.getAttribute(), paramPos++, precAttribute.getValue());
                }
            }
            dbStat.setLimit(0, maxResults);
            if (dbStat.executeStatement()) {
                DBCResultSet dbResult = dbStat.openResultSet();
                try {
                    List<DBDLabelValuePair> values = new ArrayList<DBDLabelValuePair>();
                    List<DBCAttributeMetaData> metaColumns = dbResult.getMeta().getAttributes();
                    List<DBDValueHandler> colHandlers = new ArrayList<DBDValueHandler>(metaColumns.size());
                    for (DBCAttributeMetaData col : metaColumns) {
                        colHandlers.add(DBUtils.findValueHandler(session, col));
                    }
                    // Extract enumeration values and (optionally) their descriptions
                    while (dbResult.nextRow()) {
                        // Check monitor
                        if (session.getProgressMonitor().isCanceled()) {
                            break;
                        }
                        // Get value and description
                        Object keyValue = keyValueHandler.fetchValueObject(session, dbResult, keyColumn, 0);
                        if (keyValue == null) {
                            continue;
                        }
                        String keyLabel = keyValueHandler.getValueDisplayString(keyColumn, keyValue, DBDDisplayFormat.NATIVE);
                        if (descColumns != null) {
                            keyLabel = "";
                            for (int i = 1; i < colHandlers.size(); i++) {
                                Object descValue = colHandlers.get(i).fetchValueObject(session, dbResult, metaColumns.get(i), i);
                                if (!keyLabel.isEmpty()) {
                                    keyLabel += " ";
                                }
                                keyLabel += colHandlers.get(i).getValueDisplayString(metaColumns.get(i), descValue, DBDDisplayFormat.NATIVE);
                            }
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

}

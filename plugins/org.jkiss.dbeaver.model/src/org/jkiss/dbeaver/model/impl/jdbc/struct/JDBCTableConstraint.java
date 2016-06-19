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
package org.jkiss.dbeaver.model.impl.jdbc.struct;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeValue;
import org.jkiss.dbeaver.model.data.DBDLabelValuePair;
import org.jkiss.dbeaver.model.data.DBDValueHandler;
import org.jkiss.dbeaver.model.exec.DBCResultSet;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.DBCStatement;
import org.jkiss.dbeaver.model.exec.DBCStatementType;
import org.jkiss.dbeaver.model.impl.DBObjectNameCaseTransformer;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableConstraint;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.struct.DBSConstraintEnumerable;
import org.jkiss.dbeaver.model.struct.DBSEntityAttribute;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.model.virtual.DBVUtils;
import org.jkiss.utils.CommonUtils;

import java.math.BigInteger;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * JDBC abstract constraint
 */
public abstract class JDBCTableConstraint<TABLE extends JDBCTable>
    extends AbstractTableConstraint<TABLE>
    implements DBSConstraintEnumerable, DBPSaveableObject
{
    private boolean persisted;

    protected JDBCTableConstraint(TABLE table, String name, @Nullable String description, DBSEntityConstraintType constraintType, boolean persisted) {
        super(table, name, description, constraintType);
        this.persisted = persisted;
    }

    @NotNull
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
        // Use default one
        return readKeyEnumeration(
            session,
            keyColumn,
            keyPattern,
            preceedingKeys,
            maxResults);
    }

    private Collection<DBDLabelValuePair> readKeyEnumeration(
        DBCSession session,
        DBSEntityAttribute keyColumn,
        Object keyPattern,
        List<DBDAttributeValue> preceedingKeys,
        int maxResults)
        throws DBException
    {
        final TABLE table = getParentObject();
        assert table != null;

        DBDValueHandler keyValueHandler = DBUtils.findValueHandler(session, keyColumn);

        if (keyPattern != null) {
            if (keyPattern instanceof CharSequence) {
                if (((CharSequence)keyPattern).length() > 0) {
                    keyPattern = "%" + keyPattern.toString() + "%";
                } else {
                    keyPattern = null;
                }
            } else if (keyPattern instanceof Number) {
                // Subtract gap value to see some values before specified
                int gapSize =  maxResults / 2;
                if (keyPattern instanceof Integer) {
                    keyPattern = (Integer) keyPattern - gapSize;
                } else if (keyPattern instanceof Short) {
                    keyPattern = (Short) keyPattern - gapSize;
                } else if (keyPattern instanceof Long) {
                    keyPattern = (Long) keyPattern - gapSize;
                } else if (keyPattern instanceof Float) {
                    keyPattern = (Float) keyPattern - gapSize;
                } else if (keyPattern instanceof Double) {
                    keyPattern = (Double) keyPattern - gapSize;
                } else if (keyPattern instanceof BigInteger) {
                    keyPattern = ((BigInteger) keyPattern).subtract(BigInteger.valueOf(gapSize));
                }
            } else {
                // not supported
                keyPattern = null;
            }
        }

        StringBuilder query = new StringBuilder();
        query.append("SELECT ").append(DBUtils.getQuotedIdentifier(keyColumn));

        String descColumns = DBVUtils.getDictionaryDescriptionColumns(session.getProgressMonitor(), keyColumn);
        Collection<DBSEntityAttribute> descAttributes = null;
        if (descColumns != null) {
            descAttributes = DBVEntity.getDescriptionColumns(session.getProgressMonitor(), table, descColumns);
            query.append(", ").append(descColumns);
        }
        query.append(" FROM ").append(DBUtils.getObjectFullName(table));
        if (!CommonUtils.isEmpty(preceedingKeys) || keyPattern != null) {
            query.append(" WHERE ");
        }
        boolean hasCond = false;
        // Preceeding keys
        if (preceedingKeys != null && !preceedingKeys.isEmpty()) {
            for (int i = 0; i < preceedingKeys.size(); i++) {
                if (hasCond) query.append(" AND ");
                query.append(DBUtils.getQuotedIdentifier(getDataSource(), preceedingKeys.get(i).getAttribute().getName())).append(" = ?");
                hasCond = true;
            }
        }
        if (keyPattern != null) {
            if (hasCond) query.append(" AND (");
            query.append(DBUtils.getQuotedIdentifier(keyColumn));
            if (keyPattern instanceof CharSequence) {
                query.append(" LIKE ?");
            } else {
                query.append(" >= ?");
            }

            // Add desc columns conditions
            if (keyPattern instanceof CharSequence && descAttributes != null) {
                for (DBSEntityAttribute descAttr : descAttributes) {
                    if (descAttr.getDataKind() == DBPDataKind.STRING) {
                        query.append(" OR ").append(DBUtils.getQuotedIdentifier(descAttr)).append(" LIKE ?");
                    }
                }
            }
            if (hasCond) query.append(")");
        }

        try (DBCStatement dbStat = session.prepareStatement(DBCStatementType.QUERY, query.toString(), false, false, false)) {
            int paramPos = 0;

            if (preceedingKeys != null && !preceedingKeys.isEmpty()) {
                for (DBDAttributeValue precAttribute : preceedingKeys) {
                    DBDValueHandler precValueHandler = DBUtils.findValueHandler(session, precAttribute.getAttribute());
                    precValueHandler.bindValueObject(session, dbStat, precAttribute.getAttribute(), paramPos++, precAttribute.getValue());
                }
            }

            if (keyPattern != null) {
                keyValueHandler.bindValueObject(session, dbStat, keyColumn, paramPos++, keyPattern);
            }

            if (keyPattern instanceof CharSequence && descAttributes != null) {
                for (DBSEntityAttribute descAttr : descAttributes) {
                    if (descAttr.getDataKind() == DBPDataKind.STRING) {
                        final DBDValueHandler valueHandler = DBUtils.findValueHandler(session, descAttr);
                        valueHandler.bindValueObject(session, dbStat, keyColumn, paramPos++, keyPattern);
                    }
                }
            }

            dbStat.setLimit(0, maxResults);
            if (dbStat.executeStatement()) {
                try (DBCResultSet dbResult = dbStat.openResultSet()) {
                    return DBVUtils.readDictionaryRows(session, keyColumn, keyValueHandler, dbResult);
                }
            } else {
                return Collections.emptyList();
            }
        }
    }

}

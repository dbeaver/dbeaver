/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.model.impl.jdbc.struct;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
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
import org.jkiss.dbeaver.model.struct.DBSEntityConstraint;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.model.virtual.DBVUtils;
import org.jkiss.utils.CommonUtils;

import java.math.BigDecimal;
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

    // Copy constructor
    protected JDBCTableConstraint(TABLE table, DBSEntityConstraint source, boolean persisted) {
        super(table, source);
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
     * @param sortByValue sort results by eky value. If false then sort by description
     * @param sortAsc sort ascending/descending
     * @param maxResults maximum enumeration values in result set     @return  @throws DBException
     */
    @Override
    public Collection<DBDLabelValuePair> getKeyEnumeration(
        DBCSession session,
        DBSEntityAttribute keyColumn,
        Object keyPattern,
        List<DBDAttributeValue> preceedingKeys,
        boolean sortByValue,
        boolean sortAsc,
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
            sortByValue,
            sortAsc,
            maxResults);
    }

    private Collection<DBDLabelValuePair> readKeyEnumeration(
        DBCSession session,
        DBSEntityAttribute keyColumn,
        Object keyPattern,
        List<DBDAttributeValue> preceedingKeys,
        boolean sortByValue,
        boolean sortAsc,
        int maxResults)
        throws DBException
    {
        final TABLE table = getParentObject();
        assert table != null;

        DBDValueHandler keyValueHandler = DBUtils.findValueHandler(session, keyColumn);

        if (keyPattern instanceof CharSequence) {
            if (((CharSequence)keyPattern).length() > 0) {
                keyPattern = "%" + keyPattern.toString() + "%";
            } else {
                keyPattern = null;
            }
        }
        boolean searchInKeys = keyPattern != null;

        if (keyPattern != null) {
            if (keyColumn.getDataKind() == DBPDataKind.NUMERIC) {
                if (keyPattern instanceof Number) {
                    // Subtract gap value to see some values before specified
                    int gapSize = maxResults / 2;
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
                    } else if (keyPattern instanceof BigDecimal) {
                        keyPattern = ((BigDecimal) keyPattern).subtract(new BigDecimal(gapSize));
                    } else {
                        searchInKeys = false;
                    }
                } else if (keyPattern instanceof String) {
                    searchInKeys = false;
                    // Ignore it
                    //keyPattern = Double.parseDouble((String) keyPattern) - gapSize;
                }
            } else if (keyPattern instanceof CharSequence && keyColumn.getDataKind() == DBPDataKind.STRING) {
                // Its ok
            } else {
                searchInKeys = false;
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
        query.append(" FROM ").append(DBUtils.getObjectFullName(table, DBPEvaluationContext.DML));

        boolean searchInDesc = keyPattern instanceof CharSequence && descAttributes != null;

        if (!CommonUtils.isEmpty(preceedingKeys) || searchInKeys || searchInDesc) {
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
            if (searchInKeys) {
                query.append(DBUtils.getQuotedIdentifier(keyColumn));
                if (keyPattern instanceof CharSequence) {
                    query.append(" LIKE ?");
                } else {
                    query.append(" >= ?");
                }
            }

            // Add desc columns conditions
            if (searchInDesc) {
                boolean hasCondition = searchInKeys;
                for (DBSEntityAttribute descAttr : descAttributes) {
                    if (descAttr.getDataKind() == DBPDataKind.STRING) {
                        if (hasCondition) {
                            query.append(" OR ");
                        }
                        query.append(DBUtils.getQuotedIdentifier(descAttr)).append(" LIKE ?");
                        hasCondition = true;
                    }
                }
            }
            if (hasCond) query.append(")");
        }
        query.append(" ORDER BY ");
        if (sortByValue) {
            query.append(DBUtils.getQuotedIdentifier(keyColumn));
        } else {
            // Sort by description
            query.append(descColumns);
        }
        if (!sortAsc) {
            query.append(" DESC");
        }

        try (DBCStatement dbStat = session.prepareStatement(DBCStatementType.QUERY, query.toString(), false, false, false)) {
            int paramPos = 0;

            if (preceedingKeys != null && !preceedingKeys.isEmpty()) {
                for (DBDAttributeValue precAttribute : preceedingKeys) {
                    DBDValueHandler precValueHandler = DBUtils.findValueHandler(session, precAttribute.getAttribute());
                    precValueHandler.bindValueObject(session, dbStat, precAttribute.getAttribute(), paramPos++, precAttribute.getValue());
                }
            }

            if (keyPattern != null && searchInKeys) {
                keyValueHandler.bindValueObject(session, dbStat, keyColumn, paramPos++, keyPattern);
            }

            if (searchInDesc) {
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

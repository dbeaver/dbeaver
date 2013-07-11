/*
 * Copyright (C) 2010-2013 Serge Rieder
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
package org.jkiss.dbeaver.model.impl.jdbc.exec;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCEntityIdentifier;
import org.jkiss.dbeaver.model.exec.DBCEntityMetaData;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.struct.rdb.DBSSchema;
import org.jkiss.dbeaver.model.struct.rdb.DBSTable;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.model.virtual.DBVEntityConstraint;
import org.jkiss.utils.CommonUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * JDBC Table MetaData
 */
public class JDBCTableMetaData implements DBCEntityMetaData {

    static final Log log = LogFactory.getLog(JDBCTableMetaData.class);

    private JDBCResultSetMetaData resultSetMetaData;
    private String catalogName;
    private String schemaName;
    private String tableName;
    private String alias;
    private List<JDBCColumnMetaData> columns = new ArrayList<JDBCColumnMetaData>();
    private List<JDBCTableIdentifier> identifiers;
    private DBSEntity table;

    public JDBCTableMetaData(JDBCResultSetMetaData resultSetMetaData, DBSEntity table, String catalogName, String schemaName, String tableName, String alias)
    {
        this.resultSetMetaData = resultSetMetaData;
        this.catalogName = catalogName;
        this.schemaName = schemaName;
        this.tableName = tableName;
        this.table = table;
        this.alias = alias;
    }

    public JDBCResultSetMetaData getResultSetMetaData()
    {
        return resultSetMetaData;
    }

    @Override
    public DBSEntity getEntity(DBRProgressMonitor monitor)
        throws DBException
    {
        if (table == null) {
            final DBPDataSource dataSource = resultSetMetaData.getResultSet().getContext().getDataSource();
            final DBSObjectContainer sc = DBUtils.getAdapter(DBSObjectContainer.class, dataSource);
            if (sc != null) {
                Class<? extends DBSObject> scChildType = sc.getChildType(monitor);
                DBSObject tableObject;
                if (!CommonUtils.isEmpty(catalogName) && scChildType != null && DBSSchema.class.isAssignableFrom(scChildType)) {
                    // Do not use catalog name
                    // Some data sources do not load catalog list but result set meta data contains one (e.g. DB2)
                    tableObject = DBUtils.getObjectByPath(monitor, sc, null, schemaName, tableName);
                } else {
                    tableObject = DBUtils.getObjectByPath(monitor, sc, catalogName, schemaName, tableName);
                }
                if (tableObject == null) {
                    throw new DBException("Table '" + tableName + "' not found in metadata catalog");
                } else if (tableObject instanceof DBSEntity) {
                    table = (DBSEntity) tableObject;
                } else {
                    throw new DBException("Unsupported table class: " + tableObject.getClass().getName());
                }
            }
        }
        return table;
    }

    public String getCatalogName()
    {
        return catalogName;
    }

    public String getSchemaName()
    {
        return schemaName;
    }

    @Override
    public String getEntityName()
    {
        return tableName;
    }

    @Override
    public String getEntityAlias()
    {
        return alias;
    }

    @Override
    public boolean isIdentified(DBRProgressMonitor monitor)
        throws DBException
    {
        return getBestIdentifier(monitor) != null;
    }

    @Override
    public DBCEntityIdentifier getBestIdentifier(DBRProgressMonitor monitor)
        throws DBException
    {
        DBSEntity table;
        try {
            table = getEntity(monitor);
        } catch (DBException e) {
            // Table not recognized
            log.debug(e);
            return null;
        }

        if (identifiers == null) {
            // Load identifiers
            identifiers = new ArrayList<JDBCTableIdentifier>();

            if (table instanceof DBSTable && ((DBSTable) table).isView()) {
                // Skip physical identifiers for views. There are nothing anyway

            } else {
                // Check constraints
                Collection<? extends DBSEntityConstraint> uniqueKeys = table.getConstraints(monitor);
                if (!CommonUtils.isEmpty(uniqueKeys)) {
                    for (DBSEntityConstraint constraint : uniqueKeys) {
                        if (constraint instanceof DBSEntityReferrer && constraint.getConstraintType().isUnique()) {
                            identifiers.add(
                                new JDBCTableIdentifier(monitor, (DBSEntityReferrer)constraint, this));
                        }
                    }
                }
                if (identifiers.isEmpty() && table instanceof DBSTable) {
                    try {
                        // Check indexes only if no unique constraints found
                        Collection<? extends DBSTableIndex> indexes = ((DBSTable)table).getIndexes(monitor);
                        if (!CommonUtils.isEmpty(indexes)) {
                            for (DBSTableIndex index : indexes) {
                                if (index.isUnique()) {
                                    identifiers.add(
                                        new JDBCTableIdentifier(monitor, index, this));
                                }
                            }
                        }
                    } catch (DBException e) {
                        // Indexes are not supported or not available
                        // Just skip them
                        log.debug(e);
                    }
                }
            }
            if (CommonUtils.isEmpty(identifiers)) {
                // No physical identifiers
                // Make new or use existing virtual identifier
                DBVEntity virtualEntity = table.getDataSource().getContainer().getVirtualModel().findEntity(table, true);
                identifiers.add(new JDBCTableIdentifier(monitor, virtualEntity.getBestIdentifier(), this));
            }
        }
        if (!CommonUtils.isEmpty(identifiers)) {
            // Find PK or unique key
            DBCEntityIdentifier uniqueId = null;
            for (DBCEntityIdentifier id : identifiers) {
                DBSEntityReferrer referrer = id.getReferrer();
                if (referrer != null && isGoodReferrer(monitor, referrer)) {
                    if (referrer.getConstraintType() == DBSEntityConstraintType.PRIMARY_KEY) {
                        return id;
                    } else if (referrer.getConstraintType().isUnique() ||
                        (referrer instanceof DBSTableIndex && ((DBSTableIndex) referrer).isUnique()))
                    {
                        uniqueId = id;
                    }
                }
            }
            return uniqueId;
        }
        return null;
    }

    private boolean isGoodReferrer(DBRProgressMonitor monitor, DBSEntityReferrer referrer) throws DBException
    {
        Collection<? extends DBSEntityAttributeRef> references = referrer.getAttributeReferences(monitor);
        if (CommonUtils.isEmpty(references)) {
            return referrer instanceof DBVEntityConstraint;
        }
        for (DBSEntityAttributeRef ref : references) {
            if (getColumnMetaData(monitor, ref.getAttribute()) == null) {
                return false;
            }
        }
        return true;
    }

    public JDBCColumnMetaData getColumnMetaData(DBRProgressMonitor monitor, DBSEntityAttribute column)
        throws DBException
    {
        for (JDBCColumnMetaData meta : columns) {
            if (meta.getAttribute(monitor) == column) {
                return meta;
            }
        }
        return null;
    }

    public List<JDBCColumnMetaData> getColumns()
    {
        return columns;
    }

    void addColumn(JDBCColumnMetaData columnMetaData)
    {
        columns.add(columnMetaData);
    }

}

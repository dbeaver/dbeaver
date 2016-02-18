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
package org.jkiss.dbeaver.ext.postgresql.model;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.postgresql.PostgreConstants;
import org.jkiss.dbeaver.ext.postgresql.PostgreUtils;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDPseudoAttribute;
import org.jkiss.dbeaver.model.data.DBDPseudoAttributeContainer;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.AbstractExecutionSource;
import org.jkiss.dbeaver.model.impl.SimpleObjectCache;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.*;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAssociation;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * PostgreTable
 */
public class PostgreTable extends PostgreTableReal implements DBDPseudoAttributeContainer
{
    private SimpleObjectCache<PostgreTable, PostgreTableForeignKey> foreignKeys = new SimpleObjectCache<>();

    private long rowCountEstimate;
    private Long rowCount;
    private boolean hasOids;
    private int tablespaceId;
    private List<PostgreTableInheritance> superTables;
    private List<PostgreTableInheritance> subTables;

    public PostgreTable(PostgreSchema catalog)
    {
        super(catalog);
    }

    public PostgreTable(
        PostgreSchema catalog,
        ResultSet dbResult)
    {
        super(catalog, dbResult);

        this.rowCountEstimate = JDBCUtils.safeGetLong(dbResult, "reltuples");
        this.hasOids = JDBCUtils.safeGetBoolean(dbResult, "relhasoids");
        this.tablespaceId = JDBCUtils.safeGetInt(dbResult, "reltablespace");
    }

    public SimpleObjectCache<PostgreTable, PostgreTableForeignKey> getForeignKeyCache() {
        return foreignKeys;
    }

    @Property(viewable = true, order = 20)
    public PostgreTablespace getTablespace(DBRProgressMonitor monitor) throws DBException {
        if (tablespaceId == 0) {
            return null;
        }
        return PostgreUtils.getObjectById(monitor, getDatabase().tablespaceCache, getDatabase(), tablespaceId);
    }

    @Property(viewable = true, order = 22)
    public long getRowCountEstimate() {
        return rowCountEstimate;
    }

    @Property(viewable = false, expensive = true, order = 23)
    public synchronized Long getRowCount(DBRProgressMonitor monitor)
    {
        if (rowCount != null) {
            return rowCount;
        }
        if (!isPersisted()) {
            // Do not count rows for views
            return null;
        }

        // Query row count
        try (DBCSession session = DBUtils.openMetaSession(monitor, getDataSource(), "Read row count")) {
            rowCount = countData(new AbstractExecutionSource(this, session.getExecutionContext(), this), session, null);
        } catch (DBException e) {
            log.debug("Can't fetch row count", e);
        }
        if (rowCount == null) {
            rowCount = -1L;
        }

        return rowCount;
    }

    @Override
    public boolean isView()
    {
        return false;
    }

    @Property(viewable = false, order = 40)
    public boolean isHasOids() {
        return hasOids;
    }

    @Override
    public Collection<? extends DBSTableIndex> getIndexes(DBRProgressMonitor monitor) throws DBException {
        return getSchema().indexCache.getObjects(monitor, getSchema(), this);
    }

    @Override
    public boolean refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException
    {
        super.refreshObject(monitor);
        getContainer().indexCache.clearObjectCache(this);
        foreignKeys.clearCache();
        superTables = null;
        subTables = null;
        rowCount = null;
        return true;
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor) throws DBException {
        return JDBCUtils.generateTableDDL(monitor, this, false);
    }

    @Override
    public void setObjectDefinitionText(String sourceText) throws DBException {
        throw new DBException("Table DDL is read-only");
    }

    @Override
    public DBDPseudoAttribute[] getPseudoAttributes() throws DBException {
        if (this.hasOids) {
            return new DBDPseudoAttribute[]{PostgreConstants.PSEUDO_ATTR_OID};
        } else {
            return null;
        }
    }

    @Association
    @Override
    public synchronized Collection<? extends DBSEntityAssociation> getAssociations(@NotNull DBRProgressMonitor monitor)
        throws DBException
    {
        final List<PostgreTableInheritance> superTables = getSuperInheritance(monitor);
        final Collection<PostgreTableForeignKey> foreignKeys = getForeignKeys(monitor);
        if (CommonUtils.isEmpty(superTables)) {
            return foreignKeys;
        } else if (CommonUtils.isEmpty(foreignKeys)) {
            return superTables;
        }
        List<DBSEntityAssociation> agg = new ArrayList<>(superTables.size() + foreignKeys.size());
        agg.addAll(superTables);
        agg.addAll(foreignKeys);
        return agg;
    }

    @Override
    public Collection<? extends DBSEntityAssociation> getReferences(@NotNull DBRProgressMonitor monitor) throws DBException {
        List<DBSEntityAssociation> refs = new ArrayList<>();
        refs.addAll(getSubInheritance(monitor));
        // This is dummy implementation
        // Get references from this schema only
        final Collection<PostgreTableForeignKey> allForeignKeys =
            getContainer().constraintCache.getTypedObjects(monitor, getContainer(), PostgreTableForeignKey.class);
        for (PostgreTableForeignKey constraint : allForeignKeys) {
            if (constraint.getAssociatedEntity() == this) {
                refs.add(constraint);
            }
        }
        return refs;
    }

    public Collection<PostgreTableForeignKey> getForeignKeys(@NotNull DBRProgressMonitor monitor) throws DBException {
        return getSchema().constraintCache.getTypedObjects(monitor, getSchema(), this, PostgreTableForeignKey.class);
    }

    @Property(viewable = false, order = 30)
    public List<PostgreTableBase> getSuperTables(DBRProgressMonitor monitor) throws DBException {
        final List<PostgreTableInheritance> si = getSuperInheritance(monitor);
        if (si.isEmpty()) {
            return Collections.emptyList();
        }
        List<PostgreTableBase> result = new ArrayList<>(si.size());
        for (int i1 = 0; i1 < si.size(); i1++) {
            result.add(si.get(i1).getAssociatedEntity());

        }
        return result;
    }

    @Property(viewable = false, order = 31)
    public List<PostgreTableBase> getSubTables(DBRProgressMonitor monitor) throws DBException {
        final List<PostgreTableInheritance> si = getSubInheritance(monitor);
        if (si.isEmpty()) {
            return Collections.emptyList();
        }
        List<PostgreTableBase> result = new ArrayList<>(si.size());
        for (int i1 = 0; i1 < si.size(); i1++) {
            result.add(si.get(i1).getParentObject());

        }
        return result;
    }

    @NotNull
    public List<PostgreTableInheritance> getSuperInheritance(DBRProgressMonitor monitor) throws DBException {
        if (superTables == null) {
            try (JDBCSession session = DBUtils.openMetaSession(monitor, getDataSource(), "Load table inheritance info")) {
                try (JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT i.*,c.relnamespace " +
                    "FROM pg_catalog.pg_inherits i,pg_catalog.pg_class c " +
                    "WHERE i.inhrelid=? AND c.oid=i.inhparent " +
                    "ORDER BY i.inhseqno")) {
                    dbStat.setInt(1, getObjectId());
                    try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                        while (dbResult.next()) {
                            final int parentSchemaId = JDBCUtils.safeGetInt(dbResult, "relnamespace");
                            final int parentTableId = JDBCUtils.safeGetInt(dbResult, "inhparent");
                            PostgreSchema schema = getDatabase().getSchema(monitor, parentSchemaId);
                            if (schema == null) {
                                log.warn("Can't find parent table's schema '" + parentSchemaId + "'");
                                continue;
                            }
                            PostgreTableBase parentTable = schema.getTable(monitor, parentTableId);
                            if (parentTable == null) {
                                log.warn("Can't find parent table '" + parentTableId + "' in '" + schema.getName() + "'");
                                continue;
                            }
                            if (superTables == null) {
                                superTables = new ArrayList<>();
                            }
                            superTables.add(
                                new PostgreTableInheritance(
                                    this,
                                    parentTable,
                                    JDBCUtils.safeGetInt(dbResult, "inhseqno"),
                                    true));
                        }
                    }
                }
            } catch (SQLException e) {
                throw new DBCException(e, getDataSource());
            }
            if (superTables == null) {
                superTables = Collections.emptyList();
            }
        }
        return superTables;
    }

    @NotNull
    public List<PostgreTableInheritance> getSubInheritance(@NotNull DBRProgressMonitor monitor) throws DBException {
        if (subTables == null) {
            try (JDBCSession session = DBUtils.openMetaSession(monitor, getDataSource(), "Load table inheritance info")) {
                try (JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT i.*,c.relnamespace " +
                    "FROM pg_catalog.pg_inherits i,pg_catalog.pg_class c " +
                    "WHERE i.inhparent=? AND c.oid=i.inhrelid")) {
                    dbStat.setInt(1, getObjectId());
                    try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                        while (dbResult.next()) {
                            final int subSchemaId = JDBCUtils.safeGetInt(dbResult, "relnamespace");
                            final int subTableId = JDBCUtils.safeGetInt(dbResult, "inhrelid");
                            PostgreSchema schema = getDatabase().getSchema(monitor, subSchemaId);
                            if (schema == null) {
                                log.warn("Can't find sub-table's schema '" + subSchemaId + "'");
                                continue;
                            }
                            PostgreTableBase subTable = schema.getTable(monitor, subTableId);
                            if (subTable == null) {
                                log.warn("Can't find sub-table '" + subTableId + "' in '" + schema.getName() + "'");
                                continue;
                            }
                            if (subTables == null) {
                                subTables = new ArrayList<>();
                            }
                            subTables.add(
                                new PostgreTableInheritance(
                                    subTable,
                                    this,
                                    JDBCUtils.safeGetInt(dbResult, "inhseqno"),
                                    true));
                        }
                    }
                }
            } catch (SQLException e) {
                throw new DBCException(e, getDataSource());
            }
            if (subTables == null) {
                subTables = Collections.emptyList();
            }
        }
        return subTables;
    }
}

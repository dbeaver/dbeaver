/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyDefferability;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.utils.CommonUtils;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.*;

/**
* Foreign key cache
*/
class ForeignKeysCache extends JDBCCompositeCache<GenericStructContainer, GenericTable, GenericTableForeignKey, GenericTableForeignKeyColumnTable> {

    private final Map<String, GenericPrimaryKey> pkMap = new HashMap<>();
    private final GenericMetaObject foreignKeyObject;

    ForeignKeysCache(TableCache tableCache)
    {
        super(
            tableCache, 
            GenericTable.class,
            GenericUtils.getColumn(tableCache.getDataSource(), GenericConstants.OBJECT_FOREIGN_KEY, JDBCConstants.FKTABLE_NAME),
            GenericUtils.getColumn(tableCache.getDataSource(), GenericConstants.OBJECT_FOREIGN_KEY, JDBCConstants.FK_NAME));
        foreignKeyObject = tableCache.getDataSource().getMetaObject(GenericConstants.OBJECT_FOREIGN_KEY);
    }

    @Override
    public void clearCache()
    {
        pkMap.clear();
        super.clearCache();
    }

    @NotNull
    @Override
    protected JDBCStatement prepareObjectsStatement(JDBCSession session, GenericStructContainer owner, GenericTable forParent)
        throws SQLException
    {
        return session.getMetaData().getImportedKeys(
            owner.getCatalog() == null ? null : owner.getCatalog().getName(),
            owner.getSchema() == null ? null : owner.getSchema().getName(),
            forParent == null ? owner.getDataSource().getAllObjectsPattern() : forParent.getName())
            .getSourceStatement();
    }

    @Nullable
    @Override
    protected GenericTableForeignKey fetchObject(JDBCSession session, GenericStructContainer owner, GenericTable parent, String fkName, JDBCResultSet dbResult)
        throws SQLException, DBException
    {
        String pkTableCatalog = GenericUtils.safeGetStringTrimmed(foreignKeyObject, dbResult, JDBCConstants.PKTABLE_CAT);
        String pkTableSchema = GenericUtils.safeGetStringTrimmed(foreignKeyObject, dbResult, JDBCConstants.PKTABLE_SCHEM);
        String pkTableName = GenericUtils.safeGetStringTrimmed(foreignKeyObject, dbResult, JDBCConstants.PKTABLE_NAME);

        int keySeq = GenericUtils.safeGetInt(foreignKeyObject, dbResult, JDBCConstants.KEY_SEQ);
        int updateRuleNum = GenericUtils.safeGetInt(foreignKeyObject, dbResult, JDBCConstants.UPDATE_RULE);
        int deleteRuleNum = GenericUtils.safeGetInt(foreignKeyObject, dbResult, JDBCConstants.DELETE_RULE);
        String pkName = GenericUtils.safeGetStringTrimmed(foreignKeyObject, dbResult, JDBCConstants.PK_NAME);
        int defferabilityNum = GenericUtils.safeGetInt(foreignKeyObject, dbResult, JDBCConstants.DEFERRABILITY);

        DBSForeignKeyModifyRule deleteRule = JDBCUtils.getCascadeFromNum(deleteRuleNum);
        DBSForeignKeyModifyRule updateRule = JDBCUtils.getCascadeFromNum(updateRuleNum);
        DBSForeignKeyDefferability defferability;
        switch (defferabilityNum) {
            case DatabaseMetaData.importedKeyInitiallyDeferred: defferability = DBSForeignKeyDefferability.INITIALLY_DEFERRED; break;
            case DatabaseMetaData.importedKeyInitiallyImmediate: defferability = DBSForeignKeyDefferability.INITIALLY_IMMEDIATE; break;
            case DatabaseMetaData.importedKeyNotDeferrable: defferability = DBSForeignKeyDefferability.NOT_DEFERRABLE; break;
            default: defferability = DBSForeignKeyDefferability.UNKNOWN; break;
        }

        if (pkTableName == null) {
            log.debug("Null PK table name");
            return null;
        }
        //String pkTableFullName = DBUtils.getFullQualifiedName(getDataSource(), pkTableCatalog, pkTableSchema, pkTableName);
        GenericTable pkTable = parent.getDataSource().findTable(session.getProgressMonitor(), pkTableCatalog, pkTableSchema, pkTableName);
        if (pkTable == null) {
            log.warn("Can't find PK table " + pkTableName);
            return null;
        }

        // Find PK
        GenericPrimaryKey pk = null;
        if (!CommonUtils.isEmpty(pkName)) {
            pk = DBUtils.findObject(pkTable.getConstraints(session.getProgressMonitor()), pkName);
            if (pk == null) {
                log.debug("Unique key '" + pkName + "' not found in table " + pkTable.getFullQualifiedName());
            }
        }
        if (pk == null) {
            String pkColumnName = GenericUtils.safeGetStringTrimmed(foreignKeyObject, dbResult, JDBCConstants.PKCOLUMN_NAME);
            GenericTableColumn pkColumn = pkTable.getAttribute(session.getProgressMonitor(), pkColumnName);
            if (pkColumn == null) {
                log.warn("Can't find PK table " + pkTable.getFullQualifiedName() + " column " + pkColumnName);
                return null;
            }

            Collection<GenericPrimaryKey> uniqueKeys = pkTable.getConstraints(session.getProgressMonitor());
            if (uniqueKeys != null) {
                for (GenericPrimaryKey pkConstraint : uniqueKeys) {
                    if (pkConstraint.getConstraintType().isUnique() && DBUtils.getConstraintAttribute(session.getProgressMonitor(), pkConstraint, pkColumn) != null) {
                        pk = pkConstraint;
                        break;
                    }
                }
            }
            if (pk == null) {
                log.warn("Can't find unique key for table " + pkTable.getFullQualifiedName() + " column " + pkColumn.getName());
                // Too bad. But we have to create new fake PK for this FK
                String pkFullName = pkTable.getFullQualifiedName() + "." + pkName;
                pk = pkMap.get(pkFullName);
                if (pk == null) {
                    pk = new GenericPrimaryKey(pkTable, pkName, null, DBSEntityConstraintType.PRIMARY_KEY, true);
                    pkMap.put(pkFullName, pk);
                    // Add this fake constraint to it's owner
                    pk.getTable().addUniqueKey(pk);
                }
                pk.addColumn(new GenericTableConstraintColumn(pk, pkColumn, keySeq));
            }
        }
        if (CommonUtils.isEmpty(fkName)) {
            // [JDBC] Some drivers return empty foreign key names
            fkName = parent.getName().toUpperCase() + "_FK_" + pkTable.getName().toUpperCase(Locale.ENGLISH);
        }
        return new GenericTableForeignKey(parent, fkName, null, pk, deleteRule, updateRule, defferability, true);
    }

    @Nullable
    @Override
    protected GenericTableForeignKeyColumnTable[] fetchObjectRow(
        JDBCSession session,
        GenericTable parent, GenericTableForeignKey foreignKey, JDBCResultSet dbResult)
        throws SQLException, DBException
    {
        String pkColumnName = GenericUtils.safeGetStringTrimmed(foreignKeyObject, dbResult, JDBCConstants.PKCOLUMN_NAME);
        GenericTableConstraintColumn pkColumn = (GenericTableConstraintColumn)DBUtils.getConstraintAttribute(
            session.getProgressMonitor(),
            foreignKey.getReferencedConstraint(),
            pkColumnName);
        if (pkColumn == null) {
            log.warn("Can't find PK table " + foreignKey.getReferencedConstraint().getTable().getFullQualifiedName() + " column " + pkColumnName);
            return null;
        }
        int keySeq = GenericUtils.safeGetInt(foreignKeyObject, dbResult, JDBCConstants.KEY_SEQ);

        String fkColumnName = GenericUtils.safeGetStringTrimmed(foreignKeyObject, dbResult, JDBCConstants.FKCOLUMN_NAME);
        if (CommonUtils.isEmpty(fkColumnName)) {
            log.warn("Empty FK column for table " + foreignKey.getTable().getFullQualifiedName() + " PK column " + pkColumnName);
            return null;
        }
        GenericTableColumn fkColumn = foreignKey.getTable().getAttribute(session.getProgressMonitor(), fkColumnName);
        if (fkColumn == null) {
            log.warn("Can't find FK table " + foreignKey.getTable().getFullQualifiedName() + " column " + fkColumnName);
            return null;
        }

        return new GenericTableForeignKeyColumnTable[] {
            new GenericTableForeignKeyColumnTable(foreignKey, fkColumn, keySeq, pkColumn.getAttribute()) };
    }

    @Override
    protected void cacheChildren(DBRProgressMonitor monitor, GenericTableForeignKey foreignKey, List<GenericTableForeignKeyColumnTable> rows)
    {
        foreignKey.setColumns(rows);
    }

    @Override
    protected String getDefaultObjectName(JDBCResultSet dbResult, String parentName) {
        final String pkTableName = GenericUtils.safeGetStringTrimmed(foreignKeyObject, dbResult, JDBCConstants.PKTABLE_NAME);
        return "FK_" + parentName + "_" + pkTableName;
    }
}

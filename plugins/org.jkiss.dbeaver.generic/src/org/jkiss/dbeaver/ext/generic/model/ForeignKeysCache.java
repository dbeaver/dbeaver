/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.struct.DBSConstraintDefferability;
import org.jkiss.dbeaver.model.struct.DBSConstraintModifyRule;
import org.jkiss.dbeaver.model.struct.DBSConstraintType;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* Foreign key cache
*/
class ForeignKeysCache extends JDBCCompositeCache<GenericStructContainer, GenericTable, GenericForeignKey, GenericForeignKeyColumn> {

    Map<String, GenericPrimaryKey> pkMap = new HashMap<String, GenericPrimaryKey>();

    ForeignKeysCache(TableCache tableCache)
    {
        super(tableCache, GenericTable.class, JDBCConstants.FKTABLE_NAME, JDBCConstants.FK_NAME);
    }

    @Override
    public void clearCache()
    {
        pkMap.clear();
        super.clearCache();
    }

    protected JDBCPreparedStatement prepareObjectsStatement(JDBCExecutionContext context, GenericStructContainer owner, GenericTable forParent)
        throws SQLException, DBException
    {
        return context.getMetaData().getImportedKeys(
            owner.getCatalog() == null ? null : owner.getCatalog().getName(),
            owner.getSchema() == null ? null : owner.getSchema().getName(),
            forParent == null ? null : forParent.getName())
            .getSource();
    }

    protected GenericForeignKey fetchObject(JDBCExecutionContext context, GenericStructContainer owner, GenericTable parent, String fkName, ResultSet dbResult)
        throws SQLException, DBException
    {
        String pkTableCatalog = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.PKTABLE_CAT);
        String pkTableSchema = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.PKTABLE_SCHEM);
        String pkTableName = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.PKTABLE_NAME);

        int keySeq = JDBCUtils.safeGetInt(dbResult, JDBCConstants.KEY_SEQ);
        int updateRuleNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.UPDATE_RULE);
        int deleteRuleNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.DELETE_RULE);
        String pkName = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.PK_NAME);
        int defferabilityNum = JDBCUtils.safeGetInt(dbResult, JDBCConstants.DEFERRABILITY);

        DBSConstraintModifyRule deleteRule = JDBCUtils.getCascadeFromNum(deleteRuleNum);
        DBSConstraintModifyRule updateRule = JDBCUtils.getCascadeFromNum(updateRuleNum);
        DBSConstraintDefferability defferability;
        switch (defferabilityNum) {
            case DatabaseMetaData.importedKeyInitiallyDeferred: defferability = DBSConstraintDefferability.INITIALLY_DEFERRED; break;
            case DatabaseMetaData.importedKeyInitiallyImmediate: defferability = DBSConstraintDefferability.INITIALLY_IMMEDIATE; break;
            case DatabaseMetaData.importedKeyNotDeferrable: defferability = DBSConstraintDefferability.NOT_DEFERRABLE; break;
            default: defferability = DBSConstraintDefferability.UNKNOWN; break;
        }

        if (pkTableName == null) {
            log.debug("Null PK table name");
            return null;
        }
        //String pkTableFullName = DBUtils.getFullQualifiedName(getDataSource(), pkTableCatalog, pkTableSchema, pkTableName);
        GenericTable pkTable = parent.getDataSource().findTable(context.getProgressMonitor(), pkTableCatalog, pkTableSchema, pkTableName);
        if (pkTable == null) {
            log.warn("Can't find PK table " + pkTableName);
            return null;
        }

        // Find PK
        GenericPrimaryKey pk = null;
        if (pkName != null) {
            pk = DBUtils.findObject(pkTable.getConstraints(context.getProgressMonitor()), pkName);
            if (pk == null) {
                log.warn("Unique key '" + pkName + "' not found in table " + pkTable.getFullQualifiedName());
            }
        }
        if (pk == null) {
            String pkColumnName = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.PKCOLUMN_NAME);
            GenericTableColumn pkColumn = pkTable.getColumn(context.getProgressMonitor(), pkColumnName);
            if (pkColumn == null) {
                log.warn("Can't find PK table " + pkTable.getFullQualifiedName() + " column " + pkColumnName);
                return null;
            }

            List<GenericPrimaryKey> uniqueKeys = pkTable.getConstraints(context.getProgressMonitor());
            if (uniqueKeys != null) {
                for (GenericPrimaryKey pkConstraint : uniqueKeys) {
                    if (pkConstraint.getConstraintType().isUnique() && pkConstraint.getColumn(context.getProgressMonitor(), pkColumn) != null) {
                        pk = pkConstraint;
                        break;
                    }
                }
            }
            if (pk == null) {
                log.warn("Could not find unique key for table " + pkTable.getFullQualifiedName() + " column " + pkColumn.getName());
                // Too bad. But we have to create new fake PK for this FK
                String pkFullName = pkTable.getFullQualifiedName() + "." + pkName;
                pk = pkMap.get(pkFullName);
                if (pk == null) {
                    pk = new GenericPrimaryKey(pkTable, pkName, null, DBSConstraintType.PRIMARY_KEY, true);
                    pkMap.put(pkFullName, pk);
                    // Add this fake constraint to it's owner
                    pk.getTable().addUniqueKey(pk);
                }
                pk.addColumn(new GenericConstraintColumn(pk, pkColumn, keySeq));
            }
        }

        return new GenericForeignKey(parent, fkName, null, pk, deleteRule, updateRule, defferability, true);
    }

    protected GenericForeignKeyColumn fetchObjectRow(
        JDBCExecutionContext context,
        GenericTable parent, GenericForeignKey foreignKey, ResultSet dbResult)
        throws SQLException, DBException
    {
        String pkColumnName = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.PKCOLUMN_NAME);
        GenericConstraintColumn pkColumn = (GenericConstraintColumn)foreignKey.getReferencedKey().getColumn(context.getProgressMonitor(), pkColumnName);
        if (pkColumn == null) {
            log.warn("Can't find PK table " + foreignKey.getReferencedKey().getTable().getFullQualifiedName() + " column " + pkColumnName);
            return null;
        }
        int keySeq = JDBCUtils.safeGetInt(dbResult, JDBCConstants.KEY_SEQ);

        String fkColumnName = JDBCUtils.safeGetStringTrimmed(dbResult, JDBCConstants.FKCOLUMN_NAME);
        GenericTableColumn fkColumn = foreignKey.getTable().getColumn(context.getProgressMonitor(), fkColumnName);
        if (fkColumn == null) {
            log.warn("Can't find FK table " + foreignKey.getTable().getFullQualifiedName() + " column " + fkColumnName);
            return null;
        }

        return new GenericForeignKeyColumn(foreignKey, fkColumn, keySeq, pkColumn.getTableColumn());
    }

    protected boolean isObjectsCached(GenericTable parent)
    {
        return parent.isForeignKeysCached();
    }

    protected void cacheObjects(GenericTable parent, List<GenericForeignKey> foreignKeys)
    {
        parent.setForeignKeys(foreignKeys);
    }

    protected void cacheChildren(GenericForeignKey foreignKey, List<GenericForeignKeyColumn> rows)
    {
        foreignKey.setColumns(rows);
    }

}

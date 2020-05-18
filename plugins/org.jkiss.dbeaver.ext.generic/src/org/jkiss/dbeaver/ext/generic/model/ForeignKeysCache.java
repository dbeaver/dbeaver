/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.generic.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaObject;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntityAttributeRef;
import org.jkiss.dbeaver.model.struct.DBSEntityConstraintType;
import org.jkiss.dbeaver.model.struct.DBSEntityReferrer;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyDeferability;
import org.jkiss.dbeaver.model.struct.rdb.DBSForeignKeyModifyRule;
import org.jkiss.utils.CommonUtils;

import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.*;

/**
* Foreign key cache
*/
class ForeignKeysCache extends JDBCCompositeCache<GenericStructContainer, GenericTableBase, GenericTableForeignKey, GenericTableForeignKeyColumnTable> {

    private final Map<String, GenericUniqueKey> pkMap = new HashMap<>();
    private final GenericMetaObject foreignKeyObject;
    private Set<String> cachedFKNames;

    ForeignKeysCache(TableCache tableCache)
    {
        super(
            tableCache,
            GenericTableBase.class,
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
    protected JDBCStatement prepareObjectsStatement(JDBCSession session, GenericStructContainer owner, GenericTableBase forParent)
        throws SQLException
    {
        return session.getMetaData().getImportedKeys(
            owner.getCatalog() == null ? null : owner.getCatalog().getName(),
            owner.getSchema() == null || DBUtils.isVirtualObject(owner.getSchema()) ? null : owner.getSchema().getName(),
            forParent == null ?
                owner.getDataSource().getAllObjectsPattern() :
                forParent.getName())
            .getSourceStatement();
    }

    @Nullable
    @Override
    protected GenericTableForeignKey fetchObject(JDBCSession session, GenericStructContainer owner, GenericTableBase parent, String fkName, JDBCResultSet dbResult)
        throws SQLException, DBException
    {
        String pkTableCatalog = GenericUtils.safeGetStringTrimmed(foreignKeyObject, dbResult, JDBCConstants.PKTABLE_CAT);
        String pkTableSchema = GenericUtils.safeGetStringTrimmed(foreignKeyObject, dbResult, JDBCConstants.PKTABLE_SCHEM);
        String pkTableName = GenericUtils.safeGetStringTrimmed(foreignKeyObject, dbResult, JDBCConstants.PKTABLE_NAME);
        String fkTableCatalog = GenericUtils.safeGetStringTrimmed(foreignKeyObject, dbResult, JDBCConstants.FKTABLE_CAT);
        String fkTableSchema = GenericUtils.safeGetStringTrimmed(foreignKeyObject, dbResult, JDBCConstants.FKTABLE_SCHEM);

        int keySeq = GenericUtils.safeGetInt(foreignKeyObject, dbResult, JDBCConstants.KEY_SEQ);
        int updateRuleNum = GenericUtils.safeGetInt(foreignKeyObject, dbResult, JDBCConstants.UPDATE_RULE);
        int deleteRuleNum = GenericUtils.safeGetInt(foreignKeyObject, dbResult, JDBCConstants.DELETE_RULE);
        String pkName = GenericUtils.safeGetStringTrimmed(foreignKeyObject, dbResult, JDBCConstants.PK_NAME);
        int deferabilityNum = GenericUtils.safeGetInt(foreignKeyObject, dbResult, JDBCConstants.DEFERRABILITY);

        DBSForeignKeyModifyRule deleteRule = JDBCUtils.getCascadeFromNum(deleteRuleNum);
        DBSForeignKeyModifyRule updateRule = JDBCUtils.getCascadeFromNum(updateRuleNum);
        DBSForeignKeyDeferability deferability;
        switch (deferabilityNum) {
            case DatabaseMetaData.importedKeyInitiallyDeferred: deferability = DBSForeignKeyDeferability.INITIALLY_DEFERRED; break;
            case DatabaseMetaData.importedKeyInitiallyImmediate: deferability = DBSForeignKeyDeferability.INITIALLY_IMMEDIATE; break;
            case DatabaseMetaData.importedKeyNotDeferrable: deferability = DBSForeignKeyDeferability.NOT_DEFERRABLE; break;
            default: deferability = DBSForeignKeyDeferability.UNKNOWN; break;
        }

        if (pkTableName == null) {
            log.debug("Null PK table name");
            return null;
        }
        String pkTableFullName = DBUtils.getSimpleQualifiedName(pkTableCatalog, pkTableSchema, pkTableName);
        GenericTableBase pkTable = parent.getDataSource().findTable(session.getProgressMonitor(), pkTableCatalog, pkTableSchema, pkTableName);
        if (pkTable == null) {
            // Try to use FK catalog/schema
            pkTable = parent.getDataSource().findTable(session.getProgressMonitor(), fkTableCatalog, fkTableSchema, pkTableName);
            if (pkTable == null) {
                log.warn("Can't find PK table " + pkTableName);
                return null;
            } else {
                log.debug("PK table " + pkTableFullName + " was taken from FK container.");
            }
        }

        // Find PK
        DBSEntityReferrer pk = null;
        if (!CommonUtils.isEmpty(pkName)) {
            pk = DBUtils.findObject(pkTable.getConstraints(session.getProgressMonitor()), pkName);
            if (pk == null) {
                log.debug("Unique key '" + pkName + "' not found in table " + pkTable.getFullyQualifiedName(DBPEvaluationContext.DDL));
            }
        }
        if (pk == null) {
            String pkColumnName = GenericUtils.safeGetStringTrimmed(foreignKeyObject, dbResult, JDBCConstants.PKCOLUMN_NAME);
            GenericTableColumn pkColumn = pkTable.getAttribute(session.getProgressMonitor(), pkColumnName);
            if (pkColumn == null) {
                log.warn("Can't find PK table " + pkTable.getFullyQualifiedName(DBPEvaluationContext.DDL) + " column " + pkColumnName);
                return null;
            }

            Collection<GenericUniqueKey> uniqueKeys = pkTable.getConstraints(session.getProgressMonitor());
            if (uniqueKeys != null) {
                for (GenericUniqueKey pkConstraint : uniqueKeys) {
                    if (pkConstraint.getConstraintType().isUnique() && DBUtils.getConstraintAttribute(session.getProgressMonitor(), pkConstraint, pkColumn) != null) {
                        pk = pkConstraint;
                        break;
                    }
                }
            }
            if (pk == null) {
                // No PK. Let's try unique indexes
                Collection<GenericTableIndex> indexes = pkTable.getIndexes(session.getProgressMonitor());
                if (indexes != null) {
                    for (GenericTableIndex index : indexes) {
                        if (index.isUnique() && DBUtils.getConstraintAttribute(session.getProgressMonitor(), index, pkColumn) != null) {
                            pk = index;
                            break;
                        }
                    }
                }
            }
            if (pk == null) {
                log.warn("Can't find unique key for table " + pkTable.getFullyQualifiedName(DBPEvaluationContext.DDL) + " column " + pkColumn.getName() + ". Making fake one.");
                GenericUniqueKey fakePk;
                // Too bad. But we have to create new fake PK for this FK
                if (pkName == null) {
                    pkName = "primary_key";
                }
                String pkFullName = pkTable.getFullyQualifiedName(DBPEvaluationContext.DDL) + "." + pkName;
                fakePk = pkMap.get(pkFullName);
                if (fakePk == null) {
                    fakePk = new GenericUniqueKey(pkTable, pkName, null, DBSEntityConstraintType.PRIMARY_KEY, true);
                    pkMap.put(pkFullName, fakePk);
                    // Add this fake constraint to it's owner
                    fakePk.getTable().addUniqueKey(fakePk);
                }
                fakePk.addColumn(new GenericTableConstraintColumn(fakePk, pkColumn, keySeq));
                pk = fakePk;
            }
        }
        if (CommonUtils.isEmpty(fkName)) {
            // [JDBC] Some drivers return empty foreign key names
            fkName = parent.getName().toUpperCase() + "_FK_" + pkTable.getName().toUpperCase(Locale.ENGLISH);
        }
        return new GenericTableForeignKey(parent, fkName, null, pk, deleteRule, updateRule, deferability, true);
    }

    @Nullable
    @Override
    protected GenericTableForeignKeyColumnTable[] fetchObjectRow(
        JDBCSession session,
        GenericTableBase parent, GenericTableForeignKey foreignKey, JDBCResultSet dbResult)
        throws SQLException, DBException
    {
        String pkColumnName = GenericUtils.safeGetStringTrimmed(foreignKeyObject, dbResult, JDBCConstants.PKCOLUMN_NAME);
        DBSEntityReferrer referencedConstraint = foreignKey.getReferencedConstraint();
        if (referencedConstraint == null) {
            log.warn("Null reference constraint in FK '" + foreignKey.getFullyQualifiedName(DBPEvaluationContext.DDL) + "'");
            return null;
        }
        DBSEntityAttributeRef pkColumn = DBUtils.getConstraintAttribute(
            session.getProgressMonitor(),
            referencedConstraint,
            pkColumnName);
        if (pkColumn == null) {
            log.warn("Can't find PK table " + DBUtils.getObjectFullName(referencedConstraint.getParentObject(), DBPEvaluationContext.DML) + " column " + pkColumnName);
            return null;
        }
        int keySeq = GenericUtils.safeGetInt(foreignKeyObject, dbResult, JDBCConstants.KEY_SEQ);

        String fkColumnName = GenericUtils.safeGetStringTrimmed(foreignKeyObject, dbResult, JDBCConstants.FKCOLUMN_NAME);
        if (CommonUtils.isEmpty(fkColumnName)) {
            log.warn("Empty FK column for table " + foreignKey.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) + " PK column " + pkColumnName);
            return null;
        }
        GenericTableColumn fkColumn = foreignKey.getTable().getAttribute(session.getProgressMonitor(), fkColumnName);
        if (fkColumn == null) {
            log.warn("Can't find FK table " + foreignKey.getTable().getFullyQualifiedName(DBPEvaluationContext.DDL) + " column " + fkColumnName);
            return null;
        }

        return new GenericTableForeignKeyColumnTable[] {
            new GenericTableForeignKeyColumnTable(foreignKey, fkColumn, keySeq, (GenericTableColumn)pkColumn.getAttribute()) };
    }

    @Override
    protected void cacheChildren(DBRProgressMonitor monitor, GenericTableForeignKey foreignKey, List<GenericTableForeignKeyColumnTable> rows)
    {
        foreignKey.setColumns(monitor, rows);
    }

    @Override
    protected String getDefaultObjectName(JDBCResultSet dbResult, String parentName) {
        if (cachedFKNames == null) {
            cachedFKNames = new LinkedHashSet<>();
        }
        final String pkTableName = GenericUtils.safeGetStringTrimmed(foreignKeyObject, dbResult, JDBCConstants.PKTABLE_NAME);
        int keySeq = GenericUtils.safeGetInt(foreignKeyObject, dbResult, JDBCConstants.KEY_SEQ);
        String fkName = "FK_" + parentName + "_" + pkTableName;
        if (cachedFKNames.contains(fkName) && keySeq == 1) {
            // Multiple unnamed foreign keys - #8286
            // Column sequnce 1 means new FK so lets make a new one
            fkName += "_" + (cachedFKNames.size() + 1);
        }
        cachedFKNames.add(fkName);
        return fkName;
    }
}

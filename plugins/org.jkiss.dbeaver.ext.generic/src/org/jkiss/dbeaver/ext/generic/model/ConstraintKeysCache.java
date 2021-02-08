/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2021 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCCompositeCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.SQLException;
import java.util.List;
import java.util.Locale;

/**
 * Index cache implementation
 */
class ConstraintKeysCache extends JDBCCompositeCache<GenericStructContainer, GenericTableBase, GenericUniqueKey, GenericTableConstraintColumn> {

    private final GenericMetaObject pkObject;

    ConstraintKeysCache(TableCache tableCache)
    {
        super(
            tableCache,
            GenericTableBase.class,
            GenericUtils.getColumn(tableCache.getDataSource(), GenericConstants.OBJECT_PRIMARY_KEY, JDBCConstants.TABLE_NAME),
            GenericUtils.getColumn(tableCache.getDataSource(), GenericConstants.OBJECT_PRIMARY_KEY, JDBCConstants.PK_NAME));
        pkObject = tableCache.getDataSource().getMetaObject(GenericConstants.OBJECT_PRIMARY_KEY);
    }

    @NotNull
    @Override
    protected JDBCStatement prepareObjectsStatement(JDBCSession session, GenericStructContainer owner, GenericTableBase forParent)
        throws SQLException
    {
        try {
            return owner.getDataSource().getMetaModel().prepareUniqueConstraintsLoadStatement(
                session,
                owner,
                forParent);
        } catch (SQLException e) {
            throw e;
        } catch (Exception e) {
            if (forParent == null) {
                throw new SQLException("Global primary keys read not supported", e);
            } else {
                throw new SQLException(e);
            }
        }
    }

    protected String getDefaultObjectName(JDBCResultSet dbResult, String parentName) {
        int keySeq = GenericUtils.safeGetInt(pkObject, dbResult, JDBCConstants.KEY_SEQ);
        return parentName.toUpperCase(Locale.ENGLISH) + "_PK";
    }

    @Nullable
    @Override
    protected GenericUniqueKey fetchObject(JDBCSession session, GenericStructContainer owner, GenericTableBase parent, String pkName, JDBCResultSet dbResult)
        throws SQLException, DBException
    {
        return owner.getDataSource().getMetaModel().createConstraintImpl(
            parent,
            pkName,
            owner.getDataSource().getMetaModel().getUniqueConstraintType(dbResult),
            dbResult,
            true);
    }

    @Nullable
    @Override
    protected GenericTableConstraintColumn[] fetchObjectRow(
        JDBCSession session,
        GenericTableBase parent, GenericUniqueKey object, JDBCResultSet dbResult)
        throws SQLException, DBException
    {
        return parent.getDataSource().getMetaModel().createConstraintColumnsImpl(session, parent, object, pkObject, dbResult);
    }

    @Override
    protected void cacheChildren(DBRProgressMonitor monitor, GenericUniqueKey primaryKey, List<GenericTableConstraintColumn> rows)
    {
        primaryKey.setColumns(rows);
    }

}

/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.generic.GenericConstants;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaObject;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCConstants;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCStructLookupCache;

import java.sql.SQLException;

/**
 * Generic tables cache implementation
 */
public class TableCache extends JDBCStructLookupCache<GenericStructContainer, GenericTableBase, GenericTableColumn> {

    private static final Log log = Log.getLog(TableCache.class);

    final GenericDataSource dataSource;
    final GenericMetaObject tableObject;
    final GenericMetaObject columnObject;

    protected TableCache(GenericDataSource dataSource)
    {
        super(GenericUtils.getColumn(dataSource, GenericConstants.OBJECT_TABLE, JDBCConstants.TABLE_NAME));
        this.dataSource = dataSource;
        this.tableObject = dataSource.getMetaObject(GenericConstants.OBJECT_TABLE);
        this.columnObject = dataSource.getMetaObject(GenericConstants.OBJECT_TABLE_COLUMN);
        setListOrderComparator(DBUtils.<GenericTableBase>nameComparatorIgnoreCase());
    }

    public GenericDataSource getDataSource()
    {
        return dataSource;
    }

    @NotNull
    @Override
    public JDBCStatement prepareLookupStatement(@NotNull JDBCSession session, @NotNull GenericStructContainer owner, @Nullable GenericTableBase object, @Nullable String objectName) throws SQLException {
        return dataSource.getMetaModel().prepareTableLoadStatement(session, owner, object, objectName);
    }

    @Nullable
    @Override
    protected GenericTableBase fetchObject(@NotNull JDBCSession session, @NotNull GenericStructContainer owner, @NotNull JDBCResultSet dbResult)
        throws SQLException, DBException
    {
        return getDataSource().getMetaModel().createTableImpl(session, owner, tableObject, dbResult);
    }

    @Override
    protected JDBCStatement prepareChildrenStatement(@NotNull JDBCSession session, @NotNull GenericStructContainer owner, @Nullable GenericTableBase forTable)
        throws SQLException
    {
        return dataSource.getMetaModel().prepareTableColumnLoadStatement(session, owner, forTable);
    }

    @Override
    protected GenericTableColumn fetchChild(@NotNull JDBCSession session, @NotNull GenericStructContainer owner, @NotNull GenericTableBase table, @NotNull JDBCResultSet dbResult)
        throws SQLException, DBException
    {
        return dataSource.getMetaModel().fetchTableColumn(session, owner, table, dbResult);
    }

    @Override
    public void beforeCacheLoading(JDBCSession session, GenericStructContainer owner) throws DBException {
       // Do nothing
    }

    @Override
    public void afterCacheLoading(JDBCSession session, GenericStructContainer owner) throws DBException {
        // Do nothing
    }
}

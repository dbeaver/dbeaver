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

package org.jkiss.dbeaver.ext.dameng.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.dameng.DamengConstants;
import org.jkiss.dbeaver.ext.generic.model.GenericDataSource;
import org.jkiss.dbeaver.ext.generic.model.meta.GenericMetaModel;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBPDataSourceInfo;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCDatabaseMetaData;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCDataSourceInfo;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCRemoteInstance;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.SQLException;
import java.sql.Types;
import java.util.Collection;

/**
 * @author Shengkai Bai
 */
public class DamengDataSource extends GenericDataSource {

    private final TablespaceCache tablespaceCache = new TablespaceCache();

    private final UserCache userCache = new UserCache();

    private final RoleCache roleCache = new RoleCache();

    public DamengDataSource(DBRProgressMonitor monitor, DBPDataSourceContainer container, GenericMetaModel metaModel) throws DBException {
        super(monitor, container, metaModel, new DamengSQLDialect());
    }

    @Override
    protected JDBCExecutionContext createExecutionContext(JDBCRemoteInstance instance, String type) {
        return new DamengExecutionContext(instance, type);
    }

    @Override
    protected void initializeContextState(DBRProgressMonitor monitor, JDBCExecutionContext context, JDBCExecutionContext initFrom) throws DBException {
        DamengExecutionContext executionContext = (DamengExecutionContext) context;
        if (initFrom == null) {
            executionContext.refreshDefaults(monitor, true);
            return;
        }
        DamengExecutionContext executionMetaContext = (DamengExecutionContext) initFrom;
        DamengSchema defaultSchema = executionMetaContext.getDefaultSchema();
        if (defaultSchema != null) {
            executionContext.setDefaultSchema(monitor, defaultSchema);
        }
    }

    @Override
    protected DBPDataSourceInfo createDataSourceInfo(DBRProgressMonitor monitor, JDBCDatabaseMetaData metaData) {
        return new JDBCDataSourceInfo(metaData);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        super.refreshObject(monitor);
        tablespaceCache.clearCache();
        return this;
    }

    @Override
    public DBPDataKind resolveDataKind(String typeName, int valueType) {
        return getDataKind(typeName, valueType);
    }

    @NotNull
    public static DBPDataKind getDataKind(@NotNull String typeName, int valueType) {
        if (valueType == Types.JAVA_OBJECT && DamengConstants.INTERVAL_TYPES.contains(typeName)) {
            return DBPDataKind.DATETIME;
        }
        return GenericDataSource.getDataKind(typeName, valueType);

    }

    @Nullable
    public DamengTablespace getTablespaceById(DBRProgressMonitor monitor, long tableSpaceId) throws DBException {
        return DamengUtils.getObjectById(monitor, tablespaceCache, this, tableSpaceId);
    }

    @Nullable
    public DamengUser getUserById(DBRProgressMonitor monitor, long userId) throws DBException {
        return DamengUtils.getObjectById(monitor, userCache, this, userId);
    }

    @Association
    public Collection<DamengTablespace> getTablespaces(DBRProgressMonitor monitor) throws DBException {
        return tablespaceCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<DamengUser> getUsers(DBRProgressMonitor monitor) throws DBException {
        return userCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<DamengRole> getRoles(DBRProgressMonitor monitor) throws DBException {
        return roleCache.getAllObjects(monitor, this);
    }

    public TablespaceCache getTablespaceCache() {
        return tablespaceCache;
    }

    static class RoleCache extends JDBCObjectCache<DamengDataSource, DamengRole> {

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DamengDataSource damengDataSource) throws SQLException {
            return session.prepareStatement("SELECT\n" +
                    "ID,\n" +
                    "NAME,\n" +
                    "INFO1,\n" +
                    "VALID,\n" +
                    "CRTDATE\n" +
                    "FROM\n" +
                    "SYSOBJECTS\n" +
                    "WHERE\n" +
                    "TYPE$ = 'UR'\n" +
                    "AND SUBTYPE$ = 'ROLE'\n" +
                    "AND (INFO2 IS NULL\n" +
                    "OR INFO2 != 1)\n" +
                    "AND INFO1 = 0\n" +
                    "ORDER BY\n" +
                    "NAME");
        }

        @Override
        protected DamengRole fetchObject(@NotNull JDBCSession session, @NotNull DamengDataSource damengDataSource, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new DamengRole(damengDataSource, resultSet);
        }
    }

    static class UserCache extends JDBCObjectCache<DamengDataSource, DamengUser> {

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DamengDataSource dataSource) throws SQLException {
            return session.prepareStatement("SELECT\n" +
                    "USER_OBJ.*,\n" +
                    "SYSUSERS.*,\n" +
                    "TS.ID AS TABLESPACE_ID,\n" +
                    "TS.NAME AS TABLESPACE\n" +
                    "FROM\n" +
                    "SYSOBJECTS USER_OBJ\n" +
                    "LEFT JOIN SYSUSERS ON\n" +
                    "SYSUSERS.ID = USER_OBJ.ID\n" +
                    "LEFT JOIN SYS.V$TABLESPACE TS ON\n" +
                    // INFO3 BYTE(0-1) default tablespace id
                    "USER_OBJ.INFO3 & 0x000000000000FFFF = TS.ID\n" +
                    "WHERE\n" +
                    "USER_OBJ.SUBTYPE$ = 'USER'");
        }

        @Override
        protected DamengUser fetchObject(@NotNull JDBCSession session, @NotNull DamengDataSource dataSource, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new DamengUser(dataSource, resultSet);
        }
    }

    public static class TablespaceCache extends JDBCObjectCache<DamengDataSource, DamengTablespace> {

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DamengDataSource dataSource) throws SQLException {
            return session.prepareStatement("SELECT * FROM V$TABLESPACE");
        }

        @Override
        protected DamengTablespace fetchObject(@NotNull JDBCSession session, @NotNull DamengDataSource dataSource, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new DamengTablespace(dataSource, resultSet);
        }
    }
}

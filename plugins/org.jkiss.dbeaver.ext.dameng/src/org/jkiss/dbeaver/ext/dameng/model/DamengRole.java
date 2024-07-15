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
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.dameng.DamengConstants;
import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.dbeaver.model.DBPObjectWithLongId;
import org.jkiss.dbeaver.model.DBPRefreshableObject;
import org.jkiss.dbeaver.model.DBPScriptObject;
import org.jkiss.dbeaver.model.access.DBARole;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Collection;
import java.util.Map;

/**
 * @author Shengkai Bai
 */
public class DamengRole implements DBARole, DBPScriptObject, DBPObjectWithLongId, DBPRefreshableObject {

    private DamengDataSource dataSource;

    private Long id;

    private String name;

    private Timestamp createTime;

    private UserPrivCache userPrivCache = new UserPrivCache();

    private SystemPrivCache systemPrivCache = new SystemPrivCache();

    private ObjectPrivCache objectPrivCache = new ObjectPrivCache();

    public DamengRole(DamengDataSource damengDataSource, JDBCResultSet resultSet) {
        this.dataSource = damengDataSource;
        this.id = JDBCUtils.safeGetLong(resultSet, DamengConstants.ID);
        this.name = JDBCUtils.safeGetString(resultSet, DamengConstants.NAME);
        this.createTime = JDBCUtils.safeGetTimestamp(resultSet, DamengConstants.CRTDATE);
    }

    @Override
    @Property(viewable = true, order = 1)
    public long getObjectId() {
        return id;
    }

    @Override
    @Property(viewable = true, order = 2)
    public String getName() {
        return name;
    }

    @Property(viewable = true, order = 3)
    public Timestamp getCreateTime() {
        return createTime;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public boolean isPersisted() {
        return true;
    }

    @Override
    public DBSObject getParentObject() {
        return getDataSource().getContainer();
    }

    @Override
    public DBPDataSource getDataSource() {
        return dataSource;
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        return DamengUtils.getDDL(monitor, this, DamengConstants.ObjectType.ROLE, null);
    }

    @Association
    public Collection<DamengPrivUser> getUserPrivs(DBRProgressMonitor monitor) throws DBException {
        return userPrivCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<DamengPrivSystem> getSystemPrivs(DBRProgressMonitor monitor) throws DBException {
        return systemPrivCache.getAllObjects(monitor, this);
    }

    @Association
    public Collection<DamengPrivObject> getObjectPrivs(DBRProgressMonitor monitor) throws DBException {
        return objectPrivCache.getAllObjects(monitor, this);
    }

    @Override
    public DBSObject refreshObject(@NotNull DBRProgressMonitor monitor) throws DBException {
        this.userPrivCache.clearCache();
        this.objectPrivCache.clearCache();
        this.systemPrivCache.clearCache();
        return this;
    }

    static class UserPrivCache extends JDBCObjectCache<DamengRole, DamengPrivUser> {

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DamengRole damengRole) throws SQLException {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                    "SELECT DISTINCT OBJ.ID,\n" +
                            "OBJ.NAME,\n" +
                            "GRANTS.GRANTABLE,\n" +
                            "OBJ.SUBTYPE$ FROM \n" +
                            "SYSOBJECTS OBJ,\n" +
                            "sysgrants GRANTS\n" +
                            "WHERE GRANTS.PRIVID = -1\n" +
                            "AND OBJ.TYPE$ = 'UR'\n" +
                            "AND OBJ.ID = GRANTS.URID\n" +
                            "AND GRANTS.OBJID = ? ");
            dbStat.setLong(1, damengRole.getObjectId());
            return dbStat;
        }

        @Override
        protected DamengPrivUser fetchObject(@NotNull JDBCSession session, @NotNull DamengRole damengRole, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new DamengPrivUser(damengRole, resultSet);
        }
    }

    static class SystemPrivCache extends JDBCObjectCache<DamengRole, DamengPrivSystem> {

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DamengRole damengRole) throws SQLException {
            final JDBCPreparedStatement dbStat = session.prepareStatement("SELECT\n" +
                    "PRIVID AS ID,\n" +
                    "SF_GET_SYS_PRIV(PRIVID) AS PRIV,\n" +
                    "GRANT_OBJ.NAME AS GRANTOR_NAME,\n" +
                    "GRANTABLE\n" +
                    "FROM\n" +
                    "SYSGRANTS GRANTS\n" +
                    "LEFT JOIN SYSOBJECTS GRANT_OBJ ON\n" +
                    "GRANTS.GRANTOR = GRANT_OBJ.ID\n" +
                    "WHERE\n" +
                    "NOT (OBJID != -1\n" +
                    "OR COLID != -1)\n" +
                    "AND PRIVID != -1\n" +
                    "AND URID = ?");
            dbStat.setLong(1, damengRole.getObjectId());
            return dbStat;
        }

        @Override
        protected DamengPrivSystem fetchObject(@NotNull JDBCSession session, @NotNull DamengRole damengRole, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new DamengPrivSystem(damengRole, resultSet);
        }
    }

    static class ObjectPrivCache extends JDBCObjectCache<DamengRole, DamengPrivObject> {

        @NotNull
        @Override
        protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DamengRole damengRole) throws SQLException {
            final JDBCPreparedStatement dbStat = session.prepareStatement("SELECT\n" +
                    "SF_GET_OBJ_FULL_NAME(OBJID,COLID) AS OBJECT_NAME,\n" +
                    "SF_GET_SYS_PRIV(PRIVID) AS PRIV,\n" +
                    "GRANTOR_OBJ.name AS GRANTOR_NAME,\n" +
                    "GRANTABLE,\n" +
                    "GRANT_OBJ.TYPE$ AS TYPE,\n" +
                    "DECODE(GRANT_OBJ.SUBTYPE$,'PROC',DECODE(GRANT_OBJ.INFO1 & 0X01,0,'FUNCTION',1,'PROCEDURE'),GRANT_OBJ.SUBTYPE$) AS SUB_TYPE\n" +
                    "FROM\n" +
                    "SYSGRANTS GRANTS,\n" +
                    "SYSOBJECTS GRANT_OBJ,\n" +
                    "SYSOBJECTS GRANTOR_OBJ\n" +
                    "WHERE\n" +
                    "GRANT_OBJ.ID = GRANTS.OBJID\n" +
                    "AND GRANTOR_OBJ.ID = GRANTS.GRANTOR\n" +
                    "AND (OBJID != -1 OR COLID != -1)\n" +
                    "AND PRIVID != -1\n" +
                    "AND URID = ?");
            dbStat.setLong(1, damengRole.getObjectId());
            return dbStat;
        }

        @Override
        protected DamengPrivObject fetchObject(@NotNull JDBCSession session, @NotNull DamengRole damengRole, @NotNull JDBCResultSet resultSet) throws SQLException, DBException {
            return new DamengPrivObject(damengRole, resultSet);
        }
    }
}

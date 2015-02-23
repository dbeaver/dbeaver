/*
 * Copyright (C) 2010-2015 Serge Rieder
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

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.core.Log;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.DBPSaveableObject;
import org.jkiss.dbeaver.model.access.DBAUser;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.meta.Association;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;

/**
 * OracleGrantee
 */
public abstract class OracleGrantee extends OracleGlobalObject implements DBAUser, DBPSaveableObject
{
    static final Log log = Log.getLog(OracleGrantee.class);

    final RolePrivCache rolePrivCache = new RolePrivCache();
    final SystemPrivCache systemPrivCache = new SystemPrivCache();
    final ObjectPrivCache objectPrivCache = new ObjectPrivCache();


    public OracleGrantee(OracleDataSource dataSource) {
        super(dataSource, true);
    }

    @Association
    public Collection<OraclePrivRole> getRolePrivs(DBRProgressMonitor monitor) throws DBException
    {
        return rolePrivCache.getObjects(monitor, this);
    }

    @Association
    public Collection<OraclePrivSystem> getSystemPrivs(DBRProgressMonitor monitor) throws DBException
    {
        return systemPrivCache.getObjects(monitor, this);
    }

    @Association
    public Collection<OraclePrivObject> getObjectPrivs(DBRProgressMonitor monitor) throws DBException
    {
        return objectPrivCache.getObjects(monitor, this);
    }

    static class RolePrivCache extends JDBCObjectCache<OracleGrantee, OraclePrivRole> {
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, OracleGrantee owner) throws SQLException
        {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM DBA_ROLE_PRIVS WHERE GRANTEE=? ORDER BY GRANTED_ROLE");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected OraclePrivRole fetchObject(JDBCSession session, OracleGrantee owner, ResultSet resultSet) throws SQLException, DBException
        {
            return new OraclePrivRole(owner, resultSet);
        }
    }

    static class SystemPrivCache extends JDBCObjectCache<OracleGrantee, OraclePrivSystem> {
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, OracleGrantee owner) throws SQLException
        {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT * FROM DBA_SYS_PRIVS WHERE GRANTEE=? ORDER BY PRIVILEGE");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected OraclePrivSystem fetchObject(JDBCSession session, OracleGrantee owner, ResultSet resultSet) throws SQLException, DBException
        {
            return new OraclePrivSystem(owner, resultSet);
        }
    }

    static class ObjectPrivCache extends JDBCObjectCache<OracleGrantee, OraclePrivObject> {
        @Override
        protected JDBCStatement prepareObjectsStatement(JDBCSession session, OracleGrantee owner) throws SQLException
        {
            final JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT p.*,o.OBJECT_TYPE\n" +
                "FROM DBA_TAB_PRIVS p, DBA_OBJECTS o\n" +
                "WHERE p.GRANTEE=? " +
                "AND o.OWNER=p.OWNER AND o.OBJECT_NAME=p.TABLE_NAME AND o.OBJECT_TYPE<>'PACKAGE BODY'");
            dbStat.setString(1, owner.getName());
            return dbStat;
        }

        @Override
        protected OraclePrivObject fetchObject(JDBCSession session, OracleGrantee owner, ResultSet resultSet) throws SQLException, DBException
        {
            return new OraclePrivObject(owner, resultSet);
        }
    }

}
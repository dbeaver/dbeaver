/*
 * Copyright (C) 2013      Denis Forveille titou10.titou10@gmail.com
 * Copyright (C) 2010-2013 Serge Rieder serge@jkiss.org
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
package org.jkiss.dbeaver.ext.db2.model.security;

import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Utils;
import org.jkiss.dbeaver.ext.db2.editors.DB2ObjectType;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.ext.db2.model.DB2Index;
import org.jkiss.dbeaver.ext.db2.model.DB2Package;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.ext.db2.model.DB2Sequence;
import org.jkiss.dbeaver.ext.db2.model.DB2TableBase;
import org.jkiss.dbeaver.ext.db2.model.DB2Tablespace;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Cache for DB2 User/Group/Roles Authorisations
 * 
 * @author Denis Forveille
 */
public final class DB2UserAuthCache extends JDBCObjectCache<DB2UserBase, DB2UserAuthBase> {

    private static String SQL;

    // TODO DF: Add missing auth: modules, functions, columns etc..

    static {

        // Auth Columns:
        // 8 cols : CONTROLAUTH ALTERAUTH DELETEAUTH INDEXAUTH INSERTAUTH REFAUTH SELECTAUTH UPDATEAUTH
        // 6 cols : USAGEAUTH ALTERINAUTH CREATEINAUTH DROPINAUTH BINDAUTH EXECUTEAUTH

        StringBuilder sb = new StringBuilder(2048);

        sb.append("SELECT GRANTOR,GRANTORTYPE");
        sb.append("     , '").append(DB2ObjectType.TABLE.name()).append("' AS OBJ_TYPE");
        sb.append("     , TABSCHEMA AS OBJ_SCHEMA, TABNAME AS OBJ_NAME");
        sb.append("     , CONTROLAUTH, ALTERAUTH, DELETEAUTH, INDEXAUTH, INSERTAUTH, REFAUTH, SELECTAUTH, UPDATEAUTH");
        sb.append("     , NULL AS USAGEAUTH, NULL AS ALTERINAUTH, NULL AS CREATEINAUTH, NULL AS DROPINAUTH, NULL AS BINDAUTH, NULL AS EXECUTEAUTH");
        sb.append("  FROM SYSCAT.TABAUTH");
        sb.append(" WHERE GRANTEETYPE = ?"); // 1
        sb.append("   AND GRANTEE = ?"); // 2

        sb.append(" UNION ALL ");

        sb.append("SELECT GRANTOR,GRANTORTYPE");
        sb.append("     , '").append(DB2ObjectType.INDEX.name()).append("' AS OBJ_TYPE");
        sb.append("     , INDSCHEMA AS OBJ_SCHEMA, INDNAME AS OBJ_NAME");
        sb.append("     , CONTROLAUTH, NULL, NULL, NULL, NULL, NULL, NULL, NULL");
        sb.append("     , NULL, NULL, NULL, NULL, NULL, NULL");
        sb.append("  FROM SYSCAT.INDEXAUTH");
        sb.append(" WHERE GRANTEETYPE = ?");// 3
        sb.append("   AND GRANTEE = ?");// 4

        sb.append(" UNION ALL ");

        sb.append("SELECT GRANTOR,GRANTORTYPE");
        sb.append("     , '").append(DB2ObjectType.SEQUENCE.name()).append("' AS OBJ_TYPE");
        sb.append("     , SEQSCHEMA AS OBJ_SCHEMA, SEQNAME AS OBJ_NAME");
        sb.append("     , NULL, ALTERAUTH, NULL, NULL, NULL, NULL, NULL, NULL");
        sb.append("     , USAGEAUTH, NULL, NULL, NULL, NULL, NULL");
        sb.append("  FROM SYSCAT.SEQUENCEAUTH");
        sb.append(" WHERE GRANTEETYPE = ?");// 5
        sb.append("   AND GRANTEE = ?");// 6

        sb.append(" UNION ALL ");

        sb.append("SELECT GRANTOR,GRANTORTYPE");
        sb.append("     , '").append(DB2ObjectType.TABLESPACE.name()).append("' AS OBJ_TYPE");
        sb.append("     , NULL AS OBJ_SCHEMA, TBSPACE AS OBJ_NAME");
        sb.append("     , NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL");
        sb.append("     , USEAUTH, NULL, NULL, NULL, NULL, NULL");
        sb.append("  FROM SYSCAT.TBSPACEAUTH");
        sb.append(" WHERE GRANTEETYPE = ?");// 7
        sb.append("   AND GRANTEE = ?");// 8

        sb.append(" UNION ALL ");

        sb.append("SELECT GRANTOR,GRANTORTYPE");
        sb.append("     , '").append(DB2ObjectType.SCHEMA.name()).append("' AS OBJ_TYPE");
        sb.append("     , NULL AS OBJ_SCHEMA, SCHEMANAME AS OBJ_NAME");
        sb.append("     , NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL");
        sb.append("     , NULL, ALTERINAUTH, CREATEINAUTH, DROPINAUTH, NULL, NULL");
        sb.append("  FROM SYSCAT.SCHEMAAUTH");
        sb.append(" WHERE GRANTEETYPE = ?");// 9
        sb.append("   AND GRANTEE = ?");// 10

        sb.append(" UNION ALL ");

        sb.append("SELECT GRANTOR,GRANTORTYPE");
        sb.append("     , '").append(DB2ObjectType.PACKAGE.name()).append("' AS OBJ_TYPE");
        sb.append("     , PKGSCHEMA AS OBJ_SCHEMA, PKGNAME AS OBJ_NAME");
        sb.append("     , CONTROLAUTH, NULL, NULL, NULL, NULL, NULL, NULL, NULL");
        sb.append("     , NULL, NULL, NULL, NULL, BINDAUTH, EXECUTEAUTH");
        sb.append("  FROM SYSCAT.PACKAGEAUTH");
        sb.append(" WHERE GRANTEETYPE = ?");// 11
        sb.append("   AND GRANTEE = ?");// 12

        sb.append(" ORDER BY OBJ_SCHEMA, OBJ_NAME, OBJ_TYPE");
        sb.append(" WITH UR");

        SQL = sb.toString();
    }

    @Override
    protected JDBCStatement prepareObjectsStatement(JDBCExecutionContext context, DB2UserBase db2UserBase) throws SQLException
    {
        String userType = db2UserBase.getType().name();
        String userName = db2UserBase.getName();

        JDBCPreparedStatement dbStat = context.prepareStatement(SQL);
        dbStat.setString(1, userType);
        dbStat.setString(2, userName);
        dbStat.setString(3, userType);
        dbStat.setString(4, userName);
        dbStat.setString(5, userType);
        dbStat.setString(6, userName);
        dbStat.setString(7, userType);
        dbStat.setString(8, userName);
        dbStat.setString(9, userType);
        dbStat.setString(10, userName);
        dbStat.setString(11, userType);
        dbStat.setString(12, userName);
        return dbStat;
    }

    @Override
    protected DB2UserAuthBase fetchObject(JDBCExecutionContext context, DB2UserBase db2UserBase, ResultSet resultSet)
        throws SQLException, DBException
    {
        DB2DataSource db2DataSource = db2UserBase.getDataSource();
        DBRProgressMonitor monitor = context.getProgressMonitor();

        String objectSchemaName = JDBCUtils.safeGetStringTrimmed(resultSet, "OBJ_SCHEMA");
        String objectName = JDBCUtils.safeGetStringTrimmed(resultSet, "OBJ_NAME");

        DB2ObjectType objectType = CommonUtils.valueOf(DB2ObjectType.class, JDBCUtils.safeGetString(resultSet, "OBJ_TYPE"));

        switch (objectType) {
        case TABLE:
            // Can be a Table or a View..
            DB2TableBase db2TableBase = DB2Utils.findTableBySchemaNameAndName(monitor, db2DataSource, objectSchemaName, objectName);
            if (db2TableBase == null) {
                db2TableBase = DB2Utils.findViewBySchemaNameAndName(monitor, db2DataSource, objectSchemaName, objectName);
                return new DB2UserAuthView(monitor, db2UserBase, db2TableBase, resultSet);
            } else {
                return new DB2UserAuthTable(monitor, db2UserBase, db2TableBase, resultSet);
            }

        case INDEX:
            DB2Index db2Index = DB2Utils.findIndexBySchemaNameAndName(monitor, db2DataSource, objectSchemaName, objectName);
            return new DB2UserAuthIndex(monitor, db2UserBase, db2Index, resultSet);

        case SCHEMA:
            DB2Schema db2SChema = db2DataSource.getSchema(monitor, objectName);
            return new DB2UserAuthSchema(monitor, db2UserBase, db2SChema, resultSet);

        case PACKAGE:
            DB2Package db2Package = DB2Utils.findPackageBySchemaNameAndName(monitor, db2DataSource, objectSchemaName, objectName);
            return new DB2UserAuthPackage(monitor, db2UserBase, db2Package, resultSet);

        case SEQUENCE:
            DB2Sequence db2Sequence = DB2Utils
                .findSequenceBySchemaNameAndName(monitor, db2DataSource, objectSchemaName, objectName);
            return new DB2UserAuthSequence(monitor, db2UserBase, db2Sequence, resultSet);

        case TABLESPACE:
            DB2Tablespace db2Tablespace = db2DataSource.getTablespace(monitor, objectName);
            return new DB2UserAuthTablespace(monitor, db2UserBase, db2Tablespace, resultSet);

        default:
            throw new DBException("Structural problem. " + objectType + " autorisation not implemented");
        }

    }
}
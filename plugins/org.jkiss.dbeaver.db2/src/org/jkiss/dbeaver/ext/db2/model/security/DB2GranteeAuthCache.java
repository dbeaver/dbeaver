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
import org.jkiss.dbeaver.ext.db2.model.DB2Variable;
import org.jkiss.dbeaver.ext.db2.model.module.DB2Module;
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
 * Cache for DB2 Authorisations
 * 
 * @author Denis Forveille
 */
public final class DB2GranteeAuthCache extends JDBCObjectCache<DB2Grantee, DB2AuthBase> {

    private static String SQL;

    // TODO DF: Add missing auth: functions, columns etc..

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

        sb.append(" UNION ALL ");

        sb.append("SELECT GRANTOR,GRANTORTYPE");
        sb.append("     , '").append(DB2ObjectType.MODULE.name()).append("' AS OBJ_TYPE");
        sb.append("     , MODULESCHEMA AS OBJ_SCHEMA, MODULENAME AS OBJ_NAME");
        sb.append("     , NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL");
        sb.append("     , NULL, NULL, NULL, NULL, NULL, EXECUTEAUTH");
        sb.append("  FROM SYSCAT.MODULEAUTH");
        sb.append(" WHERE GRANTEETYPE = ?");// 13
        sb.append("   AND GRANTEE = ?");// 14

        sb.append(" UNION ALL ");

        // READAUTH in USAGEAUTH
        // WRITEAUTH in ALTERINAUTH
        sb.append("SELECT GRANTOR,GRANTORTYPE");
        sb.append("     , '").append(DB2ObjectType.VARIABLE.name()).append("' AS OBJ_TYPE");
        sb.append("     , VARSCHEMA AS OBJ_SCHEMA, VARNAME AS OBJ_NAME");
        sb.append("     , NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL");
        sb.append("     , READAUTH AS USAGEAUTH, WRITEAUTH AS ALTERINAUTH, NULL, NULL, NULL, NULL");
        sb.append("  FROM SYSCAT.VARIABLEAUTH");
        sb.append(" WHERE GRANTEETYPE = ?");// 15
        sb.append("   AND GRANTEE = ?");// 16

        sb.append(" ORDER BY OBJ_SCHEMA, OBJ_NAME, OBJ_TYPE");
        sb.append(" WITH UR");

        SQL = sb.toString();
    }

    @Override
    protected JDBCStatement prepareObjectsStatement(JDBCExecutionContext context, DB2Grantee db2Grantee) throws SQLException
    {
        String userType = db2Grantee.getType().name();
        String userName = db2Grantee.getName();

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
        dbStat.setString(13, userType);
        dbStat.setString(14, userName);
        dbStat.setString(15, userType);
        dbStat.setString(16, userName);
        return dbStat;
    }

    @Override
    protected DB2AuthBase fetchObject(JDBCExecutionContext context, DB2Grantee db2Grantee, ResultSet resultSet)
        throws SQLException, DBException
    {
        DB2DataSource db2DataSource = db2Grantee.getDataSource();
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
                return new DB2AuthView(monitor, db2Grantee, db2TableBase, resultSet);
            } else {
                return new DB2AuthTable(monitor, db2Grantee, db2TableBase, resultSet);
            }

        case INDEX:
            DB2Index db2Index = DB2Utils.findIndexBySchemaNameAndName(monitor, db2DataSource, objectSchemaName, objectName);
            return new DB2AuthIndex(monitor, db2Grantee, db2Index, resultSet);

        case MODULE:
            DB2Module db2Module = DB2Utils.findModuleBySchemaNameAndName(monitor, db2DataSource, objectSchemaName, objectName);
            return new DB2AuthModule(monitor, db2Grantee, db2Module, resultSet);

        case SCHEMA:
            DB2Schema db2Schema = db2DataSource.getSchema(monitor, objectName);
            return new DB2AuthSchema(monitor, db2Grantee, db2Schema, resultSet);

        case PACKAGE:
            DB2Package db2Package = DB2Utils.findPackageBySchemaNameAndName(monitor, db2DataSource, objectSchemaName, objectName);
            return new DB2AuthPackage(monitor, db2Grantee, db2Package, resultSet);

        case SEQUENCE:
            DB2Sequence db2Sequence = DB2Utils
                .findSequenceBySchemaNameAndName(monitor, db2DataSource, objectSchemaName, objectName);
            return new DB2AuthSequence(monitor, db2Grantee, db2Sequence, resultSet);

        case TABLESPACE:
            DB2Tablespace db2Tablespace = db2DataSource.getTablespace(monitor, objectName);
            return new DB2AuthTablespace(monitor, db2Grantee, db2Tablespace, resultSet);

        case VARIABLE:
            DB2Variable db2Variable = db2DataSource.getVariable(monitor, objectName);
            return new DB2AuthVariable(monitor, db2Grantee, db2Variable, resultSet);

        default:
            throw new DBException("Structural problem. " + objectType + " autorisation not implemented");
        }

    }
}
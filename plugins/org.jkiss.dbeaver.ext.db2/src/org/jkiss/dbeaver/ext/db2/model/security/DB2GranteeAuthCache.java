/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2016 Denis Forveille (titou10.titou10@gmail.com)
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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
package org.jkiss.dbeaver.ext.db2.model.security;

import java.sql.SQLException;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Utils;
import org.jkiss.dbeaver.ext.db2.editors.DB2ObjectType;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.ext.db2.model.DB2Index;
import org.jkiss.dbeaver.ext.db2.model.DB2Package;
import org.jkiss.dbeaver.ext.db2.model.DB2Routine;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.ext.db2.model.DB2Sequence;
import org.jkiss.dbeaver.ext.db2.model.DB2TableBase;
import org.jkiss.dbeaver.ext.db2.model.DB2TableColumn;
import org.jkiss.dbeaver.ext.db2.model.DB2Tablespace;
import org.jkiss.dbeaver.ext.db2.model.DB2Variable;
import org.jkiss.dbeaver.ext.db2.model.DB2XMLSchema;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2RoutineType;
import org.jkiss.dbeaver.ext.db2.model.module.DB2Module;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCStatement;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCObjectCache;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

/**
 * Cache for DB2 Authorisations
 * 
 * @author Denis Forveille
 */
public final class DB2GranteeAuthCache extends JDBCObjectCache<DB2Grantee, DB2AuthBase> {

    private static final String SQL;
    private static final String SQL_WITHOUT_MODULE;

    static {

        // Auth Columns:
        // 8 cols : CONTROLAUTH ALTERAUTH DELETEAUTH INDEXAUTH INSERTAUTH REFAUTH SELECTAUTH UPDATEAUTH
        // 6 cols : USAGEAUTH ALTERINAUTH CREATEINAUTH DROPINAUTH BINDAUTH EXECUTEAUTH

        StringBuilder sb = new StringBuilder(4096);

        sb.append("SELECT GRANTOR,GRANTORTYPE");
        sb.append("     , '").append(DB2ObjectType.TABLE.name()).append("' AS OBJ_TYPE");
        sb.append("     , TABSCHEMA AS OBJ_SCHEMA, TABNAME AS OBJ_NAME");
        sb.append("     , CONTROLAUTH, ALTERAUTH, DELETEAUTH, INDEXAUTH, INSERTAUTH, REFAUTH, SELECTAUTH, UPDATEAUTH");
        sb.append(
            "     , NULL AS USAGEAUTH, NULL AS ALTERINAUTH, NULL AS CREATEINAUTH, NULL AS DROPINAUTH, NULL AS BINDAUTH, NULL AS EXECUTEAUTH");
        sb.append("  FROM SYSCAT.TABAUTH");
        sb.append(" WHERE GRANTEETYPE = ?"); // 1
        sb.append("   AND GRANTEE = ?"); // 2

        sb.append(" UNION ALL ");

        // COLNAME in USAGEAUTH
        // PRIVTYPE in ALTERINAUTH
        // GRANTABLE in ALTERINAUTH
        sb.append("SELECT GRANTOR,GRANTORTYPE");
        sb.append("     , '").append(DB2ObjectType.COLUMN.name()).append("' AS OBJ_TYPE");
        sb.append("     , TABSCHEMA AS OBJ_SCHEMA, TABNAME AS OBJ_NAME");
        sb.append("     , NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL");
        sb.append("     , COLNAME, PRIVTYPE, GRANTABLE, NULL, NULL, NULL");
        sb.append("  FROM SYSCAT.COLAUTH ");
        sb.append(" WHERE GRANTEETYPE = ?");// 3
        sb.append("   AND GRANTEE = ?");// 4

        sb.append(" UNION ALL ");

        sb.append("SELECT GRANTOR,GRANTORTYPE");
        sb.append("     , '").append(DB2ObjectType.INDEX.name()).append("' AS OBJ_TYPE");
        sb.append("     , INDSCHEMA AS OBJ_SCHEMA, INDNAME AS OBJ_NAME");
        sb.append("     , CONTROLAUTH, NULL, NULL, NULL, NULL, NULL, NULL, NULL");
        sb.append("     , NULL, NULL, NULL, NULL, NULL, NULL");
        sb.append("  FROM SYSCAT.INDEXAUTH");
        sb.append(" WHERE GRANTEETYPE = ?");// 5
        sb.append("   AND GRANTEE = ?");// 6

        sb.append(" UNION ALL ");

        sb.append("SELECT GRANTOR,GRANTORTYPE");
        sb.append("     , '").append(DB2ObjectType.PACKAGE.name()).append("' AS OBJ_TYPE");
        sb.append("     , PKGSCHEMA AS OBJ_SCHEMA, PKGNAME AS OBJ_NAME");
        sb.append("     , CONTROLAUTH, NULL, NULL, NULL, NULL, NULL, NULL, NULL");
        sb.append("     , NULL, NULL, NULL, NULL, BINDAUTH, EXECUTEAUTH");
        sb.append("  FROM SYSCAT.PACKAGEAUTH");
        sb.append(" WHERE GRANTEETYPE = ?");// 7
        sb.append("   AND GRANTEE = ?");// 8

        sb.append(" UNION ALL ");

        // ROUTINETYPE in USAGEAUTH
        sb.append("SELECT GRANTOR,GRANTORTYPE");
        sb.append("     , '").append(DB2ObjectType.PROCEDURE.name()).append("' AS OBJ_TYPE"); // PROCEDURE or FUNCTION or METHOD
        sb.append("     , SCHEMA AS OBJ_SCHEMA, SPECIFICNAME AS OBJ_NAME");
        sb.append("     , NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL");
        sb.append("     , ROUTINETYPE, NULL, NULL, NULL, NULL, EXECUTEAUTH");
        sb.append("  FROM SYSCAT.ROUTINEAUTH");
        sb.append(" WHERE GRANTEETYPE = ?");// 9
        sb.append("   AND GRANTEE = ?");// 10

        sb.append(" UNION ALL ");

        sb.append("SELECT GRANTOR,GRANTORTYPE");
        sb.append("     , '").append(DB2ObjectType.SCHEMA.name()).append("' AS OBJ_TYPE");
        sb.append("     , NULL AS OBJ_SCHEMA, SCHEMANAME AS OBJ_NAME");
        sb.append("     , NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL");
        sb.append("     , NULL, ALTERINAUTH, CREATEINAUTH, DROPINAUTH, NULL, NULL");
        sb.append("  FROM SYSCAT.SCHEMAAUTH");
        sb.append(" WHERE GRANTEETYPE = ?");// 11
        sb.append("   AND GRANTEE = ?");// 12

        sb.append(" UNION ALL ");

        sb.append("SELECT GRANTOR,GRANTORTYPE");
        sb.append("     , '").append(DB2ObjectType.SEQUENCE.name()).append("' AS OBJ_TYPE");
        sb.append("     , SEQSCHEMA AS OBJ_SCHEMA, SEQNAME AS OBJ_NAME");
        sb.append("     , NULL, ALTERAUTH, NULL, NULL, NULL, NULL, NULL, NULL");
        sb.append("     , USAGEAUTH, NULL, NULL, NULL, NULL, NULL");
        sb.append("  FROM SYSCAT.SEQUENCEAUTH");
        sb.append(" WHERE GRANTEETYPE = ?");// 13
        sb.append("   AND GRANTEE = ?");// 14

        sb.append(" UNION ALL ");

        sb.append("SELECT GRANTOR,GRANTORTYPE");
        sb.append("     , '").append(DB2ObjectType.TABLESPACE.name()).append("' AS OBJ_TYPE");
        sb.append("     , NULL AS OBJ_SCHEMA, TBSPACE AS OBJ_NAME");
        sb.append("     , NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL");
        sb.append("     , USEAUTH, NULL, NULL, NULL, NULL, NULL");
        sb.append("  FROM SYSCAT.TBSPACEAUTH");
        sb.append(" WHERE GRANTEETYPE = ?");// 15
        sb.append("   AND GRANTEE = ?");// 16

        sb.append(" UNION ALL ");

        // READAUTH in USAGEAUTH
        // WRITEAUTH in ALTERINAUTH
        sb.append("SELECT GRANTOR,GRANTORTYPE");
        sb.append("     , '").append(DB2ObjectType.VARIABLE.name()).append("' AS OBJ_TYPE");
        sb.append("     , VARSCHEMA AS OBJ_SCHEMA, VARNAME AS OBJ_NAME");
        sb.append("     , NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL");
        sb.append("     , READAUTH AS USAGEAUTH, WRITEAUTH AS ALTERINAUTH, NULL, NULL, NULL, NULL");
        sb.append("  FROM SYSCAT.VARIABLEAUTH");
        sb.append(" WHERE GRANTEETYPE = ?");// 17
        sb.append("   AND GRANTEE = ?");// 18

        sb.append(" UNION ALL ");

        // OBJECTID as string in OBJ_NAME
        sb.append("SELECT GRANTOR,GRANTORTYPE");
        sb.append("     , '").append(DB2ObjectType.XML_SCHEMA.name()).append("' AS OBJ_TYPE");
        sb.append("     , NULL AS OBJ_SCHEMA, CAST(OBJECTID AS VARCHAR(32)) AS OBJ_NAME");
        sb.append("     , NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL");
        sb.append("     , USAGEAUTH, NULL , NULL, NULL, NULL, NULL");
        sb.append("  FROM SYSCAT.XSROBJECTAUTH ");
        sb.append(" WHERE GRANTEETYPE = ?");// 19
        sb.append("   AND GRANTEE = ?");// 20

        StringBuilder sb2 = new StringBuilder(512);
        sb2.append(" UNION ALL ");
        sb2.append("SELECT GRANTOR,GRANTORTYPE");
        sb2.append("     , '").append(DB2ObjectType.MODULE.name()).append("' AS OBJ_TYPE");
        sb2.append("     , MODULESCHEMA AS OBJ_SCHEMA, MODULENAME AS OBJ_NAME");
        sb2.append("     , NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL");
        sb2.append("     , NULL, NULL, NULL, NULL, NULL, EXECUTEAUTH");
        sb2.append("  FROM SYSCAT.MODULEAUTH");
        sb2.append(" WHERE GRANTEETYPE = ?");// 21
        sb2.append("   AND GRANTEE = ?");// 22

        StringBuilder sb3 = new StringBuilder(64);
        sb3.append(" ORDER BY OBJ_SCHEMA, OBJ_NAME, OBJ_TYPE");
        sb3.append(" WITH UR");

        SQL = sb.toString() + sb2.toString() + sb3.toString();
        SQL_WITHOUT_MODULE = sb.toString() + sb3.toString();
    }

    @Override
    protected JDBCStatement prepareObjectsStatement(@NotNull JDBCSession session, @NotNull DB2Grantee db2Grantee)
        throws SQLException
    {
        String userType = db2Grantee.getType().name();
        String userName = db2Grantee.getName();

        String sql;
        int nbMax;
        if (db2Grantee.getDataSource().isAtLeastV9_7()) {
            sql = SQL;
            nbMax = 22;
        } else {
            sql = SQL_WITHOUT_MODULE;
            nbMax = 20;
        }

        JDBCPreparedStatement dbStat = session.prepareStatement(sql);
        for (int i = 1; i <= nbMax;) {
            dbStat.setString(i++, userType);
            dbStat.setString(i++, userName);
        }
        return dbStat;
    }

    @Override
    protected DB2AuthBase fetchObject(@NotNull JDBCSession session, @NotNull DB2Grantee db2Grantee,
        @NotNull JDBCResultSet resultSet) throws SQLException, DBException
    {
        DB2DataSource db2DataSource = db2Grantee.getDataSource();
        DBRProgressMonitor monitor = session.getProgressMonitor();

        String objectSchemaName = JDBCUtils.safeGetStringTrimmed(resultSet, "OBJ_SCHEMA");
        String objectName = JDBCUtils.safeGetStringTrimmed(resultSet, "OBJ_NAME");

        DB2ObjectType objectType = CommonUtils.valueOf(DB2ObjectType.class, JDBCUtils.safeGetString(resultSet, "OBJ_TYPE"));

        switch (objectType) {
        case COLUMN:
            String columnName = JDBCUtils.safeGetStringTrimmed(resultSet, "USAGEAUTH");
            DB2TableColumn db2TableColumn = DB2Utils.findColumnBySchemaNameAndTableNameAndName(monitor, db2DataSource,
                objectSchemaName, objectName, columnName);
            return new DB2AuthColumn(monitor, db2Grantee, db2TableColumn, resultSet);

        case INDEX:
            DB2Index db2Index = DB2Utils.findIndexBySchemaNameAndName(monitor, db2DataSource, objectSchemaName, objectName);
            return new DB2AuthIndex(monitor, db2Grantee, db2Index, resultSet);

        case MODULE:
            DB2Module db2Module = DB2Utils.findModuleBySchemaNameAndName(monitor, db2DataSource, objectSchemaName, objectName);
            return new DB2AuthModule(monitor, db2Grantee, db2Module, resultSet);

        case PACKAGE:
            DB2Package db2Package = DB2Utils.findPackageBySchemaNameAndName(monitor, db2DataSource, objectSchemaName, objectName);
            return new DB2AuthPackage(monitor, db2Grantee, db2Package, resultSet);

        case PROCEDURE:
            // Can be a Function or a Procedure
            DB2RoutineType routineType = CommonUtils.valueOf(DB2RoutineType.class,
                JDBCUtils.safeGetStringTrimmed(resultSet, "USAGEAUTH"));

            switch (routineType) {
            case F:
                DB2Routine db2Udf = DB2Utils.findUDFBySchemaNameAndName(monitor, db2DataSource, objectSchemaName, objectName);
                return new DB2AuthUDF(monitor, db2Grantee, db2Udf, resultSet);
            case M:
                DB2Routine db2Method = DB2Utils.findMethodBySchemaNameAndName(monitor, db2DataSource, objectSchemaName, objectName);
                return new DB2AuthMethod(monitor, db2Grantee, db2Method, resultSet);
            case P:
                DB2Routine db2Procedure = DB2Utils.findProcedureBySchemaNameAndName(monitor, db2DataSource, objectSchemaName,
                    objectName);
                return new DB2AuthProcedure(monitor, db2Grantee, db2Procedure, resultSet);
            default:
                throw new DBException(routineType + " is not a valid DB2RoutineType");
            }

        case SCHEMA:
            DB2Schema db2Schema = db2DataSource.getSchema(monitor, objectName);
            return new DB2AuthSchema(monitor, db2Grantee, db2Schema, resultSet);

        case SEQUENCE:
            DB2Sequence db2Sequence = DB2Utils.findSequenceBySchemaNameAndName(monitor, db2DataSource, objectSchemaName,
                objectName);
            return new DB2AuthSequence(monitor, db2Grantee, db2Sequence, resultSet);

        case TABLE:
            // Can be a Table, a View or an MQT..
            DB2TableBase db2TableBase = DB2Utils.findTableBySchemaNameAndName(monitor, db2DataSource, objectSchemaName, objectName);
            if (db2TableBase != null) {
                return new DB2AuthTable(monitor, db2Grantee, db2TableBase, resultSet);
            } else {
                db2TableBase = DB2Utils.findViewBySchemaNameAndName(monitor, db2DataSource, objectSchemaName, objectName);
                if (db2TableBase != null) {
                    return new DB2AuthView(monitor, db2Grantee, db2TableBase, resultSet);
                } else {
                    db2TableBase = DB2Utils.findMQTBySchemaNameAndName(monitor, db2DataSource, objectSchemaName, objectName);
                    return new DB2AuthMaterializedQueryTable(monitor, db2Grantee, db2TableBase, resultSet);
                }
            }

        case TABLESPACE:
            DB2Tablespace db2Tablespace = db2DataSource.getTablespace(monitor, objectName);
            return new DB2AuthTablespace(monitor, db2Grantee, db2Tablespace, resultSet);

        case VARIABLE:
            DB2Variable db2Variable = db2DataSource.getVariable(monitor, objectName);
            return new DB2AuthVariable(monitor, db2Grantee, db2Variable, resultSet);

        case XML_SCHEMA:
            Long xmlSchemaId = Long.valueOf(objectName);
            DB2XMLSchema db2XmlSchema = DB2Utils.findXMLSchemaByById(monitor, db2DataSource, xmlSchemaId);
            return new DB2AuthXMLSchema(monitor, db2Grantee, db2XmlSchema, resultSet);

        default:
            throw new DBException(
                "Programming error: " + objectType + " is not supported yet and the SELECT statement must exclude it");
        }
    }
}
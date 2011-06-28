/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.struct.DBSEntityQualified;

import java.sql.SQLException;

/**
 * Oracle utils
 */
public class OracleUtils {

    static final Log log = LogFactory.getLog(OracleUtils.class);

    public static String getDDL(
        DBRProgressMonitor monitor,
        String objectType,
        DBSEntity object) throws DBCException
    {
        String objectName = object.getName();
        String objectFullName = object instanceof DBSEntityQualified ?
            ((DBSEntityQualified)object).getFullQualifiedName() : objectName;
        OracleSchema schema = null;
        if (object instanceof OracleSchemaObject) {
            schema = ((OracleSchemaObject)object).getSchema();
        } else if (object instanceof OracleTableBase) {
            schema = ((OracleTableBase)object).getContainer();
        }
        final OracleDataSource dataSource = (OracleDataSource) object.getDataSource();
        monitor.beginTask("Load sources for " + objectType + " '" + objectFullName + "'...", 1);
        final JDBCExecutionContext context = dataSource.openContext(
            monitor,
            DBCExecutionPurpose.META,
            "Load source code for " + objectType + " '" + objectFullName + "'");
        try {
            final JDBCPreparedStatement dbStat = context.prepareStatement(
                "SELECT DBMS_METADATA.GET_DDL(?,?" +
                (schema == null ? "": ",?") +
                ") TXT " +
                "FROM DUAL");
            try {
                dbStat.setString(1, objectType);
                dbStat.setString(2, object.getName());
                if (schema != null) {
                    dbStat.setString(3, schema.getName());
                }
                final JDBCResultSet dbResult = dbStat.executeQuery();
                try {
                    if (dbResult.next()) {
                        return dbResult.getString(1);
                    } else {
                        log.warn("No DDL for " + objectType + " '" + objectFullName + "'");
                        return "EMPTY DDL";
                    }
                }
                finally {
                    dbResult.close();
                }
            }
            finally {
                dbStat.close();
            }
        } catch (SQLException e) {
            throw new DBCException(e);
        } finally {
            context.close();
            monitor.done();
        }
    }

    public static String getSource(DBRProgressMonitor monitor, OracleSourceObject sourceObject, boolean body) throws DBCException
    {
        final String sourceType = sourceObject.getSourceType().name();
        final OracleSchema sourceOwner = sourceObject.getSourceOwner();
        if (sourceOwner == null) {
            log.warn("No source owner for object '" + sourceObject.getName() + "'");
            return null;
        }
        monitor.beginTask("Load sources for '" + sourceObject.getName() + "'...", 1);
        final JDBCExecutionContext context = sourceOwner.getDataSource().openContext(monitor, DBCExecutionPurpose.META, "Load source code for " + sourceType + " '" + sourceObject.getName() + "'");
        try {
            final JDBCPreparedStatement dbStat = context.prepareStatement(
                "SELECT TEXT FROM SYS.ALL_SOURCE " +
                    "WHERE TYPE=? AND OWNER=? AND NAME=? " +
                    "ORDER BY LINE");
            try {
                dbStat.setString(1, body ? sourceType + " BODY" : sourceType);
                dbStat.setString(2, sourceOwner.getName());
                dbStat.setString(3, sourceObject.getName());
                dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);
                final JDBCResultSet dbResult = dbStat.executeQuery();
                try {
                    StringBuilder source = null;
                    int lineCount = 0;
                    while (dbResult.next()) {
                        if (monitor.isCanceled()) {
                            break;
                        }
                        final String line = dbResult.getString(1);
                        if (source == null) {
                            source = new StringBuilder(200);
                        }
                        source.append(line);
                        lineCount++;
                        monitor.subTask("Line " + lineCount);
                    }
                    return source == null ? null : source.toString();
                }
                finally {
                    dbResult.close();
                }
            }
            finally {
                dbStat.close();
            }
        } catch (SQLException e) {
            throw new DBCException(e);
        } finally {
            context.close();
            monitor.done();
        }
    }

    public static String getAdminViewPrefix(OracleDataSource dataSource)
    {
        return dataSource.isAdmin() ? "SYS.DBA_" : "SYS.USER_";
    }

}

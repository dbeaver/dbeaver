/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle;

import net.sf.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.oracle.model.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSDataType;
import org.jkiss.dbeaver.model.struct.DBSObject;

import java.sql.SQLException;

/**
 * Oracle utils
 */
public class OracleUtils {

    static final Log log = LogFactory.getLog(OracleUtils.class);

    public static OracleDataType resolveDataType(DBRProgressMonitor monitor, OracleDataSource dataSource, String typeOwner, String typeName)
    {
        OracleDataType type = null;
        if (typeOwner != null) {
            try {
                final OracleSchema typeSchema = dataSource.getSchema(monitor, typeOwner);
                if (typeSchema == null) {
                    log.error("Type attr schema '" + typeOwner + "' not found");
                } else {
                    type = typeSchema.getDataType(monitor, typeName);
                }
            } catch (DBException e) {
                log.error(e);
            }
        } else {
            type = (OracleDataType)dataSource.getDataType(typeName);
        }
        if (type == null) {
            log.error("Data type '" + typeName + "' not found");
        }
        return type;
    }

    public static OracleDataTypeModifier resolveTypeModifier(String typeMod)
    {
        if (!CommonUtils.isEmpty(typeMod)) {
            try {
                return OracleDataTypeModifier.valueOf(typeMod);
            } catch (IllegalArgumentException e) {
                log.error(e);
            }
        }
        return null;
    }

    public static String getSource(DBRProgressMonitor monitor, OracleSourceObject sourceObject, boolean body) throws DBCException
    {
        final String sourceType = sourceObject.getSourceType();
        final JDBCExecutionContext context = sourceObject.getSourceOwner().getDataSource().openContext(monitor, DBCExecutionPurpose.META, "Load source code for " + sourceType + " '" + sourceObject.getName() + "'");
        try {
            final JDBCPreparedStatement dbStat = context.prepareStatement(
                "SELECT TEXT FROM SYS.ALL_SOURCE " +
                    "WHERE TYPE=? AND OWNER=? AND NAME=? " +
                    "ORDER BY LINE");
            try {
                dbStat.setString(1, body ? sourceType + " BODY" : sourceType);
                dbStat.setString(2, sourceObject.getSourceOwner().getName());
                dbStat.setString(3, sourceObject.getName());
                final JDBCResultSet dbResult = dbStat.executeQuery();
                try {
                    StringBuilder source = null;
                    while (dbResult.next()) {
                        final String line = dbResult.getString(1);
                        if (source == null) {
                            source = new StringBuilder(200);
                        }
                        source.append(line);
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
        }
    }

}

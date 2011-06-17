/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.cache.JDBCAbstractCache;
import org.jkiss.dbeaver.model.impl.struct.RelationalObjectType;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectType;
import org.jkiss.dbeaver.model.struct.DBSStructureAssistant;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * OracleStructureAssistant
 */
public class OracleStructureAssistant implements DBSStructureAssistant
{
    static protected final Log log = LogFactory.getLog(OracleStructureAssistant.class);

    private final OracleDataSource dataSource;

    public OracleStructureAssistant(OracleDataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    public DBSObjectType[] getSupportedObjectTypes()
    {
        return new DBSObjectType[] {
            RelationalObjectType.TYPE_TABLE,
            RelationalObjectType.TYPE_CONSTRAINT,
            RelationalObjectType.TYPE_INDEX,
            RelationalObjectType.TYPE_PROCEDURE,
            RelationalObjectType.TYPE_TRIGGER,
            };
    }

    public Collection<DBSObject> findObjectsByMask(
        DBRProgressMonitor monitor,
        DBSObject parentObject,
        Collection<DBSObjectType> objectTypes,
        String objectNameMask,
        int maxResults)
        throws DBException
    {
        OracleSchema schema = parentObject instanceof OracleSchema ? (OracleSchema) parentObject : null;
        JDBCExecutionContext context = dataSource.openContext(
            monitor, DBCExecutionPurpose.META, "Find objects by name");
        try {
            List<DBSObject> objects = new ArrayList<DBSObject>();

            StringBuilder objectTypeClause = new StringBuilder();
            for (DBSObjectType objectType : objectTypes) {
                String typeName;
                if (objectType == RelationalObjectType.TYPE_TABLE) {
                    typeName = "'TABLE','VIEW'";
                } else if (objectType == RelationalObjectType.TYPE_INDEX) {
                    typeName = "'INDEX'";
                } else if (objectType == RelationalObjectType.TYPE_PROCEDURE) {
                    typeName = "'PROCEDURE'";
                } else if (objectType == RelationalObjectType.TYPE_TRIGGER) {
                    typeName = "'TRIGGER'";
                } else {
                    continue;
                }
                if (objectTypeClause.length() > 0) { objectTypeClause.append(","); }
                objectTypeClause.append(typeName);
            }
            // Search all objects
            if (objectTypeClause.length() > 0) {
                searchAllObjects(context, schema, objectNameMask, objectTypeClause.toString(), maxResults, objects);
            }
            // Search constraints

            return objects;
        }
        catch (SQLException ex) {
            throw new DBException(ex);
        }
        finally {
            context.close();
        }
    }

    private void searchAllObjects(JDBCExecutionContext context, OracleSchema schema, String objectNameMask, String objectTypeClause, int maxResults, List<DBSObject> objects)
        throws SQLException, DBException
    {
        // Seek for objects
        JDBCPreparedStatement dbStat = context.prepareStatement(
            "SELECT OWNER,OBJECT_NAME,OBJECT_TYPE FROM ALL_OBJECTS WHERE " +
            "OBJECT_TYPE IN (" + objectTypeClause + ") AND OBJECT_NAME LIKE ? " +
                (schema == null ? "" : " AND OWNER=?") +
                " ORDER BY OBJECT_NAME");
        try {
            dbStat.setString(1, objectNameMask.toUpperCase());
            if (schema != null) {
                dbStat.setString(2, schema.getName());
            }
            JDBCResultSet dbResult = dbStat.executeQuery();
            try {
                while (objects.size() < maxResults && dbResult.next()) {
                    if (context.getProgressMonitor().isCanceled()) {
                        break;
                    }
                    String schemaName = JDBCUtils.safeGetString(dbResult, "OWNER");
                    String objectName = JDBCUtils.safeGetString(dbResult, "OBJECT_NAME");
                    String objectType = JDBCUtils.safeGetString(dbResult, "OBJECT_TYPE");
                    OracleSchema tableSchema = schema != null ? schema : dataSource.getSchema(context.getProgressMonitor(), schemaName);
                    if (tableSchema == null) {
                        log.debug("Table schema '" + schemaName + "' not found");
                        continue;
                    }
                    JDBCAbstractCache<OracleSchema,?> objectCache = null;
                    if ("TABLE".equals(objectType) || "VIEW".equals(objectType)) {
                        objectCache = tableSchema.getTableCache();
                    } else if ("INDEX".equals(objectType)) {
                        objectCache = tableSchema.getIndexCache();
                    } else if ("PROCEDURE".equals(objectType)) {
                        objectCache = tableSchema.getProceduresCache();
                    } else if ("TRIGGER".equals(objectType)) {
                        objectCache = tableSchema.getTriggerCache();
                    }
                    if (objectCache != null) {
                        DBSObject object = objectCache.getObject(context.getProgressMonitor(), tableSchema, objectName);
                        if (object == null) {
                            log.debug(objectType + " '" + objectName + "' not found in schema '" + tableSchema.getName() + "'");
                            continue;
                        }
                        objects.add(object);
                    }
                }
            }
            finally {
                dbResult.close();
            }
        } finally {
            dbStat.close();
        }
    }


}

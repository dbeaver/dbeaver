/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.dbeaver.ext.oracle.model;

import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.utils.CommonUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCExecutionContext;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
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
            OracleObjectType.TABLE,
            OracleObjectType.INDEX,
            OracleObjectType.PROCEDURE,
            OracleObjectType.TRIGGER,
            };
    }

    public DBSObjectType[] getHyperlinkObjectTypes()
    {
        return new DBSObjectType[] {
            OracleObjectType.TABLE,
            OracleObjectType.VIEW,
            };
    }

    public DBSObjectType[] getAutoCompleteObjectTypes()
    {
        return new DBSObjectType[] {
            OracleObjectType.TABLE,
            OracleObjectType.VIEW,
            };
    }

    public Collection<DBSObject> findObjectsByMask(
        DBRProgressMonitor monitor,
        DBSObject parentObject,
        DBSObjectType[] objectTypes,
        String objectNameMask,
        int maxResults)
        throws DBException
    {
        OracleSchema schema = parentObject instanceof OracleSchema ? (OracleSchema) parentObject : null;
        JDBCExecutionContext context = dataSource.openContext(
            monitor, DBCExecutionPurpose.META, "Find objects by name");
        try {
            List<DBSObject> objects = new ArrayList<DBSObject>();

            // Search all objects
            searchAllObjects(context, schema, objectNameMask, objectTypes, maxResults, objects);

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

    private void searchAllObjects(JDBCExecutionContext context, OracleSchema schema, String objectNameMask, DBSObjectType[] objectTypes, int maxResults, List<DBSObject> objects)
        throws SQLException, DBException
    {
        StringBuilder objectTypeClause = new StringBuilder(100);
        List<OracleObjectType> oracleObjectTypes = new ArrayList<OracleObjectType>(objectTypes.length + 2);
        for (DBSObjectType objectType : objectTypes) {
            if (objectType instanceof OracleObjectType) {
                oracleObjectTypes.add((OracleObjectType) objectType);
                if (objectType == OracleObjectType.PROCEDURE) {
                    oracleObjectTypes.add(OracleObjectType.FUNCTION);
                }
            }
        }
        for (OracleObjectType objectType : oracleObjectTypes) {
            if (objectTypeClause.length() > 0) objectTypeClause.append(",");
            objectTypeClause.append("'").append(objectType.getTypeName()).append("'");
        }
        if (objectTypeClause.length() == 0) {
            return;
        }
        // Always search for synonyms
        objectTypeClause.append(",'").append(OracleObjectType.SYNONYM.getTypeName()).append("'");
        // Seek for objects (join with public synonyms)
        JDBCPreparedStatement dbStat = context.prepareStatement(
            "SELECT OWNER,OBJECT_NAME,OBJECT_TYPE FROM ALL_OBJECTS WHERE " +
            "OBJECT_TYPE IN (" + objectTypeClause + ") AND OBJECT_NAME LIKE ? " +
            (schema == null ? "" : " AND OWNER=?") +
            "UNION ALL\n" +
            "SELECT O.OWNER,O.OBJECT_NAME,O.OBJECT_TYPE\n" +
            "FROM ALL_SYNONYMS S,ALL_OBJECTS O\n" +
            "WHERE O.OWNER=S.TABLE_OWNER AND O.OBJECT_NAME=S.TABLE_NAME AND S.OWNER='PUBLIC' AND S.SYNONYM_NAME LIKE ?" +
            "\nORDER BY OBJECT_NAME");
        try {
            objectNameMask = objectNameMask.toUpperCase();
            dbStat.setString(1, objectNameMask);
            if (schema != null) {
                dbStat.setString(2, schema.getName());
            }
            dbStat.setString(schema != null ? 3 : 2, objectNameMask);
            dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);
            JDBCResultSet dbResult = dbStat.executeQuery();
            try {
                while (objects.size() < maxResults && dbResult.next()) {
                    if (context.getProgressMonitor().isCanceled()) {
                        break;
                    }
                    String schemaName = JDBCUtils.safeGetString(dbResult, "OWNER");
                    String objectName = JDBCUtils.safeGetString(dbResult, "OBJECT_NAME");
                    String objectTypeName = JDBCUtils.safeGetString(dbResult, "OBJECT_TYPE");
                    OracleSchema tableSchema = schema != null ? schema : dataSource.getSchema(context.getProgressMonitor(), schemaName);
                    if (tableSchema == null) {
                        log.debug("Table schema '" + schemaName + "' not found");
                        continue;
                    }
                    OracleObjectType objectType = OracleObjectType.getByType(objectTypeName);
                    if (objectType != null && objectType != OracleObjectType.SYNONYM && objectType.isBrowsable() && oracleObjectTypes.contains(objectType))
                    {
                        DBSObject object = objectType.findObject(context.getProgressMonitor(), tableSchema, objectName);
                        if (object == null) {
                            log.debug(objectTypeName + " '" + objectName + "' not found in schema '" + tableSchema.getName() + "'");
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

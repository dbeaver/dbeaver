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
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.exec.DBCExecutionPurpose;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractObjectReference;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.utils.ArrayUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * OracleStructureAssistant
 */
public class OracleStructureAssistant implements DBSStructureAssistant
{
    static protected final Log log = Log.getLog(OracleStructureAssistant.class);

    private final OracleDataSource dataSource;

    public OracleStructureAssistant(OracleDataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    @Override
    public DBSObjectType[] getSupportedObjectTypes()
    {
        return new DBSObjectType[] {
            OracleObjectType.TABLE,
            OracleObjectType.PACKAGE,
            OracleObjectType.CONSTRAINT,
            OracleObjectType.FOREIGN_KEY,
            OracleObjectType.INDEX,
            OracleObjectType.PROCEDURE,
            OracleObjectType.SEQUENCE,
            OracleObjectType.TRIGGER,
            };
    }

    @Override
    public DBSObjectType[] getHyperlinkObjectTypes()
    {
        return new DBSObjectType[] {
            OracleObjectType.TABLE,
            OracleObjectType.PACKAGE,
            OracleObjectType.PROCEDURE,
        };
    }

    @Override
    public DBSObjectType[] getAutoCompleteObjectTypes()
    {
        return new DBSObjectType[] {
            OracleObjectType.TABLE,
            OracleObjectType.PACKAGE,
            OracleObjectType.PROCEDURE,
            };
    }

    @Override
    public Collection<DBSObjectReference> findObjectsByMask(
        DBRProgressMonitor monitor,
        DBSObject parentObject,
        DBSObjectType[] objectTypes,
        String objectNameMask,
        boolean caseSensitive,
        int maxResults)
        throws DBException
    {
        OracleSchema schema = parentObject instanceof OracleSchema ? (OracleSchema) parentObject : null;
        JDBCSession session = dataSource.openSession(
            monitor, DBCExecutionPurpose.META, "Find objects by name");
        try {
            List<DBSObjectReference> objects = new ArrayList<DBSObjectReference>();

            // Search all objects
            searchAllObjects(session, schema, objectNameMask, objectTypes, caseSensitive, maxResults, objects);

            if (ArrayUtils.contains(objectTypes, OracleObjectType.CONSTRAINT, OracleObjectType.FOREIGN_KEY) && objects.size() < maxResults) {
                // Search constraints
                findConstraintsByMask(session, schema, objectNameMask, objectTypes, maxResults, objects);
            }

            return objects;
        }
        catch (SQLException ex) {
            throw new DBException(ex, session.getDataSource());
        }
        finally {
            session.close();
        }
    }

    private void findConstraintsByMask(
        JDBCSession session,
        final OracleSchema schema,
        String constrNameMask,
        DBSObjectType[] objectTypes,
        int maxResults,
        List<DBSObjectReference> objects)
        throws SQLException, DBException
    {
        DBRProgressMonitor monitor = session.getProgressMonitor();

        List<DBSObjectType> objectTypesList = Arrays.asList(objectTypes);
        final boolean hasFK = objectTypesList.contains(OracleObjectType.FOREIGN_KEY);
        final boolean hasConstraints = objectTypesList.contains(OracleObjectType.CONSTRAINT);

        // Load tables
        JDBCPreparedStatement dbStat = session.prepareStatement(
                "SELECT OWNER, TABLE_NAME, CONSTRAINT_NAME, CONSTRAINT_TYPE\n" +
                "FROM SYS.ALL_CONSTRAINTS\n" +
                "WHERE CONSTRAINT_NAME like ?" + (!hasFK ? " AND CONSTRAINT_TYPE<>'R'" : "") +
                (schema != null ? " AND OWNER=?" : ""));
        try {
            dbStat.setString(1, constrNameMask);
            if (schema != null) {
                dbStat.setString(2, schema.getName());
            }
            JDBCResultSet dbResult = dbStat.executeQuery();
            try {
                int tableNum = maxResults;
                while (dbResult.next() && tableNum-- > 0) {
                    if (monitor.isCanceled()) {
                        break;
                    }
                    final String schemaName = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_OWNER);
                    final String tableName = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_TABLE_NAME);
                    final String constrName = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_CONSTRAINT_NAME);
                    final String constrType = JDBCUtils.safeGetString(dbResult, OracleConstants.COL_CONSTRAINT_TYPE);
                    final DBSEntityConstraintType type = OracleTableConstraint.getConstraintType(constrType);
                    objects.add(new AbstractObjectReference(
                        constrName,
                        dataSource.getSchema(session.getProgressMonitor(), schemaName),
                        null,
                        type == DBSEntityConstraintType.FOREIGN_KEY ? OracleObjectType.FOREIGN_KEY : OracleObjectType.CONSTRAINT)
                    {
                        @Override
                        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException
                        {
                            OracleSchema tableSchema = schema != null ? schema : dataSource.getSchema(monitor, schemaName);
                            if (tableSchema == null) {
                                throw new DBException("Constraint schema '" + schemaName + "' not found");
                            }
                            OracleTable table = tableSchema.getTable(monitor, tableName);
                            if (table == null) {
                                throw new DBException("Constraint table '" + tableName + "' not found in catalog '" + tableSchema.getName() + "'");
                            }
                            DBSObject constraint = null;
                            if (hasFK && type == DBSEntityConstraintType.FOREIGN_KEY) {
                                constraint = table.getForeignKey(monitor, constrName);
                            }
                            if (hasConstraints && type != DBSEntityConstraintType.FOREIGN_KEY) {
                                constraint = table.getConstraint(monitor, constrName);
                            }
                            if (constraint == null) {
                                throw new DBException("Constraint '" + constrName + "' not found in table '" + table.getFullQualifiedName() + "'");
                            }
                            return constraint;
                        }
                    });
                }
            }
            finally {
                dbResult.close();
            }
        } finally {
            dbStat.close();
        }
    }

    private void searchAllObjects(final JDBCSession session, final OracleSchema schema, String objectNameMask, DBSObjectType[] objectTypes, boolean caseSensitive, int maxResults, List<DBSObjectReference> objects)
        throws SQLException, DBException
    {
        StringBuilder objectTypeClause = new StringBuilder(100);
        final List<OracleObjectType> oracleObjectTypes = new ArrayList<OracleObjectType>(objectTypes.length + 2);
        for (DBSObjectType objectType : objectTypes) {
            if (objectType instanceof OracleObjectType) {
                oracleObjectTypes.add((OracleObjectType) objectType);
                if (objectType == OracleObjectType.PROCEDURE) {
                    oracleObjectTypes.add(OracleObjectType.FUNCTION);
                } else if (objectType == OracleObjectType.TABLE) {
                    oracleObjectTypes.add(OracleObjectType.VIEW);
                    oracleObjectTypes.add(OracleObjectType.MATERIALIZED_VIEW);
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
        JDBCPreparedStatement dbStat = session.prepareStatement(
            "SELECT DISTINCT OWNER,OBJECT_NAME,OBJECT_TYPE FROM (SELECT OWNER,OBJECT_NAME,OBJECT_TYPE FROM ALL_OBJECTS WHERE " +
            "OBJECT_TYPE IN (" + objectTypeClause + ") AND OBJECT_NAME LIKE ? " +
            (schema == null ? "" : " AND OWNER=?") +
            "UNION ALL\n" +
            "SELECT O.OWNER,O.OBJECT_NAME,O.OBJECT_TYPE\n" +
            "FROM ALL_SYNONYMS S,ALL_OBJECTS O\n" +
            "WHERE O.OWNER=S.TABLE_OWNER AND O.OBJECT_NAME=S.TABLE_NAME AND S.OWNER='PUBLIC' AND S.SYNONYM_NAME LIKE ?)" +
            "\nORDER BY OBJECT_NAME");
        try {
            if (!caseSensitive) {
                objectNameMask = objectNameMask.toUpperCase();
            }
            dbStat.setString(1, objectNameMask);
            if (schema != null) {
                dbStat.setString(2, schema.getName());
            }
            dbStat.setString(schema != null ? 3 : 2, objectNameMask);
            dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);
            JDBCResultSet dbResult = dbStat.executeQuery();
            try {
                while (objects.size() < maxResults && dbResult.next()) {
                    if (session.getProgressMonitor().isCanceled()) {
                        break;
                    }
                    final String schemaName = JDBCUtils.safeGetString(dbResult, "OWNER");
                    final String objectName = JDBCUtils.safeGetString(dbResult, "OBJECT_NAME");
                    final String objectTypeName = JDBCUtils.safeGetString(dbResult, "OBJECT_TYPE");
                    final OracleObjectType objectType = OracleObjectType.getByType(objectTypeName);
                    if (objectType != null && objectType != OracleObjectType.SYNONYM && objectType.isBrowsable() && oracleObjectTypes.contains(objectType))
                    {
                        OracleSchema objectSchema = dataSource.getSchema(session.getProgressMonitor(), schemaName);
                        if (objectSchema == null) {
                            log.debug("Schema '" + schemaName + "' not found. Probably was filtered");
                            continue;
                        }
                        objects.add(new AbstractObjectReference(objectName, objectSchema, null, objectType) {
                            @Override
                            public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException
                            {
                                OracleSchema tableSchema = (OracleSchema)getContainer();
                                DBSObject object = objectType.findObject(session.getProgressMonitor(), tableSchema, objectName);
                                if (object == null) {
                                    throw new DBException(objectTypeName + " '" + objectName + "' not found in schema '" + tableSchema.getName() + "'");
                                }
                                return object;
                            }
                        });
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

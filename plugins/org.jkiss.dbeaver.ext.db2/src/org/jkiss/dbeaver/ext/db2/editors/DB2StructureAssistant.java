/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille (titou10.titou10@gmail.com)
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
package org.jkiss.dbeaver.ext.db2.editors;

import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ext.db2.model.DB2DataSource;
import org.jkiss.dbeaver.ext.db2.model.DB2Schema;
import org.jkiss.dbeaver.ext.db2.model.DB2Table;
import org.jkiss.dbeaver.ext.db2.model.DB2View;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2TableType;
import org.jkiss.dbeaver.model.DBConstants;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractObjectReference;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSObject;
import org.jkiss.dbeaver.model.struct.DBSObjectReference;
import org.jkiss.dbeaver.model.struct.DBSObjectType;
import org.jkiss.dbeaver.model.struct.DBSStructureAssistant;
import org.jkiss.utils.CommonUtils;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * DB2 Structure Assistant
 * 
 * @author Denis Forveille
 */
public class DB2StructureAssistant implements DBSStructureAssistant {
    private static final Log LOG = Log.getLog(DB2StructureAssistant.class);

    // TODO DF: Work in progess

    private static final DBSObjectType[] SUPP_OBJ_TYPES = { DB2ObjectType.ALIAS, DB2ObjectType.TABLE, DB2ObjectType.VIEW,
        DB2ObjectType.MQT, DB2ObjectType.NICKNAME, DB2ObjectType.COLUMN, };

    private static final DBSObjectType[] HYPER_LINKS_TYPES = { DB2ObjectType.ALIAS, DB2ObjectType.TABLE, DB2ObjectType.VIEW,
        DB2ObjectType.MQT, DB2ObjectType.NICKNAME, };

    private static final DBSObjectType[] AUTOC_OBJ_TYPES = { DB2ObjectType.ALIAS, DB2ObjectType.TABLE, DB2ObjectType.VIEW,
        DB2ObjectType.MQT, DB2ObjectType.NICKNAME, };

    private static final String SQL_COLS_ALL;
    private static final String SQL_COLS_SCHEMA;

    private final DB2DataSource dataSource;

    // -----------------
    // Constructors
    // -----------------
    public DB2StructureAssistant(DB2DataSource dataSource)
    {
        this.dataSource = dataSource;
    }

    // -----------------
    // Method Interface
    // -----------------

    @Override
    public DBSObjectType[] getSupportedObjectTypes()
    {
        return SUPP_OBJ_TYPES;
    }

    @Override
    public DBSObjectType[] getHyperlinkObjectTypes()
    {
        return HYPER_LINKS_TYPES;
    }

    @Override
    public DBSObjectType[] getAutoCompleteObjectTypes()
    {
        return AUTOC_OBJ_TYPES;
    }

    @NotNull
    @Override
    public List<DBSObjectReference> findObjectsByMask(DBRProgressMonitor monitor, DBSObject parentObject,
                                                      DBSObjectType[] objectTypes, String objectNameMask, boolean caseSensitive, boolean globalSearch, int maxResults) throws DBException
    {

        LOG.debug(objectNameMask);

        List<DB2ObjectType> db2ObjectTypes = new ArrayList<>(objectTypes.length);
        for (DBSObjectType dbsObjectType : objectTypes) {
            db2ObjectTypes.add((DB2ObjectType) dbsObjectType);
        }

        DB2Schema schema = parentObject instanceof DB2Schema ? (DB2Schema) parentObject : null;
        if (schema == null && !globalSearch) {
            schema = dataSource.getDefaultObject();
        }

        try (JDBCSession session = DBUtils.openMetaSession(monitor, dataSource, "Find objects by name")) {
            return searchAllObjects(session, schema, objectNameMask, db2ObjectTypes, caseSensitive, maxResults);
        } catch (SQLException ex) {
            throw new DBException(ex, dataSource);
        }
    }

    // -----------------
    // Helpers
    // -----------------

    private List<DBSObjectReference> searchAllObjects(final JDBCSession session, final DB2Schema schema, String objectNameMask,
        List<DB2ObjectType> db2ObjectTypes, boolean caseSensitive, int maxResults) throws SQLException, DBException
    {
        List<DBSObjectReference> objects = new ArrayList<>();

        String searchObjectNameMask = objectNameMask;
        if (!caseSensitive) {
            searchObjectNameMask = searchObjectNameMask.toUpperCase();
        }

        int nbResults = 0;

        // Tables, Alias, Views, Nicknames, MQT
        if ((db2ObjectTypes.contains(DB2ObjectType.ALIAS)) || (db2ObjectTypes.contains(DB2ObjectType.TABLE))
            || (db2ObjectTypes.contains(DB2ObjectType.NICKNAME)) || (db2ObjectTypes.contains(DB2ObjectType.VIEW))
            || (db2ObjectTypes.contains(DB2ObjectType.MQT)))
        {
            searchTables(session, schema, searchObjectNameMask, db2ObjectTypes, maxResults, objects, nbResults);

            if (nbResults >= maxResults) {
                return objects;
            }
        }

        // Columns
        if (db2ObjectTypes.contains(DB2ObjectType.COLUMN)) {
            searchColumns(session, schema, searchObjectNameMask, db2ObjectTypes, maxResults, objects, nbResults);
        }

        return objects;
    }

    // --------------
    // Helper Classes
    // --------------

    private void searchTables(JDBCSession session, DB2Schema schema, String searchObjectNameMask,
        List<DB2ObjectType> db2ObjectTypes, int maxResults, List<DBSObjectReference> objects, int nbResults) throws SQLException,
        DBException
    {
        String baseSQL;
        if (schema != null) {
            baseSQL =
                "SELECT TABSCHEMA,TABNAME,TYPE FROM SYSCAT.TABLES\n" +
                "WHERE TABSCHEMA =? AND TABNAME LIKE ? AND TYPE IN (%s)\n" +
                "WITH UR";
        } else {
            baseSQL =
                "SELECT TABSCHEMA,TABNAME,TYPE FROM SYSCAT.TABLES\n" +
                "WHERE TABNAME LIKE ? AND TYPE IN (%s)\n" +
                "WITH UR";
        }

        String sql = buildTableSQL(baseSQL, db2ObjectTypes);

        int n = 1;
        try (JDBCPreparedStatement dbStat = session.prepareStatement(sql)) {
            if (schema != null) {
                dbStat.setString(n++, schema.getName());
                //dbStat.setString(n++, DB2Constants.SYSTEM_CATALOG_SCHEMA);
            }
            dbStat.setString(n++, searchObjectNameMask);

            dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);

            String schemaName;
            String objectName;
            DB2Schema db2Schema;
            DB2TableType tableType;
            DB2ObjectType objectType;

            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (dbResult.next()) {
                    if (session.getProgressMonitor().isCanceled()) {
                        break;
                    }

                    if (nbResults++ >= maxResults) {
                        break;
                    }

                    schemaName = JDBCUtils.safeGetStringTrimmed(dbResult, "TABSCHEMA");
                    objectName = JDBCUtils.safeGetString(dbResult, "TABNAME");
                    tableType = CommonUtils.valueOf(DB2TableType.class, JDBCUtils.safeGetString(dbResult, "TYPE"));

                    db2Schema = dataSource.getSchema(session.getProgressMonitor(), schemaName);
                    if (db2Schema == null) {
                        LOG.debug("Schema '" + schemaName + "' not found. Probably was filtered");
                        continue;
                    }

                    objectType = tableType.getDb2ObjectType();
                    objects.add(new DB2ObjectReference(objectName, db2Schema, objectType));
                }
            }
        }
    }

    private void searchColumns(JDBCSession session, DB2Schema schema, String searchObjectNameMask, List<DB2ObjectType> objectTypes,
        int maxResults, List<DBSObjectReference> objects, int nbResults) throws SQLException, DBException
    {
        String sql;
        if (schema != null) {
            sql = SQL_COLS_SCHEMA;
        } else {
            sql = SQL_COLS_ALL;
        }

        int n = 1;
        try (JDBCPreparedStatement dbStat = session.prepareStatement(sql)) {
            if (schema != null) {
                dbStat.setString(n++, schema.getName());
            }
            dbStat.setString(n++, searchObjectNameMask);

            dbStat.setFetchSize(DBConstants.METADATA_FETCH_SIZE);

            String tableSchemaName;
            String tableOrViewName;
            String columnName;
            DB2Schema db2Schema;
            DB2Table db2Table;
            DB2View db2View;

            try (JDBCResultSet dbResult = dbStat.executeQuery()) {
                while (dbResult.next()) {
                    if (session.getProgressMonitor().isCanceled()) {
                        break;
                    }

                    if (nbResults++ >= maxResults) {
                        return;
                    }

                    tableSchemaName = JDBCUtils.safeGetStringTrimmed(dbResult, "TABSCHEMA");
                    tableOrViewName = JDBCUtils.safeGetString(dbResult, "TABNAME");
                    columnName = JDBCUtils.safeGetString(dbResult, "COLNAME");

                    db2Schema = dataSource.getSchema(session.getProgressMonitor(), tableSchemaName);
                    if (db2Schema == null) {
                        LOG.debug("Schema '" + tableSchemaName + "' not found. Probably was filtered");
                        continue;
                    }
                    // Try with table, then view
                    db2Table = db2Schema.getTable(session.getProgressMonitor(), tableOrViewName);
                    if (db2Table != null) {
                        objects.add(new DB2ObjectReference(columnName, db2Table, DB2ObjectType.COLUMN));
                    } else {
                        db2View = db2Schema.getView(session.getProgressMonitor(), tableOrViewName);
                        if (db2View != null) {
                            objects.add(new DB2ObjectReference(columnName, db2View, DB2ObjectType.COLUMN));
                        }
                    }

                }
            }
        }
    }

    private class DB2ObjectReference extends AbstractObjectReference {

        private DB2ObjectReference(String objectName, DB2Schema db2Schema, DB2ObjectType objectType)
        {
            super(objectName, db2Schema, null, DB2Schema.class, objectType);
        }

        private DB2ObjectReference(String objectName, DB2Table db2Table, DB2ObjectType objectType)
        {
            super(objectName, db2Table, null, DB2Table.class, objectType);
        }

        private DB2ObjectReference(String objectName, DB2View db2View, DB2ObjectType objectType)
        {
            super(objectName, db2View, null, DB2View.class, objectType);
        }

        @Override
        public DBSObject resolveObject(DBRProgressMonitor monitor) throws DBException
        {

            DB2ObjectType db2ObjectType = (DB2ObjectType) getObjectType();

            if (getContainer() instanceof DB2Schema) {
                DB2Schema db2Schema = (DB2Schema) getContainer();

                DBSObject object = db2ObjectType.findObject(monitor, db2Schema, getName());
                if (object == null) {
                    throw new DBException(db2ObjectType + " '" + getName() + "' not found in schema '" + db2Schema.getName() + "'");
                }
                return object;
            }
            if (getContainer() instanceof DB2Table) {
                DB2Table db2Table = (DB2Table) getContainer();

                DBSObject object = db2ObjectType.findObject(monitor, db2Table, getName());
                if (object == null) {
                    throw new DBException(db2ObjectType + " '" + getName() + "' not found in table '" + db2Table.getName() + "'");
                }
                return object;
            }
            if (getContainer() instanceof DB2View) {
                DB2View db2View = (DB2View) getContainer();

                DBSObject object = db2ObjectType.findObject(monitor, db2View, getName());
                if (object == null) {
                    throw new DBException(db2ObjectType + " '" + getName() + "' not found in view '" + db2View.getName() + "'");
                }
                return object;
            }
            return null;
        }

    }

    private String buildTableSQL(String baseStatement, List<DB2ObjectType> objectTypes)
    {
        List<Character> listChars = new ArrayList<>(objectTypes.size());
        for (DB2ObjectType objectType : objectTypes) {
            if (objectType.equals(DB2ObjectType.ALIAS)) {
                listChars.add(DB2TableType.A.name().charAt(0));
            }
            if (objectType.equals(DB2ObjectType.TABLE)) {
                listChars.add(DB2TableType.G.name().charAt(0));
                listChars.add(DB2TableType.H.name().charAt(0));
                listChars.add(DB2TableType.L.name().charAt(0));
                listChars.add(DB2TableType.T.name().charAt(0));
                listChars.add(DB2TableType.U.name().charAt(0));
            }
            if (objectType.equals(DB2ObjectType.VIEW)) {
                listChars.add(DB2TableType.V.name().charAt(0));
                listChars.add(DB2TableType.W.name().charAt(0));
            }
            if (objectType.equals(DB2ObjectType.MQT)) {
                listChars.add(DB2TableType.S.name().charAt(0));
            }
            if (objectType.equals(DB2ObjectType.NICKNAME)) {
                listChars.add(DB2TableType.N.name().charAt(0));
            }

        }
        Boolean notFirst = false;
        StringBuilder sb = new StringBuilder(64);
        for (Character letter : listChars) {
            if (notFirst) {
                sb.append(",");
            } else {
                notFirst = true;
            }
            sb.append("'");
            sb.append(letter);
            sb.append("'");
        }
        return String.format(baseStatement, sb.toString());
    }

    static {
        StringBuilder sb = new StringBuilder(1024);

        sb.append("SELECT TABSCHEMA,TABNAME,COLNAME");
        sb.append("  FROM SYSCAT.COLUMNS");
        sb.append(" WHERE TABSCHEMA = ?");
        sb.append("   AND COLNAME LIKE ?");
        sb.append(" WITH UR");
        SQL_COLS_SCHEMA = sb.toString();

        sb.setLength(0);

        sb.append("SELECT TABSCHEMA,TABNAME,COLNAME");
        sb.append("  FROM SYSCAT.COLUMNS");
        sb.append(" WHERE COLNAME LIKE ?");
        sb.append(" WITH UR");
        SQL_COLS_ALL = sb.toString();

    }

}

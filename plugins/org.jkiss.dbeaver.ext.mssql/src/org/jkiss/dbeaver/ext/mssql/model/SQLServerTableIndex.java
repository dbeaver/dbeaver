/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.mssql.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.mssql.SQLServerUtils;
import org.jkiss.dbeaver.model.DBPEvaluationContext;
import org.jkiss.dbeaver.model.DBPNamedObject2;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.jdbc.struct.JDBCTableIndex;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.sql.SQLUtils;
import org.jkiss.dbeaver.model.struct.DBSObjectWithScript;
import org.jkiss.dbeaver.model.struct.rdb.DBSIndexType;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndex;
import org.jkiss.dbeaver.model.struct.rdb.DBSTableIndexColumn;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * SQLServerTableIndex
 */
public class SQLServerTableIndex extends JDBCTableIndex<SQLServerSchema, SQLServerTableBase> implements SQLServerObject, DBPNamedObject2, DBSObjectWithScript
{
    private boolean unique;
    private boolean primary;
    private String indexComment;
    private List<SQLServerTableIndexColumn> columns;
    private long objectId;
    private String ddl;

    public SQLServerTableIndex(
        SQLServerTableBase table,
        boolean unique,
        boolean primary,
        String indexName,
        DBSIndexType indexType,
        String comment,
        boolean persisted)
    {
        super(table.getContainer(), table, indexName, indexType, persisted);
        this.unique = unique;
        this.primary = primary;
        this.indexComment = comment;
    }

    // Copy constructor
    SQLServerTableIndex(DBRProgressMonitor monitor, SQLServerTable table, DBSTableIndex source) throws DBException {
        super(table.getContainer(), table, source, false);
        this.unique = source.isUnique();
        this.primary = source.isPrimary();
        this.indexComment = source.getDescription();
        if (source instanceof SQLServerTableIndex) {
            //this.cardinality = ((SQLServerTableIndex)source).cardinality;
        }
        List<? extends DBSTableIndexColumn> columns = source.getAttributeReferences(monitor);
        if (columns != null) {
            this.columns = new ArrayList<>(columns.size());
            for (DBSTableIndexColumn sourceColumn : columns) {
                this.columns.add(new SQLServerTableIndexColumn(monitor, this, sourceColumn));
            }
        }
    }

    public SQLServerTableIndex(SQLServerTableBase parent, String indexName, DBSIndexType indexType, ResultSet dbResult) {
        super(
            parent.getContainer(),
            parent,
            indexName,
            indexType,
            true);
        this.objectId = JDBCUtils.safeGetLong(dbResult, "index_id");
        this.unique = JDBCUtils.safeGetInt(dbResult, "is_unique") != 0;
        this.primary = JDBCUtils.safeGetInt(dbResult, "is_primary_key") != 0;
    }

    @NotNull
    @Override
    public SQLServerDataSource getDataSource()
    {
        return getTable().getDataSource();
    }

    @Override
    @Property(viewable = false, order = 50)
    public long getObjectId() {
        return objectId;
    }

    @Override
    @Property(viewable = true, order = 5)
    public boolean isUnique()
    {
        return unique;
    }

    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    @Override
    @Property(viewable = false, order = 6)
    public boolean isPrimary() {
        return primary;
    }

    @Nullable
    @Override
    @Property(viewable = true, multiline = true, order = 100)
    public String getDescription()
    {
        return indexComment;
    }

    public void setDescription(String indexComment) {
        this.indexComment = indexComment;
    }

    @Override
    public List<SQLServerTableIndexColumn> getAttributeReferences(DBRProgressMonitor monitor)
    {
        return columns;
    }

    public SQLServerTableIndexColumn getColumn(String columnName)
    {
        return DBUtils.findObject(columns, columnName);
    }

    void setColumns(List<SQLServerTableIndexColumn> columns)
    {
        this.columns = columns;
    }

    public void addColumn(SQLServerTableIndexColumn column)
    {
        if (columns == null) {
            columns = new ArrayList<>();
        }
        columns.add(column);
    }

    @NotNull
    @Override
    public String getFullyQualifiedName(DBPEvaluationContext context)
    {
        return DBUtils.getFullQualifiedName(getDataSource(),
            getTable().getContainer(),
            this);
    }

    @Override
    public String getObjectDefinitionText(DBRProgressMonitor monitor, Map<String, Object> options) throws DBException {
        if (!isPersisted()) {
            return null;
        }
        if (ddl == null) {
            ddl = readIndexDefinition(monitor);
        }
        return ddl;
    }

    // Index DDL gen script taken from MS technet
    // https://gallery.technet.microsoft.com/scriptcenter/SQL-Server-Generate-Index-fa790441
    private String readIndexDefinition(DBRProgressMonitor monitor) throws DBCException {
        String sql =
            "SELECT ' CREATE ' + \n" +
                "    CASE WHEN I.is_unique = 1 THEN ' UNIQUE ' ELSE '' END  +  \n" +
                "    I.type_desc COLLATE DATABASE_DEFAULT +' INDEX ' +   \n" +
                "    I.name  + ' ON '  +  \n" +
                "    Schema_name(T.Schema_id)+'.'+T.name + ' ( ' + \n" +
                "    KeyColumns + ' )  ' + \n" +
                "    ISNULL('\n\t INCLUDE ('+IncludedColumns+' ) ','') + \n" +
                "    ISNULL('\n\t WHERE  '+I.Filter_definition,'') + '\n\t WITH ( ' + \n" +
                "    CASE WHEN I.is_padded = 1 THEN ' PAD_INDEX = ON ' ELSE ' PAD_INDEX = OFF ' END + ','  + \n" +
                "    'FILLFACTOR = '+CONVERT(CHAR(5),CASE WHEN I.Fill_factor = 0 THEN 100 ELSE I.Fill_factor END) + ','  + \n" +
                "    -- default value \n" +
                "    'SORT_IN_TEMPDB = OFF '  + ','  + \n" +
                "    CASE WHEN I.ignore_dup_key = 1 THEN ' IGNORE_DUP_KEY = ON ' ELSE ' IGNORE_DUP_KEY = OFF ' END + ','  + \n" +
                "    CASE WHEN ST.no_recompute = 0 THEN ' STATISTICS_NORECOMPUTE = OFF ' ELSE ' STATISTICS_NORECOMPUTE = ON ' END + ','  + \n" +
//                "    -- default value  \n" +
//                "    ' DROP_EXISTING = ON '  + ','  + \n" +
                "    -- default value  \n" +
                "    ' ONLINE = OFF '  + ','  + \n" +
                "   CASE WHEN I.allow_row_locks = 1 THEN ' ALLOW_ROW_LOCKS = ON ' ELSE ' ALLOW_ROW_LOCKS = OFF ' END + ','  + \n" +
                "   CASE WHEN I.allow_page_locks = 1 THEN ' ALLOW_PAGE_LOCKS = ON ' ELSE ' ALLOW_PAGE_LOCKS = OFF ' END  + ' )" +
                "\n\t ON [' + \n" +
                "   DS.name + ' ] '  [CreateIndexScript] \n" +
                "FROM " + SQLServerUtils.getSystemTableName(getDatabase(), "indexes") + " I   \n" +
                " JOIN " + SQLServerUtils.getSystemTableName(getDatabase(), "tables") + " T ON T.Object_id = I.Object_id    \n" +
                " JOIN " + SQLServerUtils.getSystemTableName(getDatabase(), "sysindexes") + " SI ON I.Object_id = SI.id AND I.index_id = SI.indid   \n" +
                " JOIN (SELECT * FROM (  \n" +
                "    SELECT IC2.object_id , IC2.index_id ,  \n" +
                "        STUFF((SELECT ' , ' + C.name + CASE WHEN MAX(CONVERT(INT,IC1.is_descending_key)) = 1 THEN ' DESC ' ELSE ' ASC ' END \n" +
                "    FROM " + SQLServerUtils.getSystemTableName(getDatabase(), "index_columns") + " IC1  \n" +
                "    JOIN " + SQLServerUtils.getSystemTableName(getDatabase(), "columns") + " C   \n" +
                "       ON C.object_id = IC1.object_id   \n" +
                "       AND C.column_id = IC1.column_id   \n" +
                "       AND IC1.is_included_column = 0  \n" +
                "    WHERE IC1.object_id = IC2.object_id   \n" +
                "       AND IC1.index_id = IC2.index_id   \n" +
                "    GROUP BY IC1.object_id,C.name,index_id  \n" +
                "    ORDER BY MAX(IC1.key_ordinal)  \n" +
                "       FOR XML PATH('')), 1, 2, '') KeyColumns   \n" +
                "    FROM " + SQLServerUtils.getSystemTableName(getDatabase(), "index_columns") + " IC2   \n" +
                "    --WHERE IC2.Object_id = object_id('Person.Address') --Comment for all tables  \n" +
                "    GROUP BY IC2.object_id ,IC2.index_id) tmp3 )tmp4   \n" +
                "  ON I.object_id = tmp4.object_id AND I.Index_id = tmp4.index_id  \n" +
                " JOIN " + SQLServerUtils.getSystemTableName(getDatabase(), "stats") + " ST ON ST.object_id = I.object_id AND ST.stats_id = I.index_id   \n" +
                " JOIN " + SQLServerUtils.getSystemTableName(getDatabase(), "data_spaces") + " DS ON I.data_space_id=DS.data_space_id   \n" +
                " JOIN " + SQLServerUtils.getSystemTableName(getDatabase(), "filegroups") + " FG ON I.data_space_id=FG.data_space_id   \n" +
                " LEFT JOIN (SELECT * FROM (   \n" +
                "    SELECT IC2.object_id , IC2.index_id ,   \n" +
                "        STUFF((SELECT ' , ' + C.name  \n" +
                "    FROM " + SQLServerUtils.getSystemTableName(getDatabase(), "index_columns") + " IC1   \n" +
                "    JOIN " + SQLServerUtils.getSystemTableName(getDatabase(), "columns") + " C    \n" +
                "       ON C.object_id = IC1.object_id    \n" +
                "       AND C.column_id = IC1.column_id    \n" +
                "       AND IC1.is_included_column = 1   \n" +
                "    WHERE IC1.object_id = IC2.object_id    \n" +
                "       AND IC1.index_id = IC2.index_id    \n" +
                "    GROUP BY IC1.object_id,C.name,index_id   \n" +
                "       FOR XML PATH('')), 1, 2, '') IncludedColumns    \n" +
                "   FROM " + SQLServerUtils.getSystemTableName(getDatabase(), "index_columns") + " IC2    \n" +
                "   --WHERE IC2.Object_id = object_id('Person.Address') --Comment for all tables   \n" +
                "   GROUP BY IC2.object_id ,IC2.index_id) tmp1   \n" +
                "   WHERE IncludedColumns IS NOT NULL ) tmp2    \n" +
                "ON tmp2.object_id = I.object_id AND tmp2.index_id = I.index_id   \n" +
                "WHERE I.is_primary_key = 0 AND I.is_unique_constraint = 0 \n" +
                "AND I.Object_id = " + getTable().getObjectId() + "\n" +
                "AND I.name = '" + SQLUtils.escapeString(getDataSource(), getName()) + "'";
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read SQL Server index definition")) {
            return JDBCUtils.queryString(session, sql);
        } catch (SQLException e) {
            throw new DBCException("Error reading index definition", e);
        }
    }

    private SQLServerDatabase getDatabase() {
        return getTable().getDatabase();
    }

    @Override
    public void setObjectDefinitionText(String source) {

    }
}

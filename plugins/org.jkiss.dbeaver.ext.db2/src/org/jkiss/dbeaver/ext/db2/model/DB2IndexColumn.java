/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2013-2015 Denis Forveille titou10.titou10@gmail.com
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
package org.jkiss.dbeaver.ext.db2.model;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.ext.db2.DB2Constants;
import org.jkiss.dbeaver.ext.db2.DB2Utils;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2IndexColOrder;
import org.jkiss.dbeaver.ext.db2.model.dict.DB2IndexColVirtual;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCPreparedStatement;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCResultSet;
import org.jkiss.dbeaver.model.exec.jdbc.JDBCSession;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.impl.struct.AbstractTableIndexColumn;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;

import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * DB2 Index Column
 * 
 * @author Denis Forveille
 */
public class DB2IndexColumn extends AbstractTableIndexColumn {

    private static final String I_DEP = "SELECT BSCHEMA,BNAME FROM SYSCAT.INDEXDEP WHERE INDSCHEMA = ? AND INDNAME = ? AND BTYPE = 'V' WITH UR";

    private DB2Index            db2Index;
    private DB2TableColumn      tableColumn;

    private Integer             colSeq;
    private DB2IndexColOrder    colOrder;
    private String              collationSchema;
    private String              collationNane;

    private DB2IndexColVirtual  virtualCol;
    private String              virtualColName;
    private String              virtualColText;

    // -----------------
    // Constructors
    // -----------------

    public DB2IndexColumn(DBRProgressMonitor monitor, DB2Index db2Index, ResultSet dbResult) throws DBException
    {

        DB2DataSource db2DataSource = db2Index.getDataSource();

        this.db2Index = db2Index;
        this.colSeq = JDBCUtils.safeGetInteger(dbResult, "COLSEQ");
        this.colOrder = CommonUtils.valueOf(DB2IndexColOrder.class, JDBCUtils.safeGetString(dbResult, "COLORDER"));

        if (db2DataSource.isAtLeastV9_5()) {
            this.collationSchema = JDBCUtils.safeGetStringTrimmed(dbResult, "COLLATIONSCHEMA");
            this.collationNane = JDBCUtils.safeGetString(dbResult, "COLLATIONNAME");
        }
        if (db2DataSource.isAtLeastV10_1()) {
            this.virtualCol = CommonUtils.valueOf(DB2IndexColVirtual.class, JDBCUtils.safeGetString(dbResult, "VIRTUAL"));
            this.virtualColText = JDBCUtils.safeGetStringTrimmed(dbResult, "TEXT");
        }

        // Look for Table Column if column is not virtual...
        DB2TableBase db2Table = db2Index.getTable();
        String columnName = JDBCUtils.safeGetString(dbResult, "COLNAME");

        if ((virtualCol == null) || (virtualCol.isNotVirtual())) {
            this.tableColumn = db2Table.getAttribute(monitor, columnName);
            if (tableColumn == null) {
                throw new DBException("Column '" + columnName + "' not found in table '" + db2Table.getName() + "' for index '"
                    + db2Index.getName() + "'");
            }
        } else {
            // Virtual Column
            // Store Virtual col name instead of real table column name
            this.virtualColName = columnName;

            // Look for the associated View and get the associtaed column
            DB2View viewDep = getDependentView(monitor, db2DataSource, db2Index.getIndSchema().getName().trim(),
                db2Index.getName());
            if (viewDep != null) {
                this.tableColumn = viewDep.getAttribute(monitor, columnName);
            }
        }

    }

    public DB2IndexColumn(DB2Index db2Index, DB2TableColumn tableColumn, int ordinalPosition, boolean ascending)
    {
        this.db2Index = db2Index;
        this.tableColumn = tableColumn;
        this.colSeq = ordinalPosition;
        this.colOrder = ascending ? DB2IndexColOrder.A : DB2IndexColOrder.D; // Force Ascending or Descending ..
        this.virtualCol = DB2IndexColVirtual.N; // Force real column...
    }

    // -----------------
    // Helpers
    // -----------------
    private DB2View getDependentView(DBRProgressMonitor monitor, DB2DataSource db2DataSource, String indexSchema, String indexName)
        throws DBException
    {
        try (JDBCSession session = DBUtils.openMetaSession(monitor, this, "Read Index view dependency")) {
            try (JDBCPreparedStatement stmtSel = session.prepareStatement(I_DEP)) {
                stmtSel.setString(1, indexSchema);
                stmtSel.setString(2, indexName);
                JDBCResultSet dbResult = stmtSel.executeQuery();
                if (dbResult.next()) {
                    String viewSchema = dbResult.getString("BSCHEMA").trim();
                    String viewName = dbResult.getString("BNAME");
                    return DB2Utils.findViewBySchemaNameAndName(monitor, db2DataSource, viewSchema, viewName);
                } else {
                    return null;
                }
            }

        } catch (SQLException e) {
            throw new DBException(e, db2DataSource);
        }
    }

    // -----------------
    // Business Contract
    // -----------------
    @NotNull
    @Override
    public DB2DataSource getDataSource()
    {
        return db2Index.getDataSource();
    }

    @Override
    public DB2Index getParentObject()
    {
        return db2Index;
    }

    @NotNull
    @Override
    public DB2Index getIndex()
    {
        return db2Index;
    }

    @Nullable
    @Override
    public String getDescription()
    {
        if ((virtualCol == null) || (virtualCol.isNotVirtual())) {
            return tableColumn.getDescription();
        } else {
            return virtualCol.getName();
        }
    }

    @Override
    public int getOrdinalPosition()
    {
        return colSeq;
    }

    @Override
    public boolean isAscending()
    {
        return colOrder.isAscending();
    }

    @NotNull
    @Override
    public String getName()
    {
        if ((virtualCol != null) && (virtualCol.isVirtual())) {
            return virtualColName;
        } else {
            return tableColumn.getName();
        }
    }

    // -----------------
    // Properties
    // -----------------

    @Nullable
    @Override
    // col name in index name
    @Property(viewable = true, order = 1, id = "name")
    public DB2TableColumn getTableColumn()
    {
        return tableColumn;
    }

    // order in index schema name
    @Property(viewable = true, editable = false, order = 2, id = "indSchema")
    public Integer getColSeq()
    {
        return colSeq;
    }

    @Property(viewable = true, editable = true, order = 3, id = "table")
    public DB2IndexColOrder getColOrder()
    {
        return colOrder;
    }

    @Property(viewable = true, editable = false, order = 4, id = "indexType")
    public DB2IndexColVirtual getVirtualCol()
    {
        return virtualCol;
    }

    @Property(viewable = true, editable = false, order = 5)
    public String getVirtualColText()
    {
        return virtualColText;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_COLLATION)
    public String getCollationSchema()
    {
        return collationSchema;
    }

    @Property(viewable = false, editable = false, category = DB2Constants.CAT_COLLATION)
    public String getCollationNane()
    {
        return collationNane;
    }
}

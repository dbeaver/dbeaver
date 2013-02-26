package org.jkiss.dbeaver.ui.controls.resultset;

import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.data.DBDDataFilter;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridPos;

import java.util.*;

/**
 * Result set model
 */
public class ResultSetModel {
/*
    private DBDAttributeBinding[] columns = new DBDAttributeBinding[0];
    private DBDAttributeBinding[] visibleColumns = new DBDAttributeBinding[0];
    private DBDDataFilter dataFilter = new DBDDataFilter();
    private boolean singleSourceCells;

    // Data
    private List<Object[]> origRows = new ArrayList<Object[]>();
    private List<Object[]> curRows = new ArrayList<Object[]>();

    // Edited rows and cells
    private final Set<RowInfo> addedRows = new TreeSet<RowInfo>();
    private final Set<RowInfo> removedRows = new TreeSet<RowInfo>();
    private final Map<GridPos, Object> editedValues = new HashMap<GridPos, Object>();

    public DBDAttributeBinding[] getColumns()
    {
        return columns;
    }

    public DBDAttributeBinding[] getVisibleColumns()
    {
        return visibleColumns;
    }

    public DBDDataFilter getDataFilter()
    {
        return dataFilter;
    }

    public boolean isSingleSourceCells()
    {
        return singleSourceCells;
    }

    public List<Object[]> getOrigRows()
    {
        return origRows;
    }

    public List<Object[]> getCurRows()
    {
        return curRows;
    }

    public Set<RowInfo> getAddedRows()
    {
        return addedRows;
    }

    public Set<RowInfo> getRemovedRows()
    {
        return removedRows;
    }

    public Map<GridPos, Object> getEditedValues()
    {
        return editedValues;
    }

    public boolean setMetaData(DBDAttributeBinding[] columns)
    {
        boolean update = false;
        if (this.visibleColumns == null || this.visibleColumns.length != columns.length) {
            update = true;
        } else {
            for (int i = 0; i < this.visibleColumns.length; i++) {
                if (!this.visibleColumns[i].getMetaAttribute().equals(columns[i].getMetaAttribute())) {
                    update = true;
                    break;
                }
            }
        }
        if (update) {
            this.clearData();
            this.columns = this.visibleColumns = columns;
            this.dataFilter = new DBDDataFilter();
        }
        return update;
    }

    public void setData(List<Object[]> rows, boolean updateMetaData)
    {
        this.clearData();

        // Add new data
        this.origRows.addAll(rows);
        this.curRows.addAll(rows);

        if (updateMetaData) {
            // Check single source flag
            this.singleSourceCells = true;
            DBSEntity sourceTable = null;
            for (DBDAttributeBinding column : visibleColumns) {
                if (isColumnReadOnly(column)) {
                    break;
                }
                if (sourceTable == null) {
                    sourceTable = column.getRowIdentifier().getEntity();
                } else if (sourceTable != column.getRowIdentifier().getEntity()) {
                    singleSourceCells = false;
                    break;
                }
            }
        }
    }

    public void appendData(List<Object[]> rows)
    {
        origRows.addAll(rows);
        curRows.addAll(rows);
    }

    private void clearMetaData()
    {
        this.columns = this.visibleColumns = new DBDAttributeBinding[0];
        this.dataFilter = new DBDDataFilter();
    }

    private void clearData()
    {
        // Release all rows
        this.releaseAll();

        this.origRows = new ArrayList<Object[]>();
        this.curRows = new ArrayList<Object[]>();

        this.editedValues.clear();
        this.addedRows.clear();
        this.removedRows.clear();
    }
*/

}

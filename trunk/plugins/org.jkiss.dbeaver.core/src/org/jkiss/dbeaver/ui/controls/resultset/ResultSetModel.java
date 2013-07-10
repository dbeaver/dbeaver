package org.jkiss.dbeaver.ui.controls.resultset;

import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridPos;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * Result set model
 */
public class ResultSetModel {
    // Columns
    private DBDAttributeBinding[] columns = new DBDAttributeBinding[0];
    private List<DBDAttributeBinding> visibleColumns = new ArrayList<DBDAttributeBinding>();
    private DBDDataFilter dataFilter;
    private boolean singleSourceCells;

    // Data
    private List<Object[]> origRows = new ArrayList<Object[]>();
    private List<Object[]> curRows = new ArrayList<Object[]>();

    // Current row number (for record mode)
    private boolean hasData = false;
    // Flag saying that edited values update is in progress
    private boolean updateInProgress = false;

    // Edited rows and cells
    private final Set<RowInfo> addedRows = new TreeSet<RowInfo>();
    private final Set<RowInfo> removedRows = new TreeSet<RowInfo>();
    private final Map<GridPos, Object> editedValues = new HashMap<GridPos, Object>();
    private long executionTime;

    public ResultSetModel()
    {
        dataFilter = createDataFilter();
    }

    public DBDDataFilter createDataFilter()
    {
        List<DBDAttributeConstraint> constraints = new ArrayList<DBDAttributeConstraint>(columns.length);
        for (DBDAttributeBinding binding : columns) {
            DBDAttributeConstraint constraint = new DBDAttributeConstraint(binding);
            constraint.setVisible(visibleColumns.contains(binding));
            constraints.add(constraint);
        }
        return new DBDDataFilter(constraints);
    }

    public boolean isSingleSource()
    {
        return singleSourceCells;
    }

    /**
     * Returns single source of this result set. Usually it is a table.
     * If result set is a result of joins or contains synthetic columns then
     * single source is null. If driver doesn't support meta information
     * for queries then is will null.
     * @return single source entity
     */
    public DBSEntity getSingleSource()
    {
        if (!singleSourceCells) {
            return null;
        }
        return columns[0].getRowIdentifier().getEntity();
    }

    public boolean isCellModified(GridPos pos)
    {
        return !editedValues.isEmpty() && editedValues.containsKey(pos);
    }

    public void resetCellValue(GridPos cell, boolean delete)
    {
        if (editedValues != null && editedValues.containsKey(cell)) {
            Object[] row = this.curRows.get(cell.row);
            resetValue(row[cell.col]);
            row[cell.col] = this.editedValues.get(cell);
            this.editedValues.remove(cell);
        }
    }

    public DBDAttributeBinding[] getColumns()
    {
        return columns;
    }

    public int getColumnCount()
    {
        return columns.length;
    }

    public DBDAttributeBinding getColumn(int index)
    {
        return columns[index];
    }

    public List<DBDAttributeBinding> getVisibleColumns()
    {
        return visibleColumns;
    }

    public int getVisibleColumnCount()
    {
        return visibleColumns.size();
    }

    public DBDAttributeBinding getVisibleColumn(int index)
    {
        return visibleColumns.get(index);
    }

    public void setColumnVisibility(DBDAttributeBinding attribute, boolean visible)
    {
        DBDAttributeConstraint constraint = dataFilter.getConstraint(attribute);
        if (constraint.isVisible() != visible) {
            constraint.setVisible(visible);
            if (visible) {
                visibleColumns.add(attribute);
            } else {
                visibleColumns.remove(attribute);
            }
        }
    }

    public DBDAttributeBinding getAttributeBinding(DBSAttributeBase attribute)
    {
        for (DBDAttributeBinding binding : columns) {
            if (binding.getMetaAttribute() == attribute || binding.getEntityAttribute() == attribute) {
                return binding;
            }
        }
        return null;
    }

    DBDAttributeBinding getAttributeBinding(DBSEntity table, String columnName)
    {
        for (DBDAttributeBinding column : visibleColumns) {
            if ((table == null || column.getRowIdentifier().getEntity() == table) &&
                column.getAttributeName().equals(columnName)) {
                return column;
            }
        }
        return null;
    }

    public boolean isEmpty()
    {
        return getRowCount() <= 0 || visibleColumns.size() <= 0;
    }

    public int getRowCount()
    {
        return curRows.size();
    }

    public Object[] getRowData(int index)
    {
        return curRows.get(index);
    }

    /**
     * Gets cell value
     * @param row row index
     * @param column column index. Note: not visual column but real column index
     * @return value or null
     */
    public Object getCellValue(int row, int column)
    {
        return curRows.get(row)[column];
    }

    /**
     * Updates cell value. Saves previous value.
     * @param row row index
     * @param column column index. Note: not visual column but real column index
     * @param value new value
     * @return true on success
     */
    public boolean updateCellValue(int row, int column, Object value)
    {
        if (row < 0) {
            return false;
        }
        Object[] curRow = getRowData(row);
        Object oldValue = curRow[column];
        if ((value instanceof DBDValue && value == oldValue) || !CommonUtils.equalObjects(oldValue, value)) {
            // If DBDValue was updated (kind of LOB?) or actual value was changed
            if (DBUtils.isNullValue(oldValue) && DBUtils.isNullValue(value)) {
                // Both nulls - nothing to update
                return false;
            }
            // Do not add edited cell for new/deleted rows
            if (!isRowAdded(row) && !isRowDeleted(row)) {
                // Save old value
                GridPos cell = new GridPos(column, row);
                boolean cellWasEdited = editedValues.containsKey(cell); // use "contains" cos' old value may be "null".
                Object oldOldValue = editedValues.get(cell);
                if (cellWasEdited && !CommonUtils.equalObjects(oldValue, oldOldValue)) {
                    // Value rewrite - release previous stored old value
                    releaseValue(oldValue);
                } else {
                    editedValues.put(cell, oldValue);
                }
            }
            curRow[column] = value;
            return true;
        }
        return false;
    }

    /**
     * Sets new metadata of result set
     * @param columns columns metadata
     * @return true if new metadata differs from old one, false otherwise
     */
    public boolean setMetaData(DBDAttributeBinding[] columns)
    {
        boolean update = false;
        if (this.columns == null || this.columns.length != columns.length) {
            update = true;
        } else {
            if (dataFilter != null && dataFilter.hasFilters()) {
                // This is a filtered result set so keep old metadata.
                // Filtering modifies original query (adds subquery)
                // and it may change metadata (depends on driver)
                // but actually it doesn't change any column or table names/types
                // so let's keep old info
                return false;
            }

            for (int i = 0; i < this.columns.length; i++) {
                if (!this.columns[i].getMetaAttribute().equals(columns[i].getMetaAttribute())) {
                    update = true;
                    break;
                }
            }
        }
        if (update) {
            this.clearData();
            this.columns = columns;
            this.visibleColumns.clear();
            Collections.addAll(this.visibleColumns, this.columns);
            this.dataFilter = createDataFilter();
        }
        return update;
    }

    public void setData(List<Object[]> rows, boolean updateMetaData)
    {
        // Clear previous data
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
                    singleSourceCells = false;
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

        hasData = true;
    }

    public void appendData(List<Object[]> rows)
    {
        origRows.addAll(rows);
        curRows.addAll(rows);
    }

    void clearData()
    {
        // Refresh all rows
        this.releaseAll();
        this.origRows = new ArrayList<Object[]>();
        this.curRows = new ArrayList<Object[]>();

        this.editedValues.clear();
        this.addedRows.clear();
        this.removedRows.clear();

        hasData = false;
    }


    public boolean hasData()
    {
        return hasData;
    }

    public boolean isDirty()
    {
        return !editedValues.isEmpty() || !addedRows.isEmpty() || !removedRows.isEmpty();
    }

    boolean isColumnReadOnly(int column)
    {
        return column < 0 || column >= visibleColumns.size() || isColumnReadOnly(visibleColumns.get(column));
    }

    boolean isColumnReadOnly(DBDAttributeBinding column)
    {
        if (column.getRowIdentifier() == null || !(column.getRowIdentifier().getEntity() instanceof DBSDataManipulator)) {
            return true;
        }
        DBSDataManipulator dataContainer = (DBSDataManipulator) column.getRowIdentifier().getEntity();
        return (dataContainer.getSupportedFeatures() & DBSDataManipulator.DATA_UPDATE) == 0;
    }

    Set<RowInfo> getAddedRows()
    {
        return addedRows;
    }

    Set<RowInfo> getRemovedRows()
    {
        return removedRows;
    }

    Map<GridPos, Object> getEditedValues()
    {
        return editedValues;
    }

    public boolean isUpdateInProgress()
    {
        return updateInProgress;
    }

    void setUpdateInProgress(boolean updateInProgress)
    {
        this.updateInProgress = updateInProgress;
    }

    boolean isRowAdded(int row)
    {
        return !addedRows.isEmpty() && addedRows.contains(new RowInfo(row));
    }

    void addNewRow(int rowNum, Object[] data)
    {
        curRows.add(rowNum, data);

        addedRows.add(new RowInfo(rowNum));
    }

    boolean isRowDeleted(int row)
    {
        return !removedRows.isEmpty() && removedRows.contains(new RowInfo(row));
    }

    /**
     * Removes row with specified index from data
     * @param rowNum row number
     * @return true if row was physically removed (only in case if this row was previously added)
     * or false if it just marked as deleted
     */
    boolean deleteRow(int rowNum)
    {
        RowInfo rowInfo = new RowInfo(rowNum);
        if (addedRows.contains(rowInfo)) {
            // Remove just added row
            addedRows.remove(rowInfo);
            cleanupRow(rowNum);
            return true;
        } else {
            // Mark row as deleted
            removedRows.add(rowInfo);
            return false;
        }
    }

    void cleanupRow(int rowNum)
    {
        releaseRow(this.curRows.get(rowNum));
        this.curRows.remove(rowNum);
        this.shiftRows(rowNum, -1);
    }

    boolean cleanupRows(Set<RowInfo> rows)
    {
        if (rows != null && !rows.isEmpty()) {
            // Remove rows (in descending order to prevent concurrent modification errors)
            int[] rowsToRemove = new int[rows.size()];
            int i = 0;
            for (RowInfo rowNum : rows) rowsToRemove[i++] = rowNum.row;
            Arrays.sort(rowsToRemove);
            for (i = rowsToRemove.length; i > 0; i--) {
                cleanupRow(rowsToRemove[i - 1]);
            }
            rows.clear();
            return true;
        } else {
            return false;
        }
    }

    void shiftRows(int rowNum, int delta)
    {
        // Slide all existing edited rows/cells down
        for (GridPos cell : editedValues.keySet()) {
            if (cell.row >= rowNum) cell.row += delta;
        }
        for (RowInfo row : addedRows) {
            if (row.row >= rowNum) row.row += delta;
        }
        for (RowInfo row : removedRows) {
            if (row.row >= rowNum) row.row += delta;
        }
    }

    private void releaseAll()
    {
        for (Object[] row : curRows) {
            releaseRow(row);
        }
        // Release edited values
        for (Object oldValue : editedValues.values()) {
            releaseValue(oldValue);
        }
    }

    private static void releaseRow(Object[] values)
    {
        for (Object value : values) {
            if (value instanceof DBDValue) {
                ((DBDValue)value).release();
            }
        }
    }

    static void releaseValue(Object value)
    {
        if (value instanceof DBDValue) {
            ((DBDValue)value).release();
        }
    }

    static void resetValue(Object value)
    {
        if (value instanceof DBDContent) {
            ((DBDContent)value).resetContents();
        }
    }

    public DBDDataFilter getDataFilter()
    {
        return dataFilter;
    }

    /**
     * Sets new data filter
     * @param dataFilter data filter
     * @return true if visible columns were changed. Spreadsheet has to be refreshed
     */
    boolean setDataFilter(DBDDataFilter dataFilter)
    {
        this.dataFilter = dataFilter;
        List<DBDAttributeBinding> newColumns = this.dataFilter.getOrderedVisibleAttributes();
        if (!newColumns.equals(visibleColumns)) {
            visibleColumns = newColumns;
            return true;
        }
        return false;
    }

    void resetOrdering()
    {
        // Sort locally
        curRows = new ArrayList<Object[]>(this.origRows);
        if (!dataFilter.hasOrdering()) {
            return;
        }
        Collections.sort(curRows, new Comparator<Object[]>() {
            @Override
            public int compare(Object[] row1, Object[] row2)
            {
                int result = 0;
                for (DBDAttributeConstraint co : dataFilter.getConstraints()) {
                    final DBDAttributeBinding binding = co.getAttribute();
                    if (binding == null) {
                        continue;
                    }
                    Object cell1 = row1[binding.getAttributeIndex()];
                    Object cell2 = row2[binding.getAttributeIndex()];
                    if (cell1 == cell2) {
                        result = 0;
                    } else if (DBUtils.isNullValue(cell1)) {
                        result = 1;
                    } else if (DBUtils.isNullValue(cell2)) {
                        result = -1;
                    } else if (cell1 instanceof Comparable<?>) {
                        result = ((Comparable)cell1).compareTo(cell2);
                    } else {
                        String str1 = cell1.toString();
                        String str2 = cell2.toString();
                        result = str1.compareTo(str2);
                    }
                    if (co.isOrderDescending()) {
                        result = -result;
                    }
                    if (result != 0) {
                        break;
                    }
                }
                return result;
            }
        });
    }

    public long getExecutionTime()
    {
        return executionTime;
    }

    public void setExecutionTime(long executionTime)
    {
        this.executionTime = executionTime;
    }

}

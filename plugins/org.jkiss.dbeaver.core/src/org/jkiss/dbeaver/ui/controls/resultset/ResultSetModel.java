/*
 * Copyright (C) 2010-2014 Serge Rieder
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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.DBCStatistics;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.ui.controls.lightgrid.GridCell;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * Result set model
 */
public class ResultSetModel {

    static final Log log = LogFactory.getLog(ResultSetModel.class);

    // Columns
    private DBDAttributeBinding[] columns = new DBDAttributeBinding[0];
    private List<DBDAttributeBinding> visibleColumns = new ArrayList<DBDAttributeBinding>();
    private DBDDataFilter dataFilter;
    private boolean singleSourceCells;

    // Data
    private List<RowData> curRows = new ArrayList<RowData>();
    private int changesCount = 0;
    private volatile boolean hasData = false;
    // Flag saying that edited values update is in progress
    private volatile boolean updateInProgress = false;

    // Edited rows and cells
    private DBCStatistics statistics;

    public ResultSetModel()
    {
        dataFilter = createDataFilter();
    }

    @NotNull
    public DBDDataFilter createDataFilter()
    {
        List<DBDAttributeConstraint> constraints = new ArrayList<DBDAttributeConstraint>(columns.length);
        for (DBDAttributeBinding binding : columns) {
            addConstraints(constraints, binding);
        }
        return new DBDDataFilter(constraints);
    }

    private void addConstraints(List<DBDAttributeConstraint> constraints, DBDAttributeBinding binding) {
        DBDAttributeConstraint constraint = new DBDAttributeConstraint(binding);
        constraint.setVisible(visibleColumns.contains(binding));
        constraints.add(constraint);
        List<DBDAttributeBinding> nestedBindings = binding.getNestedBindings();
        if (nestedBindings != null) {
            for (DBDAttributeBinding nested : nestedBindings) {
                addConstraints(constraints, nested);
            }
        }
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
    @Nullable
    public DBSEntity getSingleSource()
    {
        if (!singleSourceCells) {
            return null;
        }
        DBDRowIdentifier rowIdentifier = columns[0].getRowIdentifier();
        assert rowIdentifier != null;
        return rowIdentifier.getEntity();
    }

    public void resetCellValue(GridCell cell)
    {
        RowData row = (RowData) cell.row;
        DBDAttributeBinding attr = ((DBDAttributeBinding) cell.col);
        if (row.changes != null && row.changes.containsKey(attr)) {
            resetValue(getCellValue(row, attr));
            updateCellValue(row, attr, row.changes.get(cell.col), false);
            row.changes.remove(cell.col);
            if (row.changes.isEmpty()) {
                row.changes = null;
            }
            if (row.getState() == RowData.STATE_NORMAL) {
                changesCount--;
            }
        }
    }

    public void refreshChangeCount()
    {
        changesCount = 0;
        for (RowData row : curRows) {
            if (row.getState() != RowData.STATE_NORMAL) {
                changesCount++;
            } else if (row.changes != null) {
                changesCount += row.changes.size();
            }
        }
    }

    @NotNull
    public DBDAttributeBinding[] getColumns()
    {
        return columns;
    }

    public int getColumnCount()
    {
        return columns.length;
    }

    @NotNull
    public DBDAttributeBinding getColumn(int index)
    {
        return columns[index];
    }

    public int getVisibleColumnIndex(@NotNull DBDAttributeBinding column)
    {
        return visibleColumns.indexOf(column);
    }

    @NotNull
    public List<DBDAttributeBinding> getVisibleColumns()
    {
        return visibleColumns;
    }

    public int getVisibleColumnCount()
    {
        return visibleColumns.size();
    }

    @NotNull
    public DBDAttributeBinding getVisibleColumn(int index)
    {
        return visibleColumns.get(index);
    }

    public void setColumnVisibility(@NotNull DBDAttributeBinding attribute, boolean visible)
    {
        DBDAttributeConstraint constraint = dataFilter.getConstraint(attribute);
        if (constraint != null && constraint.isVisible() != visible) {
            constraint.setVisible(visible);
            if (visible) {
                visibleColumns.add(attribute);
            } else {
                visibleColumns.remove(attribute);
            }
        }
    }

    @Nullable
    public DBDAttributeBinding getAttributeBinding(@NotNull DBSAttributeBase attribute)
    {
        for (DBDAttributeBinding binding : columns) {
            if (binding.matches(attribute)) {
                return binding;
            }
            DBDAttributeBinding subBinding = binding.getNestedBinding(attribute);
            if (subBinding != null) {
                return subBinding;
            }
        }
        return null;
    }

    @Nullable
    DBDAttributeBinding getAttributeBinding(@Nullable DBSEntity table, @NotNull String columnName)
    {
        for (DBDAttributeBinding column : visibleColumns) {
            DBDRowIdentifier rowIdentifier = column.getRowIdentifier();
            if ((table == null || (rowIdentifier != null && rowIdentifier.getEntity() == table)) &&
                column.getName().equals(columnName)) {
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

    @NotNull
    public List<RowData> getAllRows() {
        return curRows;
    }

    @NotNull
    public Object[] getRowData(int index)
    {
        return curRows.get(index).values;
    }

    @NotNull
    public RowData getRow(int index)
    {
        return curRows.get(index);
    }

    @Nullable
    public Object getCellValue(@NotNull RowData row, @NotNull DBDAttributeBinding column) {
        int depth = column.getLevel();
        if (depth == 0) {
            return row.values[column.getOrdinalPosition()];
        }
        Object curValue = row.values[column.getTopParent().getOrdinalPosition()];

        for (int i = 0; i < depth; i++) {
            if (curValue == null) {
                break;
            }
            DBDAttributeBinding attr = column.getParent(depth - i - 1);
            assert attr != null;
            if (curValue instanceof DBDStructure) {
                try {
                    curValue = ((DBDStructure) curValue).getAttributeValue(attr.getAttribute());
                } catch (DBCException e) {
                    log.warn("Error getting field [" + attr.getName() + "] value", e);
                    curValue = null;
                    break;
                }
            } else {
                log.debug("No structure value handler while trying to read nested attribute [" + attr.getName() + "]");
                curValue = null;
                break;
            }
        }

        return curValue;
    }

    /**
     * Updates cell value. Saves previous value.
     * @param row row index
     * @param attr Attribute
     * @param value new value
     * @return true on success
     */
    public boolean updateCellValue(@NotNull RowData row, @NotNull DBDAttributeBinding attr, @Nullable Object value)
    {
        return updateCellValue(row, attr, value, true);
    }

    public boolean updateCellValue(@NotNull RowData row, @NotNull DBDAttributeBinding attr, @Nullable Object value, boolean updateChanges)
    {
        int depth = attr.getLevel();
        int rootIndex;
        if (depth == 0) {
            rootIndex = attr.getOrdinalPosition();
        } else {
            rootIndex = attr.getTopParent().getOrdinalPosition();
        }
        Object rootValue = row.values[rootIndex];
        Object ownerValue = depth > 0 ? rootValue : null;
        {
            // Obtain owner value and create all intermediate values
            for (int i = 0; i < depth; i++) {
                if (ownerValue == null) {
                    // Create new owner object
                    log.warn("Null owner value");
                    return false;
                }
                if (i == depth - 1) {
                    break;
                }
                DBDAttributeBinding ownerAttr = attr.getParent(depth - i - 1);
                assert ownerAttr != null;
                if (ownerValue instanceof DBDStructure) {
                    try {
                        ownerValue = ((DBDStructure) ownerValue).getAttributeValue(ownerAttr.getAttribute());
                    } catch (DBCException e) {
                        log.warn("Error getting field [" + ownerAttr.getName() + "] value", e);
                        return false;
                    }
                }
            }
        }
        // Get old value
        Object oldValue = rootValue;
        if (ownerValue != null) {
            oldValue = null;
            if (ownerValue instanceof DBDStructure) {
                try {
                    oldValue = ((DBDStructure) ownerValue).getAttributeValue(attr.getAttribute());
                } catch (DBCException e) {
                    log.error("Error getting [" + attr.getName() + "] value", e);
                }
            } else {
                log.warn("Value [" + ownerValue + "] edit is not supported");
            }
        }
        if ((value instanceof DBDValue && value == oldValue) || !CommonUtils.equalObjects(oldValue, value)) {
            // If DBDValue was updated (kind of LOB?) or actual value was changed
            if (ownerValue == null && DBUtils.isNullValue(oldValue) && DBUtils.isNullValue(value)) {
                // Both nulls - nothing to update
                return false;
            }
            // Do not add edited cell for new/deleted rows
            if (row.getState() == RowData.STATE_NORMAL) {

                boolean cellWasEdited = row.changes != null && row.changes.containsKey(attr);
                Object oldOldValue = !cellWasEdited ? null : row.changes.get(attr);
                if (cellWasEdited && !CommonUtils.equalObjects(oldValue, oldOldValue)) {
                    // Value rewrite - release previous stored old value
                    releaseValue(oldValue);
                } else if (updateChanges) {
                    if (row.changes == null) {
                        row.changes = new IdentityHashMap<DBDAttributeBinding, Object>();
                    }
                    row.changes.put(attr, oldValue);
                }
                if (updateChanges && row.getState() == RowData.STATE_NORMAL && !cellWasEdited) {
                    changesCount++;
                }
            }
            if (ownerValue != null) {
                if (ownerValue instanceof DBDStructure) {
                    try {
                        ((DBDStructure) ownerValue).setAttributeValue(attr.getAttribute(), value);
                    } catch (DBCException e) {
                        log.error("Error setting [" + attr.getName() + "] value", e);
                    }
                } else {
                    log.warn("Value [" + ownerValue + "] edit is not supported");
                }
            } else {
                row.values[rootIndex] = value;
            }
            return true;
        }
        return false;
    }

    /**
     * Sets new metadata of result set
     * @param columns columns metadata
     * @return true if new metadata differs from old one, false otherwise
     */
    public boolean setMetaData(@NotNull DBDAttributeBinding[] columns)
    {
        boolean update = false;
        if (this.columns == null || this.columns.length != columns.length) {
            update = true;
        } else {
/*
            if (dataFilter != null && dataFilter.hasFilters()) {
                // This is a filtered result set so keep old metadata.
                // Filtering modifies original query (adds sub-query)
                // and it may change metadata (depends on driver)
                // but actually it doesn't change any column or table names/types
                // so let's keep old info
                return false;
            }
*/

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
            for (DBDAttributeBinding binding : this.columns) {
                DBDPseudoAttribute pseudoAttribute = binding.getMetaAttribute().getPseudoAttribute();
                if (pseudoAttribute == null) {
                    // Make visible "real" attributes
                    this.visibleColumns.add(binding);
                }
            }
        }
        return update;
    }

    public void setData(@NotNull List<Object[]> rows, boolean updateMetaData)
    {
        // Clear previous data
        this.clearData();

        // Add new data
        appendData(rows);

        if (updateMetaData) {
            this.dataFilter = createDataFilter();

            // Check single source flag
            this.singleSourceCells = true;
            DBSEntity sourceTable = null;
            for (DBDAttributeBinding column : visibleColumns) {
//                if (isColumnReadOnly(column)) {
//                    singleSourceCells = false;
//                    break;
//                }
                DBDRowIdentifier rowIdentifier = column.getRowIdentifier();
                if (rowIdentifier != null) {
                    if (sourceTable == null) {
                        sourceTable = rowIdentifier.getEntity();
                    } else if (sourceTable != rowIdentifier.getEntity()) {
                        singleSourceCells = false;
                        break;
                    }
                }
            }
        }

        hasData = true;
    }

    public void appendData(@NotNull List<Object[]> rows)
    {
        int rowCount = rows.size();
        List<RowData> newRows = new ArrayList<RowData>(rowCount);
        for (int i = 0; i < rowCount; i++) {
            newRows.add(
                new RowData(curRows.size() + i, rows.get(i)));
        }
        curRows.addAll(newRows);
    }

    void clearData()
    {
        // Refresh all rows
        this.releaseAll();
        this.curRows = new ArrayList<RowData>();

        hasData = false;
    }

    public boolean hasData()
    {
        return hasData;
    }

    public boolean isDirty()
    {
        return changesCount != 0;
    }

    boolean isColumnReadOnly(@NotNull DBDAttributeBinding column)
    {
        if (column.getRowIdentifier() == null || !(column.getRowIdentifier().getEntity() instanceof DBSDataManipulator) ||
            (column.getValueHandler().getFeatures() & DBDValueHandler.FEATURE_COMPOSITE) != 0) {
            return true;
        }
        DBSDataManipulator dataContainer = (DBSDataManipulator) column.getRowIdentifier().getEntity();
        return (dataContainer.getSupportedFeatures() & DBSDataManipulator.DATA_UPDATE) == 0;
    }

    public boolean isUpdateInProgress()
    {
        return updateInProgress;
    }

    void setUpdateInProgress(boolean updateInProgress)
    {
        this.updateInProgress = updateInProgress;
    }

    void addNewRow(int rowNum, @NotNull Object[] data)
    {
        RowData newRow = new RowData(curRows.size(), data);
        newRow.setVisualNumber(rowNum);
        newRow.setState(RowData.STATE_ADDED);
        shiftRows(newRow, 1);
        curRows.add(rowNum, newRow);
        changesCount++;
    }

    /**
     * Removes row with specified index from data
     * @param row row
     * @return true if row was physically removed (only in case if this row was previously added)
     * or false if it just marked as deleted
     */
    boolean deleteRow(@NotNull RowData row)
    {
        if (row.getState() == RowData.STATE_ADDED) {
            cleanupRow(row);
            return true;
        } else {
            // Mark row as deleted
            row.setState(RowData.STATE_REMOVED);
            changesCount++;
            return false;
        }
    }

    void cleanupRow(@NotNull RowData row)
    {
        releaseRow(row);
        this.curRows.remove(row.getVisualNumber());
        this.shiftRows(row, -1);
    }

    boolean cleanupRows(Collection<RowData> rows)
    {
        if (rows != null && !rows.isEmpty()) {
            // Remove rows (in descending order to prevent concurrent modification errors)
            List<RowData> rowsToRemove = new ArrayList<RowData>(rows);
            Collections.sort(rowsToRemove, new Comparator<RowData>() {
                @Override
                public int compare(RowData o1, RowData o2) {
                    return o1.getVisualNumber() - o2.getVisualNumber();
                }
            });
            for (RowData row : rowsToRemove) {
                cleanupRow(row);
            }
            return true;
        } else {
            return false;
        }
    }

    private void shiftRows(@NotNull RowData relative, int delta)
    {
        for (RowData row : curRows) {
            if (row.getVisualNumber() >= relative.getVisualNumber()) {
                row.setVisualNumber(row.getVisualNumber() + delta);
            }
            if (row.getRowNumber() >= relative.getRowNumber()) {
                row.setRowNumber(row.getRowNumber() + delta);
            }
        }
    }

    private void releaseAll()
    {
        for (RowData row : curRows) {
            releaseRow(row);
        }
    }

    private static void releaseRow(@NotNull RowData row)
    {
        for (Object value : row.values) {
            releaseValue(value);
        }
        if (row.changes != null) {
            for (Object oldValue : row.changes.values()) {
                releaseValue(oldValue);
            }
        }
    }

    static void releaseValue(@Nullable Object value)
    {
        if (value instanceof DBDValue) {
            ((DBDValue)value).release();
        }
    }

    static void resetValue(@Nullable Object value)
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
        // Check if filter misses some columns
        List<DBDAttributeConstraint> newConstraints = new ArrayList<DBDAttributeConstraint>();
        for (DBDAttributeBinding binding : columns) {
            if (dataFilter.getConstraint(binding) == null) {
                addConstraints(newConstraints, binding);
            }
        }
        if (!newConstraints.isEmpty()) {
            dataFilter.addConstraints(newConstraints);
        }

        List<DBDAttributeBinding> newBindings = new ArrayList<DBDAttributeBinding>();

        for (DBSAttributeBase attr : this.dataFilter.getOrderedVisibleAttributes()) {
            DBDAttributeBinding binding = getAttributeBinding(attr);
            if (binding != null) {
                newBindings.add(binding);
            }
        }
        if (!newBindings.equals(visibleColumns)) {
            visibleColumns = newBindings;
            return true;
        }
        return false;
    }

    void updateDataFilter(DBDDataFilter filter)
    {
        for (DBDAttributeConstraint constraint : filter.getConstraints()) {
            DBDAttributeConstraint filterConstraint = this.dataFilter.getConstraint(constraint.getAttribute());
            if (filterConstraint == null) {
                log.warn("Constraint for attribute [" + constraint.getAttribute().getName() + "] not found");
                continue;
            }
            filterConstraint.setCriteria(constraint.getCriteria());
            filterConstraint.setOperator(constraint.getOperator());
            filterConstraint.setReverseOperator(constraint.isReverseOperator());
            filterConstraint.setValue(constraint.getValue());
            if (constraint.getOrderPosition() > 0) {
                filterConstraint.setOrderPosition(constraint.getOrderPosition());
                filterConstraint.setOrderDescending(constraint.isOrderDescending());
            }
        }
        this.dataFilter.setWhere(filter.getWhere());
        this.dataFilter.setOrder(filter.getOrder());
    }

    void resetOrdering()
    {
        final boolean hasOrdering = dataFilter.hasOrdering();
        // Sort locally
        final List<DBDAttributeConstraint> orderConstraints = dataFilter.getOrderConstraints();
        Collections.sort(curRows, new Comparator<RowData>() {
            @Override
            public int compare(RowData row1, RowData row2)
            {
                if (!hasOrdering) {
                    return row1.getRowNumber() - row2.getRowNumber();
                }
                int result = 0;
                for (DBDAttributeConstraint co : orderConstraints) {
                    final DBDAttributeBinding binding = getAttributeBinding(co.getAttribute());
                    if (binding == null) {
                        continue;
                    }
                    Object cell1 = getCellValue(row1, binding);
                    Object cell2 = getCellValue(row2, binding);
                    if (cell1 == cell2) {
                        result = 0;
                    } else if (DBUtils.isNullValue(cell1)) {
                        result = 1;
                    } else if (DBUtils.isNullValue(cell2)) {
                        result = -1;
                    } else if (cell1 instanceof Comparable) {
                        result = ((Comparable)cell1).compareTo(cell2);
                    } else {
                        String str1 = String.valueOf(cell1);
                        String str2 = String.valueOf(cell2);
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
        for (int i = 0; i < curRows.size(); i++) {
            curRows.get(i).setVisualNumber(i);
        }
    }

    public DBCStatistics getStatistics()
    {
        return statistics;
    }

    public void setStatistics(DBCStatistics statistics)
    {
        this.statistics = statistics;
    }
}

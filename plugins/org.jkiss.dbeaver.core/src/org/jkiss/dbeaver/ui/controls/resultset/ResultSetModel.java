/*
 * DBeaver - Universal Database Manager
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
package org.jkiss.dbeaver.ui.controls.resultset;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.graphics.Color;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.trace.DBCTrace;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.DBSAttributeBase;
import org.jkiss.dbeaver.model.struct.DBSDataManipulator;
import org.jkiss.dbeaver.model.struct.DBSEntity;
import org.jkiss.dbeaver.model.virtual.DBVColorOverride;
import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.model.virtual.DBVUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * Result set model
 */
public class ResultSetModel {

    private static final Log log = Log.getLog(ResultSetModel.class);

    // Attributes
    private DBDAttributeBinding[] attributes = new DBDAttributeBinding[0];
    private List<DBDAttributeBinding> visibleAttributes = new ArrayList<>();
    private DBDAttributeBinding documentAttribute = null;
    private DBDDataFilter dataFilter;
    private boolean singleSourceCells;
    private DBCExecutionSource executionSource;

    // Data
    private List<ResultSetRow> curRows = new ArrayList<>();
    private Long totalRowCount = null;
    private int changesCount = 0;
    private volatile boolean hasData = false;
    // Flag saying that edited values update is in progress
    private volatile boolean updateInProgress = false;

    // Coloring
    private Map<DBDAttributeBinding, List<AttributeColorSettings>> colorMapping = new HashMap<>();

    private DBCStatistics statistics;
    private DBCTrace trace;
    private transient boolean metadataChanged;
    private transient boolean metadataDynamic;

    public static class AttributeColorSettings {
        private DBCLogicalOperator operator;
        private Object[] attributeValues;
        private Color colorForeground;
        private Color colorBackground;

        public AttributeColorSettings(DBVColorOverride co) {
            this.operator = co.getOperator();
            this.colorForeground = UIUtils.getSharedColor(co.getColorForeground());
            this.colorBackground = UIUtils.getSharedColor(co.getColorBackground());
            this.attributeValues = co.getAttributeValues();
        }

        public DBCLogicalOperator getOperator() {
            return operator;
        }

        public Object[] getAttributeValues() {
            return attributeValues;
        }

        public Color getColorForeground() {
            return colorForeground;
        }

        public Color getColorBackground() {
            return colorBackground;
        }

        public boolean evaluate(Object cellValue) {
            return operator.evaluate(cellValue, attributeValues);
        }
    }

    private final Comparator<DBDAttributeBinding> POSITION_SORTER = new Comparator<DBDAttributeBinding>() {
        @Override
        public int compare(DBDAttributeBinding o1, DBDAttributeBinding o2) {
            final DBDAttributeConstraint c1 = dataFilter.getConstraint(o1);
            final DBDAttributeConstraint c2 = dataFilter.getConstraint(o2);
            if (c1 == null) {
                log.debug("Missing constraint for " + o1);
                return -1;
            }
            if (c2 == null) {
                log.debug("Missing constraint for " + o2);
                return 1;
            }
            return c1.getVisualPosition() - c2.getVisualPosition();
        }
    };

    public ResultSetModel() {
        dataFilter = createDataFilter();
    }

    @NotNull
    public DBDDataFilter createDataFilter() {
        fillVisibleAttributes();
        List<DBDAttributeConstraint> constraints = new ArrayList<>(attributes.length);
        for (DBDAttributeBinding binding : attributes) {
            addConstraints(constraints, binding);
        }

        return new DBDDataFilter(constraints);
    }

    private void addConstraints(List<DBDAttributeConstraint> constraints, DBDAttributeBinding binding) {
        DBDAttributeConstraint constraint = new DBDAttributeConstraint(binding);
        constraint.setVisible(visibleAttributes.contains(binding) || binding.getParentObject() != null);
        constraints.add(constraint);
        List<DBDAttributeBinding> nestedBindings = binding.getNestedBindings();
        if (nestedBindings != null) {
            for (DBDAttributeBinding nested : nestedBindings) {
                addConstraints(constraints, nested);
            }
        }
    }

    public boolean isSingleSource() {
        return singleSourceCells;
    }

    /**
     * Returns single source of this result set. Usually it is a table.
     * If result set is a result of joins or contains synthetic attributes then
     * single source is null. If driver doesn't support meta information
     * for queries then is will null.
     *
     * @return single source entity
     */
    @Nullable
    public DBSEntity getSingleSource() {
        if (!singleSourceCells) {
            return null;
        }
        for (DBDAttributeBinding attr : attributes) {
            DBDRowIdentifier rowIdentifier = attr.getRowIdentifier();
            if (rowIdentifier != null) {
                return rowIdentifier.getEntity();
            }
        }
        return null;
    }

    public void resetCellValue(DBDAttributeBinding attr, ResultSetRow row) {
        if (row.getState() == ResultSetRow.STATE_REMOVED) {
            row.setState(ResultSetRow.STATE_NORMAL);
        } else if (row.changes != null && row.changes.containsKey(attr)) {
            DBUtils.resetValue(getCellValue(attr, row));
            updateCellValue(attr, row, row.changes.get(attr), false);
            row.resetChange(attr);
            if (row.getState() == ResultSetRow.STATE_NORMAL) {
                changesCount--;
            }
        }
    }

    public void refreshChangeCount() {
        changesCount = 0;
        for (ResultSetRow row : curRows) {
            if (row.getState() != ResultSetRow.STATE_NORMAL) {
                changesCount++;
            } else if (row.changes != null) {
                changesCount += row.changes.size();
            }
        }
    }

    public DBDAttributeBinding getDocumentAttribute() {
        return documentAttribute;
    }

    @NotNull
    public DBDAttributeBinding[] getAttributes() {
        return attributes;
    }

    @NotNull
    public DBDAttributeBinding getAttribute(int index) {
        return attributes[index];
    }

    @NotNull
    public List<DBDAttributeBinding> getVisibleAttributes() {
        return visibleAttributes;
    }

    public int getVisibleAttributeCount() {
        return visibleAttributes.size();
    }

    @Nullable
    public List<DBDAttributeBinding> getVisibleAttributes(DBDAttributeBinding parent) {
        final List<DBDAttributeBinding> nestedBindings = parent.getNestedBindings();
        if (nestedBindings == null || nestedBindings.isEmpty()) {
            return null;
        }
        List<DBDAttributeBinding> result = new ArrayList<>(nestedBindings);
        for (Iterator<DBDAttributeBinding> iter = result.iterator(); iter.hasNext(); ) {
            final DBDAttributeConstraint constraint = dataFilter.getConstraint(iter.next());
            if (constraint != null && !constraint.isVisible()) {
                iter.remove();
            }
        }
        return result;
    }

    @NotNull
    public DBDAttributeBinding getVisibleAttribute(int index) {
        return visibleAttributes.get(index);
    }

    public void setAttributeVisibility(@NotNull DBDAttributeBinding attribute, boolean visible) {
        DBDAttributeConstraint constraint = dataFilter.getConstraint(attribute);
        if (constraint != null && constraint.isVisible() != visible) {
            constraint.setVisible(visible);
            if (attribute.getParentObject() == null) {
                if (visible) {
                    visibleAttributes.add(attribute);
                } else {
                    visibleAttributes.remove(attribute);
                }
            }
        }
    }

    @Nullable
    public DBDAttributeBinding getAttributeBinding(@Nullable DBSAttributeBase attribute) {
        return DBUtils.findBinding(attributes, attribute);
    }

    @Nullable
    DBDAttributeBinding getAttributeBinding(@Nullable DBSEntity entity, @NotNull String attrName) {
        for (DBDAttributeBinding attribute : visibleAttributes) {
            DBDRowIdentifier rowIdentifier = attribute.getRowIdentifier();
            if ((entity == null || (rowIdentifier != null && rowIdentifier.getEntity() == entity)) &&
                attribute.getName().equals(attrName)) {
                return attribute;
            }
        }
        return null;
    }

    public boolean isEmpty() {
        return getRowCount() <= 0 || visibleAttributes.size() <= 0;
    }

    public int getRowCount() {
        return curRows.size();
    }

    @NotNull
    public List<ResultSetRow> getAllRows() {
        return curRows;
    }

    @NotNull
    public Object[] getRowData(int index) {
        return curRows.get(index).values;
    }

    @NotNull
    public ResultSetRow getRow(int index) {
        return curRows.get(index);
    }

    public Long getTotalRowCount() {
        return totalRowCount;
    }

    void setTotalRowCount(Long totalRowCount) {
        this.totalRowCount = totalRowCount;
    }

    @Nullable
    public Object getCellValue(@NotNull DBDAttributeBinding attribute, @NotNull ResultSetRow row) {
        int depth = attribute.getLevel();
        if (depth == 0) {
            final int index = attribute.getOrdinalPosition();
            if (index >= row.values.length) {
                log.debug("Bad attribute - index out of row values' bounds");
                return null;
            } else {
                return row.values[index];
            }
        }
        Object curValue = row.values[attribute.getTopParent().getOrdinalPosition()];

        for (int i = 0; i < depth; i++) {
            if (curValue == null) {
                break;
            }
            DBDAttributeBinding attr = attribute.getParent(depth - i - 1);
            assert attr != null;
            try {
                curValue = attr.extractNestedValue(curValue);
            } catch (DBCException e) {
                log.debug("Error reading nested value of [" + attr.getName() + "]", e);
                curValue = null;
                break;
            }
        }

        return curValue;
    }

    /**
     * Updates cell value. Saves previous value.
     *
     * @param attr  Attribute
     * @param row   row index
     * @param value new value
     * @return true on success
     */
    public boolean updateCellValue(@NotNull DBDAttributeBinding attr, @NotNull ResultSetRow row, @Nullable Object value) {
        return updateCellValue(attr, row, value, true);
    }

    public boolean updateCellValue(@NotNull DBDAttributeBinding attr, @NotNull ResultSetRow row, @Nullable Object value, boolean updateChanges) {
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
                try {
                    ownerValue = ownerAttr.extractNestedValue(ownerValue);
                } catch (DBCException e) {
                    log.warn("Error getting field [" + ownerAttr.getName() + "] value", e);
                    return false;
                }
            }
        }
        // Get old value
        Object oldValue = rootValue;
        if (ownerValue != null) {
            try {
                oldValue = attr.extractNestedValue(ownerValue);
            } catch (DBCException e) {
                log.error("Error getting [" + attr.getName() + "] value", e);
            }
        }
        if ((value instanceof DBDValue && value == oldValue && ((DBDValue) value).isModified()) || !CommonUtils.equalObjects(oldValue, value)) {
            // If DBDValue was updated (kind of CONTENT?) or actual value was changed
            if (ownerValue == null && DBUtils.isNullValue(oldValue) && DBUtils.isNullValue(value)) {
                // Both nulls - nothing to update
                return false;
            }
            // Do not add edited cell for new/deleted rows
            if (row.getState() == ResultSetRow.STATE_NORMAL) {

                boolean cellWasEdited = row.changes != null && row.changes.containsKey(attr);
                Object oldOldValue = !cellWasEdited ? null : row.changes.get(attr);
                if (cellWasEdited && !CommonUtils.equalObjects(oldValue, oldOldValue) && !CommonUtils.equalObjects(oldValue, value)) {
                    // Value rewrite - release previous stored old value
                    DBUtils.releaseValue(oldValue);
                } else if (updateChanges) {
                    if (value instanceof DBDValue || !CommonUtils.equalObjects(value, oldValue)) {
                        row.addChange(attr, oldValue);
                    } else {
                        updateChanges = false;
                    }
                }
                if (updateChanges && row.getState() == ResultSetRow.STATE_NORMAL && !cellWasEdited) {
                    changesCount++;
                }
            }
            if (ownerValue != null) {
                if (ownerValue instanceof DBDComposite) {
                    ((DBDComposite) ownerValue).setAttributeValue(attr.getAttribute(), value);
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

    boolean isDynamicMetadata() {
        return metadataDynamic;
    }

    boolean isMetadataChanged() {
        return metadataChanged;
    }

    /**
     * Sets new metadata of result set
     *
     * @param resultSet     resultset
     * @param newAttributes attributes metadata
     */
    public void setMetaData(@NotNull DBCResultSet resultSet, @NotNull DBDAttributeBinding[] newAttributes) {

        boolean update = false;
        DBCStatement sourceStatement = resultSet.getSourceStatement();
        if (sourceStatement != null) {
            this.executionSource = sourceStatement.getStatementSource();
        } else {
            this.executionSource = null;
        }
        if (resultSet instanceof DBCResultSetTrace) {
            this.trace = ((DBCResultSetTrace) resultSet).getExecutionTrace();
        } else {
            this.trace = null;
        }

        if (this.attributes == null || this.attributes.length == 0 || this.attributes.length != newAttributes.length || isDynamicMetadata()) {
            update = true;
        } else {
            for (int i = 0; i < this.attributes.length; i++) {
                if (!ResultSetUtils.equalAttributes(this.attributes[i].getMetaAttribute(), newAttributes[i].getMetaAttribute())) {
                    update = true;
                    break;
                }
            }
        }

        this.metadataChanged = update;
        if (update) {
            if (!ArrayUtils.isEmpty(this.attributes) && !ArrayUtils.isEmpty(newAttributes) && isDynamicMetadata() &&
                this.attributes[0].getTopParent().getMetaAttribute().getSource() == newAttributes[0].getTopParent().getMetaAttribute().getSource()) {
                // the same source
                metadataChanged = false;
            } else {
                metadataChanged = true;
            }
        }
        this.clearData();
        this.attributes = newAttributes;
        this.documentAttribute = null;

        metadataDynamic =
            this.attributes.length > 0 &&
            this.attributes[0].getTopParent().getDataSource().getInfo().isDynamicMetadata();


        {
            // Detect document attribute
            // It has to be only one attribute in list (excluding pseudo attributes).
            DBDAttributeBinding realAttr = null;
            if (attributes.length == 1) {
                realAttr = attributes[0];
            } else {
                for (DBDAttributeBinding attr : attributes) {
                    if (!attr.isPseudoAttribute()) {
                        if (realAttr != null) {
                            // more than one
                            realAttr = null;
                            break;
                        }
                        realAttr = attr;
                    }
                }
            }
            if (realAttr != null) {
                if (realAttr.getDataKind() == DBPDataKind.DOCUMENT || realAttr.getDataKind() == DBPDataKind.CONTENT) {
                    documentAttribute = realAttr;
                }
            }
            updateColorMapping();
        }
    }

    public void setData(@NotNull List<Object[]> rows) {
        // Clear previous data
        this.clearData();

        {
            // Extract nested attributes from single top-level attribute
            if (attributes.length == 1) {
                DBDAttributeBinding topAttr = attributes[0];
                if (topAttr.getDataKind() == DBPDataKind.DOCUMENT || topAttr.getDataKind() == DBPDataKind.STRUCT) {
                    List<DBDAttributeBinding> nested = topAttr.getNestedBindings();
                    if (nested != null && !nested.isEmpty()) {
                        attributes = nested.toArray(new DBDAttributeBinding[nested.size()]);
                        fillVisibleAttributes();
                    }
                }
            }
        }

        // Add new data
        appendData(rows);

        // Init data filter
        if (metadataChanged) {
            this.dataFilter = createDataFilter();
        } else {
            DBDDataFilter prevFilter = dataFilter;
            this.dataFilter = createDataFilter();
            updateDataFilter(prevFilter);
        }
        Collections.sort(this.visibleAttributes, POSITION_SORTER);

        {
            // Check single source flag
            this.singleSourceCells = true;
            DBSEntity sourceTable = null;
            for (DBDAttributeBinding attribute : visibleAttributes) {
                if (attribute.isPseudoAttribute()) {
                    continue;
                }
                DBDRowIdentifier rowIdentifier = attribute.getRowIdentifier();
                if (rowIdentifier != null) {
                    if (sourceTable == null) {
                        sourceTable = rowIdentifier.getEntity();
                    } else if (sourceTable != rowIdentifier.getEntity()) {
                        singleSourceCells = false;
                        break;
                    }
                } else {
                    // Do not mark it a multi-source.
                    // It is just some column without identifier, probably a constant or an expression
                    //singleSourceCells = false;
                    //break;
                }
            }
            updateColorMapping();
        }

        hasData = true;
    }

    boolean hasColorMapping(DBDAttributeBinding binding) {
        return colorMapping.containsKey(binding);
    }

    void updateColorMapping() {
        colorMapping.clear();
        DBSEntity entity = getSingleSource();
        if (entity == null) {
            return;
        }
        DBVEntity virtualEntity = DBVUtils.findVirtualEntity(entity, false);
        if (virtualEntity != null) {
            List<DBVColorOverride> coList = virtualEntity.getColorOverrides();
            if (!CommonUtils.isEmpty(coList)) {
                for (DBVColorOverride co : coList) {
                    DBDAttributeBinding binding = getAttributeBinding(entity, co.getAttributeName());
                    if (binding != null) {
                        List<AttributeColorSettings> cmList = colorMapping.get(binding);
                        if (cmList == null) {
                            cmList = new ArrayList<>();
                            colorMapping.put(binding, cmList);
                        }
                        cmList.add(new AttributeColorSettings(co));
                    }
                }
            }
        }
        updateRowColors(curRows);
    }

    private void updateRowColors(List<ResultSetRow> rows) {
        if (colorMapping.isEmpty()) {
            for (ResultSetRow row : rows) {
                row.foreground = null;
                row.background = null;
            }
        } else {
            for (Map.Entry<DBDAttributeBinding, List<AttributeColorSettings>> entry : colorMapping.entrySet()) {
                for (ResultSetRow row : rows) {
                    final DBDAttributeBinding binding = entry.getKey();
                    final Object cellValue = getCellValue(binding, row);
                    //final String cellStringValue = binding.getValueHandler().getValueDisplayString(binding, cellValue, DBDDisplayFormat.NATIVE);
                    for (AttributeColorSettings acs : entry.getValue()) {
                        if (acs.evaluate(cellValue)) {
                            row.foreground = acs.colorForeground;
                            row.background = acs.colorBackground;
                            break;
                        }
                    }
                }
            }
        }
    }

    public void appendData(@NotNull List<Object[]> rows) {
        int rowCount = rows.size();
        int firstRowNum = curRows.size();
        List<ResultSetRow> newRows = new ArrayList<>(rowCount);
        for (int i = 0; i < rowCount; i++) {
            newRows.add(
                new ResultSetRow(firstRowNum + i, rows.get(i)));
        }
        curRows.addAll(newRows);
        updateRowColors(newRows);
    }

    void clearData() {
        // Refresh all rows
        this.releaseAll();

        hasData = false;
    }

    public boolean hasData() {
        return hasData;
    }

    public boolean isDirty() {
        return changesCount != 0;
    }

    public boolean isAttributeReadOnly(@NotNull DBDAttributeBinding attribute) {
        if (!isSingleSource()) {
            return true;
        }
        if (attribute.getMetaAttribute().isReadOnly()) {
            return true;
        }
        DBDRowIdentifier rowIdentifier = attribute.getRowIdentifier();
        if (rowIdentifier == null || !(rowIdentifier.getEntity() instanceof DBSDataManipulator)) {
            return true;
        }
        DBSDataManipulator dataContainer = (DBSDataManipulator) rowIdentifier.getEntity();
        return (dataContainer.getSupportedFeatures() & DBSDataManipulator.DATA_UPDATE) == 0;
    }

    public boolean isUpdateInProgress() {
        return updateInProgress;
    }

    void setUpdateInProgress(boolean updateInProgress) {
        this.updateInProgress = updateInProgress;
    }

    @NotNull
    ResultSetRow addNewRow(int rowNum, @NotNull Object[] data) {
        ResultSetRow newRow = new ResultSetRow(curRows.size(), data);
        newRow.setVisualNumber(rowNum);
        newRow.setState(ResultSetRow.STATE_ADDED);
        shiftRows(newRow, 1);
        curRows.add(rowNum, newRow);
        changesCount++;
        return newRow;
    }

    /**
     * Removes row with specified index from data
     *
     * @param row row
     * @return true if row was physically removed (only in case if this row was previously added)
     * or false if it just marked as deleted
     */
    boolean deleteRow(@NotNull ResultSetRow row) {
        if (row.getState() == ResultSetRow.STATE_ADDED) {
            cleanupRow(row);
            return true;
        } else {
            // Mark row as deleted
            row.setState(ResultSetRow.STATE_REMOVED);
            changesCount++;
            return false;
        }
    }

    void cleanupRow(@NotNull ResultSetRow row) {
        row.release();
        this.curRows.remove(row.getVisualNumber());
        this.shiftRows(row, -1);
    }

    boolean cleanupRows(Collection<ResultSetRow> rows) {
        if (rows != null && !rows.isEmpty()) {
            // Remove rows (in descending order to prevent concurrent modification errors)
            List<ResultSetRow> rowsToRemove = new ArrayList<>(rows);
            Collections.sort(rowsToRemove, new Comparator<ResultSetRow>() {
                @Override
                public int compare(ResultSetRow o1, ResultSetRow o2) {
                    return o1.getVisualNumber() - o2.getVisualNumber();
                }
            });
            for (ResultSetRow row : rowsToRemove) {
                cleanupRow(row);
            }
            return true;
        } else {
            return false;
        }
    }

    private void shiftRows(@NotNull ResultSetRow relative, int delta) {
        for (ResultSetRow row : curRows) {
            if (row.getVisualNumber() >= relative.getVisualNumber()) {
                row.setVisualNumber(row.getVisualNumber() + delta);
            }
            if (row.getRowNumber() >= relative.getRowNumber()) {
                row.setRowNumber(row.getRowNumber() + delta);
            }
        }
    }

    private void releaseAll() {
        final List<ResultSetRow> oldRows = curRows;
        this.curRows = new ArrayList<>();
        this.totalRowCount = null;

        // Cleanup in separate job.
        // Sometimes model cleanup takes much time (e.g. freeing LOB values)
        // So let's do it in separate job to avoid UI locking
        new AbstractJob("Cleanup model") {
            {
                setSystem(true);
            }
            @Override
            protected IStatus run(DBRProgressMonitor monitor) {
                for (ResultSetRow row : oldRows) {
                    row.release();
                }
                return Status.OK_STATUS;
            }
        }.schedule();
    }

    public DBDDataFilter getDataFilter() {
        return dataFilter;
    }

    /**
     * Sets new data filter
     *
     * @param dataFilter data filter
     * @return true if visible attributes were changed. Spreadsheet has to be refreshed
     */
    boolean setDataFilter(DBDDataFilter dataFilter) {
        this.dataFilter = dataFilter;
        // Check if filter misses some attributes
        List<DBDAttributeConstraint> newConstraints = new ArrayList<>();
        for (DBDAttributeBinding binding : attributes) {
            if (dataFilter.getConstraint(binding) == null) {
                addConstraints(newConstraints, binding);
            }
        }
        if (!newConstraints.isEmpty()) {
            dataFilter.addConstraints(newConstraints);
        }

        List<DBDAttributeBinding> newBindings = new ArrayList<>();

        for (DBSAttributeBase attr : this.dataFilter.getOrderedVisibleAttributes()) {
            DBDAttributeBinding binding = getAttributeBinding(attr);
            if (binding != null) {
                newBindings.add(binding);
            }
        }
        if (!newBindings.equals(visibleAttributes)) {
            visibleAttributes = newBindings;
            return true;
        }
        return false;
    }

    void updateDataFilter(DBDDataFilter filter) {
        this.visibleAttributes.clear();
        Collections.addAll(this.visibleAttributes, this.attributes);
        for (DBDAttributeConstraint constraint : filter.getConstraints()) {
            DBDAttributeConstraint filterConstraint = this.dataFilter.getConstraint(constraint.getAttribute(), true);
            if (filterConstraint == null) {
                //log.warn("Constraint for attribute [" + constraint.getAttribute().getName() + "] not found");
                continue;
            }
            if (constraint.getOperator() != null) {
                filterConstraint.setOperator(constraint.getOperator());
                filterConstraint.setReverseOperator(constraint.isReverseOperator());
                filterConstraint.setValue(constraint.getValue());
            } else {
                filterConstraint.setCriteria(constraint.getCriteria());
            }
            filterConstraint.setOrderPosition(constraint.getOrderPosition());
            filterConstraint.setOrderDescending(constraint.isOrderDescending());
            filterConstraint.setVisible(constraint.isVisible());
            filterConstraint.setVisualPosition(constraint.getVisualPosition());
            if (filterConstraint.getAttribute() instanceof DBDAttributeBinding) {
                if (!constraint.isVisible()) {
                    visibleAttributes.remove(filterConstraint.getAttribute());
                } else {
                    if (!visibleAttributes.contains(filterConstraint.getAttribute())) {
                        DBDAttributeBinding attribute = (DBDAttributeBinding) filterConstraint.getAttribute();
                        if (attribute.getParentObject() == null) {
                            // Add only root attributes
                            visibleAttributes.add(attribute);
                        }
                    }
                }
            }
        }

        if (filter.getConstraints().size() != attributes.length) {
            // Update visibility
            for (Iterator<DBDAttributeBinding> iter = visibleAttributes.iterator(); iter.hasNext(); ) {
                final DBDAttributeBinding attr = iter.next();
                if (filter.getConstraint(attr, true) == null) {
                    // No constraint for this attribute: use default visibility
                    if (!isVisibleByDefault(attr)) {
                        iter.remove();
                    }
                }
            }
        }

        Collections.sort(this.visibleAttributes, POSITION_SORTER);

        this.dataFilter.setWhere(filter.getWhere());
        this.dataFilter.setOrder(filter.getOrder());
        this.dataFilter.setAnyConstraint(filter.isAnyConstraint());
    }

    public void resetOrdering() {
        final boolean hasOrdering = dataFilter.hasOrdering();
        // Sort locally
        final List<DBDAttributeConstraint> orderConstraints = dataFilter.getOrderConstraints();
        Collections.sort(curRows, new Comparator<ResultSetRow>() {
            @Override
            public int compare(ResultSetRow row1, ResultSetRow row2) {
                if (!hasOrdering) {
                    return row1.getRowNumber() - row2.getRowNumber();
                }
                int result = 0;
                for (DBDAttributeConstraint co : orderConstraints) {
                    final DBDAttributeBinding binding = getAttributeBinding(co.getAttribute());
                    if (binding == null) {
                        continue;
                    }
                    Object cell1 = getCellValue(binding, row1);
                    Object cell2 = getCellValue(binding, row2);
                    if (cell1 == cell2) {
                        result = 0;
                    } else if (DBUtils.isNullValue(cell1)) {
                        result = 1;
                    } else if (DBUtils.isNullValue(cell2)) {
                        result = -1;
                    } else if (cell1 instanceof Comparable) {
                        result = ((Comparable) cell1).compareTo(cell2);
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

    private void fillVisibleAttributes() {
        this.visibleAttributes.clear();

        if (executionSource != null && executionSource.getDataContainer() instanceof DBSEntity) {
            // Filter pseudo attributes if we query single entity
            for (DBDAttributeBinding binding : this.attributes) {
                if (isVisibleByDefault(binding)) {
                    // Make visible "real" attributes
                    this.visibleAttributes.add(binding);
                }
            }
        } else {
            Collections.addAll(this.visibleAttributes, this.attributes);
        }
    }

    private static boolean isVisibleByDefault(DBDAttributeBinding binding) {
        return !binding.isPseudoAttribute();
    }

    public DBCStatistics getStatistics() {
        return statistics;
    }

    public void setStatistics(DBCStatistics statistics) {
        this.statistics = statistics;
    }

    public DBCTrace getTrace() {
        return trace;
    }
}

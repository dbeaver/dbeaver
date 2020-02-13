/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
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
import org.eclipse.swt.graphics.RGB;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.DBException;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ModelPreferences;
import org.jkiss.dbeaver.model.DBPDataKind;
import org.jkiss.dbeaver.model.DBPDataSourceContainer;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.*;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.trace.DBCTrace;
import org.jkiss.dbeaver.model.runtime.AbstractJob;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
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
    private DBSEntity singleSourceEntity;
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
        private boolean rangeCheck;
        private boolean singleColumn;
        private Object[] attributeValues;
        private Color colorForeground, colorForeground2;
        private Color colorBackground, colorBackground2;

        AttributeColorSettings(DBVColorOverride co) {
            this.operator = co.getOperator();
            this.rangeCheck = co.isRange();
            this.singleColumn = co.isSingleColumn();
            this.colorForeground = getColor(co.getColorForeground());
            this.colorForeground2 = getColor(co.getColorForeground2());
            this.colorBackground = getColor(co.getColorBackground());
            this.colorBackground2 = getColor(co.getColorBackground2());
            this.attributeValues = co.getAttributeValues();
        }

        private static Color getColor(String color) {
            if (CommonUtils.isEmpty(color)) {
                return null;
            }
            return UIUtils.getSharedColor(color);
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
        return singleSourceEntity != null;
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
        return singleSourceEntity;
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

    /**
     * Returns real (non-virtual) attribute bindings
     */
    @NotNull
    public DBDAttributeBinding[] getRealAttributes() {
        List<DBDAttributeBinding> result = new ArrayList<>();
        for (DBDAttributeBinding attr : attributes) {
            if (!attr.isCustom()) {
                result.add(attr);
            }
        }
        return result.toArray(new DBDAttributeBinding[0]);
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

    public DBVEntity getVirtualEntity(boolean create) {
        DBSEntity entity = isSingleSource() ? getSingleSource() : null;
        return getVirtualEntity(entity, create);
    }

    public DBVEntity getVirtualEntity(DBSEntity entity, boolean create) {
        if (entity != null) {
            return DBVUtils.getVirtualEntity(entity, true);
        }
        DBSDataContainer dataContainer = getDataContainer();
        if (dataContainer != null) {
            return DBVUtils.getVirtualEntity(dataContainer, create);
        }
        return null;
    }

    @Nullable
    private DBSDataContainer getDataContainer() {
        return executionSource == null ? null : executionSource.getDataContainer();
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
        return DBUtils.getAttributeValue(attribute, attributes, row.values);
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
                    log.warn("Null owner value for '" + attr.getName() + "', row " + row.getVisualNumber());
                    return false;
                }
                if (i == depth - 1) {
                    break;
                }
                DBDAttributeBinding ownerAttr = attr.getParent(depth - i - 1);
                assert ownerAttr != null;
                try {
                    Object nestedValue = ownerAttr.extractNestedValue(ownerValue);
                    if (nestedValue == null) {
                        // Try to create nested value
                        DBCExecutionContext context = DBUtils.getDefaultContext(ownerAttr, false);
                        nestedValue = DBUtils.createNewAttributeValue(context, ownerAttr.getValueHandler(), ownerAttr.getAttribute(), DBDComplexValue.class);
                        if (ownerValue instanceof DBDComposite) {
                            ((DBDComposite) ownerValue).setAttributeValue(ownerAttr, nestedValue);
                        }
                        if (ownerAttr.getDataKind() == DBPDataKind.ARRAY) {
                            // That's a tough case. Collection of elements. We need to create first element in this collection
                            if (nestedValue instanceof DBDCollection) {
                                Object elemValue = null;
                                try {
                                    DBSDataType componentType = ((DBDCollection) nestedValue).getComponentType();
                                    DBDValueHandler elemValueHandler = DBUtils.findValueHandler(context.getDataSource(), componentType);
                                    elemValue = DBUtils.createNewAttributeValue(context, elemValueHandler, componentType, DBDComplexValue.class);
                                } catch (DBException e) {
                                    log.warn("Error while getting component type name", e);
                                }
                                ((DBDCollection) nestedValue).setContents(new Object[] { elemValue } );
                            } else {
                                log.warn("Attribute '" + ownerAttr.getName() + "' has collection type but attribute value is not a collection: " + nestedValue);
                            }
                        }
                        if (ownerValue instanceof DBDComposite) {
                            ((DBDComposite) ownerValue).setAttributeValue(ownerAttr, nestedValue);
                        }
                    }
                    ownerValue = nestedValue;
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
            // Check composite type
            if (ownerValue != null) {
                if (ownerValue instanceof DBDCollection) {
                    DBDCollection collection = (DBDCollection) ownerValue;
                    if (collection.getItemCount() > 0) {
                        ownerValue = collection.getItem(0);
                    }
                }
                if (!(ownerValue instanceof DBDComposite)) {
                    log.warn("Value [" + ownerValue + "] edit is not supported");
                    return false;
                }
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
                ((DBDComposite) ownerValue).setAttributeValue(attr.getAttribute(), value);
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

    public boolean isMetadataChanged() {
        return metadataChanged;
    }

    /**
     * Sets new metadata of result set
     *
     * @param resultSet     resultset
     * @param newAttributes attributes metadata
     */
    public void setMetaData(@NotNull DBCResultSet resultSet, @NotNull DBDAttributeBinding[] newAttributes) {

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

        this.clearData();
        this.updateMetaData(newAttributes);
    }

    void updateMetaData(@NotNull DBDAttributeBinding[] newAttributes) {
        boolean update = false;
        if (this.attributes == null || this.attributes.length == 0 || this.attributes.length != newAttributes.length || isDynamicMetadata()) {
            update = true;
        } else {
            for (int i = 0; i < this.attributes.length; i++) {
                DBCAttributeMetaData oldMeta = this.attributes[i].getMetaAttribute();
                DBCAttributeMetaData newMeta = newAttributes[i].getMetaAttribute();
                if ((oldMeta == null && newMeta != null) || (oldMeta != null && newMeta == null)) {
                    update = true;
                    break;
                } else if (oldMeta == newMeta) {
                    continue;
                }
                if (!ResultSetUtils.equalAttributes(oldMeta, newMeta)) {
                    update = true;
                    break;
                }
            }
        }

        this.metadataChanged = update;
        if (update) {
            if (!ArrayUtils.isEmpty(this.attributes) && !ArrayUtils.isEmpty(newAttributes) && isDynamicMetadata() &&
                this.attributes[0].getTopParent().getMetaAttribute() != null && newAttributes[0].getTopParent().getMetaAttribute() != null &&
                this.attributes[0].getTopParent().getMetaAttribute().getSource() == newAttributes[0].getTopParent().getMetaAttribute().getSource())
            {
                // the same source
                metadataChanged = false;
            } else {
                metadataChanged = true;
            }
        }

        this.attributes = newAttributes;
        this.documentAttribute = null;

        this.metadataDynamic =
            this.attributes.length > 0 &&
            this.attributes[0].getTopParent().getDataSource().getInfo().isDynamicMetadata();

        {
            // Detect document attribute
            // It has to be only one attribute in list (excluding pseudo attributes).
            DBDAttributeBinding realAttr = null;
            if (this.attributes.length == 1) {
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
        }
    }

    void updateDataFilter() {
        // Init data filter
        if (metadataChanged) {
            this.dataFilter = createDataFilter();
        } else {
            DBDDataFilter prevFilter = dataFilter;
            this.dataFilter = createDataFilter();
            updateDataFilter(prevFilter, false);
        }
    }

    public void setData(@NotNull List<Object[]> rows) {
        // Clear previous data
        this.clearData();

        {
            // Extract nested attributes from single top-level attribute
            if (attributes.length == 1 && attributes[0].getDataSource().getContainer().getPreferenceStore().getBoolean(ModelPreferences.RESULT_TRANSFORM_COMPLEX_TYPES)) {
                DBDAttributeBinding topAttr = attributes[0];
                if (topAttr.getDataKind() == DBPDataKind.DOCUMENT) {
                    List<DBDAttributeBinding> nested = topAttr.getNestedBindings();
                    if (nested != null && !nested.isEmpty()) {
                        attributes = nested.toArray(new DBDAttributeBinding[0]);
                        fillVisibleAttributes();
                    }
                }
            }
        }

        // Add new data
        updateColorMapping(false);
        appendData(rows, true);
        updateDataFilter();

        this.visibleAttributes.sort(POSITION_SORTER);

        {
            // Check single source flag
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
                        sourceTable = null;
                        break;
                    }
                } else {
                    // Do not mark it a multi-source.
                    // It is just some column without identifier, probably a constant or an expression
                    //singleSourceCells = false;
                    //break;
                }
            }
            singleSourceEntity = sourceTable;
        }

        hasData = true;
    }

    boolean hasColorMapping(DBDAttributeBinding binding) {
        return colorMapping.containsKey(binding);
    }

    public void updateColorMapping(boolean reset) {
        colorMapping.clear();

        DBSDataContainer dataContainer = getDataContainer();
        if (dataContainer == null) {
            return;
        }
        DBVEntity virtualEntity = DBVUtils.getVirtualEntity(dataContainer, false);
        if (virtualEntity == null) {
            return;
        }
        {
            List<DBVColorOverride> coList = virtualEntity.getColorOverrides();
            if (!CommonUtils.isEmpty(coList)) {
                for (DBVColorOverride co : coList) {
                    DBDAttributeBinding binding = DBUtils.findObject(attributes, co.getAttributeName());
                    if (binding != null) {
                        List<AttributeColorSettings> cmList =
                            colorMapping.computeIfAbsent(binding, k -> new ArrayList<>());
                        cmList.add(new AttributeColorSettings(co));
                    } else {
                        log.debug("Attribute '" + co.getAttributeName() + "' not found in bindings. Skip colors.");
                    }
                }
            }
        }
        if (reset) {
            updateRowColors(true, curRows);
        }
    }

    private void updateRowColors(boolean reset, List<ResultSetRow> rows) {
        if (colorMapping.isEmpty() || reset) {
            for (ResultSetRow row : rows) {
                row.colorInfo = null;
            }
        }
        if (!colorMapping.isEmpty()) {
            for (Map.Entry<DBDAttributeBinding, List<AttributeColorSettings>> entry : colorMapping.entrySet()) {
                if (!ArrayUtils.contains(attributes, entry.getKey())) {
                    // This may happen during FK navigation - attributes are already updated while colors mapping are still old
                    continue;
                }

                for (ResultSetRow row : rows) {
                    for (AttributeColorSettings acs : entry.getValue()) {
                        Color background = null, foreground = null;
                        if (acs.rangeCheck) {
                            if (acs.attributeValues != null && acs.attributeValues.length > 1) {
                                double minValue = ResultSetUtils.makeNumericValue(acs.attributeValues[0]);
                                double maxValue = ResultSetUtils.makeNumericValue(acs.attributeValues[1]);
                                if (acs.colorBackground != null && acs.colorBackground2 != null) {
                                    final DBDAttributeBinding binding = entry.getKey();
                                    final Object cellValue = getCellValue(binding, row);
                                    double value = ResultSetUtils.makeNumericValue(cellValue);
                                    if (value >= minValue && value <= maxValue) {
                                        foreground = acs.colorForeground;
                                        RGB rowRGB = ResultSetUtils.makeGradientValue(acs.colorBackground.getRGB(), acs.colorBackground2.getRGB(), minValue, maxValue, value);
                                        background = UIUtils.getSharedColor(rowRGB);
                                    }
                                    // FIXME: coloring value before and after range. Maybe we need an option for this.
                                    /* else if (value < minValue) {
                                        foreground = acs.colorForeground;
                                        background = acs.colorBackground;
                                    } else if (value > maxValue) {
                                        foreground = acs.colorForeground2;
                                        background = acs.colorBackground2;
                                    }*/
                                }
                            }
                        } else {
                            final DBDAttributeBinding binding = entry.getKey();
                            final Object cellValue = getCellValue(binding, row);
                            if (acs.evaluate(cellValue)) {
                                foreground = acs.colorForeground;
                                background = acs.colorBackground;
                            }
                        }
                        if (foreground != null || background != null) {
                            ResultSetRow.ColorInfo colorInfo = row.colorInfo;
                            if (colorInfo == null) {
                                colorInfo = new ResultSetRow.ColorInfo();
                                row.colorInfo = colorInfo;
                            }
                            if (!acs.singleColumn) {
                                colorInfo.rowForeground = foreground;
                                colorInfo.rowBackground = background;
                            } else {
                                // Single column color
                                if (foreground != null) {
                                    Color[] cellFgColors = colorInfo.cellFgColors;
                                    if (cellFgColors == null) {
                                        cellFgColors = new Color[attributes.length];
                                        colorInfo.cellFgColors = cellFgColors;
                                    }
                                    cellFgColors[entry.getKey().getOrdinalPosition()] = foreground;
                                }
                                if (background != null) {
                                    Color[] cellBgColors = colorInfo.cellBgColors;
                                    if (cellBgColors == null) {
                                        cellBgColors = new Color[attributes.length];
                                        colorInfo.cellBgColors = cellBgColors;
                                    }
                                    cellBgColors[entry.getKey().getOrdinalPosition()] = background;
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    void appendData(@NotNull List<Object[]> rows, boolean resetOldRows) {
        if (resetOldRows) {
            curRows.clear();
        }
        int rowCount = rows.size();
        int firstRowNum = curRows.size();
        List<ResultSetRow> newRows = new ArrayList<>(rowCount);
        for (int i = 0; i < rowCount; i++) {
            newRows.add(
                new ResultSetRow(firstRowNum + i, rows.get(i)));
        }
        curRows.addAll(newRows);

        updateRowColors(resetOldRows, newRows);
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
//        if (!isSingleSource()) {
//            return true;
//        }
        if (attribute == null || attribute.getMetaAttribute() == null || attribute.getMetaAttribute().isReadOnly()) {
            return true;
        }
        DBDRowIdentifier rowIdentifier = attribute.getRowIdentifier();
        if (rowIdentifier == null || !(rowIdentifier.getEntity() instanceof DBSDataManipulator)) {
            return true;
        }
        DBSDataManipulator dataContainer = (DBSDataManipulator) rowIdentifier.getEntity();
        return (dataContainer.getSupportedFeatures() & DBSDataManipulator.DATA_UPDATE) == 0;
    }

    public String getAttributeReadOnlyStatus(@NotNull DBDAttributeBinding attribute) {
        if (attribute == null || attribute.getMetaAttribute() == null) {
            return "Null meta attribute";
        }
        if (attribute.getMetaAttribute().isReadOnly()) {
            return "Attribute is read-only";
        }
        DBDRowIdentifier rowIdentifier = attribute.getRowIdentifier();
        if (rowIdentifier == null) {
            return "No unique identifier found";
        }
        DBSDataManipulator dataContainer = (DBSDataManipulator) rowIdentifier.getEntity();
        if (!(rowIdentifier.getEntity() instanceof DBSDataManipulator)) {
            return "Underlying entity doesn't support data modification";
        }
        if ((dataContainer.getSupportedFeatures() & DBSDataManipulator.DATA_UPDATE) == 0) {
            return "Underlying entity doesn't support data update";
        }
        return null;
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
            rowsToRemove.sort(Comparator.comparingInt(ResultSetRow::getVisualNumber));
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

    void updateDataFilter(DBDDataFilter filter, boolean forceUpdate) {
        this.visibleAttributes.clear();
        Collections.addAll(this.visibleAttributes, this.attributes);
        List<DBDAttributeConstraint> missingConstraints = new ArrayList<>();
        for (DBDAttributeConstraint constraint : filter.getConstraints()) {
            DBDAttributeConstraint filterConstraint = this.dataFilter.getConstraint(constraint.getAttribute(), true);
            if (filterConstraint == null) {
                // Constraint not found
                // Let's add it just to visualize condition in filters text
                if (constraint.getOperator() != null) {
                    missingConstraints.add(constraint);
                }
                continue;
            }
            if ((!forceUpdate &&
                constraint.getVisualPosition() != DBDAttributeConstraint.NULL_VISUAL_POSITION && constraint.getVisualPosition() != filterConstraint.getVisualPosition() &&
                constraint.getVisualPosition() == constraint.getOriginalVisualPosition()))
            {
                // If ordinal position doesn't match then probably it is a wrong attribute.
                // There can be multiple attributes with the same name in rs (in some databases)

                // Also check that original visual pos is the same as current position.
                // Otherwise this means that column was reordered visually and we must respect this change

                // We check order position only when forceUpdate=true (otherwise all previosu filters will be reset, see #6311)
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
            if (constraint.getVisualPosition() != DBDAttributeConstraint.NULL_VISUAL_POSITION) {
                filterConstraint.setVisualPosition(constraint.getVisualPosition());
            }
            DBSAttributeBase cAttr = filterConstraint.getAttribute();
            if (cAttr instanceof DBDAttributeBinding) {
                if (!constraint.isVisible()) {
                    visibleAttributes.remove(cAttr);
                } else {
                    if (!visibleAttributes.contains(cAttr)) {
                        DBDAttributeBinding attribute = (DBDAttributeBinding) cAttr;
                        if (attribute.getParentObject() == null) {
                            // Add only root attributes
                            visibleAttributes.add(attribute);
                        }
                    }
                }
            }
        }

        if (!missingConstraints.isEmpty()) {
            this.dataFilter.addConstraints(missingConstraints);
        }

        if (filter.getConstraints().size() != attributes.length) {
            // Update visibility
            for (Iterator<DBDAttributeBinding> iter = visibleAttributes.iterator(); iter.hasNext(); ) {
                final DBDAttributeBinding attr = iter.next();
                if (filter.getConstraint(attr, true) == null) {
                    // No constraint for this attribute: use default visibility
                    if (!DBDAttributeConstraint.isVisibleByDefault(attr)) {
                        iter.remove();
                    }
                }
            }
        }

        this.visibleAttributes.sort(POSITION_SORTER);

        this.dataFilter.setWhere(filter.getWhere());
        this.dataFilter.setOrder(filter.getOrder());
        this.dataFilter.setAnyConstraint(filter.isAnyConstraint());
    }

    public void resetOrdering() {
        final boolean hasOrdering = dataFilter.hasOrdering();
        // Sort locally
        final List<DBDAttributeConstraint> orderConstraints = dataFilter.getOrderConstraints();
        curRows.sort((row1, row2) -> {
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
                result = DBUtils.compareDataValues(cell1, cell2);
                if (co.isOrderDescending()) {
                    result = -result;
                }
                if (result != 0) {
                    break;
                }
            }
            return result;
        });
        for (int i = 0; i < curRows.size(); i++) {
            curRows.get(i).setVisualNumber(i);
        }
    }

    private void fillVisibleAttributes() {
        this.visibleAttributes.clear();

        boolean entityDataView = executionSource != null && executionSource.getDataContainer() instanceof DBSEntity;

        DBSObjectFilter columnFilter = null;
        if (entityDataView) {
            // Detect column filter
            DBSEntity entity = (DBSEntity) executionSource.getDataContainer();
            DBPDataSourceContainer container = entity.getDataSource().getContainer();
            if (container.getPreferenceStore().getBoolean(ResultSetPreferences.RESULT_SET_USE_NAVIGATOR_FILTERS) && attributes.length > 0) {
                DBSEntityAttribute entityAttribute = attributes[0].getEntityAttribute();
                if (entityAttribute != null) {
                    columnFilter = container.getObjectFilter(entityAttribute.getClass(), entity, false);
                }
            }
        }

        // Filter pseudo attributes if we query single entity
        for (DBDAttributeBinding binding : this.attributes) {
            if (!entityDataView || DBDAttributeConstraint.isVisibleByDefault(binding)) {
                // Make visible "real" attributes
                if (columnFilter != null && !columnFilter.matches(binding.getName())) {
                    // Filtered out by column filter
                    continue;
                }
                this.visibleAttributes.add(binding);
            }
        }
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

/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2024 DBeaver Corp and others
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
import org.jkiss.dbeaver.model.data.hints.DBDValueHintContext;
import org.jkiss.dbeaver.model.data.hints.DBDValueHintProvider;
import org.jkiss.dbeaver.model.exec.*;
import org.jkiss.dbeaver.model.exec.trace.DBCTrace;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.dbeaver.model.runtime.VoidProgressMonitor;
import org.jkiss.dbeaver.model.struct.*;
import org.jkiss.dbeaver.model.virtual.DBVColorOverride;
import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.model.virtual.DBVUtils;
import org.jkiss.dbeaver.runtime.jobs.DataSourceJob;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.dbeaver.utils.RuntimeUtils;
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

    private final ResultSetHintContext hintContext;

    // Data
    private List<ResultSetRow> curRows = new ArrayList<>();
    private Long totalRowCount = null;
    private int changesCount = 0;
    private volatile boolean hasData = false;
    // Flag saying that edited values update is in progress
    private volatile DataSourceJob updateInProgress = null;

    private DBCStatistics statistics;
    private DBCTrace trace;
    private transient boolean metadataChanged;
    private transient boolean metadataDynamic;

    public static class AttributeColorSettings {
        private final DBCLogicalOperator operator;
        private final boolean rangeCheck;
        private final boolean singleColumn;
        private final Object[] attributeValues;
        private final Color colorForeground;
        private final Color colorForeground2;
        private final Color colorBackground;
        private final Color colorBackground2;

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

    private final Comparator<DBDAttributeBinding> POSITION_SORTER = new Comparator<>() {
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

    // Coloring
    private final Map<DBDAttributeBinding, List<AttributeColorSettings>> colorMapping = new TreeMap<>(POSITION_SORTER);

    public ResultSetModel() {
        this.hintContext = new ResultSetHintContext(this::getDataContainer);
        this.dataFilter = createDataFilter();
    }

    public DBDValueHintContext getHintContext() {
        return hintContext;
    }

    public List<DBDValueHintProvider> getHintProviders(DBDAttributeBinding attr) {
        return hintContext.getHintProviders(attr);
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
        int constraintsSize = constraints.size();
        DBDAttributeConstraint constraint = new DBDAttributeConstraint(binding, constraintsSize, constraintsSize);
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

    public void resetCellValue(ResultSetCellLocation cellLocation) {
        ResultSetRow row = cellLocation.getRow();
        DBDAttributeBinding attr = cellLocation.getAttribute();
        if (row.getState() == ResultSetRow.STATE_REMOVED) {
            row.setState(ResultSetRow.STATE_NORMAL);
        } else if (row.changes != null && row.changes.containsKey(attr)) {
            DBUtils.resetValue(getCellValue(cellLocation));
            try {
                Object origValue = row.changes.get(attr);
                if (origValue instanceof DBDAttributeBinding refAttr) {
                    // We reset entire row changes. Cleanup all references on the same top attribute + reset top attribute value
                    for (var changedValues = row.changes.entrySet().iterator(); changedValues.hasNext(); ) {
                        if (changedValues.next().getValue() == origValue) {
                            changedValues.remove();
                        }
                    }
                    attr = refAttr;
                    origValue = row.changes.get(attr);
                    cellLocation = new ResultSetCellLocation(attr, cellLocation.getRow(), null);
                }
                updateCellValue(cellLocation, origValue, false);
            } catch (DBException e) {
                log.error(e);
            }
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

    @NotNull
    public List<DBDAttributeBinding> getVisibleLeafAttributes() {
        final List<DBDAttributeBinding> children = new ArrayList<>();
        final Deque<DBDAttributeBinding> parents = new ArrayDeque<>(getVisibleAttributes());

        while (!parents.isEmpty()) {
            final DBDAttributeBinding attribute = parents.removeFirst();
            final List<DBDAttributeBinding> nested = getVisibleAttributes(attribute);

            if (CommonUtils.isEmpty(nested)) {
                children.add(attribute);
            } else {
                for (int i = nested.size() - 1; i >= 0; i--) {
                    parents.offerFirst(nested.get(i));
                }
            }
        }

        return children;
    }

    public void setAttributeVisibility(@NotNull DBDAttributeBinding attribute, boolean visible) {
        DBDAttributeConstraint constraint = dataFilter.getConstraint(attribute);
        if (constraint != null && constraint.isVisible() != visible) {
            constraint.setVisible(visible);
            if (attribute.getParentObject() == null || attribute.getParentObject() == documentAttribute) {
                if (visible) {
                    final int position = Math.min(constraint.getVisualPosition(), visibleAttributes.size());
                    visibleAttributes.add(position, attribute);
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

    @Nullable
    public DBDRowIdentifier getDefaultRowIdentifier() {
        for (DBDAttributeBinding column : attributes) {
            DBDRowIdentifier rowIdentifier = column.getRowIdentifier();
            if (rowIdentifier != null) {
                return rowIdentifier;
            }
        }
        return null;
    }

    void refreshValueHandlersConfiguration() {
        for (DBDAttributeBinding binding : attributes) {
            DBDValueHandler valueHandler = binding.getValueHandler();
            if (valueHandler instanceof DBDValueHandlerConfigurable) {
                ((DBDValueHandlerConfigurable) valueHandler).refreshValueHandlerConfiguration(binding);
            }
            DBDValueRenderer valueRenderer = binding.getValueRenderer();
            if (valueRenderer != valueHandler && valueRenderer instanceof DBDValueHandlerConfigurable) {
                ((DBDValueHandlerConfigurable) valueRenderer).refreshValueHandlerConfiguration(binding);
            }
        }
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
    public Object getCellValue(@NotNull ResultSetCellLocation cellLocation) {
        return getCellValue(cellLocation.getAttribute(), cellLocation.getRow(), cellLocation.getRowIndexes(), false);
    }

    @Nullable
    public Object getCellValue(@NotNull DBDAttributeBinding attribute, @NotNull ResultSetRow row) {
        return getCellValue(attribute, row, null, false);
    }

    @Nullable
    public Object getCellValue(
        @NotNull DBDAttributeBinding attribute,
        @NotNull ResultSetRow row,
        @Nullable int[] rowIndexes,
        boolean retrieveDeepestCollectionElement
    ) {
        return DBUtils.getAttributeValue(
            attribute,
            attributes,
            row.values,
            rowIndexes,
            retrieveDeepestCollectionElement
        );
    }

    /**
     * Updates cell value. Saves previous value.
     *
     * @param cellLocation cell location
     * @param value new value
     * @return true on success
     */
    public boolean updateCellValue(
        @NotNull ResultSetCellLocation cellLocation,
        @Nullable Object value) throws DBException {
        return updateCellValue(cellLocation, value, true);
    }

    public boolean updateCellValue(
        @NotNull ResultSetCellLocation cellLocation,
        @Nullable Object value,
        boolean updateChanges
    ) throws DBException {
        return updateCellValue(
            cellLocation.getAttribute(),
            cellLocation.getRow(),
            cellLocation.getRowIndexes(),
            value,
            updateChanges);
    }

    public boolean updateCellValue(
        @NotNull DBDAttributeBinding attr,
        @NotNull ResultSetRow row,
        @Nullable int[] rowIndexes,
        @Nullable Object value,
        boolean updateChanges
    ) throws DBException {
        // 1. Update root attribute
        // 2. Save old value in history (if it is complex then save root element)
        //
        // For complex values we save original value in history only once.
        // Then copy it into a new value and edit new value
        int depth = attr.getLevel();
        int rootIndex;
        DBDAttributeBinding topAttribute;
        if (depth == 0) {
            topAttribute = attr;
            rootIndex = attr.getOrdinalPosition();
        } else {
            topAttribute = attr.getTopParent();
            rootIndex = topAttribute.getOrdinalPosition();
        }
        if (row.getState() != ResultSetRow.STATE_NORMAL) {
            updateChanges = false;
        }
        if (updateChanges && row.changes == null) {
            row.changes = new HashMap<>();
        }

        Object oldHistoricValue = updateChanges ? row.changes.get(topAttribute) : null;
        Object currentValue = row.values[rootIndex];
        Object valueToEdit = currentValue;

        if (currentValue instanceof DBDValue) {
            // It is complex
            if (updateChanges && oldHistoricValue == null) {
                // Save original to history and create a copy
                if (currentValue instanceof DBDValueCloneable vc) {
                    try {
                        valueToEdit = vc.cloneValue(new VoidProgressMonitor());
                    } catch (DBCException e) {
                        log.error("Error copying cell value", e);
                    }
                } else {
                    log.debug("Cannot copy complex value. Undo is not possible!");
                }
                row.changes.put(topAttribute, currentValue);
            }
        } else {
            if (updateChanges && oldHistoricValue == null) {
                row.changes.put(topAttribute, currentValue);
            }
        }
        if (updateChanges && attr != topAttribute) {
            // Save reference on top attribute
            row.changes.put(attr, topAttribute);
        }

        if (value instanceof DBDValue) {
            // New value if also a complex value. Probably DBDContent
            // In this case it must be root attribute
            if (attr != topAttribute && valueToEdit instanceof DBDValue ownerValue) {
                DBUtils.updateAttributeValue(ownerValue, attr, rowIndexes, value);
            } else {
                valueToEdit = value;
            }
        } else if (valueToEdit instanceof DBDValue complexValue) {
            DBUtils.updateAttributeValue(complexValue, attr, rowIndexes, value);
        } else {
            valueToEdit = value;
        }
        row.values[rootIndex] = valueToEdit;

        if (updateChanges && row.getState() == ResultSetRow.STATE_NORMAL) {
            changesCount++;
        }
        return true;
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
        if (documentAttribute != null && newAttributes.length == 1 && newAttributes[0].getDataKind() == DBPDataKind.DOCUMENT &&
            isSameSource(this.documentAttribute, newAttributes[0]))
        {
            // The same document source
            update = false;
        } else if (this.attributes == null || this.attributes.length == 0 || this.attributes.length != newAttributes.length || isDynamicMetadata()) {
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
                if (!DBExecUtils.equalAttributes(oldMeta, newMeta)) {
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

        if (metadataChanged) {
            hintContext.resetCache();
        }
    }

    private boolean isSameSource(DBDAttributeBinding attr1, DBDAttributeBinding attr2) {
        if (attr1.getMetaAttribute() == null || attr2.getMetaAttribute() == null) {
            return false;
        }
        DBCEntityMetaData ent1 = attr1.getMetaAttribute().getEntityMetaData();
        DBCEntityMetaData ent2 = attr2.getMetaAttribute().getEntityMetaData();
        if (ent1 == null || ent2 == null) {
            return false;
        }
        return
            CommonUtils.equalObjects(ent1.getCatalogName(), ent2.getCatalogName()) &&
            CommonUtils.equalObjects(ent1.getSchemaName(), ent2.getSchemaName()) &&
            CommonUtils.equalObjects(ent1.getEntityName(), ent2.getEntityName());
    }

    void resetMetaData() {
        this.attributes = new DBDAttributeBinding[0];
        this.visibleAttributes.clear();
        this.documentAttribute = null;
        this.singleSourceEntity = null;
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

    public void setData(@NotNull DBRProgressMonitor monitor, @NotNull List<Object[]> rows) {
        // Clear previous data
        this.releaseAllData();
        this.clearData();

        {
            boolean isDocumentBased = false;

            // Extract nested attributes from single top-level attribute
            if (attributes.length == 1 && attributes[0].getDataSource().getContainer().getPreferenceStore().getBoolean(ModelPreferences.RESULT_TRANSFORM_COMPLEX_TYPES)) {
                DBDAttributeBinding topAttr = attributes[0];
                if (topAttr.getDataKind() == DBPDataKind.DOCUMENT) {
                    isDocumentBased = true;
                    List<DBDAttributeBinding> nested = topAttr.getNestedBindings();
                    if (nested != null && !nested.isEmpty()) {
                        attributes = nested.toArray(new DBDAttributeBinding[0]);
                        fillVisibleAttributes();
                    }
                }
            }

            if (isDocumentBased) {
                DBSDataContainer dataContainer = getDataContainer();
                if (dataContainer instanceof DBSEntity) {
                    singleSourceEntity = (DBSEntity) dataContainer;
                }
            }
        }

        // Add new data
        updateDataFilter();
        updateColorMapping(false);
        appendData(monitor, rows, true);
        updateDataFilter();

        this.visibleAttributes.sort(POSITION_SORTER);

        if (singleSourceEntity == null) {
            singleSourceEntity = DBExecUtils.detectSingleSourceTable(
                visibleAttributes.toArray(new DBDAttributeBinding[0]));
        }

        hasData = true;
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
                    ResultSetCellLocation cellLocation = new ResultSetCellLocation(entry.getKey(), row);
                    for (AttributeColorSettings acs : entry.getValue()) {
                        Color background = null, foreground = null;
                        if (acs.rangeCheck) {
                            if (acs.attributeValues != null && acs.attributeValues.length > 1) {
                                double minValue = DBExecUtils.makeNumericValue(acs.attributeValues[0]);
                                double maxValue = DBExecUtils.makeNumericValue(acs.attributeValues[1]);
                                final Object cellValue = getCellValue(cellLocation);
                                double value = DBExecUtils.makeNumericValue(cellValue);
                                if (value >= minValue && value <= maxValue) {
                                    if (acs.colorBackground != null && acs.colorBackground2 != null && value >= minValue && value <= maxValue) {
                                            RGB bgRowRGB = ResultSetUtils.makeGradientValue(acs.colorBackground.getRGB(), acs.colorBackground2.getRGB(), minValue, maxValue, value);
                                            background = UIUtils.getSharedColor(bgRowRGB);
                                            
                                        // FIXME: coloring value before and after range. Maybe we need an option for this.
                                        /* else if (value < minValue) {
                                            foreground = acs.colorForeground;
                                            background = acs.colorBackground;
                                        } else if (value > maxValue) {
                                            foreground = acs.colorForeground2;
                                            background = acs.colorBackground2;
                                        }*/
                                    }
                                    if (acs.colorForeground != null && acs.colorForeground2 != null) {
                                        RGB fgRowRGB1 = ResultSetUtils.makeGradientValue(acs.colorForeground.getRGB(), acs.colorForeground2.getRGB(), minValue, maxValue, value);
                                        foreground = UIUtils.getSharedColor(fgRowRGB1);
                                    } else if (acs.colorForeground != null || acs.colorForeground2 != null) {
                                        foreground = acs.colorForeground != null ? acs.colorForeground : acs.colorForeground2;
                                    }
                                }
                            }
                        } else {
                            final Object cellValue = getCellValue(cellLocation);
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

    void appendData(@NotNull DBRProgressMonitor monitor, @NotNull List<Object[]> rows, boolean resetOldRows) {
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

        try {
            hintContext.resetCache();
            hintContext.initProviders(attributes);
            hintContext.cacheRequiredData(monitor, newRows, metadataChanged);
        } catch (Exception e) {
            log.debug("Error caching data for column hints", e);
        }
    }

    void clearData() {
        // Refresh all rows
        this.curRows = new ArrayList<>();
        this.totalRowCount = null;
        this.singleSourceEntity = null;

        this.hasData = false;
    }

    public boolean hasData() {
        return hasData;
    }

    public boolean isDirty() {
        return changesCount != 0;
    }

    public boolean isUpdateInProgress() {
        return updateInProgress != null;
    }

    public DataSourceJob getUpdateJob() {
        return updateInProgress;
    }

    void setUpdateInProgress(DataSourceJob updateService) {
        this.updateInProgress = updateService;
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
        int index = row.getVisualNumber();
        if (this.curRows.size() > index) {
            this.curRows.remove(index);
            this.shiftRows(row, -1);
        } else {
            log.debug("Error removing row from list: invalid row index: " + index);
        }
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

    void releaseAllData() {
        final List<ResultSetRow> oldRows = curRows;
        // Cleanup in separate job.
        // Sometimes model cleanup takes much time (e.g. freeing LOB values)
        // So let's do it in separate job to avoid UI locking
        RuntimeUtils.runTask(monitor -> {
            for (ResultSetRow row : oldRows) {
                row.release();
            }
        }, "Release values", 5000);
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

        // Construct new bindings from constraints. Exclude nested bindings
        List<DBDAttributeBinding> newBindings = new ArrayList<>();

        for (DBSAttributeBase attr : this.dataFilter.getOrderedVisibleAttributes()) {
            DBDAttributeBinding binding = getAttributeBinding(attr);
            if (binding != null && (binding.getParentObject() == null || binding.getParentObject() == documentAttribute)) {
                newBindings.add(binding);
            }
        }
        if (!newBindings.isEmpty() && !newBindings.equals(visibleAttributes)) {
            visibleAttributes = newBindings;
            updateColorMapping(true);
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

                // We check order position only when forceUpdate=true (otherwise all previous filters will be reset, see #6311)
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
            filterConstraint.setOptions(constraint.getOptions());
            DBSAttributeBase cAttr = filterConstraint.getAttribute();
            if (cAttr instanceof DBDAttributeBinding) {
                if (!constraint.isVisible()) {
                    visibleAttributes.remove(cAttr);
                } else {
                    if (!visibleAttributes.contains(cAttr)) {
                        DBDAttributeBinding attribute = (DBDAttributeBinding) cAttr;
                        if (attribute.getParentObject() == null || attribute.getParentObject() == documentAttribute) {
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

        updateColorMapping(true);
    }

    public void resetOrdering(@NotNull DBDAttributeBinding columnElement) {
        final boolean hasOrdering = dataFilter.hasOrdering();

        // First sort in original order to reset multi-column orderings
        curRows.sort(Comparator.comparingInt(ResultSetRow::getRowNumber));

        if (hasOrdering) {
            // Sort locally
            final List<DBDAttributeConstraint> orderConstraints = dataFilter.getOrderConstraints();
            curRows.sort((row1, row2) -> {
                int result = 0;
                for (DBDAttributeConstraint co : orderConstraints) {
                    final DBDAttributeBinding binding = getAttributeBinding(co.getAttribute());
                    if (binding == null) {
                        continue;
                    }
                    Object cell1 = getCellValue(new ResultSetCellLocation(binding, row1));
                    Object cell2 = getCellValue(new ResultSetCellLocation(binding, row2));
                    Comparator<Object> comparator = columnElement.getValueHandler().getComparator();
                    if (comparator != null) {
                        result = comparator.compare(cell1, cell2);
                    } else if (cell1 instanceof String && cell2 instanceof String) {
                    	result = (cell1.toString()).compareToIgnoreCase(cell2.toString());
                    } else {
                    	result = DBUtils.compareDataValues(cell1, cell2);
                    }
                          
                    if (co.isOrderDescending()) {
                        result = -result;
                    }
                    if (result != 0) {
                        break;
                    }
                }
                return result;
            });
        }
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

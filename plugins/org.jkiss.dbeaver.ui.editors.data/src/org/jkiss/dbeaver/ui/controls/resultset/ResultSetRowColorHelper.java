/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2026 DBeaver Corp and others
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
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.DBUtils;
import org.jkiss.dbeaver.model.data.DBDAttributeBinding;
import org.jkiss.dbeaver.model.exec.DBCLogicalOperator;
import org.jkiss.dbeaver.model.exec.DBExecUtils;
import org.jkiss.dbeaver.model.struct.DBSDataContainer;
import org.jkiss.dbeaver.model.virtual.DBVColorOverride;
import org.jkiss.dbeaver.model.virtual.DBVEntity;
import org.jkiss.dbeaver.model.virtual.DBVGroupRowStriping;
import org.jkiss.dbeaver.model.virtual.DBVUtils;
import org.jkiss.dbeaver.model.virtual.GroupRowStripingUtils;
import org.jkiss.dbeaver.ui.UIUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;

import java.util.*;

/**
 * Encapsulates all row color mapping and group row striping logic
 * extracted from {@link ResultSetModel}.
 */
public class ResultSetRowColorHelper {

    private static final Log log = Log.getLog(ResultSetRowColorHelper.class);

    private final ResultSetModel model;
    private final Map<DBDAttributeBinding, List<AttributeColorSettings>> colorMapping;

    public ResultSetRowColorHelper(@NotNull ResultSetModel model, @NotNull Comparator<DBDAttributeBinding> positionSorter) {
        this.model = model;
        this.colorMapping = new TreeMap<>(positionSorter);
    }

    private static class AttributeColorSettings {
        @NotNull
        private final DBCLogicalOperator operator;
        private final boolean rangeCheck;
        private final boolean singleColumn;
        @Nullable
        private final Object[] attributeValues;
        @Nullable
        private final Color colorForeground;
        @Nullable
        private final Color colorForeground2;
        @Nullable
        private final Color colorBackground;
        @Nullable
        private final Color colorBackground2;

        private AttributeColorSettings(@NotNull DBVColorOverride co) {
            this.operator = co.getOperator();
            this.rangeCheck = co.isRange();
            this.singleColumn = co.isSingleColumn();
            this.colorForeground = getColor(co.getColorForeground());
            this.colorForeground2 = getColor(co.getColorForeground2());
            this.colorBackground = getColor(co.getColorBackground());
            this.colorBackground2 = getColor(co.getColorBackground2());
            this.attributeValues = co.getAttributeValues();
        }

        @Nullable
        private static Color getColor(@Nullable String color) {
            if (CommonUtils.isEmpty(color)) {
                return null;
            }
            return UIUtils.getSharedColor(color);
        }

        public boolean evaluate(@Nullable Object cellValue) {
            return operator.evaluate(cellValue, attributeValues);
        }
    }

    public void updateColorMapping(@NotNull DBVEntity virtualEntity, boolean reset) {
        colorMapping.clear();
        processColorOverrides(virtualEntity);
        if (reset) {
            updateRowColors(true, model.getAllRows());
        }
        applyGroupRowStripingForEntity(virtualEntity, false);
    }

    public void updateColorMapping(boolean reset) {
        colorMapping.clear();

        DBSDataContainer dataContainer = model.getDataContainer();
        if (dataContainer == null) {
            return;
        }
        DBVEntity virtualEntity = DBVUtils.getVirtualEntity(dataContainer, false);
        if (virtualEntity == null) {
            return;
        }
        processColorOverrides(virtualEntity);
        if (reset) {
            updateRowColors(true, model.getAllRows());
        }
        applyGroupRowStripingForEntity(virtualEntity, false);
    }

    public void handleAppendDataColors(
        @Nullable DBVEntity virtualEntity,
        boolean resetOldRows,
        @NotNull List<ResultSetRow> newRows
    ) {
        List<ResultSetRow> allRows = model.getAllRows();
        // When group striping is active, all rows must be re-colored from scratch
        // because stripe parity depends on the full row sequence from index 0.
        if (virtualEntity != null && isGroupRowStripingActive(virtualEntity.getGroupRowStriping())) {
            updateRowColors(true, allRows);
        } else {
            updateRowColors(resetOldRows, newRows);
        }
        applyGroupRowStripingForEntity(virtualEntity, true);
    }

    public void handlePostOrdering(@Nullable DBVEntity virtualEntity) {
        if (virtualEntity != null && isGroupRowStripingActive(virtualEntity.getGroupRowStriping())) {
            updateRowColors(true, model.getAllRows());
            applyGroupRowStripingForEntity(virtualEntity, false);
        }
    }

    private void processColorOverrides(@NotNull DBVEntity virtualEntity) {
        DBDAttributeBinding[] attributes = model.getAttributes();
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

    private void updateRowColors(boolean reset, @NotNull List<ResultSetRow> rows) {
        DBDAttributeBinding[] attributes = model.getAttributes();
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
                    applyAttributeColors(entry.getKey(), row, entry.getValue(), attributes);
                }
            }
        }
    }

    private void applyAttributeColors(
        @NotNull DBDAttributeBinding binding,
        @NotNull ResultSetRow row,
        @NotNull List<AttributeColorSettings> settingsList,
        @NotNull DBDAttributeBinding[] attributes
    ) {
        ResultSetCellLocation cellLocation = new ResultSetCellLocation(binding, row);
        for (AttributeColorSettings acs : settingsList) {
            Color background = null;
            Color foreground = null;
            if (acs.rangeCheck) {
                background = resolveRangeBackground(acs, cellLocation);
                foreground = resolveRangeForeground(acs, cellLocation);
            } else {
                final Object cellValue = model.getCellValue(cellLocation);
                if (acs.evaluate(cellValue)) {
                    foreground = acs.colorForeground;
                    background = acs.colorBackground;
                }
            }
            if (foreground != null || background != null) {
                applyColorInfo(row, binding, acs.singleColumn, foreground, background, attributes);
            }
        }
    }

    @Nullable
    private Color resolveRangeBackground(@NotNull AttributeColorSettings acs, @NotNull ResultSetCellLocation cellLocation) {
        if (acs.attributeValues == null || acs.attributeValues.length <= 1) {
            return null;
        }
        if (acs.colorBackground == null || acs.colorBackground2 == null) {
            return null;
        }
        double minValue = DBExecUtils.makeNumericValue(acs.attributeValues[0]);
        double maxValue = DBExecUtils.makeNumericValue(acs.attributeValues[1]);
        double value = DBExecUtils.makeNumericValue(model.getCellValue(cellLocation));
        if (value < minValue || value > maxValue) {
            return null;
        }
        RGB bgRgb = ResultSetUtils.makeGradientValue(
            acs.colorBackground.getRGB(), acs.colorBackground2.getRGB(),
            minValue, maxValue, value);
        return UIUtils.getSharedColor(bgRgb);
    }

    @Nullable
    private Color resolveRangeForeground(@NotNull AttributeColorSettings acs, @NotNull ResultSetCellLocation cellLocation) {
        if (acs.attributeValues == null || acs.attributeValues.length <= 1) {
            return null;
        }
        double minValue = DBExecUtils.makeNumericValue(acs.attributeValues[0]);
        double maxValue = DBExecUtils.makeNumericValue(acs.attributeValues[1]);
        double value = DBExecUtils.makeNumericValue(model.getCellValue(cellLocation));
        if (value < minValue || value > maxValue) {
            return null;
        }
        if (acs.colorForeground != null && acs.colorForeground2 != null) {
            RGB fgRgb = ResultSetUtils.makeGradientValue(
                acs.colorForeground.getRGB(), acs.colorForeground2.getRGB(),
                minValue, maxValue, value);
            return UIUtils.getSharedColor(fgRgb);
        }
        if (acs.colorForeground != null) {
            return acs.colorForeground;
        }
        return acs.colorForeground2;
    }

    private static void applyColorInfo(
        @NotNull ResultSetRow row,
        @NotNull DBDAttributeBinding binding,
        boolean singleColumn,
        @Nullable Color foreground,
        @Nullable Color background,
        @NotNull DBDAttributeBinding[] attributes
    ) {
        ResultSetRow.ColorInfo colorInfo = row.colorInfo;
        if (colorInfo == null) {
            colorInfo = new ResultSetRow.ColorInfo();
            row.colorInfo = colorInfo;
        }
        if (!singleColumn) {
            colorInfo.rowForeground = foreground;
            colorInfo.rowBackground = background;
        } else {
            // Single column color
            if (foreground != null) {
                if (colorInfo.cellFgColors == null) {
                    colorInfo.cellFgColors = new Color[attributes.length];
                }
                colorInfo.cellFgColors[binding.getOrdinalPosition()] = foreground;
            }
            if (background != null) {
                if (colorInfo.cellBgColors == null) {
                    colorInfo.cellBgColors = new Color[attributes.length];
                }
                colorInfo.cellBgColors[binding.getOrdinalPosition()] = background;
            }
        }
    }

    private static boolean isGroupRowStripingActive(@Nullable DBVGroupRowStriping striping) {
        return striping != null && striping.hasValuableData();
    }

    private void applyGroupRowStripingForEntity(@Nullable DBVEntity virtualEntity, boolean applyGroupColumnSort) {
        List<ResultSetRow> curRows = model.getAllRows();
        if (CommonUtils.isEmpty(curRows) || virtualEntity == null) {
            return;
        }
        DBVGroupRowStriping striping = virtualEntity.getGroupRowStriping();
        if (!isGroupRowStripingActive(striping)) {
            return;
        }
        List<DBDAttributeBinding> groupBindings = resolveGroupBindings(striping);
        if (groupBindings == null) {
            return;
        }
        sortRowsForGroupStripingIfNeeded(applyGroupColumnSort, striping, groupBindings);
        int[] stripes = GroupRowStripingUtils.computeStripeIndices(collectGroupKeys(groupBindings));
        applyStripeBackgrounds(stripes, striping);
    }

    @Nullable
    private List<DBDAttributeBinding> resolveGroupBindings(@NotNull DBVGroupRowStriping striping) {
        DBDAttributeBinding[] attributes = model.getAttributes();
        List<DBDAttributeBinding> groupBindings = new ArrayList<>(striping.getColumnNames().size());
        for (String name : striping.getColumnNames()) {
            DBDAttributeBinding binding = DBUtils.findObject(attributes, name);
            if (binding == null) {
                log.debug("Group row striping: attribute '" + name + "' not in result set, striping skipped");
                return null;
            }
            groupBindings.add(binding);
        }
        return groupBindings;
    }

    private void sortRowsForGroupStripingIfNeeded(
        boolean applyGroupColumnSort,
        @NotNull DBVGroupRowStriping striping,
        @NotNull List<DBDAttributeBinding> groupBindings
    ) {
        if (!applyGroupColumnSort || !striping.isSortByGroupColumns()) {
            return;
        }
        List<ResultSetRow> curRows = model.getAllRows();
        curRows.sort((r1, r2) -> compareRowsForGroupStriping(r1, r2, groupBindings));
        for (int i = 0; i < curRows.size(); i++) {
            curRows.get(i).setVisualNumber(i);
        }
    }

    @NotNull
    private List<Object[]> collectGroupKeys(@NotNull List<DBDAttributeBinding> groupBindings) {
        List<ResultSetRow> curRows = model.getAllRows();
        List<Object[]> keys = new ArrayList<>(curRows.size());
        for (ResultSetRow row : curRows) {
            Object[] key = new Object[groupBindings.size()];
            for (int i = 0; i < groupBindings.size(); i++) {
                key[i] = model.getCellValue(new ResultSetCellLocation(groupBindings.get(i), row));
            }
            keys.add(key);
        }
        return keys;
    }

    private void applyStripeBackgrounds(@NotNull int[] stripes, @NotNull DBVGroupRowStriping striping) {
        Color colorA = CommonUtils.isEmpty(striping.getBackgroundColor1()) ? null : UIUtils.getSharedColor(striping.getBackgroundColor1());
        Color colorB = CommonUtils.isEmpty(striping.getBackgroundColor2()) ? null : UIUtils.getSharedColor(striping.getBackgroundColor2());
        if (colorA == null || colorB == null) {
            return;
        }
        List<ResultSetRow> curRows = model.getAllRows();
        for (int i = 0; i < curRows.size(); i++) {
            ResultSetRow row = curRows.get(i);
            ResultSetRow.ColorInfo colorInfo = row.colorInfo;
            if (colorInfo != null && colorInfo.rowBackground != null) {
                continue;
            }
            Color stripeColor = stripes[i] == 0 ? colorA : colorB;
            if (colorInfo == null) {
                colorInfo = new ResultSetRow.ColorInfo();
                row.colorInfo = colorInfo;
            }
            colorInfo.rowBackground = stripeColor;
        }
    }

    private int compareRowsForGroupStriping(
        @NotNull ResultSetRow r1,
        @NotNull ResultSetRow r2,
        @NotNull List<DBDAttributeBinding> groupBindings
    ) {
        for (DBDAttributeBinding binding : groupBindings) {
            Object v1 = model.getCellValue(new ResultSetCellLocation(binding, r1));
            Object v2 = model.getCellValue(new ResultSetCellLocation(binding, r2));
            int cmp = compareCellValues(binding, v1, v2);
            if (cmp != 0) {
                return cmp;
            }
        }
        return Integer.compare(r1.getRowNumber(), r2.getRowNumber());
    }

    private static int compareCellValues(
        @NotNull DBDAttributeBinding binding,
        @Nullable Object v1,
        @Nullable Object v2
    ) {
        if (v1 == null && v2 == null) {
            return 0;
        }
        if (v1 == null) {
            return -1;
        }
        if (v2 == null) {
            return 1;
        }
        Comparator<Object> comparator = binding.getValueHandler().getComparator();
        if (comparator != null) {
            return comparator.compare(v1, v2);
        }
        if (v1 instanceof String && v2 instanceof String) {
            return ((String) v1).compareToIgnoreCase((String) v2);
        }
        return DBUtils.compareDataValues(v1, v2);
    }
}

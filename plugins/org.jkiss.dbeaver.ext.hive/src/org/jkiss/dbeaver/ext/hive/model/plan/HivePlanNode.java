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
package org.jkiss.dbeaver.ext.hive.model.plan;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanCostNode;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNodeKind;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlanNode;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.preferences.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.preferences.DBPPropertySource;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.ByteNumberFormat;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class HivePlanNode extends AbstractExecutionPlanNode
    implements DBCPlanCostNode, DBPPropertySource {
    private static final String NUMBER_PATTERN = "[+-]?(?:\\d+(?:\\.\\d*)?|\\.\\d+)(?:[eE][+-]?\\d+)?";
    private static final Pattern ROWS_PATTERN = Pattern.compile(
        "(?i)\\bnum\\s+rows\\s*:\\s*(" + NUMBER_PATTERN + ")");
    private static final Pattern DATA_SIZE_PATTERN = Pattern.compile(
        "(?i)\\bdata\\s+size\\s*:\\s*(-?\\d+)");
    private static final Pattern BASIC_STATS_PATTERN = Pattern.compile(
        "(?i)\\bbasic\\s+stats\\s*:\\s*([a-z]+)");
    private static final Pattern COLUMN_STATS_PATTERN = Pattern.compile(
        "(?i)\\bcolumn\\s+stats\\s*:\\s*([a-z]+)");
    private static final Pattern NUMBER_VALUE_PATTERN = Pattern.compile("^\\s*(" + NUMBER_PATTERN + ")\\s*$");

    private final HivePlanNode parent;
    private final String type;
    private final Map<String, String> properties = new LinkedHashMap<>();
    private final List<HivePlanNode> nested = new ArrayList<>();
    private final List<String> inputEdges = new ArrayList<>();
    private String name;
    private Number rowCount;
    private Long estimatedDataSize;
    private String basicStats;
    private String columnStats;

    HivePlanNode(@Nullable HivePlanNode parent, @NotNull String type, @Nullable String name) {
        this.parent = parent;
        this.type = type;
        this.name = name;
    }

    void addNested(@NotNull HivePlanNode node) {
        nested.add(node);
    }

    void addProperty(@NotNull String propertyName, @NotNull String value) {
        properties.put(propertyName, value);
        String normalizedName = normalize(propertyName);
        if (name == null && "operatorid".equals(normalizedName)) {
            name = value;
        }
        if (rowCount == null) {
            if (isRowsProperty(normalizedName)) {
                rowCount = parseNumber(value);
            } else if ("statistics".equals(normalizedName)) {
                Matcher matcher = ROWS_PATTERN.matcher(value);
                if (matcher.find()) {
                    rowCount = parseNumber(matcher.group(1));
                }
            }
        }
        if ("statistics".equals(normalizedName)) {
            Matcher dataSizeMatcher = DATA_SIZE_PATTERN.matcher(value);
            if (dataSizeMatcher.find()) {
                estimatedDataSize = Long.valueOf(dataSizeMatcher.group(1));
            }
            basicStats = findGroup(BASIC_STATS_PATTERN, value);
            columnStats = findGroup(COLUMN_STATS_PATTERN, value);
        }
    }

    void addInputEdge(@NotNull String source, @Nullable String edgeType) {
        String edge = edgeType == null || edgeType.isBlank() ? source : source + " (" + edgeType + ")";
        if (!inputEdges.contains(edge)) {
            inputEdges.add(edge);
        }
    }

    @NotNull
    Map<String, String> getNodeProperties() {
        return properties;
    }

    @NotNull
    @Override
    public DBCPlanNodeKind getNodeKind() {
        String nodeTypeWithSpaces = type.toLowerCase(Locale.ENGLISH);
        String nodeType = nodeTypeWithSpaces.replace(" ", "");
        if (nodeType.contains("tablescan")) {
            return DBCPlanNodeKind.TABLE_SCAN;
        }
        if (nodeType.contains("filter")) {
            return DBCPlanNodeKind.FILTER;
        }
        if (nodeType.contains("join")) {
            return DBCPlanNodeKind.JOIN;
        }
        if (nodeType.contains("groupby") || nodeType.contains("aggregate")) {
            return DBCPlanNodeKind.AGGREGATE;
        }
        if (nodeTypeWithSpaces.contains("sort") || nodeTypeWithSpaces.contains("order")) {
            return DBCPlanNodeKind.SORT;
        }
        if (nodeType.contains("union")) {
            return DBCPlanNodeKind.UNION;
        }
        if (nodeType.contains("select") || nodeType.contains("project")) {
            return DBCPlanNodeKind.SELECT;
        }
        if (nodeType.contains("hash")) {
            return DBCPlanNodeKind.HASH;
        }
        if (nodeType.contains("listsink") || nodeType.contains("filesink") ||
            nodeType.contains("fileoutput") || nodeType.contains("fetch")) {
            return DBCPlanNodeKind.RESULT;
        }
        if (nodeType.contains("move") || nodeType.contains("insert") || nodeType.contains("update") ||
            nodeType.contains("delete")) {
            return DBCPlanNodeKind.MODIFY;
        }
        return DBCPlanNodeKind.DEFAULT;
    }

    @NotNull
    @Override
    public String getNodeName() {
        String entity = getEntity();
        if (entity != null) {
            return entity;
        }
        return name == null ? type : name;
    }

    @NotNull
    @Property(order = 0, viewable = true)
    @Override
    public String getNodeType() {
        return type;
    }

    @Nullable
    @Property(order = 2, viewable = true)
    public String getEntity() {
        String nodeType = normalize(type);
        if (nodeType.equals("stage") || nodeType.equals("executionengine") || nodeType.equals("vertex")) {
            return name;
        }
        if (nodeType.contains("tablescan")) {
            String tableName = getProperty("table");
            return tableName == null ? getProperty("alias") : tableName;
        }
        if (nodeType.contains("reduceoutput")) {
            return getProperty("outputname");
        }
        return null;
    }

    @Nullable
    @Property(order = 4, viewable = true)
    public String getInputs() {
        return inputEdges.isEmpty() ? null : String.join("; ", inputEdges);
    }

    @Nullable
    @Property(order = 24, viewable = true)
    @Override
    public String getNodeCondition() {
        String nodeType = normalize(type);
        if (nodeType.contains("filter")) {
            return formatPropertyValue(
                getFirstProperty("predicate", "filterpredicates", "filterexpression", "filterexpr"));
        }
        if (nodeType.contains("join")) {
            return formatPropertyValue(getFirstProperty(
                "conditionmap",
                "condition",
                "filterpredicates",
                "residualfilterpredicates",
                "keys"));
        }
        if (nodeType.contains("tablescan")) {
            return formatPropertyValue(getFirstProperty("filterexpression", "filterexpr", "filterpredicates"));
        }
        return formatPropertyValue(getProperty("condition"));
    }

    @Nullable
    @Override
    public HivePlanNode getParent() {
        return parent;
    }

    @NotNull
    @Override
    public List<HivePlanNode> getNested() {
        return nested;
    }

    @Nullable
    @Override
    public Number getNodeCost() {
        return null;
    }

    @Nullable
    @Override
    public Number getNodePercent() {
        return null;
    }

    @Nullable
    @Override
    public Number getNodeDuration() {
        return null;
    }

    @Nullable
    @Property(order = 21, viewable = true)
    @Override
    public Number getNodeRowCount() {
        return rowCount;
    }

    @Nullable
    @Property(order = 22, viewable = true, formatter = ByteNumberFormat.class)
    public Long getEstimatedDataSize() {
        return estimatedDataSize;
    }

    @Nullable
    @Property(order = 23, viewable = true)
    public String getStatisticsStatus() {
        if (basicStats == null) {
            return columnStats;
        }
        return columnStats == null ? basicStats : basicStats + " / " + columnStats;
    }

    @Nullable
    @Property(order = 25, viewable = true)
    public String getNodeDetails() {
        String nodeType = normalize(type);
        if (nodeType.contains("join")) {
            return formatDetails("keys", "inputvertices", "positionofbigtable");
        }
        if (nodeType.contains("groupby") || nodeType.contains("aggregate")) {
            return formatDetails("keys", "aggregations", "mode", "groupingsets", "minreductionhashaggr");
        }
        if (nodeType.contains("reduceoutput")) {
            return formatDetails(
                "keyexpressions",
                "mapreducepartitioncolumns",
                "sortorder",
                "nullsortorder");
        }
        if (nodeType.contains("dynamicpartitioningevent")) {
            return formatDetails("partitionkeyexpr", "targetcolumn", "targetinput", "targetvertex");
        }
        if (nodeType.equals("partitionvalues")) {
            return formatAllProperties();
        }
        return formatDetails("partitionvalues");
    }

    @NotNull
    @Override
    public Object getEditableValue() {
        return this;
    }

    @NotNull
    @Override
    public DBPPropertyDescriptor[] getProperties() {
        DBPPropertyDescriptor[] descriptors = new DBPPropertyDescriptor[properties.size()];
        int index = 0;
        for (String property : properties.keySet()) {
            descriptors[index++] = new PropertyDescriptor(
                "Details",
                property,
                property,
                null,
                String.class,
                false,
                null,
                null,
                false);
        }
        return descriptors;
    }

    @Nullable
    @Override
    public Object getPropertyValue(@Nullable DBRProgressMonitor monitor, @NotNull String id) {
        return properties.get(id);
    }

    @Override
    public boolean isPropertySet(@NotNull String id) {
        return properties.containsKey(id);
    }

    @Override
    public boolean isPropertyResettable(@NotNull String id) {
        return false;
    }

    @Override
    public void resetPropertyValue(@Nullable DBRProgressMonitor monitor, @NotNull String id) {
    }

    @Override
    public void resetPropertyValueToDefault(@NotNull String id) {
    }

    @Override
    public void setPropertyValue(@Nullable DBRProgressMonitor monitor, @NotNull String id, @Nullable Object value) {
    }

    private static boolean isRowsProperty(@NotNull String propertyName) {
        return "rows".equals(propertyName) ||
            "rowcount".equals(propertyName) ||
            "numrows".equals(propertyName);
    }

    @Nullable
    private static Number parseNumber(@NotNull String value) {
        Matcher matcher = NUMBER_VALUE_PATTERN.matcher(value);
        if (!matcher.matches()) {
            return null;
        }
        String number = matcher.group(1);
        if (number.indexOf('.') < 0 && number.indexOf('e') < 0 && number.indexOf('E') < 0) {
            return Long.valueOf(number);
        }
        return Double.valueOf(number);
    }

    @Nullable
    private static String findGroup(@NotNull Pattern pattern, @NotNull String value) {
        Matcher matcher = pattern.matcher(value);
        return matcher.find() ? matcher.group(1).toUpperCase(Locale.ENGLISH) : null;
    }

    @NotNull
    private static String normalize(@NotNull String value) {
        return value.toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9]", "");
    }

    @Nullable
    private String getProperty(@NotNull String normalizedName) {
        for (Map.Entry<String, String> property : properties.entrySet()) {
            if (normalizedName.equals(normalize(property.getKey())) && !property.getValue().isBlank()) {
                return property.getValue();
            }
        }
        return null;
    }

    @Nullable
    private String getFirstProperty(@NotNull String... normalizedNames) {
        for (String propertyName : normalizedNames) {
            String value = getProperty(propertyName);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    @Nullable
    private String formatDetails(@NotNull String... normalizedNames) {
        List<String> details = new ArrayList<>();
        for (String normalizedName : normalizedNames) {
            Map.Entry<String, String> property = getPropertyEntry(normalizedName);
            if (property != null && !property.getValue().isBlank()) {
                details.add(cleanPropertyName(property.getKey()) + ": " + formatPropertyValue(property.getValue()));
            }
        }
        return details.isEmpty() ? null : String.join(" | ", details);
    }

    @Nullable
    private String formatAllProperties() {
        List<String> details = new ArrayList<>();
        for (Map.Entry<String, String> property : properties.entrySet()) {
            if (!property.getValue().isBlank()) {
                details.add(cleanPropertyName(property.getKey()) + "=" + formatPropertyValue(property.getValue()));
            }
        }
        return details.isEmpty() ? null : String.join("; ", details);
    }

    @Nullable
    private Map.Entry<String, String> getPropertyEntry(@NotNull String normalizedName) {
        for (Map.Entry<String, String> property : properties.entrySet()) {
            if (normalizedName.equals(normalize(property.getKey()))) {
                return property;
            }
        }
        return null;
    }

    @NotNull
    private static String cleanPropertyName(@NotNull String value) {
        String result = value.trim();
        while (result.endsWith(":")) {
            result = result.substring(0, result.length() - 1).trim();
        }
        return result;
    }

    @Nullable
    private static String formatPropertyValue(@Nullable String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        try {
            return formatJsonValue(JsonParser.parseString(value));
        } catch (RuntimeException e) {
            return value;
        }
    }

    @NotNull
    private static String formatJsonValue(@NotNull JsonElement value) {
        if (value.isJsonPrimitive()) {
            String result = value.getAsString();
            if (value.getAsJsonPrimitive().isString() &&
                (result.stripLeading().startsWith("{") || result.stripLeading().startsWith("["))) {
                try {
                    return formatJsonValue(JsonParser.parseString(result));
                } catch (RuntimeException e) {
                    return result;
                }
            }
            return result;
        }
        if (value.isJsonArray()) {
            List<String> items = new ArrayList<>();
            for (JsonElement item : value.getAsJsonArray()) {
                items.add(formatJsonValue(item));
            }
            return String.join("; ", items);
        }
        if (value.isJsonObject()) {
            List<String> entries = new ArrayList<>();
            JsonObject object = value.getAsJsonObject();
            for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
                String formattedValue = formatJsonValue(entry.getValue());
                entries.add(entry.getKey().isBlank() ? formattedValue : entry.getKey() + "=" + formattedValue);
            }
            return String.join("; ", entries);
        }
        return value.toString();
    }
}

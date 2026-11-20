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

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.data.json.JSONUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

final class HivePlanParser {
    private static final String CBO_INFO = "cboInfo";
    private static final String OPTIMIZED_SQL = "optimizedSQL";
    private static final String STAGE_DEPENDENCIES = "STAGE DEPENDENCIES";
    private static final String STAGE_PLANS = "STAGE PLANS";

    @NotNull
    final List<HivePlanNode> parse(@NotNull String source) {
        JsonElement element = JSONUtils.GSON.fromJson(source, JsonElement.class);
        if (element == null || !element.isJsonObject()) {
            throw new IllegalArgumentException("Hive execution plan must be a JSON object");
        }

        JsonObject plan = element.getAsJsonObject();
        JsonObject stagePlans = getRequiredObject(plan, STAGE_PLANS);
        JsonObject dependencies = getOptionalObject(plan, STAGE_DEPENDENCIES);
        List<HivePlanNode> roots = new ArrayList<>();

        for (Map.Entry<String, JsonElement> entry : stagePlans.entrySet()) {
            HivePlanNode stage = new HivePlanNode(null, "Stage", entry.getKey());
            if (dependencies != null) {
                JsonElement stageDependencies = dependencies.get(entry.getKey());
                if (stageDependencies != null && stageDependencies.isJsonObject()) {
                    addProperties(stage, stageDependencies.getAsJsonObject());
                }
            }
            if (entry.getValue().isJsonObject()) {
                parseObject(stage, entry.getValue().getAsJsonObject(), false);
            } else {
                stage.addProperty("Plan", valueToString(entry.getValue()));
            }
            roots.add(stage);
        }
        if (roots.isEmpty()) {
            throw new IllegalArgumentException("Hive execution plan contains no stages");
        }

        addOptionalProperty(plan, roots.getFirst(), CBO_INFO);
        addOptionalProperty(plan, roots.getFirst(), OPTIMIZED_SQL);
        return roots;
    }

    private static void addOptionalProperty(
        @NotNull JsonObject plan,
        @NotNull HivePlanNode root,
        @NotNull String name
    ) {
        JsonElement value = plan.get(name);
        if (value != null && !value.isJsonNull()) {
            root.addProperty(name, valueToString(value));
        }
    }

    private void parseObject(@NotNull HivePlanNode parent, @NotNull JsonObject object, boolean operator) {
        JsonElement edges = null;
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            if (value == null || value.isJsonNull() || value.isJsonPrimitive()) {
                parent.addProperty(key, valueToString(value));
            } else if (isChildren(key)) {
                parseOperatorChildren(parent, value);
            } else if (isDependencyMetadata(key)) {
                parent.addProperty(key, valueToString(value));
                edges = value;
            } else if (isAliasOperatorTree(key)) {
                parseAliasOperatorTree(parent, value);
            } else if (isTransparentContainer(key)) {
                parseTransparentContainer(parent, key, value);
            } else if (value.isJsonObject()) {
                JsonObject nested = value.getAsJsonObject();
                if (operator && !shouldCreateNode(key, nested)) {
                    parent.addProperty(key, valueToString(value));
                } else {
                    addNode(parent, key, nested, isOperator(key));
                }
            } else if (value.isJsonArray()) {
                parseArray(parent, key, value.getAsJsonArray(), operator);
            }
        }
        if (edges != null) {
            applyEdges(parent, edges);
        }
    }

    private static void applyEdges(@NotNull HivePlanNode parent, @NotNull JsonElement edges) {
        if (!edges.isJsonObject()) {
            return;
        }
        for (Map.Entry<String, JsonElement> destinationEntry : edges.getAsJsonObject().entrySet()) {
            HivePlanNode destination = findVertex(parent, cleanName(destinationEntry.getKey()));
            if (destination == null) {
                continue;
            }
            JsonElement edge = destinationEntry.getValue();
            if (edge.isJsonObject()) {
                applyEdge(destination, edge.getAsJsonObject());
            } else if (edge.isJsonArray()) {
                for (JsonElement item : edge.getAsJsonArray()) {
                    if (item.isJsonObject()) {
                        applyEdge(destination, item.getAsJsonObject());
                    }
                }
            }
        }
    }

    private static void applyEdge(@NotNull HivePlanNode destination, @NotNull JsonObject edge) {
        String source = getString(edge, "parent");
        if (source != null && !source.isBlank()) {
            destination.addInputEdge(source, getString(edge, "type"));
        }
    }

    @Nullable
    private static HivePlanNode findVertex(@NotNull HivePlanNode parent, @NotNull String name) {
        for (HivePlanNode child : parent.getNested()) {
            if ("Vertex".equals(child.getNodeType()) && name.equals(child.getNodeName())) {
                return child;
            }
        }
        return null;
    }

    @Nullable
    private static String getString(@NotNull JsonObject object, @NotNull String normalizedName) {
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (normalizedName.equals(normalizeName(entry.getKey())) && entry.getValue().isJsonPrimitive()) {
                return entry.getValue().getAsString();
            }
        }
        return null;
    }

    private void parseAliasOperatorTree(@NotNull HivePlanNode parent, @NotNull JsonElement value) {
        if (!value.isJsonObject()) {
            return;
        }
        for (Map.Entry<String, JsonElement> alias : value.getAsJsonObject().entrySet()) {
            if (!alias.getValue().isJsonObject()) {
                continue;
            }
            int firstAddedNode = parent.getNested().size();
            parseObject(parent, alias.getValue().getAsJsonObject(), false);
            for (int i = firstAddedNode; i < parent.getNested().size(); i++) {
                parent.getNested().get(i).addProperty("alias:", alias.getKey());
            }
        }
    }

    private void parseTransparentContainer(
        @NotNull HivePlanNode parent,
        @NotNull String key,
        @NotNull JsonElement value
    ) {
        if (value.isJsonObject()) {
            parseObject(parent, value.getAsJsonObject(), false);
        } else if (value.isJsonArray()) {
            JsonArray array = value.getAsJsonArray();
            if (containsOnlyPrimitiveValues(array)) {
                parent.addProperty(key, valueToString(array));
                return;
            }
            for (JsonElement item : array) {
                if (item.isJsonObject()) {
                    parseObject(parent, item.getAsJsonObject(), false);
                }
            }
        }
    }

    private void parseArray(
        @NotNull HivePlanNode parent,
        @NotNull String key,
        @NotNull JsonArray array,
        boolean operator
    ) {
        if (array.isEmpty() || containsOnlyPrimitiveValues(array) ||
            operator && !isStructural(key) && !isOperator(key)) {
            parent.addProperty(key, valueToString(array));
            return;
        }

        HivePlanNode container = new HivePlanNode(parent, cleanName(key), null);
        parent.addNested(container);
        int index = 1;
        for (JsonElement item : array) {
            if (item.isJsonObject()) {
                JsonObject object = item.getAsJsonObject();
                if (object.entrySet().size() == 1) {
                    Map.Entry<String, JsonElement> child = object.entrySet().iterator().next();
                    if (child.getValue().isJsonObject()) {
                        addNode(container, child.getKey(), child.getValue().getAsJsonObject(), true);
                        continue;
                    }
                }
                HivePlanNode itemNode = new HivePlanNode(container, "Item", Integer.toString(index++));
                container.addNested(itemNode);
                parseObject(itemNode, object, false);
            } else {
                container.addProperty("Item " + index++, valueToString(item));
            }
        }
    }

    private void parseOperatorChildren(
        @NotNull HivePlanNode parent,
        @NotNull JsonElement children
    ) {
        if (children.isJsonObject()) {
            addOperatorObject(parent, children.getAsJsonObject());
        } else if (children.isJsonArray()) {
            for (JsonElement child : children.getAsJsonArray()) {
                if (child.isJsonObject()) {
                    addOperatorObject(parent, child.getAsJsonObject());
                }
            }
        }
    }

    private void addOperatorObject(@NotNull HivePlanNode parent, @NotNull JsonObject object) {
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (entry.getValue().isJsonObject()) {
                addNode(parent, entry.getKey(), entry.getValue().getAsJsonObject(), true);
            } else {
                parent.addProperty(entry.getKey(), valueToString(entry.getValue()));
            }
        }
    }

    private void addNode(
        @NotNull HivePlanNode parent,
        @NotNull String type,
        @NotNull JsonObject object,
        boolean operator
    ) {
        String nodeType = cleanName(type);
        HivePlanNode child;
        if (isExecutionEngine(nodeType)) {
            child = new HivePlanNode(parent, "Execution Engine", nodeType);
        } else if (isVertex(nodeType)) {
            child = new HivePlanNode(parent, "Vertex", nodeType);
        } else {
            child = new HivePlanNode(parent, nodeType, null);
        }
        parent.addNested(child);
        parseObject(child, object, operator);
    }

    private static void addProperties(@NotNull HivePlanNode node, @NotNull JsonObject object) {
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            node.addProperty(entry.getKey(), valueToString(entry.getValue()));
        }
    }

    private static boolean shouldCreateNode(
        @NotNull String key,
        @NotNull JsonObject object
    ) {
        if (isOperator(key) || isStructural(key)) {
            return true;
        }
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            if (isChildren(entry.getKey()) || isOperator(entry.getKey()) || isStructural(entry.getKey())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isOperator(@NotNull String key) {
        String name = key.toLowerCase(Locale.ENGLISH).replace(" ", "");
        return name.contains("operator") ||
            name.contains("tablescan") ||
            name.contains("groupby") ||
            name.contains("join") ||
            name.contains("listsink");
    }

    private static boolean isStructural(@NotNull String key) {
        String name = cleanName(key).toLowerCase(Locale.ENGLISH);
        return name.equals("tez") ||
            name.equals("spark") ||
            name.equals("map reduce") ||
            name.equals("vertices") ||
            name.equals("map operator tree") ||
            name.equals("reduce operator tree") ||
            name.equals("processor tree") ||
            name.equals("local work") ||
            name.equals("merge file operator") ||
            name.contains("map operator tree");
    }

    private static boolean isTransparentContainer(@NotNull String key) {
        String name = cleanName(key).toLowerCase(Locale.ENGLISH);
        return name.equals("vertices") ||
            name.equals("map operator tree") ||
            name.equals("reduce operator tree") ||
            name.equals("processor tree");
    }

    private static boolean isAliasOperatorTree(@NotNull String key) {
        return cleanName(key).equalsIgnoreCase("Alias -> Map Operator Tree");
    }

    private static boolean isExecutionEngine(@NotNull String type) {
        return type.equalsIgnoreCase("Tez") ||
            type.equalsIgnoreCase("Spark") ||
            type.equalsIgnoreCase("Map Reduce");
    }

    private static boolean isVertex(@NotNull String type) {
        String name = type.toLowerCase(Locale.ENGLISH);
        return name.matches("(?:map|reducer|union)\\s+\\d+");
    }

    private static boolean isChildren(@NotNull String key) {
        return "children".equalsIgnoreCase(cleanName(key));
    }

    private static boolean isDependencyMetadata(@NotNull String key) {
        return "edges".equalsIgnoreCase(cleanName(key));
    }

    private static boolean containsOnlyPrimitiveValues(@NotNull JsonArray array) {
        for (JsonElement item : array) {
            if (!item.isJsonNull() && !item.isJsonPrimitive()) {
                return false;
            }
        }
        return true;
    }

    @NotNull
    protected static String valueToString(@Nullable JsonElement value) {
        if (value == null || value.isJsonNull()) {
            return "";
        }
        return value.isJsonPrimitive() && value.getAsJsonPrimitive().isString()
            ? value.getAsString()
            : value.toString();
    }

    @NotNull
    private static String cleanName(@NotNull String name) {
        String result = name.trim();
        while (result.endsWith(":")) {
            result = result.substring(0, result.length() - 1).trim();
        }
        return result;
    }

    @NotNull
    private static String normalizeName(@NotNull String name) {
        return cleanName(name).toLowerCase(Locale.ENGLISH).replaceAll("[^a-z0-9]", "");
    }

    @NotNull
    private static JsonObject getRequiredObject(@NotNull JsonObject object, @NotNull String name) {
        JsonObject result = getOptionalObject(object, name);
        if (result == null) {
            throw new IllegalArgumentException("Hive execution plan does not contain " + name);
        }
        return result;
    }

    @Nullable
    private static JsonObject getOptionalObject(@NotNull JsonObject object, @NotNull String name) {
        JsonElement element = object.get(name);
        return element != null && element.isJsonObject() ? element.getAsJsonObject() : null;
    }
}

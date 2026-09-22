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
package org.jkiss.dbeaver.ext.mimer.model.plan;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNodeKind;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlanNode;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.utils.CommonUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * One node of the tree {@code MimerExecutionPlan} builds from the driver's {@code
 * getExplainText()} XML (schema: {@code .../Manuals/App_explain/App_explain.htm}):
 * <ul>
 * <li>Statement root - {@code <select>}/{@code <insert>}/{@code <update>}/{@code <delete>}
 * <li>Joins - {@code <innerJoin>}/{@code <outerJoin>}/{@code <crossJoin>}
 * <li>{@code <tempTable class="TempTableOrderBy|TempTableUnion|TempTableDistinct|
 *     TempTableJoinBySubselect">} - intermediate results (sort/union/distinct/dedup); not
 *     every attribute is always present
 * <li>{@code <union>}, {@code <subselect>}, {@code <constSubselect>} - the latter two can
 *     appear as a sibling of a {@code <table>} rather than nested inside a join
 * <li>{@code <table name=".." order=".." index=".." scan=".." type=".." indexlookuponly=".."
 *     rows="..">} - a single table/index access, the only element with a real {@code name}.
 *     {@code type}/{@code scan} are free-form strings (not a fixed enum) passed through
 *     verbatim; {@code indexlookuponly} is all-lowercase, unlike the docs page's example.
 * </ul>
 * All nodes carry {@code cost}/{@code hits} (rows returned)/{@code visits} (rows examined);
 * {@code tempTable} additionally carries {@code tempWrites}. An unrecognized tag still
 * renders, falling back to its raw tag name for type/kind.
 *
 * @author Mimer Information Technology
 */
public class MimerPlanNode extends AbstractExecutionPlanNode {

    private static final String ATTR_NAME = "name";
    private static final String ATTR_ORDER = "order";
    private static final String ATTR_INDEX = "index";
    private static final String ATTR_SCAN = "scan";
    private static final String ATTR_TYPE = "type";
    private static final String ATTR_INDEX_LOOKUP_ONLY = "indexlookuponly";
    private static final String ATTR_CLASS = "class";
    private static final String ATTR_COST = "cost";
    private static final String ATTR_HITS = "hits";
    private static final String ATTR_VISITS = "visits";
    private static final String ATTR_ROWS = "rows";
    private static final String ATTR_TEMP_WRITES = "tempWrites";

    private final String tagName;
    private final Map<String, String> attributes;
    private final MimerPlanNode parent;
    private final List<MimerPlanNode> nested = new ArrayList<>();

    public MimerPlanNode(@NotNull Element element, @Nullable MimerPlanNode parent) {
        this.tagName = element.getTagName();
        this.attributes = readAttributes(element);
        this.parent = parent;
        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child.getNodeType() == Node.ELEMENT_NODE) {
                nested.add(new MimerPlanNode((Element) child, this));
            }
        }
    }

    @NotNull
    private static Map<String, String> readAttributes(@NotNull Element element) {
        NamedNodeMap attrs = element.getAttributes();
        Map<String, String> result = new HashMap<>(attrs.getLength());
        for (int i = 0; i < attrs.getLength(); i++) {
            Node attr = attrs.item(i);
            result.put(attr.getNodeName(), attr.getNodeValue());
        }
        return result;
    }

    @NotNull
    @Override
    public DBCPlanNodeKind getNodeKind() {
        return switch (tagName) {
            case "select" -> DBCPlanNodeKind.SELECT;
            case "insert", "update", "delete" -> DBCPlanNodeKind.MODIFY;
            case "innerJoin", "outerJoin", "crossJoin" -> DBCPlanNodeKind.JOIN;
            case "union" -> DBCPlanNodeKind.UNION;
            case "subselect", "constSubselect" -> DBCPlanNodeKind.SELECT;
            case "table" -> "sequential".equals(attributes.get(ATTR_SCAN))
                ? DBCPlanNodeKind.TABLE_SCAN : DBCPlanNodeKind.INDEX_SCAN;
            case "tempTable" -> switch (CommonUtils.notEmpty(attributes.get(ATTR_CLASS))) {
                case "TempTableOrderBy" -> DBCPlanNodeKind.SORT;
                case "TempTableUnion" -> DBCPlanNodeKind.UNION;
                case "TempTableDistinct" -> DBCPlanNodeKind.SET;
                default -> DBCPlanNodeKind.MATERIALIZE;
            };
            default -> DBCPlanNodeKind.DEFAULT;
        };
    }

    @Nullable
    @Override
    public String getNodeName() {
        // Only <table> carries a real name (table/alias, e.g. "currencies cur")
        return attributes.get(ATTR_NAME);
    }

    @Nullable
    @Property(order = 0, viewable = true)
    @Override
    public String getNodeType() {
        return switch (tagName) {
            case "select" -> "Select";
            case "insert" -> "Insert";
            case "update" -> "Update";
            case "delete" -> "Delete";
            case "innerJoin" -> "Inner Join";
            case "outerJoin" -> "Outer Join";
            case "crossJoin" -> "Cross Join";
            case "union" -> "Union";
            case "subselect" -> "Subselect";
            case "constSubselect" -> "Const Subselect";
            case "table" -> "sequential".equals(attributes.get(ATTR_SCAN)) ? "Table Scan" : "Index Scan";
            case "tempTable" -> describeTempTable();
            default -> tagName;
        };
    }

    @NotNull
    private String describeTempTable() {
        String tempTableClass = attributes.get(ATTR_CLASS);
        if (tempTableClass == null) {
            return "Temp Table";
        }
        return switch (tempTableClass) {
            case "TempTableOrderBy" -> "Temp Table (Order By)";
            case "TempTableUnion" -> "Temp Table (Union)";
            case "TempTableDistinct" -> "Temp Table (Distinct)";
            case "TempTableJoinBySubselect" -> "Temp Table (Subselect Join)";
            default -> "Temp Table (" + tempTableClass + ")";
        };
    }

    @Nullable
    @Property(order = 1, viewable = true)
    public String getAccessOrder() {
        // Execution sequence among sibling <table> nodes in a join - which side is read
        // first, second, etc. Only meaningful on <table>.
        return attributes.get(ATTR_ORDER);
    }

    @Nullable
    @Property(order = 2, viewable = true)
    @Override
    public String getNodeDescription() {
        if (!"table".equals(tagName)) {
            return null;
        }
        StringBuilder text = new StringBuilder();
        String type = attributes.get(ATTR_TYPE);
        if (type != null) {
            text.append(type);
        }
        String index = attributes.get(ATTR_INDEX);
        if (index != null) {
            if (text.length() > 0) {
                text.append(" via ");
            }
            text.append(index);
        }
        if (CommonUtils.getBoolean(attributes.get(ATTR_INDEX_LOOKUP_ONLY), false)) {
            text.append(" (index-only)");
        }
        return text.length() > 0 ? text.toString() : null;
    }

    @Nullable
    @Property(order = 3, viewable = true)
    public String getCost() {
        return attributes.get(ATTR_COST);
    }

    @Nullable
    @Property(order = 4, viewable = true)
    public String getHits() {
        return attributes.get(ATTR_HITS);
    }

    @Nullable
    @Property(order = 5, viewable = true)
    public String getVisits() {
        return attributes.get(ATTR_VISITS);
    }

    @Nullable
    @Property(order = 6, viewable = true)
    public String getRows() {
        return attributes.get(ATTR_ROWS);
    }

    @Nullable
    @Property(order = 7, viewable = true)
    public String getTempWrites() {
        return attributes.get(ATTR_TEMP_WRITES);
    }

    @Nullable
    @Override
    public DBCPlanNode getParent() {
        return parent;
    }

    @NotNull
    @Override
    public Collection<MimerPlanNode> getNested() {
        return nested;
    }
}

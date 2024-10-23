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
package org.jkiss.dbeaver.ext.cubrid.model.plan;

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNodeKind;
import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlanNode;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import org.jkiss.utils.CommonUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CubridPlanNode extends AbstractExecutionPlanNode
{
    private static final String SEPARATOR = ":";
    private static final String SPACE = " ";
    private static Map<String, String> classNode = new HashMap<>();
    private static Map<String, String> terms = new HashMap<>();
    private static List<String> segments;
    
    private static final List<String> parentNode = List.of("subplan", "head", "outer", "inner", "Query plan");
    private static final List<String> parentExcept = List.of("iscan", "sscan");
    private static final Pattern totalPattern = Pattern.compile("\\d+\\/\\d+");
    private static final Pattern termPattern = Pattern.compile("node\\[\\d\\]");
    private static final Pattern subNodePattern = Pattern.compile("term\\[\\d\\]");
    private static final Pattern segmentPattern = Pattern.compile("(inner|outer|class|cost|follow|head|subplan|index|filtr|sort|sargs|edge|Query plan|term\\[..|node\\[..):\\s*([^\\n\\r]*)");
    private String fullText;
    private String nodeName;
    private String totalValue;
    private String name;
    private String index;
    private String term;
    private String extra;
    private long cost;
    private long row;
    private CubridPlanNode parent;
    private List<CubridPlanNode> nested = new ArrayList<>();


    public CubridPlanNode() {
        name = "Query";
    }


    public CubridPlanNode(@NotNull String queryPlan) {
        this.fullText = queryPlan;
        this.getSegments();
        parseObject();
    }

    private CubridPlanNode(CubridPlanNode parent, boolean normal, String param) {
        this.parent = parent;
        this.fullText = parent.fullText;

        if (normal) {
            parseObject();
        } else {
            String[] values = param.split(SEPARATOR);
            name = values[0];
            term = this.getTermValue(values[1].trim());
            extra = this.getExtraValue(values[1].trim());
        }

    }


    @NotNull
    @Property(order = 0, viewable = true)
    @Override
    public String getNodeType() {
        return getMethodTitle(name);
    }

    @NotNull
    @Property(order = 1, viewable = true)
    @Override
    public String getNodeName() {

        return nodeName;
    }

    @NotNull
    @Property(order = 2, viewable = true)
    public String getIndex() {
        return index;
    }

    @NotNull
    @Property(order = 3, viewable = true)
    public String getTerms() {
        return term;
    }

    @Property(order = 4, viewable = true)
    public long getCost() {
        return cost;
    }

    public void setCost(long cost) {
        this.cost = cost;
    }

    @Property(order = 5, viewable = true)
    public long getCardinality() {
        return row;
    }

    @NotNull
    @Property(order = 6, viewable = true)
    public String getTotal() {
        return this.totalValue;
    }

    @NotNull
    @Property(order = 7, viewable = true)
    public String getExtra() {
        return extra;
    }

    @NotNull
    @Property(order = 8, length = PropertyLength.MULTILINE)
    public String getFullText() {
        return fullText;
    }

    @Nullable
    @Override
    public CubridPlanNode getParent() {
        return parent;
    }

    @Nullable
    @Override
    public Collection<CubridPlanNode> getNested() {
        return nested;
    }

    @Override
    public DBCPlanNodeKind getNodeKind() {
        if ("sscan".equals(name)) {
            return DBCPlanNodeKind.TABLE_SCAN;
        } else if ("iscan".equals(name)) {
            return DBCPlanNodeKind.INDEX_SCAN;
        }
        return super.getNodeKind();
    }

    public void setAllNestedNode(List<CubridPlanNode> nodes) {
        nested.addAll(nodes);
    }

    @Nullable
    private String getMethodTitle(@NotNull String method) {

        return switch (method) {
            case "iscan" -> "Index Scan";
            case "sscan" -> "Full Scan";
            case "temp(group by)" -> "Group by Temp";
            case "temp(order by)" -> "Order by Temp";
            case "nl-join (inner join)" -> "Nested Loop - Inner Join";
            case "nl-join (cross join)" -> "Nested Loop - Cross Join";
            case "idx-join (inner join)" -> "Index Join - Inner Join";
            case "m-join (inner join)" -> "Merged - Inner Join";
            case "temp" -> "Temp";
            case "follow" -> "Follow";
            case "filtr" -> "Filter";
            default -> method;
        };
    }

    private void addNested(boolean normal, String param) {
        parent = this;
        nested.add(new CubridPlanNode(this, normal, param));
    }

    void parseNode() {
        addNested(true, null);
        while (segments.size() > 0) {
            String key = segments.get(0).split(SEPARATOR)[0];
            if (parentNode.contains(key)) {
                addNested(true, null);
            } else {
                parseObject();
                break;
            }
        }

    }

    void parseObject() {

        while (segments.size() > 0) {

            String segment = segments.remove(0);
            String[] values = segment.split(SEPARATOR);
            String key = values[0].trim();
            String value = values[1].trim();

            switch (key) {
                case "index":
                    String[] indexes = value.split(SPACE);
                    index = indexes[0];
                    extra = this.getExtraValue(indexes[0]);
                    if ((indexes.length > 1) && !subNode(this, key, value)) {
                        term = this.getTermValue(indexes[1]);
                        extra = this.getExtraValue(indexes[1]);
                    }
                    break;
                case "class":
                    this.setNameValue(value);
                    break;
                case "sort":
                    if (!parentExcept.contains(name))
                        extra = String.format("(sort %s)", value);
                default:
                    break;

            }
            if (parentNode.contains(key)) {
                name = value;
                if (!parentExcept.contains(value)) {
                    parseNode();
                    break;
                }
            } else if ("sargs".equals(key)) {
                if (!subNode(this, key, value) && !name.equals("sscan")) {
                    addNested(false, segment);
                } else {
                    term = this.getTermValue(value);
                    extra = this.getExtraValue(value);
                }

            } else if ("edge".equals(key)) {

                if (!subNode(parent, key, value) && parent.name.equals("follow")) {
                    parent.extra = this.getTermValue(value);

                } else if (!parent.name.startsWith("nl-join")) {
                    parent.addNested(false, segment);
                } else {
                    parent.term = this.getTermValue(value);
                    parent.extra = this.getExtraValue(value);
                }
            } else if ("filtr".equals(key)) {
                addNested(false, segment);
            } else if (key.contains("cost")) {
                String[] costs = value.split(" card ");
                this.cost = Long.parseLong(costs[0]);
                this.row = Long.parseLong(costs[1]);
                break;
            }
        }
    }

    private boolean subNode(CubridPlanNode node, String key, String value) {
        if (value.contains(" AND ")) {

            Matcher m = subNodePattern.matcher(value);
            int count = 1;
            while (m.find()) {
                node.addNested(false, String.format("%s %s:%s", key, count, m.group()));
                count++;
            }
            return true;
        }
        return false;
    }


    @Nullable
    private String getTermValue(String value) {
        if (CommonUtils.isNotEmpty(value)) {
            if (value.contains("node[")) {
                Matcher m = termPattern.matcher(value);
                if (m.find()) {
                    return value.replace(m.group(), classNode.get(m.group()));
                }
            } else {
                String termValue = terms.get(value);
                if (CommonUtils.isNotEmpty(termValue)) {
                    return termValue.split(" \\(sel ")[0];
                }
            }
        }
        return null;

    }

    @Nullable
    private String getExtraValue(String value) {
        String extraValue = terms.get(value);
        if (CommonUtils.isNotEmpty(extraValue)) {
            return "(sel " + extraValue.split(" \\(sel ")[1];
        }
        return null;
    }

    private void setNameValue(String value) {

        String[] values = value.split(SPACE);
        String nameValue = classNode.get(values[values.length - 1]);
        if (CommonUtils.isNotEmpty(nameValue)) {
            String temName = nameValue.split("\\(")[0];

            // make a unique name
            Set<String> setName = new LinkedHashSet<String>(Arrays.asList(temName.split(SPACE)));
            nodeName = String.join(SPACE, setName);

            this.setTotalValue(nameValue);
        }
    }

    private void setTotalValue(String value) {

        if (CommonUtils.isNotEmpty(value)) {

            Matcher m = totalPattern.matcher(value);
            if (m.find()) {
                totalValue = m.group(0);
            }
        }
    }

    @NotNull
    private List<String> getSegments() {

        Matcher matcher = segmentPattern.matcher(fullText);
        segments = new ArrayList<String>();
        while (matcher.find()) {
            String segment = matcher.group().trim();
            if (segment.startsWith("node")) {
                String[] values = segment.split(SEPARATOR);
                classNode.put(values[0], values[1]);
            } else if (segment.startsWith("term")) {
                String[] values = segment.split("]: ");
                terms.put(String.format("%s]", values[0]), values[1]);
            } else {
                segments.add(segment);
            }
        }
        this.name = segments.get(0).split(SEPARATOR)[1].trim();
        return segments;
    }

}
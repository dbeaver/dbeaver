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
    private static final String OPTIONS_SEPARATOR = ":";
    private static final String COST = "cost";
    private static final String CLASS = "class";
    private static Map<String, String> classNode = new HashMap<>();
    private static Map<String, String> terms = new HashMap<>();
    private String fullText;
    private String nodeName;
    private String name;
    private String index;
    private String term;
    private long cost;
    private long row;
    private Map<String, String> nodeProps = new HashMap<>();
    private CubridPlanNode parent;
    private List<CubridPlanNode> nested;

    public CubridPlanNode(@NotNull String queryPlan) {
        this(null, null, null, queryPlan);
    }

    private CubridPlanNode(@Nullable CubridPlanNode parent, @Nullable String name, @Nullable List<String> segments, @NotNull String fullText) {
        this.parent = parent;
        this.name = name;
        this.fullText = fullText;
        parseObject(parent == null ? this.getSegments() : segments);
        parseNode();
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
        return this.getNameOrTotal(true);
    }

    @NotNull
    @Property(order = 2, viewable = true)
    public String getIndex() {
        return index;
    }

    @NotNull
    @Property(order = 3, viewable = true)
    public String getTerms() {
        return getTermExtra(true);
    }

    @Property(order = 4, viewable = true)
    public long getCost() {
        return cost;
    }

    @Property(order = 5, viewable = true)
    public long getCardinality() {
        return row;
    }

    @NotNull
    @Property(order = 6, viewable = true)
    public String getTotal() {
        return this.getNameOrTotal(false);
    }

    @NotNull
    @Property(order = 7, viewable = true)
    public String getExtra() {
        return getTermExtra(false);
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
            default -> method;
        };
    }

    private void addNested(@NotNull String name, @NotNull List<String> value) {
        if (nested == null) {
            nested = new ArrayList<>();
        }
        nested.add(new CubridPlanNode(this, name, value, fullText));
    }

    private void parseNode() {
        for (String key : nodeProps.keySet()) {
            if (key.contains(CLASS)) {
                this.nodeName = nodeProps.get(CLASS).split(" ")[1];
            } else if (key.equals("index")) {
                this.index = nodeProps.get(key).split(" ")[0];
                this.term = nodeProps.get(key).split(" ")[1];
            } else if (key.equals("sargs")) {
                this.term = nodeProps.get(key);
            } else if (key.equals(COST)) {
                String[] values = nodeProps.get(key).split(" card ");
                this.cost = Long.parseLong(values[0]);
                this.row = Long.parseLong(values[1]);
            }
        }
    }

    private void parseObject(@NotNull List<String> segments) {
        if (!segments.isEmpty()) {
            String[] removes = segments.remove(0).split(OPTIONS_SEPARATOR);
            nodeProps.put(removes[0], removes[1].trim());
            if (removes[0].equals(COST) || segments.isEmpty()) {
                return;
            }
            String key = segments.get(0).split(OPTIONS_SEPARATOR)[0];
            if (nodeProps.containsKey(key) || removes[0].equals("subplan")) {
                addNested(removes[1].trim(), segments);
                parseObject(segments);
            } else if (key.equals(CLASS)) {
                if (!removes[0].equals("Query plan")) {
                    addNested(removes[1].trim(), segments);
                }
                parseObject(segments);
            } else {
                parseObject(segments);
            }
        }
    }

    @Nullable
    private String getTermExtra(boolean isTerm) {
        
        String termValue = terms.get(term);
        if(CommonUtils.isNotEmpty(termValue)) {
            String[] values = termValue.split(" \\(sel");
            if (isTerm)
                return values[0].trim();
            else
                return "(sel" + values[1].trim();
        }
        return null;
    }

    @Nullable
    private String getNameOrTotal(boolean isName) {
        String classNodeValue = classNode.get(nodeName);
        if(CommonUtils.isNotEmpty(classNodeValue)){
            Pattern p;
            if (isName) {
                p = Pattern.compile("\\w+ \\w+");
            } else {
                p = Pattern.compile("\\w+\\/\\w+");
            }
            Matcher m = p.matcher(classNodeValue);
            if (m.find()) {
                return m.group(0);
            }
        }
        
        return null;
    }

    @NotNull
    private List<String> getSegments() {
        Pattern pattern =
                Pattern.compile(
                        "(inner|outer|class|cost|index|sargs|Query plan|term\\[..|node\\[..):\\s*([^\\n\\r]*)");
        Matcher matcher = pattern.matcher(fullText);
        List<String> segments = new ArrayList<String>();
        while (matcher.find()) {
            String segment = matcher.group().trim();
            if (segment.startsWith("node")) {
                String[] values = segment.split(OPTIONS_SEPARATOR);
                classNode.put(values[0], values[1]);
            } else if (segment.startsWith("term")) {
                String[] values = segment.split(OPTIONS_SEPARATOR);
                terms.put(values[0], values[1]);
            } else {
                segments.add(segment);
            }
        }
        this.name = segments.get(0).split(OPTIONS_SEPARATOR)[1].trim();
        return segments;
    }


}

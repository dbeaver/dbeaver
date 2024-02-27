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

import org.jkiss.dbeaver.model.impl.plan.AbstractExecutionPlanNode;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.meta.PropertyLength;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CubridPlanNode extends AbstractExecutionPlanNode 
{

    private static String fullText;
    private static Map<String, String> classNode = new HashMap<>();

    private String nodeName;
    private String name;
    private Number cost;
    private Number row;
    private Map<String, String> nodeProps = new HashMap<>();

    private CubridPlanNode parent;
    private List<CubridPlanNode> nested;

    public CubridPlanNode(String queryPlan) 
    {
        fullText = queryPlan;
        parseObject(this.getSegments());
        parseNode();
    }

    private CubridPlanNode(CubridPlanNode parent, String name, List<String> segments) 
    {
        this.parent = parent;
        this.name = name;
        parseObject(segments);
        parseNode();
    }

    private void parseObject(List<String> segments) 
    {
        if (!segments.isEmpty()) {
            String[] removes = segments.remove(0).split(":");
            nodeProps.put(removes[0], removes[1].trim());
            if (removes[0].equals("cost") || segments.isEmpty()) {
                return;
            }
            String key = segments.get(0).split(":")[0];
            if (nodeProps.containsKey(key) || removes[0].equals("subplan")) {
                addNested(removes[1].trim(), segments);
                parseObject(segments);
            } else if (key.equals("class")) {
                if (!removes[0].equals("Query plan")) {
                    addNested(removes[1].trim(), segments);
                }
                parseObject(segments);
            } else {
                parseObject(segments);
            }
        }
    }

    private void addNested(String name, List<String> value) 
    {
        if (nested == null) {
            nested = new ArrayList<>();
        }
        nested.add(new CubridPlanNode(this, name, value));
    }

    @Property(order = 0, viewable = true)
    @Override
    public String getNodeType() 
    {
        return getMethodTitle(name);
    }

    @Property(order = 1, viewable = true)
    @Override
    public String getNodeName() 
    {
        return classNode.get(nodeName);
    }

    @Property(order = 2, viewable = true)
    public Number getCost() 
    {
        return cost;
    }

    @Property(order = 3, viewable = true)
    public Number getCardinality() 
    {
        return row;
    }

    @Property(order = 4, length = PropertyLength.MULTILINE)
    public String getFullText() 
    {
        return fullText;
    }

    @Override
    public CubridPlanNode getParent() 
    {
        return parent;
    }

    @Override
    public Collection<CubridPlanNode> getNested() 
    {
        return nested;
    }

    public String getMethodTitle(String method) 
    {

        switch (method) {
            case "iscan":
                return "Index Scan";
            case "sscan":
                return "Full Scan";
            case "temp(group by)":
                return "Group by Temp";
            case "temp(order by)":
                return "Order by Temp";
            case "nl-join (inner join)":
                return "Nested Loop - Inner Join";
            case "nl-join (cross join)":
                return "Nested Loop - Cross Join";
            case "idx-join (inner join)":
                return "Index Join - Inner Join";
            case "m-join (inner join)":
                return "Merged - Inner Join";
            case "temp":
                return "Temp";
            case "follow":
                return "Follow";
            default:
                return method;
        }
    }

    private void parseNode() {
        for (String key : nodeProps.keySet()) {
            if (key.contains("class")) {
                this.nodeName = nodeProps.get("class").split(" ")[1];
            } else if (key.equals("cost")) {
                String[] values = nodeProps.get(key).split(" card ");
                this.cost = Integer.parseInt(values[0]);
                this.row = Integer.parseInt(values[1]);
            }
        }
    }

    public List<String> getSegments() 
    {
        Pattern pattern =
                Pattern.compile(
                        "[\\n\\r]node\\[....\\s*([^\\n\\r]*)|[\\n\\r].*Query plan:\\s*([^\\n\\r]*)|[\\n\\r].*subplan:\\s*([^\\n\\r]*)|[\\n\\r].*cost:\\s*([^\\n\\r]*)|inner:\\s*([^\\n\\r]*)|outer:\\s*([^\\n\\r]*)|class:\\s*([^\\n\\r]*)");
        Matcher matcher = pattern.matcher(fullText);
        List<String> segments = new ArrayList<String>();
        while (matcher.find()) {
            String segment = matcher.group().trim();
            if (segment.split(":")[0].startsWith("node")) {
                String[] values = segment.split(":");
                classNode.put(values[0], values[1].split("\\(")[0].trim());
            } else {
                segments.add(segment);
            }
        }
        this.name = segments.get(0).split(":")[1].trim();
        return segments;
    }
}

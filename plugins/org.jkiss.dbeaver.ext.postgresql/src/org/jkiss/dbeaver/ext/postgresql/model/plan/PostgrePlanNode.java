/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2016 Serge Rieder (serge@jkiss.org)
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (version 2)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
package org.jkiss.dbeaver.ext.postgresql.model.plan;

import org.jkiss.code.Nullable;
import org.jkiss.dbeaver.model.DBPPropertyDescriptor;
import org.jkiss.dbeaver.model.DBPPropertySource;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.model.impl.PropertyDescriptor;
import org.jkiss.dbeaver.model.runtime.DBRProgressMonitor;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Postgre execution plan node
 */
public class PostgrePlanNode implements DBCPlanNode, DBPPropertySource {

    public static final String ATTR_NODE_TYPE = "Node-Type";
    public static final String ATTR_RELATION_NAME = "Relation-Name";
    public static final String ATTR_ALIAS = "Alias";
    public static final String ATTR_TOTAL_COST = "Total-Cost";
    public static final String ATTR_STARTUP_COST = "Startup-Cost";
    public static final String ATTR_INDEX_NAME = "Index-Name";

    private PostgrePlanNode parent;
    private List<PostgrePlanNode> nested;

    private String nodeType;
    private String entity;
    private String cost;
    private Map<String, String> attributes = new LinkedHashMap<>();

    public PostgrePlanNode(PostgrePlanNode parent, Element element) {
        this.parent = parent;

        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element && !"Plans".equals(child.getNodeName())) {
                attributes.put(child.getNodeName(), child.getTextContent());
            }
        }
        nodeType = attributes.remove(ATTR_NODE_TYPE);
        entity = attributes.get(ATTR_RELATION_NAME);
        if (entity != null) {
            String alias = attributes.get(ATTR_ALIAS);
            if (alias != null && !alias.equals(entity)) {
                entity += " as " + alias;
            }
        } else {
            entity = attributes.get(ATTR_INDEX_NAME);
        }
        String startCost = attributes.remove(ATTR_STARTUP_COST);
        String totalCost = attributes.remove(ATTR_TOTAL_COST);
        cost = startCost + " - " + totalCost;

        Element nestedPlansElement = XMLUtils.getChildElement(element, "Plans");
        if (nestedPlansElement != null) {
            for (Element planElement : XMLUtils.getChildElementList(nestedPlansElement, "Plan")) {
                if (nested == null) {
                    nested = new ArrayList<>();
                }
                nested.add(new PostgrePlanNode(null, planElement));
            }
        }
    }

    @Property(order = 0, viewable = true)
    public String getNodeType() {
        return nodeType;
    }

    @Property(order = 2, viewable = true)
    public String getEntity() {
        return entity;
    }

    @Property(order = 3, viewable = true)
    public String getCost() {
        return cost;
    }

    @Override
    public DBCPlanNode getParent()
    {
        return parent;
    }

    @Override
    public List<PostgrePlanNode> getNested()
    {
        return nested;
    }

    @Override
    public Object getEditableValue() {
        return this;
    }

    @Override
    public DBPPropertyDescriptor[] getPropertyDescriptors2() {
        DBPPropertyDescriptor[] props = new DBPPropertyDescriptor[attributes.size()];
        int index = 0;
        for (Map.Entry<String, String> attr : attributes.entrySet()) {
            props[index++] = new PropertyDescriptor("Source", attr.getKey(), attr.getKey(), null, String.class, false, null, null, false);
        }
        return props;
    }

    @Override
    public Object getPropertyValue(@Nullable DBRProgressMonitor monitor, Object id) {
        return attributes.get(id.toString());
    }

    @Override
    public boolean isPropertySet(Object id) {
        return false;//attributes.containsKey(id.toString());
    }

    @Override
    public boolean isPropertyResettable(Object id) {
        return false;
    }

    @Override
    public void resetPropertyValue(@Nullable DBRProgressMonitor monitor, Object id) {

    }

    @Override
    public void resetPropertyValueToDefault(Object id) {

    }

    @Override
    public void setPropertyValue(@Nullable DBRProgressMonitor monitor, Object id, Object value) {

    }

    @Override
    public boolean isDirty(Object id) {
        return false;
    }

    @Override
    public String toString() {
        StringBuilder title = new StringBuilder();
        title.append("Type: ").append(nodeType);
        String joinType = attributes.get("Join-Type");
        if (!CommonUtils.isEmpty(joinType)) {
            title.append(" (").append(joinType).append(")");
        }
        title.append("; ");
        if (!CommonUtils.isEmpty(entity)) {
            title.append("Rel: ").append(entity).append(" ");
        }
        title.append("; Cost: ").append(cost);

        return title.toString();
    }
}

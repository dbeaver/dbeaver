/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.jkiss.dbeaver.ext.postgresql.model.plan;

import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.dbeaver.model.impl.jdbc.JDBCUtils;
import org.jkiss.dbeaver.model.meta.Property;
import org.jkiss.dbeaver.ui.properties.PropertyDescriptorEx;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

/**
 * MySQL execution plan node
 */
public class PostgrePlanNode implements DBCPlanNode, IPropertySource {

    private PostgrePlanNode parent;
    private List<PostgrePlanNode> nested;

/*
    private String nodeType;
    private String startupCost;
    private String totalCost;
    private String actualStartupTime;
    private String actualTotalTime;
    private String actualRows;
    private String actualLoops;
    private String alias;
    private String relationName;
*/
    private Map<String, String> attributes = new LinkedHashMap<String, String>();

    public PostgrePlanNode(PostgrePlanNode parent, Element element) {
        this.parent = parent;

        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element && !"Plans".equals(child.getNodeName())) {
                attributes.put(child.getNodeName(), child.getTextContent());
            }
        }
/*
        nodeType = element.getAttribute("Node-Type");
        startupCost = element.getAttribute("Startup-Cost");
        totalCost element.getAttribute("Total-Cost");
        element.getAttribute("Actual-Startup-Time");
        element.getAttribute("Actual-Total-Time");
        element.getAttribute("Actual-Rows");
        element.getAttribute("Actual-Loops");
        element.getAttribute("Alias");
        element.getAttribute("Relation-Name");
*/

        Element nestedPlansElement = XMLUtils.getChildElement(element, "Plans");
        if (nestedPlansElement != null) {
            for (Element planElement : XMLUtils.getChildElementList(nestedPlansElement, "Plan")) {
                if (nested == null) {
                    nested = new ArrayList<PostgrePlanNode>();
                }
                nested.add(new PostgrePlanNode(null, planElement));
            }
        }
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
    public IPropertyDescriptor[] getPropertyDescriptors() {
        IPropertyDescriptor[] props = new IPropertyDescriptor[attributes.size()];
        int index = 0;
        for (Map.Entry<String, String> attr : attributes.entrySet()) {
            props[index++] = new PropertyDescriptorEx(null, attr.getKey(), attr.getKey(), null, String.class, false, null, null, false);
        }
        return props;
    }

    @Override
    public Object getPropertyValue(Object id) {
        return attributes.get(id.toString());
    }

    @Override
    public boolean isPropertySet(Object id) {
        return false;//attributes.containsKey(id.toString());
    }

    @Override
    public void resetPropertyValue(Object id) {

    }

    @Override
    public void setPropertyValue(Object id, Object value) {

    }
}

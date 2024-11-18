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
package org.jkiss.dbeaver.ext.kingbase.model.plan;

import java.util.LinkedHashMap;
import java.util.Map;

import org.jkiss.dbeaver.model.DBPDataSource;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Kingbase execution plan node
 */
public class KingbasePlanNodeXML extends KingbasePlanNodeBase<KingbasePlanNodeXML> {

    public KingbasePlanNodeXML(DBPDataSource dataSource, KingbasePlanNodeXML parent, Element element) {
        super(dataSource, parent);

        Map<String, String> attributes = new LinkedHashMap<>();
        for (Node child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (child instanceof Element && !"Plans".equals(child.getNodeName())) {
                attributes.put(child.getNodeName(), child.getTextContent());
            }
        }
        setAttributes(attributes);

        Element nestedPlansElement = XMLUtils.getChildElement(element, "Plans");
        if (nestedPlansElement != null) {
            for (Element planElement : XMLUtils.getChildElementList(nestedPlansElement, "Plan")) {
                nested.add(new KingbasePlanNodeXML(dataSource, null, planElement));
            }
        }
    }

}

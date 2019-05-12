/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2019 Serge Rider (serge@jkiss.org)
 * Copyright (C) 2019 Andrew Khitrin (ahitrin@gmail.com)
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

package org.jkiss.dbeaver.ext.mssql.model.plan;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.jkiss.dbeaver.ext.mssql.model.plan.adapters.SQLServerPlanAdapter_v_1_0;
import org.jkiss.dbeaver.ext.mssql.model.plan.adapters.SQLServerPlanAdapter_v_1_0_2;
import org.jkiss.dbeaver.ext.mssql.model.plan.adapters.SQLServerPlanAdapter_v_1_1;
import org.jkiss.dbeaver.ext.mssql.model.plan.adapters.SQLServerPlanAdapter_v_1_2;
import org.jkiss.dbeaver.ext.mssql.model.plan.adapters.SQLServerPlanAdapter_v_1_3;
import org.jkiss.dbeaver.ext.mssql.model.plan.adapters.SQLServerPlanAdapter_v_1_3_1;
import org.jkiss.dbeaver.ext.mssql.model.plan.adapters.SQLServerPlanAdapter_v_1_4;
import org.jkiss.dbeaver.ext.mssql.model.plan.adapters.SQLServerPlanAdapter_v_1_5;
import org.jkiss.dbeaver.ext.mssql.model.plan.adapters.SQLServerPlanAdapter_v_1_6;
import org.jkiss.dbeaver.model.exec.DBCException;
import org.jkiss.dbeaver.model.exec.plan.DBCPlanNode;
import org.jkiss.utils.xml.XMLException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

public class SQLServerPlanParser {

    public static final String rootNodeXPath = "/*[local-name() = 'ShowPlanXML']";
    public static final String VERSION_ATTR = "Version";

    public static SQLServerPlanParser instance = new SQLServerPlanParser();

    private final Map<String, SQLServerPlanAdapter<?>> adapterRegistry = new HashMap<String, SQLServerPlanAdapter<?>>() {
        {
            put("1.6", (SQLServerPlanAdapter<?>) new SQLServerPlanAdapter_v_1_6());
            put("1.5", (SQLServerPlanAdapter<?>) new SQLServerPlanAdapter_v_1_5());
            put("1.4", (SQLServerPlanAdapter<?>) new SQLServerPlanAdapter_v_1_4());
            put("1.3.1", (SQLServerPlanAdapter<?>) new SQLServerPlanAdapter_v_1_3_1());
            put("1.3", (SQLServerPlanAdapter<?>) new SQLServerPlanAdapter_v_1_3());
            put("1.2", (SQLServerPlanAdapter<?>) new SQLServerPlanAdapter_v_1_2());
            put("1.1", (SQLServerPlanAdapter<?>) new SQLServerPlanAdapter_v_1_1());
            put("1.02", (SQLServerPlanAdapter<?>) new SQLServerPlanAdapter_v_1_0_2());
            put("1.0", (SQLServerPlanAdapter<?>) new SQLServerPlanAdapter_v_1_0());

        }
    };

    private SQLServerPlanParser() {
    }

    public static SQLServerPlanParser getInstance() {
        return instance;
    }

    public List<DBCPlanNode> parse(String planString) throws DBCException {

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        Node root;

        try {
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document document = db.parse(new InputSource(new StringReader(planString)));

            XPathFactory xpf = XPathFactory.newInstance();
            XPath xpath = xpf.newXPath();
            root = (Element) xpath.evaluate(rootNodeXPath, document, XPathConstants.NODE);

            if (root == null) {
                throw new DBCException("Unsupported plan format");
            }

            Node version = root.getAttributes().getNamedItem(VERSION_ATTR);

            if (version == null) {
                throw new DBCException("Undefined plan version");
            }

            String ver = version.getNodeValue();

            if (ver == null || ver.length() == 0) {
                throw new DBCException("Undefined plan version");
            }
            
            if (!adapterRegistry.containsKey(ver)) {
                throw new DBCException("Unsupported plan version");
            }
            
            return adapterRegistry.get(ver).getNodes(planString);

        } catch (ParserConfigurationException | SAXException | IOException | XPathExpressionException | XMLException e) {

            throw new DBCException("Error parsing plan", e);
        }

    }
}

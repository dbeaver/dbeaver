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
package org.jkiss.dbeaver.registry.expressions;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.Strictness;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.model.data.DBDContent;
import org.jkiss.dbeaver.model.data.json.JSONUtils;
import org.jkiss.dbeaver.utils.MimeTypes;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLException;
import org.jkiss.utils.xml.XMLUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.namespace.QName;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.util.ArrayList;
import java.util.List;

public class ContentExpressionFunctions {

    private enum XMLExpressionResultType {
        STRING("string", XPathConstants.STRING),
        NUMBER("number", XPathConstants.NUMBER),
        BOOLEAN("boolean", XPathConstants.BOOLEAN),
        NODE("node", XPathConstants.NODE),
        NODESET("nodeset", XPathConstants.NODESET);

        String name;
        QName constant;

        XMLExpressionResultType(String name, QName constant) {
            this.name = name;
            this.constant = constant;
        }

        public QName getConstant() {
            return constant;
        }

        public static XMLExpressionResultType fromValue(String value) {
            for (XMLExpressionResultType type : XMLExpressionResultType.values()) {
                if (type.name.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            throw new IllegalArgumentException(value);
        }
    }

    private static final Log log = Log.getLog(ContentExpressionFunctions.class);

    private static final Gson GSON = new GsonBuilder()
        .setStrictness(Strictness.LENIENT)
        .serializeNulls()
        .setPrettyPrinting()
        .create();

    public static Object json(Object object) {
        if (object == null) {
            return null;
        }
        return JSONUtils.parseMap(GSON, new StringReader(object.toString()));
    }

    public static Object xml(Object object, String expression) {
        return xml(object, "string", expression);
    }

    public static Object xml(Object object, String returnType, String expression) {
        if (object == null || CommonUtils.isEmpty(expression) ||
            !(object instanceof DBDContent) || !((DBDContent) object).getContentType().equals(MimeTypes.TEXT_XML)
        ) {
            return null;
        }
        XMLExpressionResultType resultType = XMLExpressionResultType.fromValue(returnType);
        if (resultType == null) {
            resultType = XMLExpressionResultType.STRING;
        }
        try {
            Object rawValue = ((DBDContent) object).getRawValue();
            String xmlValue;
            if (rawValue instanceof SQLXML) {
                xmlValue = ((SQLXML) rawValue).getString();
            } else {
                xmlValue = rawValue.toString();
            }
            Document document = XMLUtils.parseDocument(new StringReader(xmlValue));
            XPath xPath = XPathFactory.newInstance().newXPath();
            Object result = xPath.evaluate(expression, document, resultType.getConstant());
            if (result instanceof NodeList) {
                List<String> valuesList = new ArrayList<>();
                NodeList nodeList = (NodeList) result;
                for (int i = 0; i < nodeList.getLength(); i++) {
                    valuesList.add(nodeList.item(i).getNodeValue());
                }
                return valuesList;
            }
            if (result instanceof Node) {
                return ((Node) result).getNodeValue();
            }
            return result;
        } catch (XMLException | XPathExpressionException | SQLException e) {
            log.error("Can't parse XML value", e);
        }
        return null;
    }
}

/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2020 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.editors.xml;

import org.eclipse.jface.text.formatter.ContextBasedFormattingStrategy;
import org.jkiss.dbeaver.Log;
import org.jkiss.utils.CommonUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * The formatting strategy that transforms SQL keywords to upper case
 */
public class XMLFormattingStrategy extends ContextBasedFormattingStrategy
{
    private static final Log log = Log.getLog(XMLFormattingStrategy.class);

    XMLFormattingStrategy()
    {
    }

    @Override
    public void formatterStarts(String initialIndentation)
    {
    }

    @Override
    public String format(String content, boolean isLineStart, String indentation, int[] positions)
    {
        if (CommonUtils.isEmpty(content)) {
            return content;
        }
        try {
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(content)));

            {
                // Remove whitespaces between tags
                XPath xPath = XPathFactory.newInstance().newXPath();
                NodeList nodeList = (NodeList) xPath.evaluate("//text()[normalize-space()='']",
                    document,
                    XPathConstants.NODESET);

                for (int i = 0; i < nodeList.getLength(); ++i) {
                    Node node = nodeList.item(i);
                    node.getParentNode().removeChild(node);
                }
            }

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", 2);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            if (!content.contains("<?xml")) {
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            }

            //StreamSource source = new StreamSource(new StringReader(content));
            DOMSource source = new DOMSource(document);
            StreamResult result = new StreamResult(new StringWriter());
            transformer.transform(source, result);
            return result.getWriter().toString();
        } catch (Throwable e) {
            log.debug("Error formatting XML: " + e.getMessage());
            return content;
        }
    }

    @Override
    public void formatterStops()
    {
    }

}
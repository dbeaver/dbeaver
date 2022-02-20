/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2022 DBeaver Corp and others
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
import org.xml.sax.InputSource;

import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.sax.SAXSource;
import javax.xml.transform.stream.StreamResult;
import java.io.StringReader;
import java.io.StringWriter;

/**
 * The formatting strategy that transforms SQL keywords to upper case
 */
public class XMLFormattingStrategy extends ContextBasedFormattingStrategy {

    private static final Log log = Log.getLog(XMLFormattingStrategy.class);

    XMLFormattingStrategy() {
    }

    @Override
    public void formatterStarts(String initialIndentation) {
    }

    @Override
    public String format(String content, boolean isLineStart, String indentation, int[] positions) {
        if (CommonUtils.isEmpty(content)) {
            return content;
        }
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            transformerFactory.setAttribute("indent-number", 2);
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "yes");
            if (!content.contains("<?xml")) {
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            }

            SAXParserFactory spf = SAXParserFactory.newInstance();
            spf.setNamespaceAware(true);
            spf.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            Source src = new SAXSource(spf.newSAXParser().getXMLReader(), new InputSource(new StringReader(content)));

            StreamResult result = new StreamResult(new StringWriter());
            transformer.transform(src, result);
            String resultString = result.getWriter().toString();
            if (CommonUtils.isEmpty(resultString)) {
                return content;
            }

            return resultString.replaceAll("(?m)^[ \\t]*\\r?\\n", ""); // Replace all empty lines
        } catch (Throwable e) {
            log.debug("Error formatting XML: " + e.getMessage());
            return content;
        }
    }

    @Override
    public void formatterStops() {
    }

}

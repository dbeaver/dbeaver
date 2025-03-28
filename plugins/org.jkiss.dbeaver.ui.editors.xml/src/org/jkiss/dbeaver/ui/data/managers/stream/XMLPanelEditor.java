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
package org.jkiss.dbeaver.ui.data.managers.stream;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.jkiss.dbeaver.ui.data.IValueController;
import org.jkiss.dbeaver.ui.data.managers.AbstractTextPanelEditor;
import org.jkiss.dbeaver.ui.editors.xml.XMLEditor;
import org.jkiss.utils.CommonUtils;
import org.w3c.dom.CDATASection;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.io.StringReader;
import java.io.StringWriter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
* XMLPanelEditor
*/
public class XMLPanelEditor extends AbstractTextPanelEditor<XMLEditor> {

    @Override
    protected XMLEditor createEditorParty(IValueController valueController) {
        // Override init function because standard is VEEERY slow
        return new XMLEditor() {
            @Override
            public void init(IEditorSite site, IEditorInput input) throws PartInitException {
                setSite(site);
                try {
                    doSetInput(input);
                } catch (CoreException e) {
                    throw new PartInitException("Error initializing panel XML editor", e);
                }
            }
        };
    }

    @Override
    protected String getFileFolderName() {
        return "dbeaver-xml";
    }

    @Override
    protected String getFileExtension() {
        return ".xml";
    }

    @Override
    public boolean supportMinify() {
        return true;
    }

    @Override
    public String minify(String value) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringElementContentWhitespace(true);
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document document = builder.parse(new org.xml.sax.InputSource(new StringReader(value)));

            removeWhitespaceNodes(document.getDocumentElement());

            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            transformer.setOutputProperty(OutputKeys.INDENT, "no");
            if (!value.contains("<?xml")) {
                transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            }

            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));

            String resultString = writer.toString();
            if (CommonUtils.isEmpty(resultString)) {
                return value;
            }

            return resultString;
        } catch (Throwable e) {
            return value;
        }
    }

    private static void removeWhitespaceNodes(Node node) {
        NodeList childNodes = node.getChildNodes();
        for (int i = childNodes.getLength() - 1; i >= 0; i--) {
            Node child = childNodes.item(i);

            if (child.getNodeType() == Node.TEXT_NODE) {
                if (child.getNodeValue().isBlank()) {
                    node.removeChild(child);
                }
            } else if (child.getNodeType() == Node.ELEMENT_NODE) {
                removeWhitespaceNodes(child);
            }
        }
    }
}

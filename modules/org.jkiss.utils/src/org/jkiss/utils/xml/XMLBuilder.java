/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2017 Serge Rider (serge@jkiss.org)
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

package org.jkiss.utils.xml;

import org.jkiss.utils.Base64;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Stream oriented XML document builder.
 */
public class XMLBuilder
{

    public final class Element implements AutoCloseable
    {

        private Element parent;
        private String name;
        private Map<String, String> nsStack = null;
        private int level;

        Element(
            Element parent,
            String name)
        {
            this.init(parent, name);
        }

        void init(
            Element parent,
            String name)
        {
            this.parent = parent;
            this.name = name;
            this.nsStack = null;
            this.level = parent == null ? 0 : parent.level + 1;
        }

        public String getName()
        {
            return name;
        }

        public int getLevel()
        {
            return level;
        }

        public void addNamespace(String nsURI, String nsPrefix)
        {
            if (nsStack == null) {
                nsStack = new HashMap<>();
            }
            nsStack.put(nsURI, nsPrefix);
        }

        public String getNamespacePrefix(String nsURI)
        {
            if (nsURI.equals(XMLConstants.NS_XML)) {
                return XMLConstants.PREFIX_XML;
            }
            String prefix = (nsStack == null ? null : nsStack.get(nsURI));
            return prefix != null ?
                prefix :
                (parent != null ? parent.getNamespacePrefix(nsURI) : null);
        }

        @Override
        public void close() throws IOException {
            XMLBuilder.this.endElement();
        }
    }

    // At the beginning and after tag closing
    private static final int STATE_NOTHING = 0;
    // After tag opening
    private static final int STATE_ELEM_OPENED = 1;
    // After text added
    private static final int STATE_TEXT_ADDED = 2;

    private static final int IO_BUFFER_SIZE = 8192;

    private java.io.Writer writer;

    private int state = STATE_NOTHING;

    private Element element = null;
    private boolean butify = false;

    private List<Element> trashElements = new java.util.ArrayList<>();

    public XMLBuilder(
        java.io.OutputStream stream,
        String documentEncoding)
        throws java.io.IOException
    {
        this(stream, documentEncoding, true);
    }

    public XMLBuilder(
        java.io.OutputStream stream,
        String documentEncoding,
        boolean printHeader)
        throws java.io.IOException
    {
        if (documentEncoding == null) {
            this.init(new java.io.OutputStreamWriter(stream), null, printHeader);
        } else {
            this.init(
                new java.io.OutputStreamWriter(stream, documentEncoding),
                documentEncoding,
                printHeader);
        }
    }

    public XMLBuilder(
        java.io.Writer writer,
        String documentEncoding)
        throws java.io.IOException
    {
        this(writer, documentEncoding, true);
    }

    public XMLBuilder(
        java.io.Writer writer,
        String documentEncoding,
        boolean printHeader)
        throws java.io.IOException
    {
        this.init(writer, documentEncoding, printHeader);
    }

    private Element createElement(
        Element parent,
        String name)
    {
        if (trashElements.isEmpty()) {
            return new Element(parent, name);
        } else {
            Element element = trashElements.remove(trashElements.size() - 1);
            element.init(parent, name);
            return element;
        }
    }

    private void deleteElement(
        Element element)
    {
        trashElements.add(element);
    }

    private void init(
        java.io.Writer writer,
        String documentEncoding,
        boolean printHeader)
        throws java.io.IOException
    {
        this.writer = new java.io.BufferedWriter(writer, IO_BUFFER_SIZE);

        if (printHeader) {
            if (documentEncoding != null) {
                this.writer.write(XMLConstants.XML_HEADER(documentEncoding));
            } else {
                this.writer.write(XMLConstants.XML_HEADER());
            }
        }
    }

    public boolean isButify()
    {
        return butify;
    }

    public void setButify(boolean butify)
    {
        this.butify = butify;
    }

    public Element startElement(
        String elementName)
        throws java.io.IOException
    {
        return this.startElement(null, null, elementName);
    }

    public Element startElement(
        String nsURI,
        String elementName)
        throws java.io.IOException
    {
        return this.startElement(nsURI, null, elementName);
    }

    /*
         NS prefix will be used in element name if its directly specified
         as nsPrefix parameter or if nsURI has been declared above
     */
    public Element startElement(
        String nsURI,
        String nsPrefix,
        String elementName)
        throws java.io.IOException
    {
        switch (state) {
            case STATE_ELEM_OPENED:
                writer.write('>');
            case STATE_NOTHING:
                if (butify) {
                    writer.write('\n');
                }
                break;
            default:
                break;
        }
        if (butify) {
            if (element != null) {
                for (int i = 0; i <= element.getLevel(); i++) {
                    writer.write('\t');
                }
            }
        }
        writer.write('<');

        boolean addNamespace = (nsURI != null);

        // If old nsURI specified - use prefix
        if (nsURI != null) {
            if (nsPrefix == null && element != null) {
                nsPrefix = element.getNamespacePrefix(nsURI);
                if (nsPrefix != null) {
                    // Do not add NS declaration - it was declared somewhere above
                    addNamespace = false;
                }
            }
        }

        // If we have prefix - use it in tag name
        if (nsPrefix != null) {
            elementName = nsPrefix + ':' + elementName;
        }

        writer.write(elementName);
        state = STATE_ELEM_OPENED;

        element = this.createElement(element, elementName);

        if (addNamespace) {
            this.addNamespace(nsURI, nsPrefix);
            element.addNamespace(nsURI, nsPrefix);
        }

        return element;
    }

    public XMLBuilder endElement()
        throws java.io.IOException, IllegalStateException
    {
        if (element == null) {
            throw new IllegalStateException("Close tag without open");
        }

        switch (state) {
            case STATE_ELEM_OPENED:
                writer.write("/>");
                break;
            case STATE_NOTHING:
                if (butify) {
                    writer.write('\n');
                    for (int i = 0; i < element.getLevel(); i++) {
                        writer.write('\t');
                    }
                }
            case STATE_TEXT_ADDED:
                writer.write("</");
                writer.write(element.getName());
                writer.write('>');
            default:
                break;
        }

        this.deleteElement(element);
        element = element.parent;
        state = STATE_NOTHING;

        return this;
    }

    public XMLBuilder addNamespace(String nsURI)
        throws java.io.IOException
    {
        return this.addNamespace(nsURI, null);
    }

    public XMLBuilder addNamespace(
        String nsURI,
        String nsPrefix)
        throws java.io.IOException, IllegalStateException
    {
        if (element == null) {
            throw new IllegalStateException("Namespace outside of element");
        }
        String attrName = XMLConstants.XMLNS;
        if (nsPrefix != null) {
            attrName = attrName + ':' + nsPrefix;
            element.addNamespace(nsURI, nsPrefix);
        }
        this.addAttribute(null, attrName, nsURI, true);

        return this;
    }

    public XMLBuilder addAttribute(
        String attributeName,
        String attributeValue)
        throws java.io.IOException
    {
        return this.addAttribute(null, attributeName, attributeValue, true);
    }

    public XMLBuilder addAttribute(
        String attributeName,
        int attributeValue)
        throws java.io.IOException
    {
        return this.addAttribute(null, attributeName, String.valueOf(attributeValue), false);
    }

    public XMLBuilder addAttribute(
        String attributeName,
        long attributeValue)
        throws java.io.IOException
    {
        return this.addAttribute(null, attributeName, String.valueOf(attributeValue), false);
    }

    public XMLBuilder addAttribute(
        String attributeName,
        boolean attributeValue)
        throws java.io.IOException
    {
        return this.addAttribute(null, attributeName, String.valueOf(attributeValue), false);
    }

    public XMLBuilder addAttribute(
        String attributeName,
        float attributeValue)
        throws java.io.IOException
    {
        return this.addAttribute(null, attributeName, String.valueOf(attributeValue), false);
    }

    public XMLBuilder addAttribute(
        String attributeName,
        double attributeValue)
        throws java.io.IOException
    {
        return this.addAttribute(null, attributeName, String.valueOf(attributeValue), false);
    }

    public XMLBuilder addAttribute(
        String nsURI,
        String attributeName,
        String attributeValue)
        throws java.io.IOException
    {
        return this.addAttribute(nsURI, attributeName, attributeValue, true);
    }

    private XMLBuilder addAttribute(
        String nsURI,
        String attributeName,
        String attributeValue,
        boolean escape)
        throws java.io.IOException, IllegalStateException
    {
        switch (state) {
            case STATE_ELEM_OPENED: {
                if (nsURI != null) {
                    String nsPrefix = element.getNamespacePrefix(nsURI);
                    if (nsPrefix == null) {
                        throw new IllegalStateException(
                            "Unknown attribute '" + attributeName + "' namespace URI '" + nsURI + "' in element '" + element.getName() + "'");
                    }
                    attributeName = nsPrefix + ':' + attributeName;
                }
                writer.write(' ');
                writer.write(attributeName);
                writer.write("=\"");
                writer.write(escape ? XMLUtils.escapeXml(attributeValue) : attributeValue);
                writer.write('"');
                break;
            }
            case STATE_TEXT_ADDED:
            case STATE_NOTHING:
                throw new IllegalStateException(
                    "Attribute ouside of element");
            default:
                break;
        }

        return this;
    }

    public XMLBuilder addText(
        CharSequence textValue)
        throws java.io.IOException
    {
        return addText(textValue, true);
    }

    public XMLBuilder addText(
        CharSequence textValue,
        boolean escape)
        throws java.io.IOException
    {
        switch (state) {
            case STATE_ELEM_OPENED:
                writer.write('>');
            case STATE_TEXT_ADDED:
            case STATE_NOTHING:
                break;
            default:
                break;
        }
        this.writeText(textValue, escape);

        state = STATE_TEXT_ADDED;

        return this;
    }

    /**
     * Adds entire content of specified reader as text
     *
     * @param reader text reader
     * @return self reference
     * @throws java.io.IOException on IO error
     */
    public XMLBuilder addText(
        java.io.Reader reader)
        throws java.io.IOException
    {
        switch (state) {
            case STATE_ELEM_OPENED:
                writer.write('>');
            case STATE_TEXT_ADDED:
            case STATE_NOTHING:
                break;
            default:
                break;
        }

        writer.write("<![CDATA[");
        char[] writeBuffer = new char[8192];
        for (int br = reader.read(writeBuffer); br != -1; br = reader.read(writeBuffer)) {
            writer.write(new String(writeBuffer, 0, br));
        }
        writer.write("]]>");

        state = STATE_TEXT_ADDED;

        return this;
    }

    public XMLBuilder addTextData(
        String text)
        throws java.io.IOException
    {
        switch (state) {
            case STATE_ELEM_OPENED:
                writer.write('>');
            case STATE_TEXT_ADDED:
            case STATE_NOTHING:
                break;
            default:
                break;
        }

        writer.write("<![CDATA[");
        writer.write(text);
        writer.write("]]>");

        state = STATE_TEXT_ADDED;

        return this;
    }

    /**
     * Adds content of specified stream as Base64 encoded text
     *
     * @param stream Input content stream
     * @param length Content length (this parameter must be correctly specified)
     * @return self reference
     * @throws java.io.IOException on IO error
     */
    public XMLBuilder addBinary(
        java.io.InputStream stream,
        int length)
        throws java.io.IOException
    {
        switch (state) {
            case STATE_ELEM_OPENED:
                writer.write('>');
            case STATE_TEXT_ADDED:
            case STATE_NOTHING:
                break;
            default:
                break;
        }

        Base64.encode(stream, length, writer);
        state = STATE_TEXT_ADDED;

        return this;
    }

    public XMLBuilder addBinary(
        byte[] buffer)
        throws java.io.IOException
    {
        switch (state) {
            case STATE_ELEM_OPENED:
                writer.write('>');
            case STATE_TEXT_ADDED:
            case STATE_NOTHING:
                break;
            default:
                break;
        }

        Base64.encode(buffer, 0, buffer.length, writer);
        state = STATE_TEXT_ADDED;

        return this;
    }

    /**
     * Adds character content as is without any escaping or validation
     * @param textValue content
     * @return self reference
     * @throws java.io.IOException
     */
    public XMLBuilder addContent(
        CharSequence textValue)
        throws java.io.IOException
    {
        writer.write(textValue.toString());
        return this;
    }

    public XMLBuilder addComment(
        String commentValue)
        throws java.io.IOException
    {
        switch (state) {
            case STATE_ELEM_OPENED:
                writer.write('>');
            case STATE_NOTHING:
                if (butify) {
                    writer.write('\n');
                }
                break;
            default:
                break;
        }
        writer.write("<!--");
        writer.write(commentValue);
        writer.write("-->");
        if (butify) {
            writer.write('\n');
        }
        state = STATE_TEXT_ADDED;

        return this;
    }

    public XMLBuilder addElement(
        String elementName,
        String elementValue)
        throws java.io.IOException
    {
        this.startElement(elementName);
        this.addText(elementValue);
        this.endElement();
        return this;
    }

    public XMLBuilder addElementText(
        String elementName,
        String elementValue)
        throws java.io.IOException
    {
        this.startElement(elementName);
        this.addTextData(elementValue);
        this.endElement();
        return this;
    }

    public XMLBuilder flush()
        throws java.io.IOException
    {
        writer.flush();
        return this;
    }

    private XMLBuilder writeText(CharSequence textValue, boolean escape)
        throws java.io.IOException
    {
        if (textValue != null) {
            writer.write(escape ? XMLUtils.escapeXml(textValue) : textValue.toString());
        }
        return this;
    }

}

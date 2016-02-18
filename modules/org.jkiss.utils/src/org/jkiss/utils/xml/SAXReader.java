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
package org.jkiss.utils.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SAX document reader
 */
public final class SAXReader implements org.xml.sax.ContentHandler {

    public static final int DEFAULT_POOL_SIZE = 10;

    private static javax.xml.parsers.SAXParserFactory saxParserFactory = null;
    private static List<Parser> parsersPool = new ArrayList<>();

    private org.xml.sax.InputSource inputSource;
    private org.xml.sax.Locator locator;

    private Map<String, Object> attributes = new HashMap<>();
    private List<SAXListener> elementLayers = new ArrayList<>();
    private SAXListener curListener;
    private StringBuilder textValue = new StringBuilder();
    private int depth = 0;
    private boolean handleWhiteSpaces = false;

    /**
     * Private constructor.
     * Initialize parser.
     */
    private SAXReader() {
    }

    /**
     * Standard constructor.
     * Initialize parser and prepare input stream for reading.
     */
    public SAXReader(
        java.io.InputStream stream) {
        this();
        inputSource = new org.xml.sax.InputSource(stream);
    }

    /**
     * Standard constructor.
     * Initialize parser and prepare input stream for reading.
     */
    public SAXReader(
        java.io.Reader reader) {
        this();
        inputSource = new org.xml.sax.InputSource(reader);
    }

    public boolean isHandleWhiteSpaces() {
        return handleWhiteSpaces;
    }

    public void setHandleWhiteSpaces(
        boolean flag) {
        handleWhiteSpaces = flag;
    }

    public org.xml.sax.Locator getLocator() {
        return locator;
    }

    /**
     * Parse input stream and handle XML tags.
     */
    public void parse(
        SAXListener listener)
        throws java.io.IOException, XMLException {
        // Initialize SAX parser
        Parser parser = acquireParser();

        // Get reader and parse using SAX2 API
        try {
            org.xml.sax.XMLReader saxReader = parser.getSAXParser().getXMLReader();
            saxReader.setErrorHandler(new ParseErrorHandler());
            saxReader.setContentHandler(this);

            curListener = listener;

            elementLayers.add(listener);

            saxReader.parse(inputSource);
        } catch (org.xml.sax.SAXParseException toCatch) {
            throw new XMLException(
                "Document parse error (line " + toCatch.getLineNumber() + ", pos " + toCatch.getColumnNumber(),
                toCatch);
        } catch (org.xml.sax.SAXException toCatch) {
            throw new XMLException(
                "Document reading SAX exception",
                XMLUtils.adaptSAXException(toCatch));
        } finally {
            parser.close();
        }
    }

    public synchronized static Parser acquireParser()
        throws XMLException {
        try {
            if (saxParserFactory == null) {
                try {
                    saxParserFactory = javax.xml.parsers.SAXParserFactory.newInstance();
                    saxParserFactory.setNamespaceAware(true);
                    saxParserFactory.setValidating(false);
                } catch (javax.xml.parsers.FactoryConfigurationError toCatch) {
                    throw new XMLException(
                        "SAX factory configuration error",
                        toCatch);
                }
            }

            for (int i = 0; i < parsersPool.size(); i++) {
                Parser parser = parsersPool.get(i);
                if (parser != null) {
                    if (!parser.isAcquired()) {
                        parser.acquire();
                        return parser;
                    }
                } else {
                    parsersPool.remove(i);
                    parser = new Parser(saxParserFactory.newSAXParser(), true);
                    parsersPool.add(parser);
                    return parser;
                }
            }
            if (parsersPool.size() == DEFAULT_POOL_SIZE) {
                throw new XMLException(
                    "Maximum SAX Parsers Number Exceeded (" + DEFAULT_POOL_SIZE + ")");
            }
            Parser parser = new Parser(saxParserFactory.newSAXParser(), true);
            parsersPool.add(parser);
            return parser;
        } catch (javax.xml.parsers.ParserConfigurationException toCatch) {
            throw new XMLException(
                "SAX Parser Configuration error",
                toCatch);
        } catch (org.xml.sax.SAXException toCatch) {
            throw new XMLException(
                "SAX Parser error",
                toCatch);
        }
    }

    /**
     * Closes parser and frees all resources.
     */
    public void close() {
        if (elementLayers != null) {
            elementLayers.clear();
            elementLayers = null;
        }
        inputSource = null;
        curListener = null;
    }

    /**
     * Set listener for next event.
     */
    public void setListener(
        SAXListener listener) {
        curListener = listener;
    }

    public boolean hasAttribute(
        String name) {
        return attributes.get(name) != null;
    }

    public Object getAttribute(
        String name) {
        return attributes.get(name);
    }

    public void setAttribute(
        String name,
        Object value) {
        attributes.put(name, value);
    }

    public Object removeAttribute(
        String name) {
        return attributes.remove(name);
    }

    private void handleText()
        throws org.xml.sax.SAXException {
        curListener = elementLayers.get(elementLayers.size() - 1);
        try {
            String value = textValue.toString();
//			String data = value.length() == 0 ? value : new String(value.getBytes(), "UTF-8");

            curListener.saxText(this, value);
        } catch (Exception toCatch) {
            throw new org.xml.sax.SAXException(toCatch);
        } finally {
            textValue.setLength(0);
        }
    }

    ///////////////////////////////////////////////////////////////
    // SAX Context Handler overrides
    ///////////////////////////////////////////////////////////////

    @Override
    public void startDocument() {
        // just do-nothing
    }

    @Override
    public void endDocument() {
        this.close();
    }

    @Override
    public void startElement(
        String namespaceURI,
        String localName,
        String qName,
        org.xml.sax.Attributes attributes)
        throws org.xml.sax.SAXException {
        if (depth++ > 0) {
            this.handleText();
        }

        curListener = elementLayers.get(elementLayers.size() - 1);

        try {
            curListener.saxStartElement(this, namespaceURI, localName, attributes);
        } catch (XMLException toCatch) {
            throw new org.xml.sax.SAXException(toCatch);
        }

        elementLayers.add(curListener);
    }

    @Override
    public void endElement(
        String namespaceURI,
        String localName,
        String qName)
        throws org.xml.sax.SAXException {
        this.handleText();

        elementLayers.remove(elementLayers.size() - 1);

        curListener = elementLayers.get(elementLayers.size() - 1);
        try {
            curListener.saxEndElement(this, namespaceURI, localName);
        } catch (XMLException toCatch) {
            throw new org.xml.sax.SAXException(toCatch);
        }
        depth--;
    }

    @Override
    public void startPrefixMapping(
        String prefix,
        String uri) {
        // just do-nothing
    }

    @Override
    public void endPrefixMapping(
        String prefix) {
        // just do-nothing
    }

    @Override
    public void characters(
        char[] ch,
        int start,
        int length) {
        textValue.append(ch, start, length);
    }

    @Override
    public void ignorableWhitespace(
        char[] ch,
        int start,
        int length) {
        if (handleWhiteSpaces) {
            textValue.append(ch, start, length);
        }
    }

    @Override
    public void processingInstruction(
        String target,
        String data) {
        // just do-nothing
    }

    @Override
    public void setDocumentLocator(
        org.xml.sax.Locator locator) {
        this.locator = locator;
    }

    @Override
    public void skippedEntity(
        String name) {
        // just do-nothing
    }

    static public class Parser {
        private javax.xml.parsers.SAXParser saxParser;
        private boolean isAcquired;

        public Parser(javax.xml.parsers.SAXParser saxParser, boolean isAcquired) {
            this.saxParser = saxParser;
            this.isAcquired = isAcquired;
        }

        public void setSAXParser(javax.xml.parsers.SAXParser saxParser) {
            this.saxParser = saxParser;
        }

        public void acquire() {
            isAcquired = true;
        }

        public void close() {
            isAcquired = false;
        }

        public javax.xml.parsers.SAXParser getSAXParser() {
            return saxParser;
        }

        public boolean isAcquired() {
            return isAcquired;
        }
    }

    static class ParseErrorHandler implements org.xml.sax.ErrorHandler {

        @Override
        public void error(org.xml.sax.SAXParseException exception) {

        }

        @Override
        public void fatalError(org.xml.sax.SAXParseException exception) {

        }

        @Override
        public void warning(org.xml.sax.SAXParseException exception) {

        }

    }


}

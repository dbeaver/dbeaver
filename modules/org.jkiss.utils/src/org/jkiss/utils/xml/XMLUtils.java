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

import org.jkiss.code.NotNull;
import org.jkiss.code.Nullable;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Common XML utils
 */
public class XMLUtils
{

	public static org.w3c.dom.Document parseDocument(String fileName)
		throws XMLException
	{
		return parseDocument(new java.io.File(fileName));
	}

	public static org.w3c.dom.Document parseDocument(java.io.File file)
		throws XMLException
	{
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder xmlBuilder = dbf.newDocumentBuilder();
			return xmlBuilder.parse(file);
		} catch (Exception er) {
			throw new XMLException("Error parsing XML document", er);
		}
	}

	public static org.w3c.dom.Document parseDocument(java.io.InputStream is)
		throws XMLException
	{
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder xmlBuilder = dbf.newDocumentBuilder();
			return xmlBuilder.parse(is);
		} catch (Exception er) {
			throw new XMLException("Error parsing XML document", er);
		}
	}

	public static org.w3c.dom.Document parseDocument(java.io.Reader is)
		throws XMLException
	{
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder xmlBuilder = dbf.newDocumentBuilder();
			return xmlBuilder.parse(new InputSource(is));
		} catch (Exception er) {
			throw new XMLException("Error parsing XML document", er);
		}
	}

	public static org.w3c.dom.Document createDocument()
		throws XMLException
	{
		try {
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder xmlBuilder = dbf.newDocumentBuilder();
			return xmlBuilder.newDocument();
		} catch (Exception er) {
			throw new XMLException("Error creating XML document", er);
		}
	}

	public static Element getChildElement(Element element,
		String childName)
	{
		for (org.w3c.dom.Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
			if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE &&
				((Element) node).getTagName().equals(childName)) {
				return (Element) node;
			}
		}
		return null;
	}

	@Nullable
	public static String getChildElementBody(Element element,
		String childName)
	{
		for (org.w3c.dom.Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
			if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE &&
				((Element) node).getTagName().equals(childName)) {
				return getElementBody((Element) node);
			}
		}
		return null;
	}

	@Nullable
	public static String getElementBody(Element element)
	{
		org.w3c.dom.Node valueNode = element.getFirstChild();
		if (valueNode == null) {
			return null;
		}
		if (valueNode.getNodeType() == org.w3c.dom.Node.TEXT_NODE) {
			return valueNode.getNodeValue();
		} else {
			return null;
		}
	}

	// Get list of all child elements of specified node
	@NotNull
	public static Collection<Element> getChildElementList(
        Element parent,
        String nodeName)
	{
		List<Element> list = new ArrayList<>();
		for (org.w3c.dom.Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
			if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE &&
				nodeName.equals(node.getNodeName()))
			{
				list.add((Element) node);
			}
		}
		return list;
	}

	// Get list of all child elements of specified node
	@NotNull
	public static Collection<Element> getChildElementListNS(
		Element parent,
		String nsURI)
	{
		List<Element> list = new ArrayList<>();
		for (org.w3c.dom.Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
			if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE &&
				node.getNamespaceURI().equals( nsURI ))
			{
				list.add((Element) node);
			}
		}
		return list;
	}

	// Get list of all child elements of specified node
	public static Collection<Element> getChildElementListNS(
		Element parent,
		String nodeName,
		String nsURI)
	{
		List<Element> list = new ArrayList<>();
		for (org.w3c.dom.Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
			if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE &&
				node.getLocalName().equals( nodeName ) &&
				node.getNamespaceURI().equals( nsURI ))
			{
				list.add((Element) node);
			}
		}
		return list;
	}

	// Get list of all child elements of specified node
	@NotNull
	public static Collection<Element> getChildElementList(
		Element parent,
		String[] nodeNameList)
	{
		List<Element> list = new ArrayList<>();
		for (org.w3c.dom.Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
			if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
				for (int i = 0; i < nodeNameList.length; i++) {
					if (node.getNodeName().equals( nodeNameList[i] )) {
						list.add((Element) node);
					}
				}
			}
		}
		return list;
	}

	// Find one child element with specified name
	@Nullable
	public static Element findChildElement(
		Element parent)
	{
		for (org.w3c.dom.Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
			if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE)
			{
				return (Element)node;
			}
		}
		return null;
	}

	public static Object escapeXml(Object obj) {
		if (obj == null) {
			return null;
		} else if (obj instanceof CharSequence) {
			return escapeXml((CharSequence)obj);
		} else {
			return obj;
		}
	}

	public static String escapeXml(CharSequence str) {
		if (str == null) {
			return null;
		}
		StringBuilder res = null;
		int strLength = str.length();
		for (int i = 0; i < strLength; i++) {
			char c = str.charAt(i);
			String repl = encodeXMLChar(c);
			if (repl == null) {
				if (res != null) {
					res.append(c);
				}
			} else {
				if (res == null) {
					res = new StringBuilder(str.length() + 5);
					for (int k = 0; k < i; k++) {
						res.append(str.charAt(k));
					}
				}
				res.append(repl);
			}
		}
		return res == null ? str.toString() : res.toString();
	}

	public static boolean isValidXMLChar(char c)
	{
		return (c >= 32 || c == '\n' || c == '\r' || c == '\t');
	}

	/**
	 * Encodes a char to XML-valid form replacing &,',",<,> with special XML encoding.
	 *
	 * @param ch char to convert
	 * @return XML-encoded text
	 */
	public static String encodeXMLChar(char ch) {
		switch (ch) {
			case '&':
				return "&amp;";
			case '\"':
				return "&quot;";
			case '\'':
				return "&#39;";
			case '<':
				return "&lt;";
			case '>':
				return "&gt;";
			default:
				return null;
		}
	}

    public static XMLException adaptSAXException(Exception toCatch)
    {
        if (toCatch instanceof XMLException) {
            return (XMLException)toCatch;
        } else if (toCatch instanceof org.xml.sax.SAXException) {
            String message = toCatch.getMessage();
            Exception embedded = ((org.xml.sax.SAXException)toCatch).getException();
            if (embedded != null && embedded.getMessage() != null && embedded.getMessage().equals(message)) {
                // Just SAX wrapper - skip it
                return adaptSAXException(embedded);
            } else {
                return new XMLException(
					message,
					embedded != null ? adaptSAXException(embedded) : null);
            }
        } else {
            return new XMLException(toCatch.getMessage(), toCatch);
        }
    }

	public static Collection<Element> getChildElementList(Element element) {
		List<Element> children = new ArrayList<>();
		for (org.w3c.dom.Node node = element.getFirstChild(); node != null; node = node.getNextSibling()) {
			if (node.getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
				children.add((Element)node);
			}
		}
		return children;
	}
}

/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.utils.xml;

/**
	SAX document listener
*/
public interface SAXListener {

	public void saxStartElement(
		SAXReader reader,
		String namespaceURI, 
		String localName, 
		org.xml.sax.Attributes atts)
		throws XMLException;

	public void saxText(
		SAXReader reader,
		String data)
		throws XMLException;

	public void saxEndElement(
		SAXReader reader,
		String namespaceURI,
		String localName)
		throws XMLException;

}

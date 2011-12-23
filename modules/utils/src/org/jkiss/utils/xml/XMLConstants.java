/*
 * Copyright (c) 2011, Serge Rieder and others. All Rights Reserved.
 */

package org.jkiss.utils.xml;

/**
	XML Constants
*/
public class XMLConstants {

	public static final String		XMLNS = "xmlns";
	public static final String		NS_XML = "http://www.w3.org/TR/REC-xml";
	public static final String		PREFIX_XML = "xml";
	public static final String		ATTR_LANG = "lang";

	public static String XML_HEADER()
	{
		return "<?xml version=\"1.0\"?>";
	}

	public static String XML_HEADER(String encoding)
	{
		return "<?xml version=\"1.0\" encoding=\"" + encoding + "\"?>";
	}

}

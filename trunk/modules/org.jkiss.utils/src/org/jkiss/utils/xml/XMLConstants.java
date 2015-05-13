/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2015 Serge Rieder (serge@jkiss.org)
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

/*
 * Copyright (C) 2010-2015 Serge Rieder
 * serge@jkiss.org
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
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

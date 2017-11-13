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

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

import org.xml.sax.Attributes;

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


    /**
     * Empty listener supposed to skip element subtrees
     */
    public static class EmptyListener implements SAXListener {

        @Override
        public void saxStartElement(SAXReader reader, String namespaceURI, String localName, Attributes atts) throws XMLException {
        }

        @Override
        public void saxText(SAXReader reader, String data) throws XMLException {
        }

        @Override
        public void saxEndElement(SAXReader reader, String namespaceURI, String localName) throws XMLException {
        }
    }
    public static final SAXListener EMPTY_LISTENER = new EmptyListener();

}

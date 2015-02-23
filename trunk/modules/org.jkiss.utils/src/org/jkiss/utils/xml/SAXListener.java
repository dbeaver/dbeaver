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

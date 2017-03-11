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

import org.xml.sax.Attributes;

/**
	SAX document listener
*/
public interface SAXListener {

	void saxStartElement(
        SAXReader reader,
        String namespaceURI,
        String localName,
        org.xml.sax.Attributes atts)
		throws XMLException;

	void saxText(
        SAXReader reader,
        String data)
		throws XMLException;

	void saxEndElement(
        SAXReader reader,
        String namespaceURI,
        String localName)
		throws XMLException;


    /**
     * Empty listener supposed to skip element subtrees
     */
    class BaseListener implements SAXListener {

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
    SAXListener EMPTY_LISTENER = new BaseListener();

}

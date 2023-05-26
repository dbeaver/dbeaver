/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2023 DBeaver Corp and others
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
package org.jkiss.dbeaver.ext.ui.tipoftheday;

import org.eclipse.core.runtime.Platform;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.jkiss.utils.xml.XMLUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TipsXmlHandler extends DefaultHandler {

    private static final String TIP = "tip";
    private final String productEdition;
    private boolean tipTagStarted;
    private boolean tipApplicable;

    private StringBuilder tipTagContent = new StringBuilder();
    private final List<String> tips = new ArrayList<>();
    private static final List<String> HTML_TAGS = Arrays.asList("br", "b", "i", "u", "q", "a", "p", "div");
    private static final String TAG_BRACKET_BEGIN = "<";
    private static final String TAG_BRACKET_END = ">";
    private static final String SLASH = "/";

    public TipsXmlHandler() {
        productEdition = Platform.getProduct().getProperty("appEdition");
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (HTML_TAGS.contains(qName) && tipTagStarted) {
            tipTagContent.append(TAG_BRACKET_BEGIN).append(qName);
            for (int i = 0; i < attributes.getLength(); i++) {
                tipTagContent.append(" ").append(attributes.getQName(i)).append("=\"").append(XMLUtils.escapeXml(attributes.getValue(i))).append("\"");
            }
            tipTagContent.append(TAG_BRACKET_END);
        }
        if (qName.equalsIgnoreCase(TIP)) {
            this.tipTagStarted = true;
            this.tipApplicable = true;
            String tipProducts = attributes.getValue("product");
            if (!CommonUtils.isEmpty(tipProducts) && !CommonUtils.isEmpty(productEdition)) {
                this.tipApplicable = ArrayUtils.contains(tipProducts.split(","), productEdition);
            }
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (tipTagStarted) {
            tipTagContent.append(new String(ch, start, length));
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equalsIgnoreCase(TIP)) {
            if (tipApplicable) {
                this.tips.add(tipTagContent.toString());
            }
            this.tipTagStarted = false;
            this.tipApplicable = true;
            tipTagContent = new StringBuilder();
        }

        if (HTML_TAGS.contains(qName) && tipTagStarted) {
            tipTagContent.append(TAG_BRACKET_BEGIN).append(SLASH).append(qName).append(TAG_BRACKET_END);
        }
    }

    public List<String> getTips() {
        return tips;
    }
}

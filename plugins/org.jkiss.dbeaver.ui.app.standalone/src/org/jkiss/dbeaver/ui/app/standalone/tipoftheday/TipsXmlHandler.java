/*
 * DBeaver - Universal Database Manager
 * Copyright (C) 2010-2025 DBeaver Corp and others
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
package org.jkiss.dbeaver.ui.app.standalone.tipoftheday;

import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.PlatformUI;
import org.jkiss.code.NotNull;
import org.jkiss.dbeaver.Log;
import org.jkiss.dbeaver.ui.ActionUtils;
import org.jkiss.utils.ArrayUtils;
import org.jkiss.utils.CommonUtils;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

public class TipsXmlHandler extends DefaultHandler {
    private static final Log log = Log.getLog(TipsXmlHandler.class);

    private static final String TIPS_XML_FILE = "tips.xml";
    private static final String TIPS_SCHEMA_XML_FILE = "tips.xsd";

    private static final String TIP = "tip";
    private static final String COMMAND_REF = "commandRef";
    private static final String COMMAND_ID = "commandId";

    private final String productEdition;
    private boolean tipApplicable;
    private Tip.Builder tipBuilder;
    private final List<Tip> tips = new ArrayList<>();

    private TipsXmlHandler() {
        this.productEdition = Platform.getProduct().getProperty("appEdition");
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        if (qName.equalsIgnoreCase(TIP)) {
            this.tipBuilder = new Tip.Builder();
            this.tipApplicable = true;
            String tipProducts = attributes.getValue("product");
            if (!CommonUtils.isEmpty(tipProducts) && !CommonUtils.isEmpty(this.productEdition)) {
                this.tipApplicable = ArrayUtils.contains(tipProducts.split(","), this.productEdition);
            }
        } else if (qName.equalsIgnoreCase(COMMAND_REF) && this.tipBuilder != null) {
            String commandId = attributes.getValue(COMMAND_ID);
            String description = ActionUtils.findCommandDescription(commandId, PlatformUI.getWorkbench(), false);
            if (!CommonUtils.isEmpty(description)) {
                this.tipBuilder.appendBoldText(description);
            } else {
                log.error("No command found by id: " + commandId + ". Consider removing obsolete tip or fixing command id.");
            }
        } else if (this.tipBuilder != null) {
            this.tipBuilder.startElement(qName, attributes.getValue("href"));
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if (this.tipBuilder != null) {
            this.tipBuilder.appendText(new String(ch, start, length));
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equalsIgnoreCase(TIP)) {
            if (this.tipApplicable) {
                tips.add(this.tipBuilder.build());
            }
            this.tipBuilder = null;
            this.tipApplicable = true;
        } else if (this.tipBuilder != null) {
            this.tipBuilder.endElement(qName);
        }
    }

    @NotNull
    public List<Tip> getTips() {
        return this.tips;
    }

    @NotNull
    public static InputStream openTipsFile() {
        return ShowTipOfTheDayHandler.class.getResourceAsStream(TIPS_XML_FILE);
    }

    @NotNull
    public static InputStream openTipsSchemaFile() {
        return ShowTipOfTheDayHandler.class.getResourceAsStream(TIPS_SCHEMA_XML_FILE);
    }

    @NotNull
    public static List<Tip> loadTips() {
        List<Tip> result = new ArrayList<>();
        try (InputStream tipsInputStream = openTipsFile()) {
            SAXParserFactory factory = SAXParserFactory.newInstance();

            SAXParser saxParser = factory.newSAXParser();

            TipsXmlHandler handler = new TipsXmlHandler();
            saxParser.parse(tipsInputStream, handler);
            result.addAll(handler.getTips());

            if (!result.isEmpty() && result.size() > 1) {
                Collections.shuffle(result);
            }
        } catch (Throwable e) {
            log.error("Error reading tips", e);
        }
        return result;
    }
}
